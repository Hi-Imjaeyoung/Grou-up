package growup.spring.springserver.file.service;

import growup.spring.springserver.file.FileType;
import growup.spring.springserver.file.domian.File;
import growup.spring.springserver.file.dto.FileResDto;
import growup.spring.springserver.file.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @InjectMocks
    private FileService fileService;



    @Test
    void getFileHistory_Success() {
        // given
        String email = "test@example.com";
        FileType fileType = FileType.ADVERTISING_REPORT;
        File mockFile = new File(1L, "testFile", LocalDateTime.now(), 10L, 5L, 2L, fileType, null);

        when(fileRepository.findByFileTypeAndMember_Email(fileType, email))
                .thenReturn(List.of(mockFile));

        // when
        List<FileResDto> result = fileService.getFileHistory(fileType, email);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testFile", result.get(0).getFileName());
        verify(fileRepository, times(1)).findByFileTypeAndMember_Email(fileType, email);
    }

    @Test
    void getFileHistory_EmptyResult() {
        // given
        String email = "test@example.com";
        FileType fileType = FileType.ADVERTISING_REPORT;
        when(fileRepository.findByFileTypeAndMember_Email(fileType, email))
                .thenReturn(Collections.emptyList());

        // when
        List<FileResDto> result = fileService.getFileHistory(fileType, email);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(fileRepository, times(1)).findByFileTypeAndMember_Email(fileType, email);
    }

    @Test
    void getFileHistory_Exception() {
        // given
        String email = "test@example.com";
        FileType fileType = FileType.ADVERTISING_REPORT;

        when(fileRepository.findByFileTypeAndMember_Email(fileType, email))
                .thenThrow(new RuntimeException("Database error"));

        // when
        List<FileResDto> result = fileService.getFileHistory(fileType, email);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(fileRepository, times(1)).findByFileTypeAndMember_Email(fileType, email);
    }
}