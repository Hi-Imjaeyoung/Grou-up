package growup.spring.springserver.keyword.repository;

import growup.spring.springserver.global.config.QueryDslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class KeywordCoveringIndexTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("keyword 테이블에 캠페인·날짜 조건과 광고비·매출 집계를 덮는 인덱스가 생성된다")
    void coveringIndexExists() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.INDEX_COLUMNS " +
                        "WHERE TABLE_NAME = 'KEYWORD' AND INDEX_NAME = 'IDX_KEYWORD_CAMPAIGN_DATE_COST_SALES' " +
                        "ORDER BY ORDINAL_POSITION",
                String.class);

        assertThat(columns).containsExactly("CAMPAIGN_ID", "DATE", "AD_COST", "AD_SALES");
    }
}
