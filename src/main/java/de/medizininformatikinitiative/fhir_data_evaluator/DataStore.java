package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.parser.DataFormatException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final int pageCount;
    private final FhirContext context;
    private final IFhirPath applicationFhirPathEngine;

    private final Logger logger = LoggerFactory.getLogger(DataStore.class);

    public DataStore(WebClient webClient, int pageCount, FhirContext context, IFhirPath fhirPathEngine) {
        this.webClient = webClient;
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
                .bodyToMono(String.class)
                .doOnNext(response -> logger.debug("Initial query success: {}", appendPageCount(query)))
                .map(this::parseBundle)
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

    /**
     * Parses a bundle string into a {@link Bundle} and handles errors.
     *
     * @param   bundle string to parse
     * @return  the parsed bundle
     * @throws BundleParsingException   if the hapi parser could not parse the bundle
     */
    private Bundle parseBundle(String bundle) {
        try {
            return context.newJsonParser().parseResource(Bundle.class, bundle);
        } catch (DataFormatException e) {
            throw new BundleParsingException(e, findMalformedResource(bundle, e));
        }
    }

    /**
     * Parses the bundle as raw json an iterates over each entry to find the resource that hapi fails to parse.
     *
     * @param bundle            the bundle that contains the malformed resource
     * @param originalException the original parsing exception that occurred when the whole bundle was parsed
     * @return                  the ID of the malformed resource
     */
    private String findMalformedResource(String bundle, DataFormatException originalException) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode bundleEntries;
        try {
            bundleEntries = mapper.readTree(bundle).get("entry");
        } catch (JsonProcessingException e) {
            throw originalException;
        }
        for (var bundleEntry : bundleEntries) {
            var resource = bundleEntry.get("resource");
            try {
                context.newJsonParser().parseResource(resource.toString());
            } catch (DataFormatException e) {
                var resType = resource.get("resourceType").toString().replaceAll("\"", "");
                var resId = resource.get("id").toString().replaceAll("\"", "");
                return resType + "/" + resId;
            }
        }
        throw originalException;
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
                .map(response -> context.newJsonParser().parseResource(Bundle.class, response));
    }

    String appendPageCount(String query) {
        return query.contains("?") ? query + "&_count=" + this.pageCount : query + "?_count=" + this.pageCount;
    }

}
