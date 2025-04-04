package de.caritas.cob.messageservice;

import io.sentry.spring.jakarta.EnableSentry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableSentry
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class MessageServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(MessageServiceApplication.class, args);
  }

}
