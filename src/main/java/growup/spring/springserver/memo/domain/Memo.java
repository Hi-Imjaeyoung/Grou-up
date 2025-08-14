package growup.spring.springserver.memo.domain;

import growup.spring.springserver.campaign.domain.Campaign;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@EntityListeners(AuditingEntityListener.class)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@AllArgsConstructor
public class Memo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private LocalDate date;

    @Lob // 1. @Lob 어노테이션을 추가해 "이건 큰 데이터야!" 라고 알려준다.
    @Column(columnDefinition = "TEXT") // 2. (선택사항) TEXT 타입임을 명시적으로 지정
    private String contents;

    @ManyToOne
    @JoinColumn(name = "campaignId", referencedColumnName = "campaignId")
    private Campaign campaign;

    public void updateContents(String contents){
        this.contents = contents;
//        this.date = LocalDate.now();
    }

    public String getDate(){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        return this.date.format(formatter);
    }

    public String getRawDate(){
        return this.date.toString();
    }
}
