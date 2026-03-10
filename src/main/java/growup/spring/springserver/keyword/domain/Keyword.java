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
