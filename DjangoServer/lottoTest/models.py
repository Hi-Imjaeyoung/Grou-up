from django.db import models

class ExcelFile(models.Model):
    file = models.FileField(upload_to='excel_files/')

    class Meta:
        db_table = 'lotto'

class Lotto(models.Model):
    uploaded_at = models.DateTimeField(auto_now_add=True)

    lotto_id = models.IntegerField(null=True)  # 각 로또의 ID
    number_one = models.IntegerField()  # 첫 번째 번호
    number_two = models.IntegerField()  # 두 번째 번호
    number_three = models.IntegerField()  # 세 번째 번호
    number_four = models.IntegerField()  # 네 번째 번호
    number_five = models.IntegerField()  # 다섯 번째 번호
    number_six = models.IntegerField()  # 여섯 번째 번호

    def __str__(self):
        return str(self.lotto_id)

    class Meta:
        db_table = 'lottodata'