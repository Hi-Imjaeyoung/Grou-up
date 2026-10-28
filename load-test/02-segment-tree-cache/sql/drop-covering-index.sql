-- 인덱스 없음 (Before): FK 인덱스 (campaign_id) 만 남긴다.
-- MariaDB 는 커버링 인덱스를 만들 때 같은 선두 컬럼의 FK 자동 인덱스를 지우고, FK 를 받쳐 줄 인덱스가 없으면
-- DROP 을 거부한다(ERROR 1553). 그래서 (campaign_id) 인덱스를 먼저 만든 뒤 커버링 인덱스를 지운다.
CREATE INDEX IF NOT EXISTS idx_keyword_campaign ON keyword (campaign_id);
DROP INDEX IF EXISTS idx_keyword_campaign_date_cost_sales ON keyword;

