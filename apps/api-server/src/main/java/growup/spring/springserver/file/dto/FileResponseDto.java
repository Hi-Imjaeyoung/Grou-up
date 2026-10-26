package growup.spring.springserver.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileResponseDto {
    private Long id;
    private String fileName;
    private LocalDate fileUploadDate;
    private Long fileAllCount;
    private Long fileNewCount;
    private Long fileDuplicateCount;
}
