from csv import excel
from datetime import datetime
import time

import pandas as pd
from django.shortcuts import render, get_object_or_404
from django.views.decorators.csrf import csrf_exempt
from django.db import transaction
from .models import Member, Campaign, Execution, CampaignOptionDetails, Keyword, Category, ExcelFile
from .forms import ExcelFileForm
from django.http import JsonResponse
import jwt
import logging

logger = logging.getLogger(__name__)


@csrf_exempt
def upload_excel(request):
    if request.method == 'POST':
        form = ExcelFileForm(request.POST, request.FILES)
        if form.is_valid():
            return handle_excel_upload(request, form)
    else:
        form = ExcelFileForm()
    return render(request, 'upload_excel.html', {'form': form})

@csrf_exempt
def upload_category(request):
    print("upload_category upload start")
    if request.method == 'POST':
        form = ExcelFileForm(request.POST, request.FILES)
        if form.is_valid():
            return handle_category_upload(request, form)
    else:
        form = ExcelFileForm()
    return "success"

def handle_category_upload(request, form):

    start_time = time.time() * 100
    form = ExcelFileForm(request.POST, request.FILES)

    category_excel = pd.read_excel(request.FILES['file'], skiprows=2)
    if form.is_valid():
        with transaction.atomic():
            # 중복 체크를 위한 Set 생성
            existing_categories = set(Category.objects.values_list('cat_option_id', flat=True))

            # 업데이트할 Execution 객체 리스트
            executions_to_update = []

            for index, row in category_excel.iterrows():
                cat_option_id = row["옵션 ID"]
                cat_ad_product_name = row["업체 등록 상품명"]
                cat_detail = row["등록 옵션명"]

                if pd.isna(cat_option_id):
                    continue

                # 중복 체크
                if cat_option_id not in existing_categories:
                    # 중복이 없으면 데이터 저장
                    Category.objects.create(
                        cat_option_id=cat_option_id,
                        cat_ad_product_name=cat_ad_product_name,
                        cat_detail=cat_detail
                    )
                    existing_categories.add(cat_option_id)  # 추가하여 중복 체크 유지

                try:
                    get_execution = Execution.objects.get(execution_id=cat_option_id)
                    get_execution.exe_detail_category = cat_detail
                    executions_to_update.append(get_execution)  # 업데이트할 객체를 리스트에 추가
                except Execution.DoesNotExist:
                    # 해당 execution_id가 존재하지 않으면 그냥 패스
                    continue
            # 한 번에 업데이트
            if executions_to_update:
                Execution.objects.bulk_update(executions_to_update, ['exe_detail_category'])

    end_time = time.time() * 1000
    time_taken = end_time - start_time
    print(f"time_taken :{time_taken} ")
    return render(request, 'upload_excel.html', {'form': form, 'data': end_time})

def handle_excel_upload(request, form):
    start_time = time.time() * 1000  # 밀리세컨드 단위

    email = some_protected_view(request)
    member = get_object_or_404(Member, email=email)
    logger.info(f"Member Info: Email={member.email}")

    excel = pd.read_excel(request.FILES['file'], dtype={'날짜': str})
    aggregated_data, keyword_data = aggregate_data(excel, member)

    with transaction.atomic():  # 트랜잭션 시작
        save_campaign_option_details(aggregated_data)
        save_keywords(keyword_data)

    end_time = time.time() * 1000
    time_taken = end_time - start_time
    print(f"Time taken: {time_taken} milliseconds")

    return render(request, 'upload_excel.html', {'form': form, 'data': excel.to_html()})


def aggregate_data(excel, member):
    aggregated_data = {}
    keyword_data = {}

    for index, row in excel.iterrows():
        campaign = get_or_create_campaign(row, member)
        execution = get_or_create_execution(row, campaign)

        cop_date = datetime.strptime(str(row['날짜']), '%Y%m%d').date()
        cop_search_type = row['광고 노출 지면']

        detail_key = (cop_date, execution.execution_id, cop_search_type)
        update_aggregated_data(aggregated_data, detail_key, row, execution, cop_date, cop_search_type)

        key_keyword = row['키워드']
        keyword_key = (cop_date, campaign.campaign_id, key_keyword)
        update_keyword_data(keyword_data, keyword_key, row, campaign, cop_date,key_keyword, cop_search_type)

    return aggregated_data, keyword_data


def update_aggregated_data(aggregated_data, detail_key, row, execution, cop_date, cop_search_type):
    if detail_key not in aggregated_data:
        aggregated_data[detail_key] = {
            'cop_date': cop_date,
            'execution': execution,
            'cop_impressions': 0,
            'cop_clicks': 0,
            'cop_adcost': 0,
            'cop_sales': 0,
            'cop_adsales': 0,
            'cop_roas': 0,
            'cop_cvr': 0,
            'cop_click_rate': 0,
            'cop_search_type': cop_search_type
        }
        # 집계
    aggregated_data[detail_key]['cop_impressions'] += row['노출수']
    aggregated_data[detail_key]['cop_clicks'] += row['클릭수']
    aggregated_data[detail_key]['cop_adcost'] += row['광고비']
    aggregated_data[detail_key]['cop_sales'] += row['총 판매수량(1일)']
    aggregated_data[detail_key]['cop_adsales'] += row['총 전환매출액(1일)']


