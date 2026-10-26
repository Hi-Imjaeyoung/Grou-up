package growup.spring.springserver.keywordBid.domain;

import growup.spring.springserver.campaign.domain.Campaign;
import jakarta.persistence.*;
import lombok.*;

@Builder
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class KeywordBid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String keyword;

    private Long bid;

    @ManyToOne
    @JoinColumn(name = "campaignId",referencedColumnName = "campaignId")
    private Campaign campaign;

    public void updateBid(Long bid){
        this.bid = bid;
    }
}
