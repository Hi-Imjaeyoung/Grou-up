from django.db import models

class ExcelFile(models.Model):
    file = models.FileField(upload_to='excel_files/')

    class Meta:
        db_table = 'lotto'

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

class Record(models.Model):
    date = models.CharField(max_length=255)
    billing_method = models.CharField(max_length=255)
    sales_method = models.CharField(max_length=255)
    ad_type = models.CharField(max_length=255)
    campaign_id = models.CharField(max_length=255)
    campaign_name = models.CharField(max_length=255)
    ad_group = models.CharField(max_length=255)
    product_advertised = models.CharField(max_length=255)
    option_id_advertised = models.CharField(max_length=255)
    product_with_conversion_sales = models.CharField(max_length=255)
    option_id_with_conversion_sales = models.CharField(max_length=255)
    ad_placement = models.CharField(max_length=255)
    keyword = models.CharField(max_length=255)
    impressions = models.IntegerField()
    clicks = models.IntegerField()
    ad_cost = models.DecimalField(max_digits=15, decimal_places=2)
    click_through_rate = models.DecimalField(max_digits=5, decimal_places=2)
    total_orders_1_day = models.IntegerField()
    direct_orders_1_day = models.IntegerField()
    indirect_orders_1_day = models.IntegerField()
    total_sales_quantity_1_day = models.IntegerField()
    direct_sales_quantity_1_day = models.IntegerField()
    indirect_sales_quantity_1_day = models.IntegerField()
    total_conversion_revenue_1_day = models.DecimalField(max_digits=15, decimal_places=2)
    direct_conversion_revenue_1_day = models.DecimalField(max_digits=15, decimal_places=2)
    indirect_conversion_revenue_1_day = models.DecimalField(max_digits=15, decimal_places=2)
    total_orders_14_days = models.IntegerField()
    direct_orders_14_days = models.IntegerField()
    indirect_orders_14_days = models.IntegerField()
    total_sales_quantity_14_days = models.IntegerField()
    direct_sales_quantity_14_days = models.IntegerField()
    indirect_sales_quantity_14_days = models.IntegerField()
    total_conversion_revenue_14_days = models.DecimalField(max_digits=15, decimal_places=2)
    direct_conversion_revenue_14_days = models.DecimalField(max_digits=15, decimal_places=2)
    indirect_conversion_revenue_14_days = models.DecimalField(max_digits=15, decimal_places=2)
    total_ad_roi_1_day = models.DecimalField(max_digits=10, decimal_places=2)
    direct_ad_roi_1_day = models.DecimalField(max_digits=10, decimal_places=2)
    indirect_ad_roi_1_day = models.DecimalField(max_digits=10, decimal_places=2)
    total_ad_roi_14_days = models.DecimalField(max_digits=10, decimal_places=2)
    direct_ad_roi_14_days = models.DecimalField(max_digits=10, decimal_places=2)
    indirect_ad_roi_14_days = models.DecimalField(max_digits=10, decimal_places=2)
    campaign_start_date = models.CharField(max_length=255)
    member = models.ForeignKey(Member, on_delete=models.CASCADE)  # Member와의 1:N 단방향 관계 설정

    def __str__(self):
        return f"Campaign ID: {self.campaign_id}, Date: {self.date}"

    class Meta:
        db_table = 'record'

