package com.planora.backend.config;

import com.planora.backend.client.GithubClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class GithubClientConfig {

    private static final int BUFFER_SIZE_10_MB = 10 * 1024 * 1024;

    @Bean
    public HttpServiceProxyFactory githubHttpServiceProxyFactory() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(BUFFER_SIZE_10_MB))
                .build();

        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .exchangeStrategies(strategies)
                .build();

        return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
    }

    @Bean
    public GithubClient githubClient(HttpServiceProxyFactory githubHttpServiceProxyFactory) {
        return githubHttpServiceProxyFactory.createClient(GithubClient.class);
    }

}
