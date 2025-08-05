package growup.spring.springserver.campaign.domain;

import growup.spring.springserver.exclusionKeyword.domain.ExclusionKeyword;
import growup.spring.springserver.execution.domain.Execution;
import growup.spring.springserver.keyword.domain.Keyword;
import growup.spring.springserver.keywordBid.domain.KeywordBid;
import growup.spring.springserver.log.domain.Log;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.margin.domain.Margin;
import growup.spring.springserver.marginforcampaign.domain.MarginForCampaign;
import growup.spring.springserver.memo.domain.Memo;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import lombok.*;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@EntityListeners(AuditingEntityListener.class)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@AllArgsConstructor
public class Campaign {

    @Id
    private Long campaignId;

    private String camCampaignName;

    private String camAdType;

    @Builder.Default
    private Boolean camOpen = false;

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.REMOVE,fetch = FetchType.LAZY)
    private List<Keyword> keywordList = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.REMOVE,fetch = FetchType.LAZY)
    private List<ExclusionKeyword> exclusionKeywords = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.REMOVE,fetch = FetchType.LAZY)
    private List<KeywordBid> keywordBids = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.REMOVE,fetch = FetchType.LAZY)
    private List<Memo> memos = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.REMOVE,fetch = FetchType.LAZY)
    private List<MarginForCampaign> marginForCampaigns = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.REMOVE,fetch = FetchType.LAZY)
    private List<Execution> executions= new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.REMOVE,fetch = FetchType.LAZY)
    private List<Log> logs = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.REMOVE,fetch = FetchType.LAZY)
    private List<Margin> margins= new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "email", referencedColumnName = "email")
    private Member member;

}