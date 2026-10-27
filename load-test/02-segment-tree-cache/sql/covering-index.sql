-- 커버링 인덱스 Before/After 측정용 (MariaDB). 엔티티 Keyword 의 @Index 와 같은 정의다.
-- api-server 는 ddl-auto: update 라서 기동할 때 인덱스가 없으면 다시 만든다.
-- "인덱스 없음" 측정은 서버를 띄운 뒤 아래 DROP 을 실행하고 진행한다.

-- 인덱스 없음 (Before)
DROP INDEX IF EXISTS idx_keyword_campaign_date_cost_sales ON keyword;

-- 인덱스 있음 (After)
CREATE INDEX IF NOT EXISTS idx_keyword_campaign_date_cost_sales
    ON keyword (campaign_id, date, ad_cost, ad_sales);

-- 확인: Extra 에 "Using index" 가 보이면 테이블을 읽지 않고 인덱스만으로 처리된 것이다
EXPLAIN
SELECT k.date, c.cam_ad_type, SUM(k.ad_cost), SUM(k.ad_sales)
FROM keyword k
         JOIN campaign c ON c.campaign_id = k.campaign_id
         JOIN member m ON m.email = c.email
WHERE m.email = 'test@test.com'
  AND k.date BETWEEN '2025-06-01' AND '2025-12-31'
GROUP BY k.date, c.cam_ad_type;
