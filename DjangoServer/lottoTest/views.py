from csv import excel
from datetime import datetime
import time

import pandas as pd
from django.shortcuts import render, get_object_or_404
from django.views.decorators.csrf import csrf_exempt
from django.db import transaction
from .models import Member, Campaign, Execution, CampaignOptionDetails, Keyword, Category, ExcelFile, KeywordDetail, \
    Margin, NetSales
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
        return JsonResponse({'status': 'error', 'message': 'Failed to upload Excel file.'}, status=500)
    return JsonResponse({'status': 'success', 'message': 'Excel file uploaded successfully.'}, status=200)


@csrf_exempt
def upload_category(request):
    print("upload_category upload start")
    if request.method == 'POST':
        form = ExcelFileForm(request.POST, request.FILES)
        if form.is_valid():
            return handle_category_upload(request, form)
    else:
        return JsonResponse({'status': 'error', 'message': 'Failed to upload category file.'}, status=500)
    return JsonResponse({'status': 'success', 'message': 'Category file uploaded successfully.'}, status=200)


@csrf_exempt
def upload_margin(request):
    if request.method == 'POST':
        form = ExcelFileForm(request.POST, request.FILES)
        if form.is_valid():
            # handle_upload_margin에서 반환된 값을 그대로 반환
            return handle_upload_margin(request, form)
    else:
        return JsonResponse({'status': 'error', 'message': 'Failed to upload margin file.'}, status=500)

    return JsonResponse({'status': 'success', 'message': 'Margin file uploaded successfully.'}, status=200)


def handle_upload_margin(request, form):
    uploaded_file = request.FILES['file']

    # 1️⃣ 파일명에서 날짜 추출
    file_name = uploaded_file.name
    start_index = file_name.find('-') + 1  # '-' 다음 위치 찾기
    date_str = file_name[start_index:start_index + 8]  # 8자리 날짜 추출
    net_date = datetime.strptime(date_str, '%Y%m%d').date()  # LocalDate 변환

    # 2️⃣ 엑셀 파일 읽기
    df = pd.read_excel(uploaded_file)
    df = df.iloc[:-1]

    # 3️⃣ 옵션명 기준으로 순판매금액과 순판매수 합산
    df_grouped = df.groupby('옵션명', as_index=False).agg({
        '순 판매 금액(전체 거래 금액 - 취소 금액)': 'sum',
        '순 판매 상품 수(전체 거래 상품 수 - 취소 상품 수)': 'sum'
    })

    # 4️⃣ 로그인한 유저 가져오기
    email = some_protected_view(request)
    member = get_object_or_404(Member, email=email)

    # 5️⃣ 데이터베이스 저장 (업데이트 or 새로 생성)
    for _, row in df_grouped.iterrows():
        product_name = row['옵션명']
        total_sales_amount = row['순 판매 금액(전체 거래 금액 - 취소 금액)']
        total_sales_count = row['순 판매 상품 수(전체 거래 상품 수 - 취소 상품 수)']

        if total_sales_count == 0:
            continue

        # 같은 날짜 + 옵션명이 있는지 확인
        try:
            net_sales = NetSales.objects.get(
                net_product_name=product_name,
                net_date=net_date,
                email=member
            )
            # 이미 존재하는 경우 업데이트
            net_sales.net_sales_count = total_sales_count
            net_sales.net_sales_amount = total_sales_amount
            net_sales.save()

        except NetSales.DoesNotExist:
            # 존재하지 않으면 새로 생성
            net_sales = NetSales(
                net_product_name=product_name,
                net_sales_count=total_sales_count,
                net_sales_amount=total_sales_amount,
                net_date=net_date,
                email=member
            )
            net_sales.save()

    # 데이터를 'data'로 추가하여 render할 때 반환
    return render(request, 'upload_excel.html', {'form': form, 'data': member})

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
    aggregated_data, keyword_data, margin_data = aggregate_data(excel, member)
    with transaction.atomic():  # 트랜잭션 시작
        save_campaign_option_details(aggregated_data)
        insert_count, duplicate_count = save_keywords(keyword_data)
        save_margin(margin_data)
    end_time = time.time() * 1000
    time_taken = end_time - start_time

    total_count = excel.shape[0]
    print(f"엑셀 총 row = {total_count}")
    print(f"합쳐저서 들어간 총 데이터 = {insert_count}")
    print(f"중복 데이터 = {duplicate_count}")

    print(f"Time taken: {time_taken} milliseconds")

    return render(request, 'upload_excel.html',
                  {'form': form,
                   'data': excel.to_html(),
                   'summary': {
                       'total_count': total_count,
                       'insert_count': insert_count,
                       'duplicate_count': duplicate_count,
                   }})


def aggregate_data(excel, member):
    aggregated_data = {}
    keyword_data = {}
    margin_data = {}
    for index, row in excel.iterrows():
        campaign = get_or_create_campaign(row, member)
        execution = get_or_create_execution(row, campaign)

        cop_date = datetime.strptime(str(row['날짜']), '%Y%m%d').date()
        cop_search_type = row['광고 노출 지면']

        detail_key = (cop_date, execution.exe_id, cop_search_type)
        update_aggregated_data(aggregated_data, detail_key, row, execution, cop_date, cop_search_type)

        key_keyword = row['키워드']
        if pd.isna(key_keyword):
            key_keyword = ""
        else:
            # 공백 제거 및 띄어쓰기 없이 처리
            key_keyword = str(key_keyword).replace(" ", "")

        keyword_key = (cop_date, campaign.campaign_id, key_keyword)
        update_keyword_data(keyword_data, keyword_key, row, campaign, cop_date, key_keyword, cop_search_type)

        margin_key = (cop_date, campaign.campaign_id)
        update_margin_data(margin_data, margin_key, row, campaign, cop_date)

    return aggregated_data, keyword_data, margin_data


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
    aggregated_data[detail_key]['cop_sales'] += row['총 판매수량(14일)']
    aggregated_data[detail_key]['cop_adsales'] += row['총 전환매출액(14일)']


