package growup.spring.springserver.marginforcampaign.domain;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.global.support.Role;
import growup.spring.springserver.marginforcampaign.dto.MfcDto;
import growup.spring.springserver.marginforcampaign.support.MarginType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@EntityListeners(AuditingEntityListener.class)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@AllArgsConstructor
@Table(indexes = {
        @Index(name = "idx_product_name_email", columnList = "mfcProductName"),
})
public class MarginForCampaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mfc_id")
    private Long id;

    private String mfcProductName; // 상품명
    private Long mfcSalePrice;
    private Long mfcTotalPrice; // 총비용
    private Long mfcCostPrice; // 원가

    @Enumerated(EnumType.STRING)
    private MarginType mfcType; // 반품비

    private Long mfcReturnPrice; // 반품비
    private Long mfcPerPiece; //  1개당 마진
    private Double mfcZeroRoas; // 제로 Roas


    @ManyToOne
    @JoinColumn(name = "campaignId", referencedColumnName = "campaignId")
    private Campaign campaign;

    public void updateExistingProduct(MfcDto data) {
        this.mfcProductName = data.getMfcProductName();
        this.mfcSalePrice =data.getMfcSalePrice();
        this.mfcTotalPrice = data.getMfcTotalPrice();
        this.mfcCostPrice= data.getMfcCostPrice();
        this.mfcType = data.getMfcType();
        this.mfcReturnPrice = data.getMfcReturnPrice();
        this.mfcPerPiece = data.getMfcPerPiece();
        this.mfcZeroRoas = data.getMfcZeroRoas();
    }
}