package com.n2305.swmb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n2305.swmb.mailbluster.MailBlusterAPI;
import com.n2305.swmb.mailbluster.PartnerCampaignIDMapper;
import com.n2305.swmb.properties.MailBlusterProperties;
import com.n2305.swmb.properties.ShopwareProperties;
import com.n2305.swmb.shopware.FilterQueryParamSerializer;
import com.n2305.swmb.shopware.ShopwareAPI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Clock;

@SpringBootApplication
public class ShopwareMailBlusterApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopwareMailBlusterApplication.class, args);
	}

	@Bean
	public ShopwareAPI shopwareAPI(
		WebClient.Builder webClientBuilder,
		ShopwareProperties swProps,
		ObjectMapper objectMapper,
		Clock clock
	) {
		return new ShopwareAPI(
			webClientBuilder.baseUrl(swProps.getBaseUri())
				.defaultHeaders(headers -> headers.setBasicAuth(
					swProps.getUsername(),
					swProps.getPassword()
				))
				.build(),
			new FilterQueryParamSerializer(),
			objectMapper,
			swProps,
			clock
		);
	}

	@Bean
	public MailBlusterAPI mailBlusterAPI(
		WebClient.Builder webClientBuilder,
		MailBlusterProperties mbProps
	) {
		return new MailBlusterAPI(
			webClientBuilder.baseUrl(mbProps.getBaseUri())
			.defaultHeaders(headers -> headers.set("Authorization", mbProps.getApiKey()))
			.build()
		);
	}

	@Bean
	public PartnerCampaignIDMapper partnerCampaignIDMapper(
		MailBlusterProperties mbProps
	) {
		return new PartnerCampaignIDMapper(
			mbProps.getPartnerToCampaignMap(),
			mbProps.getCampaignMappingPassThroughPattern()
		);
	}

	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapperFactory().get();
	}

	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}
