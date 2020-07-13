package com.n2305.swmb.shopware;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class SWOrderTest {
    static Stream<Arguments> provideIPAddresses() {
        return Stream.of(
            arguments(
                "2a02:810b:c940:1409:eda9:::",
                "2a02:810b:c940:1409:eda9:0:0:0"
            ),
            arguments(
                "2a02:810b:c940:1409:eda9::1",
                "2a02:810b:c940:1409:eda9:0:0:1"
            ),
            arguments(
                "23.5.23.5",
                "23.5.23.5"
            )
        );
    }

    @ParameterizedTest(name = "ip {0}")
    @MethodSource("provideIPAddresses")
    void testIPSanitation(String ipAddress, String expected) {
        SWOrder order = new SWOrder.Builder()
            .withRemoteAddress(ipAddress)
            .build();

        assertEquals(expected, order.getRemoteAddress());
    }
}
