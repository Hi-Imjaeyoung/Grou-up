package growup.spring.springserver.file.repository;

import growup.spring.springserver.file.FileType;
import growup.spring.springserver.file.domian.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByFileTypeAndMember_Email(FileType fileType, String email);
}
