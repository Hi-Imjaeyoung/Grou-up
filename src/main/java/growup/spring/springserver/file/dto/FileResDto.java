package growup.spring.springserver.file.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileResDto {
    private Long id;
    private String fileName;
    private LocalDateTime fileUploadDate;
    private Long fileAllCount;
    private Long fileNewCount;
    private Long fileDuplicateCount;
}