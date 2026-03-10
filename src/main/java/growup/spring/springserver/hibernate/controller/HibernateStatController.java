package growup.spring.springserver.hibernate.controller;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.persistence.EntityManagerFactory; // javax.persistence일 수도 있음 (버전 확인)

@RestController
public class HibernateStatController {

    private final EntityManagerFactory entityManagerFactory;

    public HibernateStatController(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    // 1. 현재까지의 통계 정보를 싹 초기화 (테스트 시작 전에 호출)
    @PostMapping("/test/stats/reset")
    public void resetStats() {
        getStatistics().clear();
    }

    // 2. 실행된 쿼리 총 횟수 가져오기
    @GetMapping("/test/stats/query-count")
    public long getQueryCount() {
        return getStatistics().getQueryExecutionCount();
    }

    // Helper 메서드
    private Statistics getStatistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}