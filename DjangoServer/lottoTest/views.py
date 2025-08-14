import os
from io import BytesIO

import boto3
from django.utils import timezone
import json
from datetime import datetime
import time
import re
import pandas as pd
from django.shortcuts import render, get_object_or_404
from django.views.decorators.csrf import csrf_exempt
from django.db import transaction
from .models import Member, Campaign, Execution, CampaignOptionDetails, Keyword, Category, ExcelFile, KeywordDetail, \
    Margin, NetSales, MarginType, File, FileType
from .forms import ExcelFileForm
from django.http import JsonResponse
import jwt
import logging
from django.conf import settings

logger = logging.getLogger(__name__)


@csrf_exempt
def upload_excel(request):
    if request.method == 'POST':
        print("A 엑셀 업로드 시작")
        return handle_excel_upload(request)

    else:
        return JsonResponse({'status': 'error', 'message': 'Failed to upload Excel file.'}, status=500)



def extract(file_name):
    base = os.path.splitext(os.path.basename(file_name))[0]

    # 2️⃣ 정규식으로 마지막 두 개의 8자리 숫자 그룹 찾기
    m = re.search(r'_(\d{8})_(\d{8})$', base)
    if not m:
        return JsonResponse({'status': 'error', 'message': 'Failed to upload Excel file.'}, status=500)

    start_str, end_str = m.group(1), m.group(2)

    # 3️⃣ 문자열 → date 객체
    start_date = datetime.strptime(start_str, '%Y%m%d').date()
    end_date = datetime.strptime(end_str, '%Y%m%d').date()

    return start_date, end_date


import boto3
import json
import time
from io import BytesIO
import pandas as pd
from django.db import transaction
from django.shortcuts import get_object_or_404
from django.http import JsonResponse
from django.conf import settings


