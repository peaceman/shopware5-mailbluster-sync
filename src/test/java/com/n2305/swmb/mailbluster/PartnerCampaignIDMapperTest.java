package com.n2305.swmb.mailbluster;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class PartnerCampaignIDMapperTest {
    static Stream<Arguments> provideData() {
        return Stream.of(
            arguments(
                Map.of("foobar", 23),
                "",
                "foobar",
                23
            ),
            arguments(
                Map.of("foobar", 23),
                "",
                "unknown",
                null
            ),
            arguments(
                Map.of("foobar", 23),
                "mb-(\\d+)",
                "mb-5",
                5
            ),
            arguments(
                Map.of("foobar", 23),
                "mb-(\\d+)",
                "mb-nan",
                null
            ),
            arguments(
                Map.of("foobar", 23),
                "mb-(\\d+)",
                null,
                null
            ),
            arguments(
                Map.of("foobar", 23),
                "mb-(\\d+)",
                "",
                null
            ),
            arguments(
                Map.of(),
                null,
                "",
                null
            )
        );
    }

    @ParameterizedTest
    @MethodSource("provideData")
    void apply(Map<String, Integer> mapping, String pattern, String input, Integer expected) {
        PartnerCampaignIDMapper mapper = new PartnerCampaignIDMapper(mapping, pattern);
        Integer result = mapper.apply(input);

        assertEquals(expected, result);
    }
}