package growup.spring.springserver.file.domian;

import growup.spring.springserver.file.FileType;
import growup.spring.springserver.login.domain.Member;
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
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //파일명
    private String fileName;

    // 파일 업로드한 날짜
    private LocalDate fileUploadDate;

    // 엑셀파일 총 갯수
    private Long fileAllCount;

    // 새롭게 들어간 갯수
    private Long fileNewCount;

    // 기존꺼에 들어간 갯수
    private Long fileDuplicateCount;

    @Enumerated(EnumType.STRING)
    private FileType fileType; // 타입 로켓그로스, 판매자배송

    @ManyToOne
    @JoinColumn(name = "email", referencedColumnName = "email")
    private Member member;
}