def update_margin_data(margin_data, margin_key, row, campaign, cop_date):
    if margin_key not in margin_data:
        margin_data[margin_key] = {
            'mar_date': cop_date,
            'mar_ad_budget': 0,
            'mar_impressions': 0,
            'mar_clicks': 0,
            'mar_ad_conversion_sales': 0,
            'mar_ad_conversion_sales_count':0,
            'mar_ad_cost': 0,
            'mar_ad_margin': 0,
            'mar_net_profit': 0,
            'mar_target_efficiency': 0,
            'mar_actual_sales': 0,
            'mar_sales': 0,
            'campaign': campaign,
        }
    margin_data[margin_key]['mar_impressions'] += row['노출수']
    margin_data[margin_key]['mar_clicks'] += row['클릭수']
    margin_data[margin_key]['mar_ad_cost'] += row['광고비']
    margin_data[margin_key]['mar_ad_conversion_sales'] += row['총 판매수량(14일)']
    margin_data[margin_key]['mar_ad_conversion_sales_count'] += row['총 주문수(14일)']
    margin_data[margin_key]['mar_sales'] += row['총 전환매출액(14일)']


def update_keyword_data(keyword_data, keyword_key, row, campaign, cop_date, key_keyword, cop_search_type):
    key_exclude_flag = row.get('제외여부', False)
    cv_option_id = row['광고전환매출발생 상품명']  
    orders = row['총 판매수량(14일)']  # 판매 수량
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
            'key_product_sales': {},
        }

    keyword_data[keyword_key]['key_impressions'] += row['노출수']
    keyword_data[keyword_key]['key_clicks'] += row['클릭수']
    keyword_data[keyword_key]['key_adcost'] += row['광고비']
    keyword_data[keyword_key]['key_total_sales'] += row['총 판매수량(14일)']
    keyword_data[keyword_key]['key_adsales'] += row['총 전환매출액(14일)']

    # 전환발생매출ID로 keyProductSales 필드 업데이트
    if orders >= 1:
        if cv_option_id not in keyword_data[keyword_key]['key_product_sales']:
            keyword_data[keyword_key]['key_product_sales'][cv_option_id] = 0
        keyword_data[keyword_key]['key_product_sales'][cv_option_id] += orders


def save_campaign_option_details(aggregated_data):
    buffer = []
    for key, data in aggregated_data.items():
        if not CampaignOptionDetails.objects.filter(
                execution__exe_id=data['execution'].exe_id,  # exe_id를 기준으로 필터링
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
                cop_click_rate=(data['cop_clicks'] / data['cop_impressions']) * 100 if data[
                                                                                           'cop_impressions'] > 0 else 0,
                cop_search_type=data['cop_search_type'],
                execution=data['execution']  # execution 객체 그대로 사용
            ))
    CampaignOptionDetails.objects.bulk_create(buffer)


def save_keyword_details(keyword_details_data):
    buffer = []
    for key, data in keyword_details_data.items():
        if not KeywordDetail.objects.filter(
                kde_date=data['kde_date'],
                kde_keyword=data['kde_keyword'],
                kde_exe_id=data['kde_exe_id'],
                campaign=data['campaign'],
        ).exists():
            buffer.append(KeywordDetail(
                kde_date=data['kde_date'],
                kde_keyword=data['kde_keyword'],
                kde_exe_id=data['kde_exe_id'],
                kde_quantity_sold=data['kde_quantity_sold'],
                kde_sales_revenue=data['kde_sales_revenue'],
                campaign=data['campaign'],  # Campaign 객체 추가
            ))
    KeywordDetail.objects.bulk_create(buffer)


def save_margin(margin_data):
    buffer = []
    # 날짜 + 캠페인
    for key, data in margin_data.items():
        if not Margin.objects.filter(
                mar_date=data['mar_date'],
                campaign=data['campaign'],
        ).exists():
            buffer.append(Margin(
                mar_date=data['mar_date'],
                campaign=data['campaign'],
                mar_ad_budget=data['mar_ad_budget'],
                mar_impressions=data['mar_impressions'],
                mar_clicks=data['mar_clicks'],
                mar_ad_conversion_sales=data['mar_ad_conversion_sales'],
                mar_ad_conversion_sales_count = data['mar_ad_conversion_sales_count'],
                mar_ad_cost=data['mar_ad_cost'],
                mar_ad_margin=data['mar_ad_margin'],
                mar_net_profit=data['mar_net_profit'],
                mar_target_efficiency=data['mar_target_efficiency'],
                mar_actual_sales=data['mar_actual_sales'],
                mar_sales = data['mar_sales'],
            ))
    Margin.objects.bulk_create(buffer)


def save_keywords(keyword_data):
    buffer = []
    insert_count = 0
    duplicate_count = 0
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
                campaign=data['campaign'],
                key_product_sales=data['key_product_sales'],
            ))
            insert_count += 1  # 들어간 값
        else:
            duplicate_count += 1  # 안 들어간 값
    Keyword.objects.bulk_create(buffer)
    return insert_count, duplicate_count


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
        exe_id=execution_id,
        campaign_id=campaign,
        defaults={
            'exe_product_name': row['광고집행 상품명'],
            'exe_detail_category': "",
            'campaign_id': campaign,
            'exe_sale_price' : 0,
            'exe_total_price' : 0,
            'exe_cost_price' : 0,
            'exe_per_piece' : 0,
            'exe_zero_roas' : 0,
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
