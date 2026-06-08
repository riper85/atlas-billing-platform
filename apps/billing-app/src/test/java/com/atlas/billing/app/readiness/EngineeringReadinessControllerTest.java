package com.atlas.billing.app.readiness;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EngineeringReadinessController.class)
class EngineeringReadinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsPhaseZeroReadiness() throws Exception {
        mockMvc.perform(get("/internal/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app").value("atlas-billing-platform"))
                .andExpect(jsonPath("$.phase").value("phase-0"))
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
