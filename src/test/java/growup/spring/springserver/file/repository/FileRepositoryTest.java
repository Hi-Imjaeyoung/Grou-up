package growup.spring.springserver.file.repository;

import growup.spring.springserver.file.FileType;
import growup.spring.springserver.file.domian.File;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.login.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
class FileRepositoryTest {
    @Autowired
    private FileRepository fileRepository;
    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setup() {
        Member member = memberRepository.save(newMember("fa7271@naver.com"));
        Member member2 = memberRepository.save(newMember("windy7271@naver.com"));

        fileRepository.save(newFile("test4.txt", member2, FileType.ADVERTISING_REPORT, LocalDate.of(2023, 10, 4),LocalDate.of(2023, 10, 20)));
        fileRepository.save(newFile("test5.txt", member2, FileType.ADVERTISING_REPORT, LocalDate.of(2023, 10, 5),LocalDate.of(2023, 10, 5)));
        fileRepository.save(newFile("test6.txt", member2, FileType.ADVERTISING_REPORT, LocalDate.of(2023, 10, 6),LocalDate.of(2023, 10, 6)));

        fileRepository.save(newFile("test7.txt", member2, FileType.ADVERTISING_REPORT, LocalDate.of(2023, 10, 1),LocalDate.of(2023, 10,5)));
        fileRepository.save(newFile("test8.txt", member2, FileType.NET_SALES_REPORT, LocalDate.of(2023, 10, 2),LocalDate.of(2023, 10, 2)));
        fileRepository.save(newFile("test9.txt", member2, FileType.NET_SALES_REPORT, LocalDate.of(2023, 10, 6),LocalDate.of(2023, 10, 6)));

    }

    @Test
    @DisplayName("findByFileUploadDateBetween - successCase1")
    void findByFileUploadDateBetween_success() {
        LocalDate startDate = LocalDate.of(2023, 10, 3);
        LocalDate endDate = LocalDate.of(2023, 10, 5);

        List<File> result = fileRepository.findByMemberEmailAndFileTypeAndFileStartDateLessThanEqualAndFileEndDateGreaterThanEqual(
                "windy7271@naver.com",
                FileType.ADVERTISING_REPORT,
                endDate,
                startDate
        );

        assertThat(result)
                .hasSize(3)
                .allSatisfy(file -> {
                    assertThat(file.getFileStartDate())
                            .isBeforeOrEqualTo(endDate);
                    assertThat(file.getFileEndDate())
                            .isAfterOrEqualTo(startDate);
                });
    }


    private Member newMember(String email) {
        return Member.builder().email(email).build();
    }

    private File newFile(String fileName, Member member, FileType fileType, LocalDate startDate, LocalDate endDate) {
        return File.builder()
                .fileName(fileName)
                .fileType(fileType)
                .fileStartDate(startDate)
                .fileEndDate(endDate)
                .member(member)
                .build();
    }
}