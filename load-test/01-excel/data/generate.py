"""
① 업로드 부하 테스트용 엑셀 파일을 만든다(openpyxl 필요).
seed-data.sql 의 사용자 user01~user50 각각에 대해, 자기 캠페인 10개 × 옵션 10개 = 100행 파일을 만든다.
행 수는 하루 등록 데이터(100건 이하)에 맞췄다. 2026-10-27 측정은 옵션 50개(500행)였다.
열은 다운로드 양식과 같다: 캠패인 ID, 캠페인 명, 옵션명, 판매가, 원가, 총 비용(쿠팡), 반품비.

사용: python generate.py [캠페인당 옵션 수, 기본 10]   → 이 폴더에 upload-user01.xlsx ~ upload-user50.xlsx 생성

사용자마다 옵션명은 같고 가격만 다른 B 파일(upload-userNN-b.xlsx)도 만든다. 단계별 소요 시간 측정
(excel-upload-stage.js)에서 A·B 를 번갈아 올려 재업로드가 실제 UPDATE 가 되게 한다. 같은 파일을 다시 올리면
개선 전 구성(JPA 변경 감지)은 값이 그대로라 UPDATE 쿼리를 보내지 않는다. A 파일 값은 B 와 별개의 난수열(시드 42)이라 그대로다.
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
rng_b = random.Random(43)
for user in range(1, USERS + 1):
    number = f"{user:02d}"
    wb, wb_b = Workbook(), Workbook()
    ws, ws_b = wb.active, wb_b.active
    ws.append(HEADERS)
    ws_b.append(HEADERS)
    for j in range(1, CAMPAIGNS_PER_USER + 1):
        campaign_id = (user - 1) * CAMPAIGNS_PER_USER + j
        for k in range(1, OPTIONS_PER_CAMPAIGN + 1):
            names = [campaign_id, f"테스트캠페인{number}-{j}", f"옵션{number}-{j}-{k}"]
            sale = rng.randrange(10_000, 50_000, 100)
            ws.append(names + [sale, sale // 3, sale // 2, rng.randrange(0, 5_000, 100)])
            sale_b = rng_b.randrange(10_000, 50_000, 100)
            ws_b.append(names + [sale_b, sale_b // 3, sale_b // 2, rng_b.randrange(0, 5_000, 100)])
    wb.save(out_dir / f"upload-user{number}.xlsx")
    wb_b.save(out_dir / f"upload-user{number}-b.xlsx")
print(f"{USERS}명 × A·B 파일 생성({CAMPAIGNS_PER_USER * OPTIONS_PER_CAMPAIGN}행씩): {out_dir}")
