package growup.spring.springserver.campaignoptiondetails.domain;

import growup.spring.springserver.execution.domain.Execution;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@EntityListeners(AuditingEntityListener.class)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@AllArgsConstructor
public class CampaignOptionDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate copDate;  // 날짜

    private Long copImpressions;  // 노출수

    private Long copSales;  // 주문수

    private Double copAdcost;  // 광고비

    private Double copAdsales;  // 광고매출

    private Double copRoas;  // ROAS

    private Long copClicks;  // 클릭수

    private Double copClickRate;  // 클릭률

    private Double copCvr;  // 전환율

    private String copSearchType;  // 검색 비검색

    private Long copNetSales; // 순판매수

    @ManyToOne
    @JoinColumn(name = "execution_id", referencedColumnName = "execution_id")
    private Execution execution;
}