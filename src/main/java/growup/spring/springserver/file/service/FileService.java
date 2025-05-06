package growup.spring.springserver.file.service;

import growup.spring.springserver.file.FileType;
import growup.spring.springserver.file.TypeChangeFile;
import growup.spring.springserver.file.dto.FileResDto;
import growup.spring.springserver.file.repository.FileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class FileService {
    private final FileRepository fileRepository;

    // 파일 타입별로 동작이 달라질 가능성은 없어 보임
    // 인터페이스 추상화 굳이 ??
    public List<FileResDto> getFileHistory(FileType fileType, String email) {
        try{
            return fileRepository.findByFileTypeAndMember_Email(fileType, email)
                    .stream()
                    .map(TypeChangeFile::entityToDto)
                    .toList();
        }catch (Exception e) {
            log.warn("FileService.getFileHistory() 가져오는 중에 에러가 발생 하였습니다. {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
