package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;

import static de.medizininformatikinitiative.fhir_data_evaluator.GroupEvaluatorTest.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MeasureEvaluatorUnitTest {
    static final FHIRPathEngine pathEngine = createPathEngine();
    @Mock
    DataStore dataStore;

    private void assertCodeableConcept(CodeableConcept was, String expectedSystem, String expectedCode) {
        assertThat(was.getCodingFirstRep().getSystem()).isEqualTo(expectedSystem);
        assertThat(was.getCodingFirstRep().getCode()).isEqualTo(expectedCode);
    }

    private void assertInitialPopulation(MeasureReport.MeasureReportGroupPopulationComponent reportPopulation) {
        assertThat(reportPopulation.getCount()).isEqualTo(1);
        assertCodeableConcept(reportPopulation.getCode(), INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE);
    }

    private void assertInitialPopulation(MeasureReport.StratifierGroupPopulationComponent reportPopulation) {
        assertThat(reportPopulation.getCount()).isEqualTo(1);
        assertCodeableConcept(reportPopulation.getCode(), INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE);
    }

    @Test
    void oneGroup_oneStratifier_ofOneComponent() {
        when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
        Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                .setStratifier(List.of(
                        new Measure.MeasureGroupStratifierComponent().setComponent(List.of(
                                        new Measure.MeasureGroupStratifierComponentComponent()
                                                .setCriteria(COND_CODE_PATH)
                                                .setCode(new CodeableConcept(COND_DEF_CODING))))
                                .setCode(new CodeableConcept(COND_DEF_CODING))))
                .setPopulation(List.of(getInitialPopulation()));
        Measure measure = new Measure().setGroup(List.of(measureGroup));
        MeasureEvaluator measureEvaluator = new MeasureEvaluator(dataStore, pathEngine);

        var result = measureEvaluator.evaluateMeasure(measure).block();

        assertInitialPopulation(result.getGroup().get(0).getPopulation().get(0));
        assertInitialPopulation(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getPopulation().get(0));
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getCode().get(0), COND_DEF_SYSTEM, COND_DEF_CODE);
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getValue(), COND_VALUE_SYSTEM, COND_VALUE_CODE);
        // TODO is this wanted behavior or should it have a component instead of a value?
    }

    @Test
    void oneGroup_oneStratifier_ofTwoComponents() {
        when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                getCondition().setClinicalStatus(new CodeableConcept(new Coding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY))))));
        Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                .setStratifier(List.of(
                        new Measure.MeasureGroupStratifierComponent().setComponent(List.of(
                                        new Measure.MeasureGroupStratifierComponentComponent()
                                                .setCriteria(COND_CODE_PATH)
                                                .setCode(new CodeableConcept(COND_DEF_CODING)),
                                        new Measure.MeasureGroupStratifierComponentComponent()
                                                .setCriteria(COND_STATUS_PATH)
                                                .setCode(new CodeableConcept(STATUS_DEF_CODING))))
                                .setCode(new CodeableConcept(COND_DEF_CODING))))
                .setPopulation(List.of(getInitialPopulation()));
        Measure measure = new Measure().setGroup(List.of(measureGroup));
        MeasureEvaluator measureEvaluator = new MeasureEvaluator(dataStore, pathEngine);

        var result = measureEvaluator.evaluateMeasure(measure).block();

        assertInitialPopulation(result.getGroup().get(0).getPopulation().get(0));
        assertInitialPopulation(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getPopulation().get(0));
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getCode().get(0), COND_DEF_SYSTEM, COND_DEF_CODE);
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getComponent().get(0).getCode(), COND_DEF_SYSTEM, COND_DEF_CODE);
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getComponent().get(0).getValue(), COND_VALUE_SYSTEM, COND_VALUE_CODE);
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getComponent().get(1).getCode(), STATUS_DEF_SYSTEM, STATUS_DEF_CODE);
        assertCodeableConcept(result.getGroup().get(0).getStratifier().get(0).getStratum().get(0).getComponent().get(1).getValue(), STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE);
    }

    @Test
    void twoGroups_sameStratifier() {
        when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
        Measure.MeasureGroupComponent measureGroup_1 = getMeasureGroup()
                .setStratifier(List.of(
                        new Measure.MeasureGroupStratifierComponent()
                                .setCriteria(COND_CODE_PATH)
                                .setCode(new CodeableConcept(COND_DEF_CODING))))
                .setPopulation(List.of(getInitialPopulation()));
        Measure.MeasureGroupComponent measureGroup_2 = getMeasureGroup()
                .setStratifier(List.of(
                        new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                .setPopulation(List.of(getInitialPopulation()));
        Measure measure = new Measure().setGroup(List.of(measureGroup_1, measureGroup_2));
        MeasureEvaluator measureEvaluator = new MeasureEvaluator(dataStore, pathEngine);

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
