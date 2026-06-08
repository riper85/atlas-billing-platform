package com.atlas.billing.platform.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AtlasWebAutoConfiguration {

    @Bean
    public AtlasWebMarker atlasWebMarker() {
        return new AtlasWebMarker("platform-starter-web");
    }
}
