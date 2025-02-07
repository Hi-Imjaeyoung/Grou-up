from django.db import models
from django.db.models import Model


class ExcelFile(models.Model):
    file = models.FileField(upload_to='excel_files/')

    class Meta:
        db_table = 'lotto'
# 멤버 테이블
class Member(models.Model):
    email = models.CharField(max_length=255, primary_key=True)
    created_at = models.DateTimeField(auto_now_add=True)
    name = models.CharField(max_length=255)
    password = models.CharField(max_length=255)
    remain_membership_time = models.DateTimeField(max_length=255)
    role = models.CharField(max_length=255)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        managed = False  # Django가 이 테이블을 관리하지 않도록 설정
        db_table = 'member'

class NetSales(models.Model):
    id = models.AutoField(primary_key=True)
    net_product_name = models.CharField(max_length=255) # 상품명
    net_sales_count = models.BigIntegerField() # 순 판매수
    net_sales_amount = models.BigIntegerField() # 순 판매 금액
    net_date = models.DateField()  # 날짜
    email = models.ForeignKey(Member, on_delete=models.CASCADE, db_column='email')

    def __str__(self):
        return str(self.id)
    class Meta:
        db_table = 'net_sales'
# 캠페인 테이블
class Campaign(models.Model):
    campaign_id = models.CharField(max_length=255,primary_key=True)
    cam_ad_type = models.CharField(max_length=255)
    cam_campaign_name = models.CharField(max_length=255)
    email = models.ForeignKey(Member, on_delete=models.CASCADE, db_column='email')

    def __str__(self):
        return f"Campaign ID: {self.campaign_id}, Name: {self.cam_campaign_name}"

    class Meta:
        db_table = 'campaign'
# 광고 실행 테이블
class Execution(models.Model):
    execution_id = models.AutoField(primary_key=True)  # 새로운 고유 PK
    exe_id = models.CharField(max_length=255)  # 기존 `exeId` 필드
    exe_product_name = models.CharField(max_length=255)
    exe_detail_category = models.CharField(max_length=255)
    exe_sale_price = models.CharField(max_length=255)
    exe_total_price = models.CharField(max_length=255)
    exe_cost_price = models.CharField(max_length=255)
    exe_per_piece = models.FloatField()  # 1개당 마진
    exe_zero_roas = models.FloatField()  # 제로Roas

    campaign_id = models.ForeignKey(Campaign, on_delete=models.CASCADE, db_column='campaign_id')

    def __str__(self):
        return f"Execution ID: {self.exe_id}, Product: {self.exe_product_name}"

    class Meta:
        db_table = 'execution'
# 키워드 테이블
class Keyword(models.Model):
    id = models.AutoField(primary_key=True)  # 기본 키로 설정
    key_keyword = models.CharField(max_length=255)  # 키워드
    key_impressions = models.BigIntegerField()  # 노출수
    key_clicks = models.BigIntegerField()  # 클릭수
    key_click_rate = models.FloatField()  # 클릭률
    key_total_sales = models.BigIntegerField()  # 총 주문수
    key_cvr = models.FloatField()  # 전환율
    key_cpc = models.FloatField()  # CPC
    key_adcost = models.FloatField()  # 광고비
    key_adsales = models.FloatField()  # 광고매출
    key_roas = models.FloatField()  # ROAS
    key_date = models.DateField()  # 날짜
    key_search_type = models.CharField(max_length=255)  # 검색 비검색
    key_exclude_flag = models.BooleanField(default=False)  # 제외여부

    # json 필드
    key_product_sales = models.CharField(max_length=1000)  # 상품 ID 및 판매량 저장

    campaign = models.ForeignKey(Campaign, on_delete=models.CASCADE, db_column='campaign_id')

    def __str__(self):
        return f"Keyword ID: {self.id}, Keyword: {self.key_keyword}"

    class Meta:
        db_table = 'keyword'

# 캠페인 옵션 디테일 테이블
class CampaignOptionDetails(models.Model):
    id = models.AutoField(primary_key=True)  # 기본 키로 설정
    cop_date = models.DateField()  # 날짜
    cop_impressions = models.BigIntegerField()  # 노출수
    cop_sales = models.BigIntegerField()  # 주문수
    cop_adcost = models.FloatField()  # 광고비
    cop_adsales = models.FloatField()  # 광고매출
    cop_roas = models.FloatField()  # ROAS
    cop_clicks = models.BigIntegerField()  # 클릭수
    cop_click_rate = models.FloatField()  # 클릭률
    cop_cvr = models.FloatField()  # 전환율
    cop_search_type = models.CharField(max_length=255)  # 검색 비검색



    execution = models.ForeignKey(Execution, on_delete=models.CASCADE, db_column='execution_id')

    def __str__(self):
        return f"Campaign Option Detail ID: {self.id}"
    class Meta:
        db_table = 'campaign_option_details'

# 키워드 디테일 테이블
class KeywordDetail(models.Model):
    id = models.AutoField(primary_key=True)
    kde_date = models.DateField()  # 날짜
    kde_keyword = models.CharField(max_length=255) # 키워드
    kde_exe_id = models.BigIntegerField() # 옵션 아이디
    kde_quantity_sold = models.BigIntegerField() # 판매 수량
    kde_sales_revenue = models.BigIntegerField() # 판매 매출

    campaign = models.ForeignKey(Campaign, on_delete=models.CASCADE, db_column='campaign_id')

    def __str__(self):
        return f"Keyword Detail ID: {self.id}"
    class Meta:
        db_table = 'keyword_detail'
# 메모 테이블
class Memo(models.Model):
    id = models.AutoField(primary_key=True)  # 기본 키로 설정
    mem_date = models.DateField()  # 날짜 (시간은 필요 없음)
    mem_content = models.TextField()  # 메모 내용

    campaign = models.ForeignKey(Campaign, on_delete=models.CASCADE, db_column='campaign_id')

    def __str__(self):
        return f"Memo ID: {self.id}"
    class Meta:
        db_table = 'memo'

class Margin(models.Model):
    id = models.AutoField(primary_key=True)  # 기본 키
    mar_date = models.DateField()  # 날짜
    mar_ad_budget = models.FloatField()  # 광고 예산
    mar_impressions = models.BigIntegerField()  # 노출 수
    mar_clicks = models.BigIntegerField()  # 클릭 수
    mar_ad_conversion_sales = models.BigIntegerField()  # 광고 전환 판매 수
    mar_ad_conversion_sales_count = models.BigIntegerField() # 광고 전환 주문수
    mar_ad_cost = models.BigIntegerField()  # 광고 비
    mar_sales = models.BigIntegerField()  # 엑셀기준 매출

    mar_ad_margin = models.BigIntegerField()  # 광고 마진 수
    mar_net_profit = models.FloatField()  # 순이익


    mar_target_efficiency = models.FloatField()  # 목표 효율성

    mar_actual_sales = models.BigIntegerField()  # 실제 판매 수

    campaign = models.ForeignKey(Campaign, on_delete=models.CASCADE, db_column='campaign_id')

    def __str__(self):
        return f"Margin ID: {self.id}"
    class Meta:
        db_table = 'margin'

class Category(models.Model):
    cat_option_id = models.BigIntegerField(max_length=255,primary_key=True)
    cat_ad_product_name = models.CharField(max_length=255)
    cat_detail = models.CharField(max_length=255)

    def __str__(self):
        return f"Category ID: {self.id}"
    class Meta:
        db_table = 'category'