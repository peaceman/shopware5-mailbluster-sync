package com.n2305.swmb.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "shopware")
public class ShopwareProperties {
    private String baseUri;
    private String username;
    private String password;
    private String exportedAttribute;

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
}
