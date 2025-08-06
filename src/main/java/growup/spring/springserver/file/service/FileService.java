package growup.spring.springserver.file.service;

import growup.spring.springserver.campaign.domain.Campaign;
import growup.spring.springserver.campaign.service.CampaignService;

import growup.spring.springserver.exception.file.FileNotFoundException;
import growup.spring.springserver.file.FileType;
import growup.spring.springserver.file.TypeChangeFile;
import growup.spring.springserver.file.domian.File;
import growup.spring.springserver.file.dto.FileResDto;
import growup.spring.springserver.file.dto.FileResponseDto;
import growup.spring.springserver.file.repository.FileRepository;
import growup.spring.springserver.margin.service.MarginService;
import growup.spring.springserver.netsales.service.NetSalesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
@Slf4j
public class FileService {
    private final FileRepository fileRepository;
    private final CampaignService campaignService;
    private final MarginService marginService;
    private final NetSalesService netSalesService;

    /**
     * 파일 업로드 이력을 조회.
     *
     * @param email     회원 이메일
     * @param startDate 시작 날짜
     * @param endDate   종료 날짜
     * @return FileResDto
     */
    public FileResDto getFileHistory(String email, LocalDate startDate, LocalDate endDate) {
        List<File> advData = getByMemberEmailAndFileTypeAndFileStartDateLessThanEqualAndFileEndDateGreaterThanEqual(
                email, FileType.ADVERTISING_REPORT, startDate, endDate);

        List<File> marData = getByMemberEmailAndFileTypeAndFileStartDateLessThanEqualAndFileEndDateGreaterThanEqual(
                email, FileType.NET_SALES_REPORT, startDate, endDate);

        Map<LocalDate, FileResponseDto> localDateListMap = getNetSalesReport(marData);

        List<LocalDate> advertisingReport = getAdvertisingReport(advData);


        return FileResDto.builder()
                .advertisingReport(advertisingReport)
                .netSalesReport(localDateListMap)
                .build();
    }

    private List<File> getByMemberEmailAndFileTypeAndFileStartDateLessThanEqualAndFileEndDateGreaterThanEqual(String email, FileType type, LocalDate startDate, LocalDate endDate) {
        return fileRepository.findByMemberEmailAndFileTypeAndFileStartDateLessThanEqualAndFileEndDateGreaterThanEqual(
                email,
                type,
                endDate,
                startDate
        );
    }


    public Map<LocalDate, FileResponseDto> getNetSalesReport(List<File> marData) {
        return marData.stream()
                .collect(Collectors.toMap(
                        File::getFileStartDate,
                        TypeChangeFile::entityToDto
                ));
    }

    public List<LocalDate> getAdvertisingReport(List<File> advData) {
        HashSet<LocalDate> dateSet = new HashSet<>();

        for (File f : advData) {
            LocalDate startDate = f.getFileStartDate();
            LocalDate endDate = f.getFileEndDate();
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                dateSet.add(date);
            }
        }
        return new ArrayList<>(dateSet);
    }
    /**
     * 순매출 리포트 삭제
     *
     * @param email 회원 이메일
     * @param id    파일 ID
     * @return Map<String, Long> - 성공 메시지와 ID
     */
    public List<Map<String, Integer>> deleteNetSalesFile(String email, Long id) {
        // 1. 삭제 할 날짜 가져오기
        LocalDate date = fileDate(id);

        // 2. 마진 테이블 날짜 date 인거 실판매갯수, 반품개수, 비용, updated 가져와서 바꿔줌
        int marginDeleteCount = deleteMargin(date, email);

        // 3. netSalesReport 테이블에서 해당 파일 삭제
        int netSalesDeleteCount = netSalesService.deleteNetSalesReport(date, email);
        // 4.File 삭제
        fileRepository.deleteById(id);


        return List.of(
                Map.of("marginDeleteCount",  marginDeleteCount),
                Map.of("netSalesDeleteCount",  netSalesDeleteCount)
        );
    }


    public int deleteMargin(LocalDate date, String email) {

        // 2.1 캠페인 목록 가져오기
        List<Long> campaignList = getCampaignList(email);

        // 2) 마진 관련 MarginService에 위임
        return marginService.deleteMarginsForNetSale(date, campaignList);
    }

    public List<Long> getCampaignList(String email) {
        return campaignService.getCampaignsByEmail(email)
                .stream()
                .map(Campaign::getCampaignId)
                .toList();
    }

    public LocalDate fileDate(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(FileNotFoundException::new)
                .getFileStartDate();
    }

}
