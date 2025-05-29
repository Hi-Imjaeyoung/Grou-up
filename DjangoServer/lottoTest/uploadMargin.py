from datetime import datetime

import pandas as pd
from django.shortcuts import render, get_object_or_404
from django.views.decorators.csrf import csrf_exempt
from django.db import transaction
from .models import Member, \
    NetSales, MarginType, FileType
from .forms import ExcelFileForm
from django.http import JsonResponse
import jwt
import logging

from .views import some_protected_view, save_file

logger = logging.getLogger(__name__)

# 옵션별 집계에 사용할 컬럼명 상수
GROUP_BY_FIELDS = [
    '옵션명/Option Name/选项名称',
    '판매방식/Sales Method/销售方式',
]

AGG_COLS = {
    '총 매출(원)/Total Transaction amount(₩)/总销售额(₩)': 'sum',
    '총 판매수/Total Units Sold/购买总量': 'sum',
    '총 취소된 상품수/Total Number of Cancellation Units/取消销售数量': 'sum',
    '판매량/Units Sold/购买数量': 'sum',
    '방문자/Visitors/商品页面访问人数': 'sum',
    '조회/Page Views/商品页面浏览次数': 'sum',
    '장바구니/Add to Cart/加购数量': 'sum',
}

# 판매방식 → DB enum 매핑
TYPE_MAP = {
    '로켓그로스': MarginType.ROCKET_GROWTH.value,
    '판매자배송': MarginType.SELLER_DELIVERY.value,
}

CHUNK_SIZE = 500  # bulk 처리 청크 크기


@csrf_exempt
def upload_margin(request):
    if request.method == 'POST':
        form = ExcelFileForm(request.POST, request.FILES)
        if form.is_valid():
            return _handle_upload_margin(request, form)

    return JsonResponse({
        'status': 'error',
        'message': 'Failed to upload margin file.'
    }, status=500)


def _handle_upload_margin(request, form):
    # 1️⃣ 파일 읽고 DataFrame 준비
    uploaded = form.cleaned_data['file']
    # 1️⃣.1 날짜 추출
    file_name_date = uploaded.name.split('_')[0]

    # 파일 읽고 그룹화
    df = pd.read_excel(uploaded).iloc[:-1]
    df_grouped = df.groupby(GROUP_BY_FIELDS, as_index=False).agg(AGG_COLS)

    total_groups = len(df_grouped)

    # 2️⃣ 회원 조회
    email = some_protected_view(request)
    member = get_object_or_404(Member, email=email)

    # 3️⃣ 기존 레코드 매핑
    net_date = datetime.strptime(file_name_date, '%Y-%m-%d').date()
    existing = NetSales.objects.filter(net_date=net_date, email=member)
    exist_map = {
        (o.net_product_name, o.net_type): o
        for o in existing
    }
    to_create = []
    to_update = []
    new_count = updated_count = 0

    # 4️⃣ 반복문 돌면서 누적/생성 로직
    for _, row in df_grouped.iterrows():
        name = row['옵션명/Option Name/选项名称']
        raw_type = row['판매방식/Sales Method/销售方式']
        sold = row['총 판매수/Total Units Sold/购买总量']
        amount = row['총 매출(원)/Total Transaction amount(₩)/总销售额(₩)']
        ret = row['총 취소된 상품수/Total Number of Cancellation Units/取消销售数量']
        # units = row['판매량/Units Sold/购买数量']
        # visitors = row['방문자/Visitors/商品页面访问人数']
        # views = row['조회/Page Views/商品页面浏览次数']
        # carts = row['장바구니/Add to Cart/加购数量']
        # 모두 0건이면 skip
        if sold == 0 and ret == 0:
            continue

        net_type = TYPE_MAP.get(raw_type)
        if not net_type:
            continue
        key = (name, net_type)
        if key in exist_map:
            # 기존 레코드 덮어쓰기
            obj = exist_map[key]
            obj.net_sales_count = sold
            obj.net_sales_amount = amount
            obj.net_return_count = abs(ret)
            # obj.units_sold =  units
            # obj.visitors =  visitors
            # obj.page_views =  views
            # obj.cart_adds =  carts

            to_update.append(obj)
            updated_count += 1
        else:
            to_create.append(NetSales(
                net_product_name=name,
                net_type=net_type,
                net_sales_count=sold,
                net_sales_amount=amount,
                net_return_count=abs(ret),
                # units_sold=units,
                # visitors=visitors,
                # page_views=views,
                # cart_adds=carts,
                net_date=net_date,
                email=member,
            ))
            new_count += 1

    # 5️⃣ bulk 작업
    with transaction.atomic():
        for i in range(0, len(to_create), CHUNK_SIZE):
            NetSales.objects.bulk_create(to_create[i:i + CHUNK_SIZE])

        if to_update:
            NetSales.objects.bulk_update(
                to_update,
                [
                    'net_sales_count', 'net_sales_amount', 'net_return_count'
                    # 'units_sold', 'visitors', 'page_views', 'cart_adds'
                ]
            )

    file_record, created  = (
        save_file(member, uploaded.name, total_groups, new_count, updated_count, FileType.NET_SALES_REPORT.value)
    )

    if created:
        # 첫 등록된 파일
        message = "새 파일이 성공적으로 등록되었습니다."
    else:
        # 이미 있던 파일
        message = "새로운 파일로 덮어쓰였습니다."

    # 6️⃣ 결과 반환
    preview = df_grouped.head(10).to_dict(orient='records')
    return JsonResponse({
        'status': 'success',
        'message': message,
        'data': {
            'file_all_count': total_groups,
            'upload_new_count': new_count,
            'upload_duplicate_count': updated_count

        }
    }, status=200)
