package com.springbatch.job;


import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import org.springframework.boot.CommandLineRunner;

@Configuration
@RequiredArgsConstructor
public class HelloConfiguration {
    // 필수 두가지, job, step 을 만들기 위한 빌더패턴 준비
//    private final JobBuilderFactory jobBuilderFactory;
//    private final StepBuilderFactory stepBuilderFactory;
    // spring 5.0 부터  deprecated 되었음, 대체하기위해 jobbuilder와 stepBuilder를 만들어 job과 step을 만들어줌
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JobLauncher jobLauncher;

    @Bean
    public CommandLineRunner runJob() {
        return args -> {
            try {
                JobExecution jobExecution = jobLauncher.run(helloJob(), new JobParametersBuilder().toJobParameters());
                System.out.println("Job Status: " + jobExecution.getStatus());
                System.out.println("Job completed");
            } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException |
                     JobParametersInvalidException e) {
                e.printStackTrace();
            }
        };
    }

    @Bean
    public Job helloJob() {
        JobBuilder jobBuilder = new JobBuilder("helloJob", jobRepository);
        SimpleJobBuilder simpleJobBuilder = jobBuilder.incrementer(new RunIdIncrementer())
                .start(helloStep1())
                .next(helloStep2());
        return simpleJobBuilder.build();

    }

    // 스탭에는 item reader, processor, writer 가 들어가야함
    @JobScope // 스탭에서 트랜잭션을 사용하기 위해서는 @JobScope 어노테이션을 붙여줘야함
    @Bean
    public Step helloStep1() {
        StepBuilder stepBuilder = new StepBuilder("helloStep1", jobRepository);
//        return stepBuilderFactory.get("helloStep")
//                .tasklet(helloTasklet())
//                .build();
        return stepBuilder
            .tasklet(helloTasklet1(), transactionManager)
            .build();
    }

    @Bean
    public Step helloStep2() {
        StepBuilder stepBuilder = new StepBuilder("helloStep2", jobRepository);
//        return stepBuilderFactory.get("helloStep")
//                .tasklet(helloTasklet())
//                .build();
        return stepBuilder
                .tasklet(helloTasklet2(), transactionManager)
                .tasklet(finishTasklet(), transactionManager)
                .build();
    }


    // 읽거나 쓰거나 할게 없는 단순한 작업이 필요하다면 Tasklet 을 사용하면 됨
    @StepScope // 스탭 하위에서 동작하기 때문에 StepScope 어노테이션을 붙여줘야함. 빼도 상관은 없는거같은데;
    @Bean
    public Tasklet helloTasklet1() {
        return (contribution, chunkContext) -> {
            System.out.println("Hello, World!11");
            return RepeatStatus.FINISHED;
        };
    }
    @Bean
    public Tasklet helloTasklet2() {
        return (contribution, chunkContext) -> {
            System.out.println("Hello, World!222");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Tasklet finishTasklet() {
        return (contribution, chunkContext) -> {
            System.out.println("Hello, this is finish!");
            return RepeatStatus.FINISHED;
        };
    }


}
