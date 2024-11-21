import pandas as pd
import time
from decimal import Decimal
from django.shortcuts import render
from django.views.decorators.csrf import csrf_exempt
from django.shortcuts import get_object_or_404

from .models import Record, Member
from .forms import ExcelFileForm

# 토큰 파싱
from django.http import JsonResponse
import jwt


@csrf_exempt
def upload_excel(request):
    if request.method == 'POST':
        form = ExcelFileForm(request.POST, request.FILES)
        if form.is_valid():

            email = some_protected_view(request)
            member = get_object_or_404(Member, email=email)
            print(
                f"Member Info: Email={member.email}, Name={member.name}, Role={member.role}, CreatedAt={member.created_at}, UpdatedAt={member.updated_at}")

            excel = pd.read_excel(request.FILES['file'])

            # 각 행을 모델 인스턴스로 변환
            start_time = time.time() * 1000  # 밀리세컨드 단위
            records = []

            for index, row in excel.iterrows():
                # %가 포함된 문자열을 Decimal로 변환
                def convert_percentage(value):
                    if isinstance(value, str) and '%' in value:
                        return Decimal(value.replace('%', '').strip()) / 100
                    return Decimal(value)

                record = Record(
                    date=row['날짜'],
                    billing_method=row['과금방식'],
                    sales_method=row['판매방식'],
                    ad_type=row['광고유형'],
                    campaign_id=row['캠페인 ID'],
                    campaign_name=row['캠페인명'],
                    ad_group=row['광고그룹'],
                    product_advertised=row['광고집행 상품명'],
                    option_id_advertised=row['광고집행 옵션ID'],
                    product_with_conversion_sales=row['광고전환매출발생 상품명'],
                    option_id_with_conversion_sales=row['광고전환매출발생 옵션ID'],
                    ad_placement=row['광고 노출 지면'],
                    keyword=row['키워드'],
                    impressions=row['노출수'],
                    clicks=row['클릭수'],
                    ad_cost=Decimal(row['광고비']),
                    click_through_rate=convert_percentage(row['클릭률']),
                    total_orders_1_day=row['총 주문수(1일)'],
                    direct_orders_1_day=row['직접 주문수(1일)'],
                    indirect_orders_1_day=row['간접 주문수(1일)'],
                    total_sales_quantity_1_day=row['총 판매수량(1일)'],
                    direct_sales_quantity_1_day=row['직접 판매수량(1일)'],
                    indirect_sales_quantity_1_day=row['간접 판매수량(1일)'],
                    total_conversion_revenue_1_day=Decimal(row['총 전환매출액(1일)']),
                    direct_conversion_revenue_1_day=Decimal(row['직접 전환매출액(1일)']),
                    indirect_conversion_revenue_1_day=Decimal(row['간접 전환매출액(1일)']),
                    total_orders_14_days=row['총 주문수(14일)'],
                    direct_orders_14_days=row['직접주문수(14일)'],
                    indirect_orders_14_days=row['간접 주문수(14일)'],
                    total_sales_quantity_14_days=row['총 판매수량(14일)'],
                    direct_sales_quantity_14_days=row['직접 판매수량(14일)'],
                    indirect_sales_quantity_14_days=row['간접 판매수량(14일)'],
                    total_conversion_revenue_14_days=Decimal(row['총 전환매출액(14일)']),
                    direct_conversion_revenue_14_days=Decimal(row['직접 전환매출액(14일)']),
                    indirect_conversion_revenue_14_days=Decimal(row['간접 전환매출액(14일)']),
                    total_ad_roi_1_day=convert_percentage(row['총광고수익률(1일)']),
                    direct_ad_roi_1_day=convert_percentage(row['직접광고수익률(1일)']),
                    indirect_ad_roi_1_day=convert_percentage(row['간접광고수익률(1일)']),
                    total_ad_roi_14_days=convert_percentage(row['총광고수익률(14일)']),
                    direct_ad_roi_14_days=convert_percentage(row['직접광고수익률(14일)']),
                    indirect_ad_roi_14_days=convert_percentage(row['간접광고수익률(14일)']),
                    campaign_start_date=row['캠페인 시작일'],
                    member = member

                )

                records.append(record)

            Record.objects.bulk_create(records)  # 데이터를 한 번에 삽입
            total_records = Record.objects.count()
            print(f"total count: {total_records}")

            end_time = time.time() * 1000
            time_taken = end_time - start_time
            print(f"Time taken: {time_taken} milliseconds")
            return render(request, 'upload_excel.html', {'form': form, 'data': excel.to_html()})
    else:
        form = ExcelFileForm()
    return render(request, 'upload_excel.html', {'form': form})


def some_protected_view(request):
    auth_header = request.headers.get('Authorization', None)

    if auth_header is None:
        return JsonResponse({'error': 'Authorization header missing'}, status=401)

    try:
        # Bearer <token> 형태이므로 "Bearer " 부분을 제거
        token = auth_header.split(' ')[1]

        # 토큰 해석 (서명 검증 없이)
        decoded_payload = jwt.decode(token, options={"verify_signature": False})

        # 디코딩된 토큰 내용 출력
        print(f"Token decoded: {decoded_payload}")

        # 'sub'에서 이메일만 추출
        sub_value = decoded_payload['sub']
        email = sub_value.split(':')[0]

        return email
    except jwt.InvalidTokenError:
        return JsonResponse({'error': 'Invalid token'}, status=401)
