package growup.spring.springserver.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {

    // 1. [I/O 전용] DB 조회용. 크기는 HikariCP maximum-pool-size 에 맞춘다 (풀 크기 튜닝 측정에서 바꿔 가며 잰다)
    @Bean(name = "ioExecutor")
    public Executor ioExecutor(@Value("${thread-pool.io.size:30}") int ioPoolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(ioPoolSize);
        executor.setMaxPoolSize(ioPoolSize);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("IO-Worker-");
        executor.initialize();
        return executor;
    }

    // 2. [CPU 전용] 트리 빌드용
    @Bean(name = "cpuExecutor")
    public Executor cpuExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int processors = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(processors + 1);
        executor.setMaxPoolSize(processors + 1);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("CPU-Worker-");
        executor.initialize();
        return executor;
    }
}