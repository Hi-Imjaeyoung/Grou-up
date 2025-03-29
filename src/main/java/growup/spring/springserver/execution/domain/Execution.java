package growup.spring.springserver.execution.domain;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaignoptiondetails.domain.CampaignOptionDetails;
import growup.spring.springserver.margin.domain.Margin;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@EntityListeners(AuditingEntityListener.class)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@AllArgsConstructor
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id")
    private Long id;

    private Long exeId;

    private String exeProductName;
    private String exeDetailCategory;
    private Long exeSalePrice;
    private Long exeTotalPrice;
    private Long exeCostPrice;
    private Double exePerPiece; //  1개당 마진
    private Double exeZeroRoas; // 제로 Roas
    @ManyToOne
    @JoinColumn(name = "campaignId", referencedColumnName = "campaignId")
    private Campaign campaign;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.REMOVE,fetch = FetchType.LAZY)
    @Builder.Default
    private List<CampaignOptionDetails> margins = new ArrayList<>();


    public void update(Long exeSalePrice, Long exeTotalPrice, Long exeCostPrice, Double exePerPiece, Double exeZeroRoas) {
        this.exeSalePrice =exeSalePrice;
        this.exeTotalPrice = exeTotalPrice;
        this.exeCostPrice = exeCostPrice;
        this.exePerPiece = exePerPiece;
        this.exeZeroRoas = exeZeroRoas;
    }
}