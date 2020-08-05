package com.n2305.swmb.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String stateFolderPath;

    public String getStateFolderPath() {
        return stateFolderPath;
    }

    public AppProperties setStateFolderPath(String stateFolderPath) {
        this.stateFolderPath = stateFolderPath;
        return this;
    }
}
