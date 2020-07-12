package com.n2305.swmb.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "mailbluster")
public class MailBlusterProperties {
    private String baseUri;
    private String apiKey;
    private int requestsPerMinute;
    private List<CampaignMappingEntry> campaignMappings;

    public String getBaseUri() {
        return baseUri;
    }

    public MailBlusterProperties setBaseUri(String baseUri) {
        this.baseUri = baseUri;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public MailBlusterProperties setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public MailBlusterProperties setRequestsPerMinute(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
        return this;
    }

    public List<CampaignMappingEntry> getCampaignMappings() {
        return campaignMappings;
    }

    public MailBlusterProperties setCampaignMappings(List<CampaignMappingEntry> campaignMappings) {
        this.campaignMappings = campaignMappings;
        return this;
    }

    public Map<String, Integer> getPartnerToCampaignMap() {
        return Optional.of(campaignMappings)
            .map(campaignMappings -> campaignMappings.stream()
                .collect(Collectors.toMap(
                    CampaignMappingEntry::getPartner,
                    CampaignMappingEntry::getCampaign
                )))
            .orElse(Map.of());
    }

    static class CampaignMappingEntry {
        private String partner;
        private int campaign;

        public String getPartner() {
            return partner;
        }

        public CampaignMappingEntry setPartner(String partner) {
            this.partner = partner;
            return this;
        }

        public int getCampaign() {
            return campaign;
        }

        public CampaignMappingEntry setCampaign(int campaign) {
            this.campaign = campaign;
            return this;
        }
    }
}
