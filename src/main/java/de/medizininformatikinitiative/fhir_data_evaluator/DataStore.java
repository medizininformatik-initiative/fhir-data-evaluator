package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import static de.medizininformatikinitiative.fhir_data_evaluator.ResourceWithIncludes.processBundleIncludes;

public class DataStore {

    private final WebClient webClient;
    private final IParser parser;
    private final int pageCount;
    private final FhirContext context;
    private final IFhirPath applicationFhirPathEngine;

    private final Logger logger = LoggerFactory.getLogger(DataStore.class);

    public DataStore(WebClient webClient, IParser parser, int pageCount, FhirContext context, IFhirPath fhirPathEngine) {
        this.webClient = webClient;
        this.parser = parser;
        this.pageCount = pageCount;
        this.context = context;
        this.applicationFhirPathEngine = fhirPathEngine;
    }

    /**
     * Executes {@code query} and returns all resources found with that query.
     *
     * @param query the fhir search query
     * @return the resources found with the {@code query}
     */
    public Flux<ResourceWithIncludes> getResources(String query) {
        return webClient.get()
                .uri(appendPageCount(query))
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(response -> {
                    logger.debug("Initial query success: {}", appendPageCount(query));
                    logger.trace("Response: {}", response);
                })
                .map(response -> parser.parseResource(Bundle.class, response))
                .expand(bundle -> Optional.ofNullable(bundle.getLink("next"))
                        .map(link -> fetchPage(webClient, link.getUrl()))
                        .orElse(Mono.empty()))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(e -> e instanceof WebClientResponseException &&
                                shouldRetry(((WebClientResponseException) e).getStatusCode())))
                .flatMap(bundle -> Flux.fromStream(processBundleIncludes(bundle, applicationFhirPathEngine, context)));
    }

    /**
     * Posts a FHIR Bundle to a FHIR server.
     *
     * @param bundle    the Bundle to post to the FHIR server
     * @return          a {@link Mono<Void>} that completes when the request is successful, or  signals an error Mono on
     *                  failure
     */
    public Mono<Void> postReport(String bundle) {
        return webClient.post()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .bodyValue(bundle)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(errorBody ->
                                        new IOException(String.format("Failed uploading MeasureReport with status " +
                                                "code: '%s' and body: '%s'", clientResponse.statusCode(), errorBody)))
                                .switchIfEmpty(Mono.error(new IOException(String.format("Failed uploading MeasureReport " +
                                        "with status code: '%s'", clientResponse.statusCode())))))
                .bodyToMono(Void.class);
    }

    private static boolean shouldRetry(HttpStatusCode code) {
        return code.is5xxServerError() || code.value() == 404;
    }

    private Mono<Bundle> fetchPage(WebClient client, String url) {
        return client.get()
                .uri(URI.create(url))
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(response -> logger.trace("Fetch page success: {}", url))
                .map(response -> parser.parseResource(Bundle.class, response));
    }

    String appendPageCount(String query) {
        return query.contains("?") ? query + "&_count=" + this.pageCount : query + "?_count=" + this.pageCount;
    }

}
