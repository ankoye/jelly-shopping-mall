package com.ankoye.jelly.pay;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * @author ankoye@qq.com
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class
})
@EnableDubbo(scanBasePackages = "com.ankoye.jelly.pay.service.impl")
public class RPCPayApplication {
    public static void main(String[] args) {
        SpringApplication.run(RPCPayApplication.class, args);
    }
}
