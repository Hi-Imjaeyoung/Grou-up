package growup.spring.springserver.log.domain;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@EntityListeners(AuditingEntityListener.class)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@AllArgsConstructor
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String logContent;

    @ManyToOne
    @JoinColumn(name = "campaignId", referencedColumnName = "campaignId")
    private Campaign campaign;
}