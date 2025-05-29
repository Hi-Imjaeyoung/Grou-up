package growup.spring.springserver.file.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileResDto {
    // 광고 리포트 (ADVERTISING_REPORT)
    private Map<LocalDate, List<FileResponseDto>> advertisingReport;


    // 순매출 리포트 (NET_SALES_REPORT)
    private Map<LocalDate, List<FileResponseDto>> netSalesReport;
}