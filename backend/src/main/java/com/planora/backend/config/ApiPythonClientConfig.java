package com.planora.backend.config;

import com.planora.backend.client.ApiPythonClient;
import com.planora.backend.client.GithubClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

public class ApiPythonClientConfig {

    private static final int BUFFER_SIZE_10_MB = 10 * 1024 * 1024;

    @Bean
    public HttpServiceProxyFactory httpServiceProxyFactory() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(BUFFER_SIZE_10_MB))
                .build();

        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8022")
                .exchangeStrategies(strategies)
                .build();

        return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
    }

    @Bean
    public ApiPythonClient apiPythonClient(HttpServiceProxyFactory factory) {
        return factory.createClient(ApiPythonClient.class);
    }

}
