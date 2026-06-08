package com.atlas.billing.app.readiness;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/readiness")
class EngineeringReadinessController {

    @GetMapping
    ReadinessResponse readiness() {
        return new ReadinessResponse("atlas-billing-platform", "phase-0", "ok");
    }

    record ReadinessResponse(String app, String phase, String status) {}
}
