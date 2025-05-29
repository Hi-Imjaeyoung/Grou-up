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
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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

        fileRepository.save(newFile("test.txt", member, FileType.ADVERTISING_REPORT, LocalDate.of(2023, 10, 1)));
        fileRepository.save(newFile("test3.txt", member, FileType.ADVERTISING_REPORT, LocalDate.of(2023, 10, 2)));
        fileRepository.save(newFile("test2.txt", member, FileType.ADVERTISING_REPORT, LocalDate.of(2022, 10, 3)));

        fileRepository.save(newFile("test4.txt", member2, FileType.ADVERTISING_REPORT, LocalDate.of(2023, 10, 4)));
        fileRepository.save(newFile("test5.txt", member2, FileType.ADVERTISING_REPORT, LocalDate.of(2023, 10, 5)));
        fileRepository.save(newFile("test6.txt", member2, FileType.ADVERTISING_REPORT, LocalDate.of(2023, 10, 6)));

    }

    @Test
    @DisplayName("findByFileTypeAndMember_Email(): Success case")
    void test1() {
        List<File> result = fileRepository.findByMember_EmailAndFileUploadDateBetween("fa7271@naver.com",LocalDate.of(2023,10,1),LocalDate.of(2023,11,29));
        System.out.println("result = " + Arrays.toString(result.toArray()));
        assertThat(result).hasSize(2);

        assertAll(
                () -> assertThat(result.get(0).getFileName()).isEqualTo("test.txt"),
                () -> assertThat(result.get(1).getFileName()).isEqualTo("test3.txt")
        );
    }

    @Test
    @DisplayName("findByFileTypeAndMember_Email(): Failcase 1. 없는 아이디")
    void test2() {
        List<File> result = fileRepository.findByMember_EmailAndFileUploadDateBetween("nonexistent@naver.com", LocalDate.of(2023, 10, 1), LocalDate.of(2023, 11, 29));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByFileTypeAndMember_Email(): Failcase 1. 데이터 없음")
    void test3() {
        List<File> result = fileRepository.findByMember_EmailAndFileUploadDateBetween("fa7271@naver.com", LocalDate.of(2024, 10, 1), LocalDate.of(2024, 11, 29));

        assertThat(result).isEmpty();
    }

    private Member newMember(String email) {
        return Member.builder().email(email).build();
    }

    private File newFile(String fileName, Member member, FileType fileType, LocalDate localDate) {
        return File.builder()
                .fileName(fileName)
                .fileType(fileType)
                .fileUploadDate(localDate)
                .member(member)
                .build();
    }
}