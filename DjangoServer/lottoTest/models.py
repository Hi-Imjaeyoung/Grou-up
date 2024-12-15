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
# 광고 실행 ㅌ[ㅇ;ㅂ,ㄹ
class Execution(models.Model):
    execution_id = models.CharField(max_length=255, primary_key=True)
    exe_product_name = models.CharField(max_length=255)
    exe_detail_category = models.CharField(max_length=255)
    campaign_id = models.ForeignKey(Campaign, on_delete=models.CASCADE, db_column='campaign_id')

    def __str__(self):
        return f"Execution ID: {self.execution_id}, Product: {self.exe_product_name}"

    class Meta:
        db_table = 'execution'
# 키워드 테이블
class Keyword(models.Model):
    keyword_id = models.AutoField(primary_key=True)  # 기본 키로 설정
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
    key_exclude_flag = models.BooleanField(default=False)  # 제외여부

    campaign = models.ForeignKey(Campaign, on_delete=models.CASCADE, db_column='campaignId')

    def __str__(self):
        return f"Keyword ID: {self.keyword_id}, Keyword: {self.key_keyword}"

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

    execution = models.ForeignKey(Execution, on_delete=models.CASCADE, db_column='executionId')

    def __str__(self):
        return f"Campaign Option Detail ID: {self.id}"
    class Meta:
        db_table = 'campaign_option_details'

# 메모 테이블
class Memo(models.Model):
    id = models.AutoField(primary_key=True)  # 기본 키로 설정
    mem_date = models.DateField()  # 날짜 (시간은 필요 없음)
    mem_content = models.TextField()  # 메모 내용

    campaign = models.ForeignKey(Campaign, on_delete=models.CASCADE, db_column='campaignId')

    def __str__(self):
        return f"Memo ID: {self.id}"
    class Meta:
        db_table = 'memo'

class Margin(models.Model):
    id = models.AutoField(primary_key=True)  # 기본 키
    mar_campaign_id = models.CharField(max_length=255)  # 캠페인 ID
    mar_date = models.DateField()  # 날짜
    mar_target_efficiency = models.FloatField()  # 목표 효율성
    mar_ad_revenue = models.FloatField()  # 광고 수익
    mar_ad_budget = models.FloatField()  # 광고 예산
    mar_ad_cost_ratio = models.FloatField()  # 광고비 비율
    mar_cpc_unit_cost = models.FloatField()  # CPC 단가
    mar_impressions = models.BigIntegerField()  # 노출 수
    mar_clicks = models.BigIntegerField()  # 클릭 수
    mar_ad_conversion_sales = models.BigIntegerField()  # 광고 전환 판매 수
    mar_actual_sales = models.BigIntegerField()  # 실제 판매 수
    mar_ad_margin = models.BigIntegerField()  # 광고 머지 수
    mar_net_profit = models.FloatField()  # 순이익
    def __str__(self):
        return f"Margin ID: {self.id}"
    class Meta:
        db_table = 'margin'

