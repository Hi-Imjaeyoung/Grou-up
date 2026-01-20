package growup.spring.springserver.file.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;
import growup.spring.springserver.exception.file.FileNotFoundException;
import growup.spring.springserver.file.FileType;
import growup.spring.springserver.file.domian.File;
import growup.spring.springserver.file.dto.FileResDto;
import growup.spring.springserver.file.dto.FileResponseDto;
import growup.spring.springserver.file.repository.FileRepository;
import growup.spring.springserver.login.domain.Member;
import growup.spring.springserver.margin.repository.MarginRepository;
import growup.spring.springserver.margin.service.MarginService;
import growup.spring.springserver.netsales.service.NetSalesService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;
    @Mock
    private MarginRepository marginRepository;
    @Mock
    private MarginService marginService;
    @Mock
    private CampaignService campaignService;
    @Mock
    private NetSalesService netSalesService;

    @InjectMocks
    private FileService fileService;

    List<File> netFiles;

    @BeforeEach
    void setUp() {
        Member member = getMember();
        netFiles = List.of(
                File.builder().id(1L).member(member).fileName("file1.txt").fileStartDate(LocalDate.of(2024,11,1)).fileEndDate(LocalDate.of(2024,11,1)).fileType(FileType.NET_SALES_REPORT).build(),
                File.builder().id(2L).member(member).fileName("file2.txt").fileStartDate(LocalDate.of(2024,11,2)).fileEndDate(LocalDate.of(2024,11,2)).fileType(FileType.NET_SALES_REPORT).build(),
                File.builder().id(4L).member(member).fileName("file3.txt").fileStartDate(LocalDate.of(2024,11,5)).fileEndDate(LocalDate.of(2024,11,5)).fileType(FileType.NET_SALES_REPORT).build()

        );
    }



    @Test
    @DisplayName("mapMarDataByStartDate - successCase1.")
    void mapMarDataByStartDate_successCase1 () {
        Map<LocalDate, FileResponseDto> result = fileService.getNetSalesReport(netFiles);

        assertThat(result).hasSize(3)
                .containsKeys(
                        LocalDate.of(2024,11,1),
                        LocalDate.of(2024,11,2),
                        LocalDate.of(2024,11,5)
                );

        assertThat(result.get(LocalDate.of(2024,11,1)).getFileName())
                .isEqualTo("file1.txt");
        assertThat(result.get(LocalDate.of(2024,11,2)).getFileName())
                .isEqualTo("file2.txt");
        assertThat(result.get(LocalDate.of(2024,11,5)).getFileName())
                .isEqualTo("file3.txt");
    }



    @Test
    @DisplayName("getFileHistory - Total successCase")
    void getFileHistory() {
        LocalDate start = LocalDate.of(2024, 11, 1);
        LocalDate end = LocalDate.of(2024, 11, 30);

        when(fileRepository
                .findByMemberEmailAndFileTypeAndFileStartDateLessThanEqualAndFileEndDateGreaterThanEqual(
                        anyString(),
                        eq(FileType.NET_SALES_REPORT),
                        eq(end),
                        eq(start)
                ))
                .thenReturn(netFiles);
        when(marginRepository
                .findDistinctAdvDatesByEmailAndDateBetween(
                        anyString(),
                        eq(start),
                        eq(end)
                )).thenReturn(List.of(
                        LocalDate.of(2024,11,1),
                        LocalDate.of(2024,11,2),
                        LocalDate.of(2024,11,3)
                ));

        FileResDto result = fileService.getFileHistory(getMember().getEmail(), start, end);

        assertAll(
                () -> assertThat(result.getNetSalesReport()).hasSize(3),
                () -> assertThat(result.getNetSalesReport().get(LocalDate.of(2024,11,2)).getFileName()).isEqualTo("file2.txt"),
                () -> assertThat(result.getNetSalesReport().get(LocalDate.of(2024,11,5)).getFileName()).isEqualTo("file3.txt"),

                () -> assertThat(result.getAdvertisingReport()).hasSize(3),
                () -> assertThat(result.getAdvertisingReport()).containsExactlyInAnyOrder(
                        LocalDate.of(2024,11,1),
                        LocalDate.of(2024,11,2),
                        LocalDate.of(2024,11,3)
                )
        );

    }

    @Test
    @DisplayName("fileDate - successCase")
    void fileDate_successCase() {
        Long id = 1L;
        LocalDate expectedDate = LocalDate.of(2024, 11, 1);
        File file = getFile(1L, "file1.txt", expectedDate, FileType.NET_SALES_REPORT);

        when(fileRepository.findById(id)).thenReturn(Optional.of(file));

        LocalDate result = fileService.fileDate(id);

        // then
        assertThat(result).isEqualTo(expectedDate);
        verify(fileRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("fileDate - successCase2 ErrorCase")
    void fileDate_errorCase() {
        Long id = 1L;

        when(fileRepository.findById(id)).thenReturn(Optional.empty());

        final Exception result = assertThrows(FileNotFoundException.class,
                ()->fileService.fileDate(1L));
        //then
        assertThat(result.getMessage()).isEqualTo("해당 파일이 존재하지 않습니다.");
    }

    @Test
    @DisplayName("getCampaignList - successCase")
    void getCampaignList_successCase() {
        String email = "test@test.com";
        List<Campaign> campaigns = List.of(
                Campaign.builder().campaignId(1L).camCampaignName("Campaign 1").build(),
                Campaign.builder().campaignId(2L).camCampaignName("Campaign 2").build()
        );
        when(campaignService.getCampaignsByEmailPossibleEmpty(email)).thenReturn(campaigns);


        List<Long> result = fileService.getCampaignList(email);

        System.out.println("result = " + result);
        // then
        assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result).containsExactlyInAnyOrder(1L, 2L)
        );

    }
    @Test
    @DisplayName("deleteNetSalesFile - Total successCase")
    void deleteNetSalesFile_successCase() {
        // given
        String email = "test@test.com";
        Long id = 1L;
        LocalDate date = LocalDate.of(2024, 11, 1);

        // 1) fileRepository.findById(id) 스텁
        File file = getFile(id, "file1.txt", date, FileType.NET_SALES_REPORT);
        when(fileRepository.findById(id)).thenReturn(Optional.of(file));

        // 2) 마진 삭제
        int marginDeleteCount = 3;
        when(marginService.refactoryDeleteMarginsForNetSale(eq(date), anyList()))
                .thenReturn(marginDeleteCount);

        // 3) 순매출 삭제
        int netSalesDeleteCount = 2;
        when(netSalesService.deleteNetSalesReport(date, email))
                .thenReturn(netSalesDeleteCount);

        // when
        List<Map<String, Integer>> result = fileService.deleteNetSalesFile(email, id);

        // 호출하는지 확인
        verify(fileRepository).findById(id);
        verify(marginService).refactoryDeleteMarginsForNetSale(eq(date), anyList());
        verify(netSalesService).deleteNetSalesReport(date, email);

        System.out.println("result = " + result);

    }




    public Member getMember() {
        return Member.builder()
                .email("test@test.com")
                .build();
    }
    public File getFile(Long id, String name, LocalDate time, FileType type ) {
        return File.builder()
                .id(id)
                .fileName(name)
                .fileStartDate(time)
                .fileType(type)
                .build();
    }
}