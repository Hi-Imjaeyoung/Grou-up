package growup.spring.springserver.file.repository;

import growup.spring.springserver.file.FileType;
import growup.spring.springserver.file.domian.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;


@Repository
public interface FileRepository extends JpaRepository<File, Long> {


    List<File> findByMemberEmailAndFileTypeAndFileStartDateLessThanEqualAndFileEndDateGreaterThanEqual(
            String   email,
            FileType fileType,
            LocalDate endDate,    // 조회 종료일 → fileStartDate ≤ endDate
            LocalDate startDate   // 조회 시작일 → fileEndDate   ≥ startDate
    );
}
