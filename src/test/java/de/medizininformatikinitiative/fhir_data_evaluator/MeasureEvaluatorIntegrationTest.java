package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
            new HashableCoding("http://fhir-data-evaluator/strat/system", "icd10-code", "some-display"),
            new HashableCoding("http://fhir.de/CodeSystem/bfarm/icd-10-gm", "I60.1", "some-display"));
    public static final StratumComponent ACTIVE = new StratumComponent(
            new HashableCoding("http://fhir-data-evaluator/strat/system", "condition-clinical-status", "some-display"),
            new HashableCoding("http://terminology.hl7.org/CodeSystem/condition-clinical", "active", "some-display"));

    static final StratumComponent G_DL = new StratumComponent(
            new HashableCoding("http://fhir-data-evaluator/strat/system", "observation-value-code", "some-display"),
            HashableCoding.ofSingleCodeValue("g/dL"));
    static final StratumComponent NG_ML = new StratumComponent(
            new HashableCoding("http://fhir-data-evaluator/strat/system", "observation-value-code", "some-display"),
            HashableCoding.ofSingleCodeValue("ng/mL"));
    static final StratumComponent COMPARATOR_GT = new StratumComponent(
            new HashableCoding("http://fhir-data-evaluator/strat/system", "observation-value-comparator", "some-display"),
            HashableCoding.ofSingleCodeValue(">"));
    static final StratumComponent EXISTS_TRUE = new StratumComponent(
            new HashableCoding("http://fhir-data-evaluator/strat/system", "observation-value-code-exists", "some-display"),
            HashableCoding.ofSingleCodeValue("true"));
    static final StratumComponent EXISTS_FALSE = new StratumComponent(
            new HashableCoding("http://fhir-data-evaluator/strat/system", "observation-value-code-exists", "some-display"),
            HashableCoding.ofSingleCodeValue("false"));

    static final String measure1 = "src/test/resources/de/medizininformatikinitiative/fhir_data_evaluator/FhirDataEvaluatorTest/Measures/Measure-IntegrationTest-Measure-1.json";
    static final String measure2 = "src/test/resources/de/medizininformatikinitiative/fhir_data_evaluator/FhirDataEvaluatorTest/Measures/Measure-IntegrationTest-Measure-2.json";
    static final String measure3_1 = "src/test/resources/de/medizininformatikinitiative/fhir_data_evaluator/FhirDataEvaluatorTest/Measures/Measure-IntegrationTest-Measure-3-1.json";
    static final String measure3_2 = "src/test/resources/de/medizininformatikinitiative/fhir_data_evaluator/FhirDataEvaluatorTest/Measures/Measure-IntegrationTest-Measure-3-2.json";
    static final String measure4 = "src/test/resources/de/medizininformatikinitiative/fhir_data_evaluator/FhirDataEvaluatorTest/Measures/Measure-IntegrationTest-Measure-4.json";

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
                    .bodyValue(Files.readString(Path.of("src/test/resources/de/medizininformatikinitiative/fhir_data_evaluator/FhirDataEvaluatorTest/Bundle.json")))
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
    @DisplayName("Test Condition with single criteria")
    public void test_measure_1() throws IOException {
        var measure = parser.parseResource(Measure.class, slurpMeasure(measure1));

        var reportResult = measureEvaluator.evaluateMeasure(measure).block();

        assertThat(getCodingStratumByKey(reportResult.getGroup().get(0).getStratifier().get(0).getStratum(), Set.of(I60))
                .getPopulation().get(0).getCount())
                .isEqualTo(100);
    }

    @Test
    @DisplayName("Test Condition with components")
    public void test_measure_2() throws IOException {
        var measure = parser.parseResource(Measure.class, slurpMeasure(measure2));

        var reportResult = measureEvaluator.evaluateMeasure(measure).block();
        assertThat(getCodingStratumByKey(reportResult.getGroup().get(0).getStratifier().get(0).getStratum(), Set.of(I60, ACTIVE))
                .getPopulation().get(0).getCount())
                .isEqualTo(100);
    }

    @Test
    @DisplayName("Test Observation value code of type CodeType")
    public void test_measure_3_1() throws IOException {
        var measure = parser.parseResource(Measure.class, slurpMeasure(measure3_1));

        var reportResult = measureEvaluator.evaluateMeasure(measure).block();

        assertThat(getCodeStratumByKey(reportResult.getGroup().get(0).getStratifier().get(0).getStratum(), Set.of(G_DL))
                .getPopulation().get(0).getCount())
                .isEqualTo(1);
        assertThat(getCodeStratumByKey(reportResult.getGroup().get(0).getStratifier().get(0).getStratum(), Set.of(NG_ML))
                .getPopulation().get(0).getCount())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Test Observation value code of type Enumeration")
    public void test_measure_3_2() throws IOException {
        var measure = parser.parseResource(Measure.class, slurpMeasure(measure3_2));

        var reportResult = measureEvaluator.evaluateMeasure(measure).block();

        assertThat(getCodeStratumByKey(reportResult.getGroup().get(0).getStratifier().get(0).getStratum(), Set.of(COMPARATOR_GT))
                .getPopulation().get(0).getCount())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("Test Observation value exists")
    public void test_measure_4() throws IOException {
        var measure = parser.parseResource(Measure.class, slurpMeasure(measure4));

        var reportResult = measureEvaluator.evaluateMeasure(measure).block();
        assertThat(getCodeStratumByKey(reportResult.getGroup().get(0).getStratifier().get(0).getStratum(), Set.of(EXISTS_TRUE))
                .getPopulation().get(0).getCount())
                .isEqualTo(2);
        assertThat(getCodeStratumByKey(reportResult.getGroup().get(0).getStratifier().get(0).getStratum(), Set.of(EXISTS_FALSE))
                .getPopulation().get(0).getCount())
                .isEqualTo(1);
    }


    private static String slurpMeasure(String measurePath) throws IOException {
        return Files.readString(Path.of(measurePath));
    }

    private MeasureReport.StratifierGroupComponent getCodingStratumByKey(List<MeasureReport.StratifierGroupComponent> strati, Set<StratumComponent> keySet) {
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

    private MeasureReport.StratifierGroupComponent getCodeStratumByKey(List<MeasureReport.StratifierGroupComponent> strati, Set<StratumComponent> keySet) {
        for (MeasureReport.StratifierGroupComponent stratum : strati) {
            if (stratum.hasValue()) {
                if (HashableCoding.ofSingleCodeValue(stratum.getValue().getCodingFirstRep().getCode()).equals(keySet.iterator().next().value())) {
                    return stratum;
                }
            } else {
                if (stratum.getComponent().stream().map(component ->
                                new StratumComponent(
                                        HashableCoding.ofFhirCoding(component.getCode().getCodingFirstRep()),
                                        HashableCoding.ofSingleCodeValue(component.getValue().getCodingFirstRep().getCode()))).collect(Collectors.toSet()).
                        equals(keySet)) {
                    return stratum;
                }
            }
        }
        return null;
    }

}
