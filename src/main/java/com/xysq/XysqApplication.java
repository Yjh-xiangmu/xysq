package com.xysq;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.xysq.mapper")
public class XysqApplication {
	public static void main(String[] args) {
		SpringApplication.run(XysqApplication.class, args);
		System.out.println("====== 校园兴趣社区分享平台启动成功 ======》》http://localhost:8080/");
	}
}