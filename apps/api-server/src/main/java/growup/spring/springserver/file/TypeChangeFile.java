package growup.spring.springserver.file;

import growup.spring.springserver.file.domian.File;
import growup.spring.springserver.file.dto.FileResponseDto;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TypeChangeFile {
    public static FileResponseDto entityToDto(File file) {
        return FileResponseDto.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .fileUploadDate(file.getFileUploadDate())
                .fileAllCount(file.getFileAllCount())
                .fileNewCount(file.getFileNewCount())
                .fileDuplicateCount(file.getFileDuplicateCount())
                .build();
    }
}
