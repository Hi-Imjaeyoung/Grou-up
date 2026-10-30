"""
① 업로드 부하 테스트용 엑셀 파일을 만든다(openpyxl 필요).
seed-data.sql 의 사용자 user01~user50 각각에 대해, 자기 캠페인 10개 × 옵션 10개 = 100행 파일을 만든다.
행 수는 하루 등록 데이터(100건 이하)에 맞췄다. 2026-10-27 측정은 옵션 50개(500행)였다.
열은 다운로드 양식과 같다: 캠패인 ID, 캠페인 명, 옵션명, 판매가, 원가, 총 비용(쿠팡), 반품비.

사용: python generate.py [캠페인당 옵션 수, 기본 10]   → 이 폴더에 upload-user01.xlsx ~ upload-user50.xlsx 생성
"""
import pathlib
import random
import sys

from openpyxl import Workbook

USERS = 50
CAMPAIGNS_PER_USER = 10  # seed-data.sql: campaign_id = (사용자 번호 - 1) × 10 + j
OPTIONS_PER_CAMPAIGN = int(sys.argv[1]) if len(sys.argv) > 1 else 10
HEADERS = ["캠패인 ID", "캠페인 명", "옵션명", "판매가", "원가", "총 비용(쿠팡)", "반품비"]

out_dir = pathlib.Path(__file__).resolve().parent
rng = random.Random(42)
for user in range(1, USERS + 1):
    number = f"{user:02d}"
    wb = Workbook()
    ws = wb.active
    ws.append(HEADERS)
    for j in range(1, CAMPAIGNS_PER_USER + 1):
        campaign_id = (user - 1) * CAMPAIGNS_PER_USER + j
        for k in range(1, OPTIONS_PER_CAMPAIGN + 1):
            sale = rng.randrange(10_000, 50_000, 100)
            ws.append([campaign_id, f"테스트캠페인{number}-{j}", f"옵션{number}-{j}-{k}",
                       sale, sale // 3, sale // 2, rng.randrange(0, 5_000, 100)])
    wb.save(out_dir / f"upload-user{number}.xlsx")
print(f"{USERS}개 파일 생성({CAMPAIGNS_PER_USER * OPTIONS_PER_CAMPAIGN}행씩): {out_dir}")
