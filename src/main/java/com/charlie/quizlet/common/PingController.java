package com.charlie.quizlet.common;

import java.time.Clock;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "System")
public class PingController {

    private final Clock clock;

    @GetMapping(ApiPaths.PING)
    @SecurityRequirements
    @Operation(summary = "Check that the server is running")
    public Map<String, Object> ping() {
        return Map.of("status", "ok", "time", clock.instant());
    }
}
