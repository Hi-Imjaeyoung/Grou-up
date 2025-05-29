package growup.spring.springserver.file.repository;

import growup.spring.springserver.file.domian.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByMember_EmailAndFileUploadDateBetween(
            String email,
            LocalDate startDate,
            LocalDate endDate
    );
}
