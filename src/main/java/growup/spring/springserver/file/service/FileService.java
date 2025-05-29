package growup.spring.springserver.file.service;

import growup.spring.springserver.file.FileType;
import growup.spring.springserver.file.TypeChangeFile;
import growup.spring.springserver.file.domian.File;
import growup.spring.springserver.file.dto.FileResDto;
import growup.spring.springserver.file.dto.FileResponseDto;
import growup.spring.springserver.file.repository.FileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class FileService {
    private final FileRepository fileRepository;

    /**
     * 파일 업로드 이력을 조회.
     * @param email     회원 이메일
     * @param startDate 시작 날짜
     * @param endDate   종료 날짜
     * @return FileResDto
     */
    public FileResDto getFileHistory(String email, LocalDate startDate, LocalDate endDate) {

        List<File> all = fileRepository
                .findByMember_EmailAndFileUploadDateBetween(email, startDate, endDate);

        // FileType → (날짜→FileResponseDto) 맵
        Map<FileType, Map<LocalDate, List<FileResponseDto>>> byTypeAndDate = getFileTypeMapMap(all);

        return FileResDto.builder()
                .advertisingReport(
                        byTypeAndDate.getOrDefault(FileType.ADVERTISING_REPORT, Collections.emptyMap())
                )
                .netSalesReport(
                        byTypeAndDate.getOrDefault(FileType.NET_SALES_REPORT, Collections.emptyMap())
                )
                .build();
    }

    public Map<FileType, Map<LocalDate, List<FileResponseDto>>> getFileTypeMapMap(List<File> all) {
        return all.stream()
                .collect(Collectors.groupingBy(
                        File::getFileType,                                 // 1차 키: 파일 타입별
                        Collectors.groupingBy(
                                File::getFileUploadDate,                      // 2차 키: 업로드 날짜별
                                Collectors.mapping(
                                        TypeChangeFile::entityToDto,              // File → FileResponseDto
                                        Collectors.toList()                       // 같은 날짜면 리스트에 추가
                                )
                        )
                ));
    }
}