def update_keyword_data(keyword_data, keyword_key, row, campaign, cop_date,key_keyword,cop_search_type):
    key_exclude_flag = row.get('제외여부', False)
    if keyword_key not in keyword_data:
        keyword_data[keyword_key] = {
            'campaign': campaign,
            'key_keyword': key_keyword,
            'key_impressions': 0,
            'key_clicks': 0,
            'key_click_rate': 0,
            'key_total_sales': 0,
            'key_cvr': 0,
            'key_cpc': 0,
            'key_adcost': 0,
            'key_adsales': 0,
            'key_roas': 0,
            'key_date': cop_date,
            'key_exclude_flag': key_exclude_flag,
            'key_search_type': cop_search_type,
        }

    keyword_data[keyword_key]['key_impressions'] += row['노출수']
    keyword_data[keyword_key]['key_clicks'] += row['클릭수']
    keyword_data[keyword_key]['key_adcost'] += row['광고비']
    keyword_data[keyword_key]['key_total_sales'] += row['총 판매수량(1일)']
    keyword_data[keyword_key]['key_adsales'] += row['총 전환매출액(1일)']


def save_campaign_option_details(aggregated_data):
    buffer = []
    for key, data in aggregated_data.items():
        if not CampaignOptionDetails.objects.filter(
                execution=data['execution'],
                cop_date=data['cop_date'],
                cop_search_type=data['cop_search_type']
        ).exists():
            buffer.append(CampaignOptionDetails(
                cop_date=data['cop_date'],
                cop_impressions=data['cop_impressions'],
                cop_sales=data['cop_sales'],
                cop_adcost=data['cop_adcost'],
                cop_adsales=data['cop_adsales'],
                cop_roas=(data['cop_adsales'] / data['cop_adcost']) * 100 if data['cop_adcost'] > 0 else 0,
                cop_clicks=data['cop_clicks'],
                cop_cvr=round((data['cop_sales'] / data['cop_clicks']) * 100, 2) if data['cop_clicks'] > 0 else 0,
                cop_click_rate=(data['cop_clicks'] / data['cop_impressions']) * 100 if data['cop_impressions'] > 0 else 0,
                cop_search_type=data['cop_search_type'],
                execution=data['execution']
            ))
    CampaignOptionDetails.objects.bulk_create(buffer)


def save_keywords(keyword_data):
    buffer = []
    for key, data in keyword_data.items():
        if not Keyword.objects.filter(
                key_keyword=data['key_keyword'],
                key_date=data['key_date'],
                campaign=data['campaign']
        ).exists():
            buffer.append(Keyword(
                key_keyword=data['key_keyword'],
                key_impressions=data['key_impressions'],
                key_clicks=data['key_clicks'],
                key_click_rate=round((data['key_clicks'] / data['key_impressions']) * 100, 2) if data[
                                                                                                     'key_impressions'] > 0 else 0,
                key_total_sales=data['key_total_sales'],
                key_cvr=round((data['key_total_sales'] / data['key_clicks']) * 100, 2) if data[
                                                                                              'key_clicks'] > 0 else 0,
                key_cpc=round((data['key_adcost'] * 1.1) / data['key_clicks']) if data[
                                                                                      'key_clicks'] > 0 else 0,
                key_adcost=round(data['key_adcost'] * 1.1),
                key_adsales=data['key_adsales'],
                key_roas=round(data['key_adsales'] / (data['key_adcost'] * 1.1), 2) * 100 if data[
                                                                                                 'key_adcost'] > 0 else 0,
                key_date=data['key_date'],
                key_exclude_flag=data['key_exclude_flag'],
                key_search_type=data['key_search_type'],
                campaign=data['campaign']

            ))
    Keyword.objects.bulk_create(buffer)


def get_or_create_campaign(row, member):
    campaign_id = row['캠페인 ID']
    campaign, created = Campaign.objects.get_or_create(
        campaign_id=campaign_id,
        defaults={
            'cam_campaign_name': row['캠페인명'],
            'cam_ad_type': row['광고유형'],
            'email': member
        }
    )
    return campaign


def get_or_create_execution(row, campaign):
    execution_id = row['광고집행 옵션ID']
    execution, created = Execution.objects.get_or_create(
        execution_id=execution_id,
        defaults={
            'exe_product_name': row['광고집행 상품명'],
            'exe_detail_category': "",
            'campaign_id': campaign,
        }
    )
    return execution


def some_protected_view(request):
    auth_header = request.headers.get('Authorization', None)
    if auth_header is None:
        return JsonResponse({'error': 'Authorization header missing'}, status=401)

    try:
        token = auth_header.split(' ')[1]
        decoded_payload = jwt.decode(token, options={"verify_signature": False})
        sub_value = decoded_payload['sub']
        email = sub_value.split(':')[0]
        return email
    except jwt.InvalidTokenError:
        return JsonResponse({'error': 'Invalid token'}, status=401)
