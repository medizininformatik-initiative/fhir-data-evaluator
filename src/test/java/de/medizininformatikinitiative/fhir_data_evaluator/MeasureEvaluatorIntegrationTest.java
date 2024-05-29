package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Testcontainers
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@ActiveProfiles("test")
class MeasureEvaluatorIntegrationTest {

    public static final StratumComponent I60 = new StratumComponent(
            new HashableCoding("http://fhir-evaluator/strat/system", "icd10-code", "some-display"),
            new HashableCoding("http://fhir.de/CodeSystem/bfarm/icd-10-gm", "I60.1", "some-display"));
    public static final StratumComponent ACTIVE = new StratumComponent(
            new HashableCoding("http://fhir-evaluator/strat/system", "condition-clinical-status", "some-display"),
            new HashableCoding("http://terminology.hl7.org/CodeSystem/condition-clinical", "active", "some-display"));

    @TestConfiguration
    static class Config {
        @Bean
        WebClient webClient() {
            var host = "%s:%d".formatted(blaze.getHost(), blaze.getFirstMappedPort());
            WebClient.Builder builder = WebClient.builder()
                    .baseUrl("http://%s/fhir".formatted(host))
                    .defaultHeader("Accept", "application/fhir+json")
                    .defaultHeader("X-Forwarded-Host", host);

            return builder.build();
        }

    }

    @Autowired
    private MeasureEvaluator measureEvaluator;
    @Autowired
    private WebClient webClient;
    @Autowired
    private IParser parser;

    private static final Logger logger = LoggerFactory.getLogger(MeasureEvaluatorIntegrationTest.class);
    private static boolean dataImported = false;


    @SuppressWarnings("resource")
    @Container
    private static final GenericContainer<?> blaze = new GenericContainer<>("samply/blaze:0.25")
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withEnv("LOG_LEVEL", "debug")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200))
            .withLogConsumer(new Slf4jLogConsumer(logger));

    @BeforeEach
    void setUp() throws IOException {
        if (!dataImported) {
            webClient.post()
                    .contentType(APPLICATION_JSON)
                    .bodyValue(Files.readString(Path.of("src/test/resources/FhirDataEvaluatorTest/Bundle.json")))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            dataImported = true;
        }

    }

    @BeforeAll
    static void init() {
        System.setProperty("fhir.pageCount", "10");
    }

    @Test
    public void test_measure_1() throws IOException {
        var measure = parser.parseResource(Measure.class, slurpMeasure("src/test/resources/FhirDataEvaluatorTest/Measures/measure-1.json"));

        var reportResult = measureEvaluator.evaluateMeasure(measure).block();

        assertThat(getStratumByKey(reportResult.getGroup().get(0).getStratifier().get(0).getStratum(), Set.of(I60))
                .getPopulation().get(0).getCount())
                .isEqualTo(100);
    }

    @Test
    public void test_measure_2() throws IOException {
        var measure = parser.parseResource(Measure.class, slurpMeasure("src/test/resources/FhirDataEvaluatorTest/Measures/measure-2.json"));

        var reportResult = measureEvaluator.evaluateMeasure(measure).block();
        assertThat(getStratumByKey(reportResult.getGroup().get(0).getStratifier().get(0).getStratum(), Set.of(I60, ACTIVE))
                .getPopulation().get(0).getCount())
                .isEqualTo(100);
    }


    private static String slurpMeasure(String measurePath) throws IOException {
        return Files.readString(Path.of(measurePath));
    }

    private MeasureReport.StratifierGroupComponent getStratumByKey(List<MeasureReport.StratifierGroupComponent> strati, Set<StratumComponent> keySet) {
        for (MeasureReport.StratifierGroupComponent stratum : strati) {
            if (stratum.hasValue()) {
                if (HashableCoding.ofFhirCoding(stratum.getValue().getCodingFirstRep()).equals(keySet.iterator().next().value())) {
                    return stratum;
                }
            } else {
                if (stratum.getComponent().stream().map(component ->
                                new StratumComponent(
                                        HashableCoding.ofFhirCoding(component.getCode().getCodingFirstRep()),
                                        HashableCoding.ofFhirCoding(component.getValue().getCodingFirstRep()))).collect(Collectors.toSet()).
                        equals(keySet)) {
                    return stratum;
                }
            }
        }
        return null;
    }

}
