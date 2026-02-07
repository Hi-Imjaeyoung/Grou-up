package growup.spring.springserver.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {

    // 1. [I/O 전용] DB 조회용 (기다리는 애들이니 넉넉하게!)
    @Bean(name = "ioExecutor")
    public Executor ioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20); // DB 커넥션 풀 사이즈랑 비슷하게 맞추면 좋음
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("IO-Worker-");
        executor.initialize();
        return executor;
    }

    // 2. [CPU 전용] 트리 빌드용 (일하는 애들이니 타이트하게!)
    @Bean(name = "cpuExecutor")
    public Executor cpuExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int processors = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(processors + 1); // 코어 수 + 1
        executor.setMaxPoolSize(processors + 1);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("CPU-Worker-");
        executor.initialize();
        return executor;
    }
}