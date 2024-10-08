package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

@Component
public class DataStore {

    private final WebClient client;
    private final IParser parser;
    private final int pageCount;

    public DataStore(WebClient client, IParser parser, @Value("${fhir.pageCount}") int pageCount) {
        this.client = client;
        this.parser = parser;
        this.pageCount = pageCount;
    }


    /**
     * Executes {@code populationQuery} and returns all resources found with that query.
     *
     * @param populationQuery the fhir search query defining the population
     * @return the resources found with the {@code populationQuery}
     */
    public Flux<Resource> getPopulation(String populationQuery) {
        return client.get()
                .uri(appendPageCount(populationQuery))
                .retrieve()
                .bodyToFlux(String.class)
                .map(response -> parser.parseResource(Bundle.class, response))
                .expand(bundle -> Optional.ofNullable(bundle.getLink("next"))
                        .map(link -> fetchPage(client, link.getUrl()))
                        .orElse(Mono.empty()))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(e -> e instanceof WebClientResponseException &&
                                shouldRetry(((WebClientResponseException) e).getStatusCode())))
                .flatMap(bundle -> Flux.fromStream(bundle.getEntry().stream().map(Bundle.BundleEntryComponent::getResource)));
    }

    private static boolean shouldRetry(HttpStatusCode code) {
        return code.is5xxServerError() || code.value() == 404;
    }

    private Mono<Bundle> fetchPage(WebClient client, String url) {
        return client.get()
                .uri(URI.create(url))
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> parser.parseResource(Bundle.class, response));
    }

    String appendPageCount(String query) {
        return query.contains("?") ? query + "&_count=" + this.pageCount : query + "?_count=" + this.pageCount;
    }

}
