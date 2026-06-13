package com.exam.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.exam.system.mapper")
@SpringBootApplication
public class ExamSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExamSystemApplication.class, args);
    }
}
