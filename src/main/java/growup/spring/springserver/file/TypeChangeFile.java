package growup.spring.springserver.file;

import growup.spring.springserver.file.domian.File;
import growup.spring.springserver.file.dto.FileResDto;

public class TypeChangeFile {
    public static FileResDto entityToDto(File file) {
        return FileResDto.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .fileUploadDate(file.getFileUploadDate())
                .fileAllCount(file.getFileAllCount())
                .fileNewCount(file.getFileNewCount())
                .fileDuplicateCount(file.getFileDuplicateCount())
                .build();
    }
}
