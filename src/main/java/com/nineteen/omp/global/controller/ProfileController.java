package com.nineteen.omp.global.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@Slf4j
public class ProfileController {

  @Value("${spring.profiles.active}")
  private String profile;

  @GetMapping
  public String getProfile() {
    return profile;
  }

  @GetMapping("/sleep")
  public String sleep() {
    log.info("sleep start 5min");
    try {
      Thread.sleep(1000 * 60 * 3);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return profile;
  }
}
