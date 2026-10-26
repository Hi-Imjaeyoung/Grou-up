package growup.spring.springserver.exclusionKeyword.domain;

import growup.spring.springserver.campaign.domain.Campaign;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExclusionKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String exclusionKeyword;

    @CreationTimestamp
    @Column(name = "createTime")
    private LocalDateTime insDate;

    @ManyToOne
    @JoinColumn(name = "campaignId", referencedColumnName = "campaignId")
    private Campaign campaign;
}
