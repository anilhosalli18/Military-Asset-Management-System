package com.mams;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class MamsApplicationTests {

    @Test
    void contextLoads() {
        // Basic smoke test to be verified with active DB container/service
    }
}
