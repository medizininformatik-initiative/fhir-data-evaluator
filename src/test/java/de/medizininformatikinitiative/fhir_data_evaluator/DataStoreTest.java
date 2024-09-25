package de.medizininformatikinitiative.fhir_data_evaluator;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

import java.io.IOException;

class DataStoreTest {
    private static MockWebServer mockStore;

    private DataStore dataStore;

    @BeforeAll
    static void setUp() throws IOException {
        mockStore = new MockWebServer();
        mockStore.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockStore.shutdown();
    }

    @BeforeEach
    void initialize() {
        WebClient client = WebClient.builder()
                .baseUrl("http://localhost:%d/fhir".formatted(mockStore.getPort()))
                .defaultHeader("Accept", "application/fhir+json")
                .build();
        IParser parser = FhirContext.forR4().newJsonParser();
        dataStore = new DataStore(client, parser, 1000);
    }

    @ParameterizedTest
    @DisplayName("retires the request")
    @ValueSource(ints = {404, 500, 503, 504})
    void execute_retry(int statusCode) {
        mockStore.enqueue(new MockResponse().setResponseCode(statusCode));
        mockStore.enqueue(new MockResponse().setResponseCode(200)
                .setBody("{\"resourceType\":\"Bundle\", \"entry\": [{\"resource\": {\"resourceType\":\"Observation\"}}]}"));

        var result = dataStore.getPopulation("/Observation");
        StepVerifier.create(result).expectNextCount(1).verifyComplete();
    }

    @Test
    @DisplayName("fails after 3 unsuccessful retires")
    void execute_retry_fails() {
        mockStore.enqueue(new MockResponse().setResponseCode(500));
        mockStore.enqueue(new MockResponse().setResponseCode(500));
        mockStore.enqueue(new MockResponse().setResponseCode(500));
        mockStore.enqueue(new MockResponse().setResponseCode(500));
        mockStore.enqueue(new MockResponse().setResponseCode(200));

        var result = dataStore.getPopulation("/Observation");

        StepVerifier.create(result).expectErrorMessage("Retries exhausted: 3/3").verify();
    }

    @Test
    @DisplayName("doesn't retry a 400")
    void execute_retry_400() {
        mockStore.enqueue(new MockResponse().setResponseCode(400));

        var result = dataStore.getPopulation("/Observation");

        StepVerifier.create(result).expectError(WebClientResponseException.BadRequest.class).verify();
    }
}
