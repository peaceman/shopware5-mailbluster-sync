package com.n2305.swmb.mailbluster;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PartnerCampaignIDMapper implements Function<String, Integer> {
    private final Map<String, Integer> partnerCampaignMap;
    private final Pattern passThroughPattern;

    public PartnerCampaignIDMapper(
        Map<String, Integer> partnerCampaignMap,
        String passThroughPattern
    ) {
        this.partnerCampaignMap = partnerCampaignMap;
        this.passThroughPattern = Pattern.compile(Optional.ofNullable(passThroughPattern).orElse(""));
    }

    @Override
    public Integer apply(String partnerID) {
        Optional<String> op = Optional.ofNullable(partnerID);

        return op.map(this.partnerCampaignMap::get)
            .or(() -> op.map(this::tryPassThrough))
            .orElse(null);
    }

    private Integer tryPassThrough(String partnerID) {
        Matcher matcher = passThroughPattern.matcher(partnerID);
        if (!matcher.matches() || matcher.groupCount() < 1)
            return null;

        return Integer.valueOf(matcher.group(1));
    }
}