def handle_excel_upload(request):
    s3_client = None
    s3_key = None
    original_file_name = None

    try:
        data = json.loads(request.body)
        s3_key = data.get('key')  # S3 object key
        original_file_name = data.get('fileName')  # 원본 파일명

        if not s3_key or not original_file_name:
            return JsonResponse({'error': 'key and fileName are required'}, status=400)

        print(f"Processing S3 file: {s3_key}")
        print(f"Original file name: {original_file_name}")

        start_time = time.time() * 1000

        # AWS 설정값 가져오기 - getattr를 사용하여 안전하게 접근
        aws_access_key_id = getattr(settings, 'MY_AWS_ACCESS_KEY_ID', None)
        aws_secret_access_key = getattr(settings, 'MY_AWS_SECRET_ACCESS_KEY', None)
        aws_storage_bucket_name = getattr(settings, 'MY_AWS_STORAGE_BUCKET_NAME', None)
        aws_s3_region_name = getattr(settings, 'MY_AWS_S3_REGION_NAME', None)

        # AWS 설정값 검증
        if not all([aws_access_key_id, aws_secret_access_key, aws_storage_bucket_name, aws_s3_region_name]):
            missing_settings = []
            if not aws_access_key_id: missing_settings.append('MY_AWS_ACCESS_KEY_ID')
            if not aws_secret_access_key: missing_settings.append('MY_AWS_SECRET_ACCESS_KEY')
            if not aws_storage_bucket_name: missing_settings.append('MY_AWS_STORAGE_BUCKET_NAME')
            if not aws_s3_region_name: missing_settings.append('MY_AWS_S3_REGION_NAME')

            return JsonResponse({
                'error': f'AWS 설정이 누락되었습니다: {", ".join(missing_settings)}'
            }, status=500)

        # S3에서 파일 다운로드
        s3_client = boto3.client(
            's3',
            region_name=aws_s3_region_name,
            aws_access_key_id=aws_access_key_id,
            aws_secret_access_key=aws_secret_access_key,
        )

        # S3에서 파일 객체 가져오기
        response = s3_client.get_object(Bucket=aws_storage_bucket_name, Key=s3_key)
        file_content = response['Body'].read()

        # BytesIO를 사용해 메모리에서 엑셀 파일 읽기
        excel_buffer = BytesIO(file_content)
        excel = pd.read_excel(excel_buffer, dtype={'날짜': str})

        print(f"Excel loaded successfully. Rows: {excel.shape[0]}")

        # 파일명에서 날짜 추출
        start_date, end_date = extract(original_file_name)

        # 사용자 정보 가져오기
        email = some_protected_view(request)
        if isinstance(email, JsonResponse):  # 에러 응답인 경우
            return email

        member = get_object_or_404(Member, email=email)

        # 기존 로직 실행
        aggregated_data, keyword_data, margin_data = aggregate_data(excel, member)

        with transaction.atomic():
            save_campaign_option_details(aggregated_data)
            insert_count, duplicate_count = save_keywords(keyword_data)
            save_margin(margin_data)

        end_time = time.time() * 1000
        time_taken = end_time - start_time
        total_count = excel.shape[0]

        # 파일 정보 저장
        file_type = FileType.ADVERTISING_REPORT.value
        save_file(member, original_file_name, total_count, insert_count, duplicate_count, file_type, start_date,
                  end_date)

        # Margin 업데이트
        Margin.objects.filter(
            mar_date__range=(start_date, end_date),
            campaign__email=member
        ).update(mar_updated=True)

        print(f"Processing completed in {time_taken} ms")
        print(f"{email} 회원이 총 {total_count} 중 {insert_count} 개의 데이터가 들어 갔습니다")
        print(f"{email} 회원이 총 {total_count} 중 {duplicate_count} 개의 데이터가 겹쳐 continue 되었습니다")

        # 모든 처리가 성공적으로 완료된 경우에만 S3 파일 삭제
        try:
            s3_client.delete_object(Bucket=aws_storage_bucket_name, Key=s3_key)
            print(f"{original_file_name} 은 삭제 완료 하였습니다.")
        except Exception as delete_error:
            print(f"Warning: Failed to delete S3 file {s3_key}: {str(delete_error)}")
            # 파일 삭제 실패는 전체 프로세스의 성공에 영향을 주지 않음

        return JsonResponse({
            'status': 'success',
            'data': {
                'total_count': total_count,
                'insert_count': insert_count,
                'duplicate_count': duplicate_count,
                'processing_time': time_taken
            },
            'message': 'Excel file processed successfully from S3.'
        }, status=200)

    except Exception as e:
        print(f"Error processing S3 file: {str(e)}")
        print(f"S3 파일 {s3_key} 보존됨 (처리 실패로 인해)")

        return JsonResponse({
            'status': 'error',
            'message': f'Failed to process S3 file: {str(e)}'
        }, status=500)

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
            key_keyword = "-"
        else:
            key_keyword = str(key_keyword)

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
            'mar_return_cost' : 0,
            'mar_return_count' : 0,
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
            'impressions': 0,
            'clicks': 0,
            'click_rate': 0,
            'total_sales': 0,
            'cvr': 0,
            'cpc': 0,
            'adCost': 0,
            'adSales': 0,
            'roas': 0,
            'date': cop_date,
            'key_exclude_flag': key_exclude_flag,
            'key_search_type': cop_search_type,
            'key_product_sales': {},
        }

    keyword_data[keyword_key]['impressions'] += row['노출수']
    keyword_data[keyword_key]['clicks'] += row['클릭수']
    keyword_data[keyword_key]['adCost'] += row['광고비']
    keyword_data[keyword_key]['total_sales'] += row['총 판매수량(14일)']
    keyword_data[keyword_key]['adSales'] += row['총 전환매출액(14일)']

    # 전환발생매출ID로 keyProductSales 필드 업데이트
    if orders >= 1:
        if cv_option_id not in keyword_data[keyword_key]['key_product_sales']:
            keyword_data[keyword_key]['key_product_sales'][cv_option_id] = 0
        keyword_data[keyword_key]['key_product_sales'][cv_option_id] += orders


