package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.fhirpath.IFhirPath;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;

import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.CONDITION_QUERY;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.COND_CODE_PATH;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.COND_DEF_CODE;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.COND_DEF_CODING;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.COND_DEF_SYSTEM;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.COND_STATUS_PATH;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.COND_VALUE_CODE;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.COND_VALUE_SYSTEM;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.INITIAL_POPULATION_CODE;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.POPULATION_SYSTEM;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.SOME_DISPLAY;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.STATUS_DEF_CODE;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.STATUS_DEF_CODING;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.STATUS_DEF_SYSTEM;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.STATUS_VALUE_CODE;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.STATUS_VALUE_SYSTEM;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.createPathEngine;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.getCondition;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.getInitialPopulation;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.getMeasureGroup;
import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.wrapWithoutIncludes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MeasureEvaluatorUnitTest {

    @Mock
    DataStore dataStore;
    IFhirPath pathEngine;
    MeasureEvaluator measureEvaluator;

    @BeforeEach
    void setUp() {
        pathEngine = createPathEngine();
        measureEvaluator = new MeasureEvaluator(dataStore, pathEngine, 4);
    }

    private void assertCodeableConcept(CodeableConcept was, String expectedSystem, String expectedCode) {
        assertThat(was.getCodingFirstRep().getSystem()).isEqualTo(expectedSystem);
        assertThat(was.getCodingFirstRep().getCode()).isEqualTo(expectedCode);
    }

    private void assertInitialPopulation(MeasureReport.MeasureReportGroupPopulationComponent reportPopulation) {
        assertThat(reportPopulation.getCount()).isEqualTo(1);
        assertCodeableConcept(reportPopulation.getCode(), POPULATION_SYSTEM, INITIAL_POPULATION_CODE);
    }

    private void assertInitialPopulation(MeasureReport.StratifierGroupPopulationComponent reportPopulation) {
        assertThat(reportPopulation.getCount()).isEqualTo(1);
        assertCodeableConcept(reportPopulation.getCode(), POPULATION_SYSTEM, INITIAL_POPULATION_CODE);
    }

    @Test
    void oneGroup_oneStratifier_ofOneComponent() {
        when(dataStore.getResources("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(wrapWithoutIncludes(pathEngine, getCondition())));
        Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                .setStratifier(List.of(
                        new Measure.MeasureGroupStratifierComponent().setComponent(List.of(
                                        new Measure.MeasureGroupStratifierComponentComponent()
                                                .setCriteria(COND_CODE_PATH)
                                                .setCode(new CodeableConcept(COND_DEF_CODING))))
                                .setCode(new CodeableConcept(COND_DEF_CODING))))
                .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
        Measure measure = new Measure().setGroup(List.of(measureGroup));

        var result = measureEvaluator.evaluateMeasure(measure).block();

        assertInitialPopulation(result.getGroup().get(0).getPopulation().get(0));
        assertInitialPopulation(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getPopulation().get(0));
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getCode().get(0), COND_DEF_SYSTEM, COND_DEF_CODE);
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getValue(), COND_VALUE_SYSTEM, COND_VALUE_CODE);
        // TODO is this wanted behavior or should it have a component instead of a value?
    }

    @Test
    void oneGroup_oneStratifier_ofTwoComponents() {
        when(dataStore.getResources("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(wrapWithoutIncludes(pathEngine,
                getCondition().setClinicalStatus(new CodeableConcept(new Coding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY))))));
        Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                .setStratifier(List.of(
                        new Measure.MeasureGroupStratifierComponent().setComponent(List.of(
                                        new Measure.MeasureGroupStratifierComponentComponent()
                                                .setCriteria(COND_CODE_PATH)
                                                .setCode(new CodeableConcept(COND_DEF_CODING)),
                                        new Measure.MeasureGroupStratifierComponentComponent()
                                                .setCriteria(COND_STATUS_PATH)
                                                .setCode(new CodeableConcept(STATUS_DEF_CODING.toCoding()))))
                                .setCode(new CodeableConcept(COND_DEF_CODING))))
                .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
        Measure measure = new Measure().setGroup(List.of(measureGroup));

        var result = measureEvaluator.evaluateMeasure(measure).block();

        assertInitialPopulation(result.getGroup().get(0).getPopulation().get(0));
        assertInitialPopulation(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getPopulation().get(0));
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getComponent().get(0).getCode(), STATUS_DEF_SYSTEM, STATUS_DEF_CODE);
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getComponent().get(0).getValue(), STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE);
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getComponent().get(1).getCode(), COND_DEF_SYSTEM, COND_DEF_CODE);
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getComponent().get(1).getValue(), COND_VALUE_SYSTEM, COND_VALUE_CODE);
    }

    @Test
    void twoGroups_sameStratifier() {
        when(dataStore.getResources("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(wrapWithoutIncludes(pathEngine, getCondition())));
        Measure.MeasureGroupComponent measureGroup_1 = getMeasureGroup()
                .setStratifier(List.of(
                        new Measure.MeasureGroupStratifierComponent()
                                .setCriteria(COND_CODE_PATH)
                                .setCode(new CodeableConcept(COND_DEF_CODING))))
                .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
        Measure.MeasureGroupComponent measureGroup_2 = getMeasureGroup()
                .setStratifier(List.of(
                        new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
        Measure measure = new Measure().setGroup(List.of(measureGroup_1, measureGroup_2));

        var result = measureEvaluator.evaluateMeasure(measure).block();

        assertInitialPopulation(result.getGroup().get(0).getPopulation().get(0));
        assertInitialPopulation(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getPopulation().get(0));
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getCode().get(0), COND_DEF_SYSTEM, COND_DEF_CODE);
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getValue(), COND_VALUE_SYSTEM, COND_VALUE_CODE);
        assertInitialPopulation(result.getGroup().get(1).getPopulation().get(0));
        assertInitialPopulation(result.getGroup().get(1).getStratifier().get(0).getStratum().get(0).getPopulation().get(0));
        assertCodeableConcept(result.getGroup().get(1).getStratifier().get(0).getCode().get(0), COND_DEF_SYSTEM, COND_DEF_CODE);
        assertCodeableConcept(result.getGroup().get(1).getStratifier().get(0).getStratum().get(0).getValue(), COND_VALUE_SYSTEM, COND_VALUE_CODE);
    }
}
