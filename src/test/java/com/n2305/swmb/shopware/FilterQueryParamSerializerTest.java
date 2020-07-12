package com.n2305.swmb.shopware;

import com.n2305.swmb.shopware.ShopwareAPI.Filter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class FilterQueryParamSerializerTest {
    private static final FilterQueryParamSerializer serializer = new FilterQueryParamSerializer();

    static Stream<Arguments> provideFilters() {
        return Stream.of(
            arguments(
                "multi filter",
                List.of(
                    new Filter("alpha", "0"),
                    new Filter("beta", "not beta")
                ),
                Map.of(
                    "filter[0][property]", "alpha",
                    "filter[0][value]", "0",
                    "filter[0][expression]", "LIKE",
                    "filter[1][property]", "beta",
                    "filter[1][value]", "not beta",
                    "filter[1][expression]", "LIKE"
                )
            ),
            arguments(
                "optional operator",
                List.of(
                    new Filter("gamma", "23", null, true)
                ),
                Map.of(
                    "filter[0][property]", "gamma",
                    "filter[0][value]", "23",
                    "filter[0][expression]", "LIKE",
                    "filter[0][operator]", "1"
                )
            ),
            arguments(
                "null value",
                List.of(
                    new Filter("gamma", null, "=")
                ),
                Map.of(
                    "filter[0][property]", "gamma",
                    "filter[0][expression]", "="
                )
            )
        );
    }

    @ParameterizedTest(name = "run #{index} {0}")
    @MethodSource("provideFilters")
    void testSerialization(String testName, List<Filter> filters, Map<String, String> expected) {
        assertEquals(expected, serializer.serialize(filters));
    }
}