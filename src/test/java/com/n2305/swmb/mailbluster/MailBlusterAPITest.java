package com.n2305.swmb.mailbluster;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class MailBlusterAPITest {
    WireMockServer wireMockServer;

    @BeforeEach
    void startMockServer() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterEach
    void stopMockServer() {
        wireMockServer.stop();
    }

    @Test
    void testCreateOrderSerialization() throws IOException {
        MBOrder order = new MBOrder.Builder()
            .withId("order_id_0001")
            .withCampaignId(2)
            .withCurrency("USD")
            .withTotalPrice(10.43)
            .withCustomer(new MBOrder.Customer.Builder()
                .withFirstName("Richard")
                .withLastName("Hendricks")
                .withEmail("richard@example.com")
                .withSubscribed(true)
                .build())
            .withItems(List.of(
                new MBOrder.Product("101", "Reign Html Template", 2.13, 1),
                new MBOrder.Product("102", "Slick Html Template", 4.15, 2)
            ))
            .build();

        MailBlusterAPI mbAPI = new MailBlusterAPI(WebClient.builder()
            .baseUrl(wireMockServer.baseUrl()).build());

        wireMockServer.stubFor(post("/api/orders").willReturn(aResponse().withStatus(200)));

        mbAPI.createOrder(order)
            .block();

        wireMockServer.verify(postRequestedFor(urlMatching("/api/orders"))
            .withRequestBody(equalToJson(new String(getClass()
                .getResourceAsStream("create-order.json").readAllBytes()))));
    }
}