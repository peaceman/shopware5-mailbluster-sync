package com.n2305.swmb.shopware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.n2305.swmb.ObjectMapperFactory;
import com.n2305.swmb.properties.ShopwareProperties;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopwareAPITest {
    WireMockServer wireMockServer;
    MockWebServer server;
    WebClient webClient;
    ShopwareAPI api;
    static ShopwareProperties swProps;
    static ObjectMapper objectMapper;
    static Clock clock;

    @BeforeAll
    static void beforeAllSetup() {
        swProps = new ShopwareProperties();
        swProps.setExportedAttribute("mbExport");

        objectMapper = new ObjectMapperFactory().get();
        clock = Clock.fixed(Instant.ofEpochSecond(23232323), ZoneId.of("UTC+01"));
    }

    @BeforeEach
    void beforeEach() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        this.webClient = WebClient.builder()
            .codecs(ccc -> ccc.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper)))
            .baseUrl(wireMockServer.baseUrl())
            .build();

        this.api = new ShopwareAPI(
            webClient,
            new FilterQueryParamSerializer(),
            objectMapper,
            swProps,
            clock
        );
    }

    @AfterEach
    void stopMockServer() {
        wireMockServer.stop();
    }

    @Test
    void testFetchOrders() throws ExecutionException, InterruptedException, URISyntaxException, IOException {
        wireMockServer.stubFor(get(urlPathMatching("/api/orders"))
            .willReturn(okJson(stringFromResource("orders.json"))));

        List<OrderListItem> orderListItems = api.fetchOrders(List.of(
            new ShopwareAPI.Filter("status", "0", "="),
            new ShopwareAPI.Filter("newsletter", "1", "=")
        )).block();

        Map<String, String> expectedQueryParams = new LinkedHashMap<>();

        expectedQueryParams.put("filter[0][property]", "status");
        expectedQueryParams.put("filter[0][value]", "0");
        expectedQueryParams.put("filter[0][expression]", "=");
        expectedQueryParams.put("filter[1][property]", "newsletter");
        expectedQueryParams.put("filter[1][value]", "1");
        expectedQueryParams.put("filter[1][expression]", "=");

        RequestPatternBuilder reqPatternBuilder = getRequestedFor(urlPathMatching("/api/orders"));
        expectedQueryParams.forEach((k, v) -> {
            try {
                reqPatternBuilder.withQueryParam(URLEncoder.encode(k, "UTF-8"), equalTo(v));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
        wireMockServer.verify(reqPatternBuilder);


        assertEquals(5, orderListItems.size());
        assertThat(new OrderListItem(2, "20001", OffsetDateTime.now(clock)), is(in(orderListItems)));
    }

    @Test
    void testFetchOrder() throws InterruptedException, IOException {
        wireMockServer.stubFor(get(urlPathMatching("/api/orders/2"))
            .willReturn(okJson(stringFromResource("order.json"))));

        SWOrder order = api.fetchOrder(2).block();
        assertEquals(2, order.getId());
    }

    @Test
    void testMarkOrderAsExported() throws InterruptedException, JsonProcessingException {
        wireMockServer.stubFor(put(urlPathMatching("/api/orders/23"))
            .willReturn(ok()));

        SWOrder swOrder = mock(SWOrder.class);
        when(swOrder.getId()).thenReturn(23);

        api.markOrderAsExported(swOrder).block();

        wireMockServer.verify(putRequestedFor(urlPathMatching("/api/orders/23"))
            .withRequestBody(matchingJsonPath(
                "$.attribute." + swProps.getExportedAttribute(),
                equalTo(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(clock)))
            )));
    }

    private String stringFromResource(String path) throws IOException {
        return new String(getClass().getResourceAsStream(path).readAllBytes());
    }
}