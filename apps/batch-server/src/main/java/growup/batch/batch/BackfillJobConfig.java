package growup.batch.batch;



import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class BackfillJobConfig {

    private static final int CHUNK_SIZE = 2000; // 메모리 상한선 $O(2000)$ 고정

    @Bean
    public Job backfillMonthlySummaryJob(JobRepository jobRepository, Step backfillStep) {
        return new JobBuilder("backfillMonthlySummaryJob", jobRepository)
                .start(backfillStep)
                .build();
    }

    @Bean
    public Step backfillStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JdbcPagingItemReader<KeywordRawDto> reader,
            ItemProcessor<KeywordRawDto, MonthlySummaryDto> processor,
            JdbcBatchItemWriter<MonthlySummaryDto> writer
    ) {
        return new StepBuilder("backfillStep", jobRepository)
                .<KeywordRawDto, MonthlySummaryDto>chunk(CHUNK_SIZE, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<KeywordRawDto> rawDataItemReader(DataSource dataSource) throws Exception {
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.ASCENDING); // PK 기준 Keyset 정렬

        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("SELECT id, campaign_id, ad_cost, ad_sales, date");
        queryProvider.setFromClause("FROM keyword");
        queryProvider.setSortKeys(sortKeys);

        return new JdbcPagingItemReaderBuilder<KeywordRawDto>()
                .name("rawDataItemReader")
                .dataSource(dataSource)
                .pageSize(CHUNK_SIZE)
                .queryProvider(queryProvider.getObject())
                .rowMapper((rs, rowNum) -> new KeywordRawDto(
                        rs.getLong("id"),
                        rs.getLong("campaign_id"),
                        rs.getDouble("ad_cost"),
                        rs.getDouble("ad_sales"),
                        rs.getDate("date").toLocalDate()
                ))
                .build();
    }

    /**
     * [Processor] Raw Data -> Monthly Summary 데이터 변환
     */
    @Bean
    public ItemProcessor<KeywordRawDto, MonthlySummaryDto> rawDataProcessor() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-01");
        return raw -> new MonthlySummaryDto(
                raw.campaign_id(),
                raw.ad_cost(),
                raw.ad_sale(),
                raw.date().format(formatter)
        );
    }

    /**
     * [Writer] JDBC Bulk Write & Native UPSERT
     * JPA를 배제하고 JDBC Batch API를 직접 호출하며,
     * ON DUPLICATE KEY UPDATE를 통해 1번의 I/O로 Atomic 갱신 및 멱등성을 보장합니다.
     */
    @Bean
    public JdbcBatchItemWriter<MonthlySummaryDto> monthlySummaryItemWriter(DataSource dataSource) {
        String sql = """
            INSERT INTO monthly_summary (campaign_id, summary_date, total_cost, total_sale)
            VALUES (:campaign_id, :year_month, :ad_cost, :ad_sale)
            ON DUPLICATE KEY UPDATE
                total_cost = total_cost + VALUES(total_cost),
                total_sale = total_sale + VALUES(total_sale);
            """;

        return new JdbcBatchItemWriterBuilder<MonthlySummaryDto>()
                .dataSource(dataSource)
                .sql(sql)
                .beanMapped() // DTO 필드명을 SQL 파라미터(:merchantId 등)에 자동 매핑
                .build();
    }
}