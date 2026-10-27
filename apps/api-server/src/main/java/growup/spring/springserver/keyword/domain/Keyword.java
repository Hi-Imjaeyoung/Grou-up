package growup.spring.springserver.keyword.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import growup.spring.springserver.global.domain.CoupangExcelData;
import growup.spring.springserver.campaign.domain.Campaign;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Map;

@EntityListeners(AuditingEntityListener.class)
@Entity
// 대시보드 집계(캠페인·기간 조건, 광고비·매출 합계)를 테이블 조회 없이 인덱스만으로 처리한다.
// 원래 인덱스 DDL이 남아 있지 않아 KeywordRepositoryImpl 쿼리를 기준으로 다시 설계했다(known-issues O4).
@Table(indexes = {
        @Index(name = "idx_keyword_campaign_date_cost_sales", columnList = "campaignId, date, adCost, adSales"),
})
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Keyword extends CoupangExcelData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String keyKeyword;  // 키워드

    @Builder.Default
    private Boolean keyExcludeFlag = false;  // 제외여부

    private String keySearchType;  // 검색 비검색

//    @Type(JsonBinaryType.class) // JSON 필드 매핑
    @Type(JsonStringType.class)   // 이걸로 바꿔봐!
    @Column(columnDefinition = "json")
    private Map<String, Long> keyProductSales; // JSON 형태로 상품ID와 판매량 저장

    @ManyToOne
    @JoinColumn(name = "campaignId", referencedColumnName = "campaignId")
    private Campaign campaign;
}
