-- ② 재측정용 더미 데이터 (MariaDB 10.11). 2026-10-27 작성.
-- 원래 데이터는 testdatagenerator 로 사용자·월 단위로 여러 번 덧붙여 만들어 분포가 기록되지 않았다.
-- k6 스크립트의 사용자 구성(Light 1~400, Heavy 401~475, Outlier 476~500)에 맞춰 다시 설계했다.
--
--   사용자 500명 × 캠페인 10개 (광고 유형 3종을 번갈아 배정)
--   기간 2025-06-01 ~ 2025-12-31 (214일)
--   캠페인당 keyword 행: Light 300, Heavy 1,200, Outlier 3,600
--   합계 400×10×300 + 75×10×1,200 + 25×10×3,600 = 3,000,000 행
--
-- 실행 전 api-server 를 bottleNeckTest 프로필로 한 번 띄워 테이블을 만든다(ddl-auto: update).
-- 실행: docker exec -i grouup-mariadb mariadb -uroot -p<비밀번호> grouup < seed-data.sql

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE keyword;
TRUNCATE TABLE campaign;
TRUNCATE TABLE member;
SET FOREIGN_KEY_CHECKS = 1;

-- 회원: user01 ~ user500 (JwtAuthFilterStub 은 토큰 "GUTokenuserNN" 을 userNN@test.com 으로 바꾼다)
INSERT INTO member (email, name, role, created_at, updated_at)
SELECT CONCAT('user', IF(seq < 10, CONCAT('0', seq), seq), '@test.com'),
       CONCAT('user', IF(seq < 10, CONCAT('0', seq), seq)),
       'USER', NOW(), NOW()
FROM seq_1_to_500;

-- 캠페인: 사용자별 10개, campaign_id = (사용자 번호 - 1) × 10 + j
INSERT INTO campaign (campaign_id, cam_ad_type, cam_campaign_name, cam_open, email)
SELECT (u.seq - 1) * 10 + j.seq,
       CASE j.seq % 3 WHEN 0 THEN '매출최적화' WHEN 1 THEN '수동 성과형' ELSE '간편 매출 스타트' END,
       CONCAT('테스트캠페인', IF(u.seq < 10, CONCAT('0', u.seq), u.seq), '-', j.seq),
       b'1',
       CONCAT('user', IF(u.seq < 10, CONCAT('0', u.seq), u.seq), '@test.com')
FROM seq_1_to_500 u
         CROSS JOIN seq_1_to_10 j;

-- keyword: 사용자 유형별 캠페인당 행 수만큼 생성. 날짜·수치는 RAND(시드)로 만든다.
INSERT INTO keyword (campaign_id, date, key_keyword, key_search_type, key_exclude_flag,
                     impressions, clicks, click_rate, total_sales, cvr, cpc, ad_cost, ad_sales, roas)
SELECT t.campaign_id,
       t.date,
       CONCAT('키워드', t.seq % 200),
       IF(t.seq % 5 = 0, '비검색', '검색'),
       b'0',
       t.impressions,
       t.clicks,
       ROUND(t.clicks / t.impressions * 100, 2),
       t.total_sales,
       ROUND(t.total_sales / GREATEST(t.clicks, 1) * 100, 2),
       ROUND(t.ad_cost / GREATEST(t.clicks, 1), 2),
       t.ad_cost,
       t.ad_sales,
       ROUND(t.ad_sales / GREATEST(t.ad_cost, 1) * 100, 2)
FROM (SELECT c.campaign_id,
             s.seq,
             DATE '2025-06-01' + INTERVAL FLOOR(RAND(1) * 214) DAY AS date,
             100 + FLOOR(RAND(2) * 9900)                          AS impressions,
             1 + FLOOR(RAND(3) * 99)                              AS clicks,
             FLOOR(RAND(4) * 10)                                  AS total_sales,
             ROUND(RAND(5) * 10000)                               AS ad_cost,
             ROUND(RAND(6) * 50000)                               AS ad_sales
      FROM campaign c
               JOIN seq_1_to_3600 s
                    ON s.seq <= CASE
                                    WHEN c.campaign_id <= 4000 THEN 300   -- Light: user01 ~ user400
                                    WHEN c.campaign_id <= 4750 THEN 1200  -- Heavy: user401 ~ user475
                                    ELSE 3600 END                         -- Outlier: user476 ~ user500
     ) t;

-- 확인
SELECT COUNT(*) AS keyword_rows, MIN(date), MAX(date) FROM keyword;
SELECT CASE
           WHEN c.campaign_id <= 4000 THEN 'Light'
           WHEN c.campaign_id <= 4750 THEN 'Heavy'
           ELSE 'Outlier' END AS user_type,
       COUNT(*)              AS keyword_rows
FROM keyword k
         JOIN campaign c ON c.campaign_id = k.campaign_id
GROUP BY user_type;
