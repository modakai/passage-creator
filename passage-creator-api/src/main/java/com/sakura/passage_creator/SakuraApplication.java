package com.sakura.passage_creator;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.modulith.Modulithic;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 主类，项目启动入口
 *
 * @author sakura
 * @from sakura
 */
@Modulithic(systemName = "passage-creator-api")
@SpringBootApplication
@MapperScan("com.sakura.passage_creator.**.repository")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@EnableAsync
public class SakuraApplication {

    public static void main(String[] args) {
        SpringApplication.run(SakuraApplication.class, args);
    }
}
