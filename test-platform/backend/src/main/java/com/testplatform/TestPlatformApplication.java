package com.testplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author greada
 * @version 1.0.0
 */
@SpringBootApplication
@MapperScan("com.testplatform.mapper")
public class TestPlatformApplication {
    public static void main(String[] args) {
		// 加一行注释
        SpringApplication.run(TestPlatformApplication.class, args);
    }
}
