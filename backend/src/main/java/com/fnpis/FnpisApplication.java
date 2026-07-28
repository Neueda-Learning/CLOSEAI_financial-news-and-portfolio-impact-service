package com.fnpis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Financial News and Portfolio Impact Service.
 *
 * <p>Layering follows architecture document section 2: api -> service ->
 * repository -> domain, with integration/ and scheduler/ as side paths.
 * Two rules the whole team relies on:
 *
 * <ul>
 *   <li>Controllers hold no business logic - validate, convert, delegate.</li>
 *   <li>Services never call third-party HTTP directly - go through a
 *       Provider interface in integration/ (decisions 1 and 2).</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
public class FnpisApplication {

    public static void main(String[] args) {
        SpringApplication.run(FnpisApplication.class, args);
    }
}
