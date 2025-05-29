package growup.spring.springserver.file.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.file.FileType;
import growup.spring.springserver.file.domian.File;
import growup.spring.springserver.file.dto.FileResDto;
import growup.spring.springserver.file.dto.FileResponseDto;
import growup.spring.springserver.file.repository.FileRepository;
import growup.spring.springserver.login.domain.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;



    @Test
    @DisplayName("getFileHistory_getFileTypeMapMap: 성공 케이스")
    void getFileHistory_getFileTypeMapMap_success() {
        Member member = getMember();
        List<File> files = List.of(
                File.builder().id(1L).member(member).fileName("file1.txt").fileUploadDate(LocalDate.of(2024,11,11)).fileType(FileType.ADVERTISING_REPORT).build(),
                File.builder().id(2L).member(member).fileName("file2.txt").fileUploadDate(LocalDate.of(2024,11,11)).fileType(FileType.NET_SALES_REPORT).build(),
                File.builder().id(1L).member(member).fileName("file3.txt").fileUploadDate(LocalDate.of(2024,11,12)).fileType(FileType.ADVERTISING_REPORT).build(),
                File.builder().id(2L).member(member).fileName("file4.txt").fileUploadDate(LocalDate.of(2024,11,13)).fileType(FileType.NET_SALES_REPORT).build(),
                File.builder().id(1L).member(member).fileName("file5.txt").fileUploadDate(LocalDate.of(2024,11,13)).fileType(FileType.ADVERTISING_REPORT).build()
        );
//        Ad = 3개 Net = 2개

        Map<FileType, Map<LocalDate, List<FileResponseDto>>> grouped =
                fileService.getFileTypeMapMap(files);

        assertThat(grouped).hasSize(2);
        assertThat(grouped.get(FileType.ADVERTISING_REPORT)).hasSize(3);
        assertThat(grouped.get(FileType.NET_SALES_REPORT)).hasSize(2);

        Map<LocalDate, List<FileResponseDto>> adv = grouped.get(FileType.ADVERTISING_REPORT);

        assertThat(adv.keySet()).containsExactlyInAnyOrder(
                LocalDate.of(2024,11,11),
                LocalDate.of(2024,11,12),
                LocalDate.of(2024,11,13)
        );
    }

    @Test
    @DisplayName("getFileHistory: All suceess")
    void getFileHistory() {
        Member member = getMember();

        when(fileRepository.findByMember_EmailAndFileUploadDateBetween(
                member.getEmail(), LocalDate.of(2024,11,11), LocalDate.of(2024,11,13)))
                .thenReturn(List.of(
                        File.builder().id(1L).member(member).fileName("file1.txt").fileUploadDate(LocalDate.of(2024,11,11)).fileType(FileType.ADVERTISING_REPORT).build(),
                        File.builder().id(2L).member(member).fileName("file2.txt").fileUploadDate(LocalDate.of(2024,11,11)).fileType(FileType.NET_SALES_REPORT).build(),
                        File.builder().id(3L).member(member).fileName("file3.txt").fileUploadDate(LocalDate.of(2024,11,12)).fileType(FileType.ADVERTISING_REPORT).build(),
                        File.builder().id(4L).member(member).fileName("file4.txt").fileUploadDate(LocalDate.of(2024,11,13)).fileType(FileType.NET_SALES_REPORT).build(),
                        File.builder().id(5L).member(member).fileName("file5.txt").fileUploadDate(LocalDate.of(2024,11,13)).fileType(FileType.ADVERTISING_REPORT).build()
                ));

        FileResDto result = fileService.getFileHistory(member.getEmail(), LocalDate.of(2024,11,11), LocalDate.of(2024,11,13));

        assertThat(result).isNotNull();
        assertThat(result.getAdvertisingReport()).hasSize(3);
        assertThat(result.getNetSalesReport()).hasSize(2);
        assertThat(result.getAdvertisingReport().keySet()).containsExactlyInAnyOrder(
                LocalDate.of(2024,11,11),
                LocalDate.of(2024,11,12),
                LocalDate.of(2024,11,13)
        );
        assertThat(result.getNetSalesReport().keySet()).containsExactlyInAnyOrder(
                LocalDate.of(2024,11,11),
                LocalDate.of(2024,11,13)
        );

    }
    public Member getMember() {
        return Member.builder()
                .email("test@test.com")
                .build();
    }
}