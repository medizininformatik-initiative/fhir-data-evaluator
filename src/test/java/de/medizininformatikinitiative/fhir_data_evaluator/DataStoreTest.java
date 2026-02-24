package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.context.FhirContext;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class DataStoreTest {

    @Nested
    class TestGet {
        private static MockWebServer mockStore;

        private DataStore dataStore;

        @BeforeEach
        void setUp() throws IOException {
            mockStore = new MockWebServer();
            mockStore.start();
        }

        @AfterEach
        void tearDown() throws IOException {
            mockStore.shutdown();
        }

        @BeforeEach
        void initialize() {
            WebClient client = WebClient.builder()
                    .baseUrl("http://localhost:%d/fhir".formatted(mockStore.getPort()))
                    .defaultHeader("Accept", "application/fhir+json")
                    .build();
            FhirContext context = FhirContext.forR4();
            dataStore = new DataStore(client, 1000, context, context.newFhirPath());
        }

        @ParameterizedTest
        @DisplayName("retries the request")
        @ValueSource(ints = {404, 500, 503, 504})
        void execute_retry(int statusCode) {
            mockStore.enqueue(new MockResponse().setResponseCode(statusCode));
            mockStore.enqueue(new MockResponse().setResponseCode(200)
                    .setBody("{\"resourceType\":\"Bundle\", \"entry\": [{\"resource\": {\"resourceType\":\"Observation\"}, \"search\": {\"mode\": \"match\"}}]}"));

            var result = dataStore.getResources("/Observation");
            StepVerifier.create(result).expectNextCount(1).verifyComplete();
        }

        @Test
        @DisplayName("fails after 3 unsuccessful retries")
        void execute_retry_fails() {
            mockStore.enqueue(new MockResponse().setResponseCode(500));
            mockStore.enqueue(new MockResponse().setResponseCode(500));
            mockStore.enqueue(new MockResponse().setResponseCode(500));
            mockStore.enqueue(new MockResponse().setResponseCode(500));
            mockStore.enqueue(new MockResponse().setResponseCode(200));

            var result = dataStore.getResources("/Observation");

            StepVerifier.create(result).expectErrorMessage("Retries exhausted: 3/3").verify();
        }

        @Test
        @DisplayName("doesn't retry a 400")
        void execute_retry_400() {
            mockStore.enqueue(new MockResponse().setResponseCode(400));

            var result = dataStore.getResources("/Observation");

            StepVerifier.create(result).expectError(WebClientResponseException.BadRequest.class).verify();
        }

        @Nested
        class TestParsingErrors {

            public static final String VALID_ENCOUNTER = """
                    {
                         "resourceType": "Bundle",
                         "type": "searchset",
                         "entry": [
                             {
                                 "resource": {
                                     "resourceType": "Encounter",
                                     "id": "enc-1",
                                     "location": [
                                         {
                                             "status": "planned"
                                         }
                                     ]
                                 },
                                 "search": {
                                     "mode": "match"
                                 }
                             }
                         ]
                     }
                    """;

            public static final String INVALID_ENCOUNTER = """
                    {
                         "resourceType": "Bundle",
                         "type": "searchset",
                         "entry": [
                             {
                                 "resource": {
                                     "resourceType": "Encounter",
                                     "id": "enc-1",
                                     "location": [
                                         {
                                             "status": "asdf"
                                         }
                                     ]
                                 },
                                 "search": {
                                     "mode": "match"
                                 }
                             }
                         ]
                     }
                    """;

            public static String VALID_AND_INVALID_ENCOUNTERS = """
                    {
                         "resourceType": "Bundle",
                         "type": "searchset",
                         "entry": [
                             {
                                 "resource": {
                                     "resourceType": "Encounter",
                                     "id": "enc-1",
                                     "location": [
                                         {
                                             "status": "planned"
                                         }
                                     ]
                                 },
                                 "search": {
                                     "mode": "match"
                                 }
                             },
                             {
                                 "resource": {
                                     "resourceType": "Encounter",
                                     "id": "enc-2",
                                     "location": [
                                         {
                                             "status": "asdf"
                                         }
                                     ]
                                 },
                                 "search": {
                                     "mode": "match"
                                 }
                             }
                         ]
                     }
                    """;

            @Test
            void testValidEncounter() {
                mockStore.enqueue(new MockResponse().setBody(VALID_ENCOUNTER));

                var result = dataStore.getResources("some-query");

                StepVerifier.create(result).expectNextCount(1).verifyComplete();
            }

            @Test
            void testInvalidEncounter() {
                mockStore.enqueue(new MockResponse().setBody(INVALID_ENCOUNTER));

                var result = dataStore.getResources("some-query");

                StepVerifier.create(result).expectErrorSatisfies(e -> assertThat(e)
                            .isInstanceOf(BundleParsingException.class)
                            .hasMessage("Failed parsing resource Encounter/enc-1"))
                        .verify();
            }

            @Test
            void testValidAndInvalidEncounter() {
                mockStore.enqueue(new MockResponse().setBody(VALID_AND_INVALID_ENCOUNTERS));

                var result = dataStore.getResources("some-query");

                StepVerifier.create(result).expectErrorSatisfies(e -> assertThat(e)
                                .isInstanceOf(BundleParsingException.class)
                                .hasMessage("Failed parsing resource Encounter/enc-2"))
                        .verify();
            }
        }
    }

    @Nested
    class TestPost {
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
            FhirContext context = FhirContext.forR4();
            dataStore = new DataStore(client, 1000, context, context.newFhirPath());
        }

        @Test
        void test_errorResponse_400() {
            mockStore.enqueue(new MockResponse().setResponseCode(400).setBody("error encountered"));

            var result = dataStore.postReport("");

            StepVerifier.create(result).verifyErrorMessage(
                    "Failed uploading MeasureReport with status code: '400 BAD_REQUEST' and body: 'error encountered'");
        }

        @Test
        void test_errorResponse_withoutBody() {
            mockStore.enqueue(new MockResponse().setResponseCode(400));

            var result = dataStore.postReport("");

            StepVerifier.create(result).verifyErrorMessage(
                    "Failed uploading MeasureReport with status code: '400 BAD_REQUEST'");
        }

        @Test
        void test_errorResponse_500() {
            mockStore.enqueue(new MockResponse().setResponseCode(500).setBody("error encountered"));

            var result = dataStore.postReport("");

            StepVerifier.create(result).verifyErrorMessage(
                    "Failed uploading MeasureReport with status code: '500 INTERNAL_SERVER_ERROR' and body: 'error encountered'");
        }
    }
}
