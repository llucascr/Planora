package com.planora.backend.config;

import com.planora.backend.client.GithubIssueClient;
import com.planora.backend.client.GithubLabelClient;
import com.planora.backend.client.GithubRepositoryClient;
import com.planora.backend.client.GithubSearchClient;
import com.planora.backend.client.GithubWebhookClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class GithubClientConfig {

    private static final int BUFFER_SIZE_10_MB = 10 * 1024 * 1024;

    @Value("${github.api.base-url:https://api.github.com}")
    private String baseUrl;

    @Bean
    public HttpServiceProxyFactory githubHttpServiceProxyFactory() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(BUFFER_SIZE_10_MB))
                .build();

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(strategies)
                .build();

        return HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build();
    }

    @Bean
    public GithubIssueClient githubIssueClient(HttpServiceProxyFactory githubHttpServiceProxyFactory) {
        return githubHttpServiceProxyFactory.createClient(GithubIssueClient.class);
    }

    @Bean
    public GithubRepositoryClient githubRepositoryClient(HttpServiceProxyFactory githubHttpServiceProxyFactory) {
        return githubHttpServiceProxyFactory.createClient(GithubRepositoryClient.class);
    }

    @Bean
    public GithubWebhookClient githubWebhookClient(HttpServiceProxyFactory githubHttpServiceProxyFactory) {
        return githubHttpServiceProxyFactory.createClient(GithubWebhookClient.class);
    }

    @Bean
    public GithubLabelClient githubLabelClient(HttpServiceProxyFactory githubHttpServiceProxyFactory) {
        return githubHttpServiceProxyFactory.createClient(GithubLabelClient.class);
    }

    @Bean
    public GithubSearchClient githubSearchClient(HttpServiceProxyFactory githubHttpServiceProxyFactory) {
        return githubHttpServiceProxyFactory.createClient(GithubSearchClient.class);
    }
}
