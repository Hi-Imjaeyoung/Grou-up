package growup.spring.springserver.file.domian;

import growup.spring.springserver.login.domain.Member;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Getter
@AllArgsConstructor
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //파일명
    private String fileName;

    // 파일 업로드한 날짜
    private LocalDateTime fileUploadDate;

    // 넣은 총 갯수
    private Long fileAllCount;

    // 중복 빼고 들어간 갯수
    private Long fileUniqueCount;

    @ManyToOne
    @JoinColumn(name = "email", referencedColumnName = "email")
    private Member member;


}