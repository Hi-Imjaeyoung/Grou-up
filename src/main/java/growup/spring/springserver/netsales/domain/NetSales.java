package growup.spring.springserver.netsales.domain;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.marginforcampaign.support.MarginType;
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
public class NetSales {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String netProductName;
    @Enumerated(EnumType.STRING) // Enum을 문자열로 저장
    private MarginType netType; // 타입 로켓그로스, 판매자배송
    private Long netSalesAmount; // 총 매출(원)
    private Long netSalesCount; // 순 판매수
    private Long netReturnCount; // 반품수
    private Long netCancelPrice; // 총 취소 금액
    private LocalDate netDate;
    @ManyToOne
    @JoinColumn(name = "email", referencedColumnName = "email")
    private Member member;

}