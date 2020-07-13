package com.n2305.swmb.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "shopware")
public class ShopwareProperties {
    private String baseUri;
    private String username;
    private String password;
    private String exportedAttribute;
    private Intervals intervals;

    public String getBaseUri() {
        return baseUri;
    }

    public ShopwareProperties setBaseUri(String baseUri) {
        this.baseUri = baseUri;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public ShopwareProperties setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public ShopwareProperties setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getExportedAttribute() {
        return exportedAttribute;
    }

    public ShopwareProperties setExportedAttribute(String exportedAttribute) {
        this.exportedAttribute = exportedAttribute;
        return this;
    }

    public Intervals getIntervals() {
        return intervals;
    }

    public ShopwareProperties setIntervals(Intervals intervals) {
        this.intervals = intervals;
        return this;
    }

    public static class Intervals {
        private Duration onEmptyList;
        private Duration resetLastFetchedOrderTime;

        public Duration getOnEmptyList() {
            return onEmptyList;
        }

        public Intervals setOnEmptyList(Duration onEmptyList) {
            this.onEmptyList = onEmptyList;
            return this;
        }

        public Duration getResetLastFetchedOrderTime() {
            return resetLastFetchedOrderTime;
        }

        public Intervals setResetLastFetchedOrderTime(Duration resetLastFetchedOrderTime) {
            this.resetLastFetchedOrderTime = resetLastFetchedOrderTime;
            return this;
        }
    }
}
