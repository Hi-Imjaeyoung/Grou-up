package growup.spring.springserver.marginforcampaignchangedbyperiod.domain;

import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.marginforcampaignchangedbyperiod.dto.MarginChangeSaveRequestDto;
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
public class MarginForCampaignChangedByPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 기본 키

    private LocalDate date;
    private Long salePrice;
    private Long totalPrice;
    private Long costPrice;
    private Long returnPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mfc_id", referencedColumnName = "mfc_id")
    private MarginForCampaign marginForCampaign;

    public void updateFromDto(MarginChangeSaveRequestDto dto) {
        this.salePrice = dto.salePrice();
        this.totalPrice = dto.totalPrice();
        this.costPrice = dto.costPrice();
        this.returnPrice = dto.returnPrice();
    }
}