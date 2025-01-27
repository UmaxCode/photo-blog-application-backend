package org.umaxcode.lambda_apiGateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import org.umaxcode.lambda_apiGateway.controller.PingController;


@SpringBootApplication
// We use direct @Import instead of @ComponentScan to speed up cold starts
// @ComponentScan(basePackages = "org.umaxcode.controller")
@Import({ PingController.class })
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}