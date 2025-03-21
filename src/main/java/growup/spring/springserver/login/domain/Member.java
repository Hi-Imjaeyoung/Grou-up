package growup.spring.springserver.login.domain;

import growup.spring.springserver.global.support.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@AllArgsConstructor
public class Member {
    @Id
    private String email;
    private String password;
    private String name;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // 남은 멤버십 기간
    @CreatedDate
    private LocalDateTime remainMembershipTime;

    // 구매 금액에 따른 role 부여
    @Enumerated(EnumType.STRING)
    private Role role;

//    Erd V2
    private Long sunshine; // 햇살

    private String recommendationCode; // 나의 추천인 코드

    private Long totalCount; // 나를 추천한 사람들의 수

    private Long monthlyReferralCount; // 이번달 나를 추천한 사람들의 수

    private String referralMember; // 내가 추천한 사람의 추천인 코드

    private Long expirationTime; // 토큰 만료시간

    public void updateMember(String myCode) {
        this.sunshine= 0L;
        this.recommendationCode=myCode;
        this.totalCount=0L;
        this.monthlyReferralCount=0L;
        this.expirationTime=60L;
    }
}