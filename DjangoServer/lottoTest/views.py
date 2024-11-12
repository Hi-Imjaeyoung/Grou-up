from django.shortcuts import render, redirect
import pandas as pd
import time
from .forms import ExcelFileForm
from .models import  Lotto

from django.db import transaction


def upload_excel(request):
    if request.method == 'POST':
        form = ExcelFileForm(request.POST, request.FILES)
        if form.is_valid():
            # 엑셀 파일 읽기
            excel = pd.read_excel(request.FILES['file'])

            # 각 행을 모델 인스턴스로 변환
            start_time = time.time() * 1000  # 밀리세컨드 단위
            records = [
                Lotto(
                    lotto_id=row['num'],
                    number_one=row['one'],
                    number_two=row['two'],
                    number_three=row['three'],
                    number_four=row['four'],
                    number_five=row['five'],
                    number_six=row['six']
                )
                for index, row in excel.iterrows()
            ]
            Lotto.objects.bulk_create(records)  # 데이터를 한 번에 삽입
            total_records = Lotto.objects.count()
            print(f"total count: {total_records}")

            end_time = time.time() * 1000
            time_taken = end_time - start_time
            print(f"Time taken: {time_taken} milliseconds")
            return render(request, 'upload_excel.html', {'form': form, 'data': excel.to_html()})
    else:
        form = ExcelFileForm()
    return render(request, 'upload_excel.html', {'form': form})