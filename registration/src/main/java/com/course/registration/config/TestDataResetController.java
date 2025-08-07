package com.course.registration.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

@Profile("dev")                // dev 프로파일에서만 활성화
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class TestDataResetController {
    private final DataSource dataSource;

    @PostMapping("/reset")
    public void resetAll() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("TRUNCATE TABLE audit_log");
        jdbc.execute("TRUNCATE TABLE waitlist");
        jdbc.execute("TRUNCATE TABLE enrollment");
    }
}