def save_campaign_option_details(aggregated_data):
    buffer = []
    for key, data in aggregated_data.items():
        if not CampaignOptionDetails.objects.filter(
                execution__exe_id=data['execution'].execution_id,  # exe_id를 기준으로 필터링
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
    buffer_create = []
    buffer_update = []

    # 기존 Margin 데이터 조회
    existing_margins = Margin.objects.filter(
        mar_date__in=[data['mar_date'] for data in margin_data.values()],
        campaign__in=[data['campaign'] for data in margin_data.values()]
    )

    # 기존 데이터를 (날짜, 캠페인) 키로 매핑
    existing_margins_map = {
        (margin.mar_date, margin.campaign_id): margin
        for margin in existing_margins
    }

    # 날짜 + 캠페인 기준으로 데이터 처리
    for key, data in margin_data.items():
        existing_margin = existing_margins_map.get((data['mar_date'], data['campaign'].campaign_id))
        if existing_margin:

            # 기존 Margin 업데이트
            existing_margin.mar_impressions = data['mar_impressions']
            existing_margin.mar_clicks = data['mar_clicks']
            existing_margin.mar_ad_conversion_sales = data['mar_ad_conversion_sales']
            existing_margin.mar_ad_conversion_sales_count = data['mar_ad_conversion_sales_count']
            existing_margin.mar_ad_cost = data['mar_ad_cost']
            existing_margin.mar_sales = data['mar_sales']

            buffer_update.append(existing_margin)
        else:
            # 새 Margin 생성
            buffer_create.append(Margin(
                mar_date=data['mar_date'],
                campaign=data['campaign'],
                mar_ad_budget=data['mar_ad_budget'],
                mar_impressions=data['mar_impressions'],
                mar_clicks=data['mar_clicks'],
                mar_ad_conversion_sales=data['mar_ad_conversion_sales'],
                mar_ad_conversion_sales_count=data['mar_ad_conversion_sales_count'],
                mar_ad_cost=data['mar_ad_cost'],
                mar_ad_margin=data['mar_ad_margin'],
                mar_net_profit=data['mar_net_profit'],
                mar_target_efficiency=data['mar_target_efficiency'],
                mar_actual_sales=data['mar_actual_sales'],
                mar_return_cost =data['mar_return_cost'],
                mar_return_count = data['mar_return_count'],
                mar_sales=data['mar_sales'],
            ))

    # 데이터 저장
    if buffer_create:
        Margin.objects.bulk_create(buffer_create)

    if buffer_update:
        Margin.objects.bulk_update(
            buffer_update,
            [
                'mar_ad_budget', 'mar_impressions', 'mar_clicks',
                'mar_ad_conversion_sales', 'mar_ad_conversion_sales_count',
                'mar_ad_cost', 'mar_ad_margin', 'mar_net_profit', 'mar_return_cost',
                'mar_target_efficiency', 'mar_actual_sales', 'mar_sales'
            ]
        )


def save_keywords(keyword_data):
    buffer = []
    insert_count = 0
    duplicate_count = 0
    for key, data in keyword_data.items():
        key_product_sales_json = json.dumps(data['key_product_sales'], ensure_ascii=False)  # ✅ 한글 깨짐 방지

        if not Keyword.objects.filter(
                key_keyword=data['key_keyword'],
                date=data['date'],
                campaign=data['campaign']
        ).exists():
            buffer.append(Keyword(
                key_keyword=data['key_keyword'],
                impressions=data['impressions'],
                clicks=data['clicks'],
                click_rate=round((data['clicks'] / data['impressions']) * 100, 2) if data[
                                                                                                     'impressions'] > 0 else 0,
                total_sales=data['total_sales'],
                cvr=round((data['total_sales'] / data['clicks']) * 100, 2) if data[
                                                                                              'clicks'] > 0 else 0,
                cpc=round((data['adCost'] * 1.1) / data['clicks']) if data[
                                                                                      'clicks'] > 0 else 0,
                ad_cost=round(data['adCost'] * 1.1),
                ad_sales=data['adSales'],
                roas=round(data['adSales'] / (data['adCost'] * 1.1), 2) * 100 if data[
                                                                                                 'adCost'] > 0 else 0,
                date=data['date'],
                key_exclude_flag=data['key_exclude_flag'],
                key_search_type=data['key_search_type'],
                campaign=data['campaign'],
                key_product_sales=key_product_sales_json,
            ))
            insert_count += 1
        else:
            duplicate_count += 1
    Keyword.objects.bulk_create(buffer)
    return insert_count, duplicate_count


def save_file(member, file_name,
              file_all_count, file_new_count,
              file_duplicate_count, file_type,
              start_date, end_date):
    file_record, created = File.objects.update_or_create(
        email     = member,
        file_type = file_type,
        file_name = file_name,
        defaults={
            'file_upload_date'     : timezone.localdate(),
            'file_all_count'       : file_all_count,
            'file_new_count'       : file_new_count,
            'file_duplicate_count' : file_duplicate_count,
            'file_start_date'           : start_date,
            'file_end_date'             : end_date
        }
    )
    return file_record, created
def save_file_for_netSales(member, start_date, end_date,
                        file_all_count, file_new_count,
                        file_duplicate_count, file_type, file_name):
    """
    이메일, 파일타입, 시작일, 종료일로 조회해서 update_or_create
    """
    return File.objects.update_or_create(
        email           = member,
        file_type       = file_type,
        file_start_date = start_date,
        file_end_date   = end_date,
        defaults        = {
            'file_name' : file_name,
            'file_upload_date'     : timezone.localdate(),
            'file_all_count'       : file_all_count,
            'file_new_count'       : file_new_count,
            'file_duplicate_count' : file_duplicate_count,
        }
    )

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
