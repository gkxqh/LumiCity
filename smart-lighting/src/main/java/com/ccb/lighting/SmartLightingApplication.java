package com.ccb.lighting;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智慧城市照明综合控制系统 - 启动类
 *
 * @SpringBootApplication 包含三合一：
 *   @ComponentScan     扫描 com.ccb.lighting 包下的所有 @Component/@Service/@Controller
 *   @EnableAutoConfiguration  自动配置 Spring Boot
 *   @Configuration      本身也是配置类
 *
 * @MapperScan 批量扫描所有模块的 mapper 包，不用每个接口加 @Mapper
 *
 * @EnableAsync 开启异步方法支持（@Async）
 * @EnableScheduling 开启定时任务支持（@Scheduled）—— 照明策略定时执行用
 */
@SpringBootApplication
@MapperScan("com.ccb.lighting.module.*.mapper")
@EnableAsync
@EnableScheduling
public class SmartLightingApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartLightingApplication.class, args);
        System.out.println("""
                
                ========================================
                智慧城市照明综合控制系统 启动成功
                接口文档: http://localhost:8080/api/swagger-ui.html
                ========================================
                """);
    }
}
