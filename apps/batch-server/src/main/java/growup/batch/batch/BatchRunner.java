package growup.batch.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BatchRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final Job backfillMonthlySummaryJob;

    public BatchRunner(JobLauncher jobLauncher, Job backfillMonthlySummaryJob) {
        this.jobLauncher = jobLauncher;
        this.backfillMonthlySummaryJob = backfillMonthlySummaryJob;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> [Backfill Batch] 작업을 시작합니다...");

        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis()) // 매 실행 시 고유 Parameter 부여
                .toJobParameters();

        var execution = jobLauncher.run(backfillMonthlySummaryJob, jobParameters);

        System.out.println(">>> [Backfill Batch] 완료 상태: " + execution.getStatus());
        System.out.println(">>> [Backfill Batch] Exit Code: " + execution.getExitStatus().getExitCode());
    }
}