package com.klef.sdp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class UserServiceApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(
            UserServiceApplication.class,
            args
        );

        System.out.println(
            "User Service is Running......!"
        );
    }
}
