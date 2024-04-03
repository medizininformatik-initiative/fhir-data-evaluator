package de.medizininformatikinitiative.fhir_data_evaluator;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupEvaluatorTest {

    static final String POPULATION_QUERY = "Condition?_profile=https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose";
    static final Expression COND_CODE_EXP = expressionOfExpression("Condition.code.coding.where(system='http://fhir.de/CodeSystem/bfarm/icd-10-gm')");
    static final Expression COND_STATUS_EXP = expressionOfExpression("Condition.clinicalStatus.coding");
    static final String COND_CODING = "cond_coding";
    static final String COND_SYSTEM = "http://fhir.de/CodeSystem/bfarm/icd-10-gm";
    static final String COND_DEF_CODE = "cond-def-code";
    static final String COND_DEF_SYSTEM = "cond-def-sys";
    static final String SOME_DISPLAY = "some-display";
    static final String STATUS_VALUE_CODE = "active";
    static final String STATUS_VALUE_SYSTEM = "http://terminology.hl7.org/CodeSystem/condition-clinical";
    static final String STATUS_DEF_CODE = "clinical-status";
    static final String STATUS_DEF_SYSTEM = "http://fhir-evaluator/strat/system";
    static final String INITIAL_POPULATION_CODE = "initial-population";
    static final String INITIAL_POPULATION_SYSTEM = "population-system";
    static final FHIRPathEngine pathEngine = createPathEngine();

    @Mock
    DataStore dataStore;

    private static Measure.MeasureGroupStratifierComponent getStratifier() {
        return new Measure.MeasureGroupStratifierComponent();
    }

    private static Expression expressionOfExpression(String expStr) {
        Expression expression = new Expression();
        return expression.setExpression(expStr);
    }

    private static Condition getCondition() {
        Coding condCoding = new Coding().setSystem(COND_SYSTEM).setCode(COND_CODING);
        CodeableConcept condConcept = new CodeableConcept().addCoding(condCoding);

        return new Condition().setCode(condConcept);
    }

    private static ComponentKeyPair getStratifierKey(String defCode, String defSystem, String valueCode, String valueSystem) {
        return new ComponentKeyPair(
                new HashableCoding(defSystem, defCode, SOME_DISPLAY),
                new HashableCoding(valueSystem, valueCode, SOME_DISPLAY));
    }

    private static FHIRPathEngine createPathEngine() {
        final FhirContext context = FhirContext.forR4();
        final DefaultProfileValidationSupport validation = new DefaultProfileValidationSupport(context);
        final IWorkerContext worker = new HapiWorkerContext(context, validation);
        return new FHIRPathEngine(worker);
    }

    private Measure.MeasureGroupComponent getMeasureGroup() {
        Measure.MeasureGroupComponent measureGroup = new Measure.MeasureGroupComponent();
        Measure.MeasureGroupPopulationComponent population = new Measure.MeasureGroupPopulationComponent()
                .setCriteria(new Expression().setExpression(POPULATION_QUERY));

        return measureGroup.setPopulation(List.of(population));
    }

    @Nested
    class StratifierOfSingleCriteria {
        @Nested
        class SingleStratifierInGroup {
            @Test
            public void test_oneStratifierElement_oneResultValue() {
                when(dataStore.getPopulation(POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setStratifier(List.of(getStratifier().setCriteria(COND_CODE_EXP)
                                .setCode(new CodeableConcept(new Coding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY))))).setPopulation(List.of())
                        .setPopulation(List.of(new Measure.MeasureGroupPopulationComponent()
                                .setCode(new CodeableConcept(new Coding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY)))
                                .setCriteria(new Expression().setExpressionElement(new StringType(POPULATION_QUERY)))));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).counts()).isPresent();
                assertThat((result.stratifierResults().get(0)))
                        .isEqualTo(new StratifierResult(
                                Optional.of(Map.of(Set.of(getStratifierKey(COND_DEF_CODE, COND_DEF_SYSTEM, COND_CODING, COND_SYSTEM)),
                                        new PopulationsCount(new PopulationCount(new HashableCoding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY), 1)))),
                                new HashableCoding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY)));
            }

            @Test
            public void test_oneStratifierElement_twoSameResultValues() {
                when(dataStore.getPopulation(POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition(), getCondition())));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setStratifier(List.of(getStratifier().setCriteria(COND_CODE_EXP)
                                .setCode(new CodeableConcept(new Coding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY)))))
                        .setPopulation(List.of(new Measure.MeasureGroupPopulationComponent()
                                .setCode(new CodeableConcept(new Coding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY)))
                                .setCriteria(new Expression().setExpressionElement(new StringType(POPULATION_QUERY)))));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(2);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).counts()).isPresent();
                assertThat(result.stratifierResults().get(0)).isEqualTo(
                        new StratifierResult(Optional.of(Map.of(
                                Set.of(getStratifierKey(COND_DEF_CODE, COND_DEF_SYSTEM, COND_CODING, COND_SYSTEM)),
                                new PopulationsCount(new PopulationCount(new HashableCoding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY), 2)))),
                                new HashableCoding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY)));

            }

            @Test
            public void test_oneStratifierElement_twoDifferentResultValues() {
                //TODO
            }

            @Nested
            class SomeResultsNotPresent {
                //TODO
            }
        }

        @Nested
        class MultileStratifiersInGroup {

            @Test
            public void test_twoSameStratifierElements() {
                when(dataStore.getPopulation(POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setStratifier(List.of(
                                getStratifier().setCriteria(COND_CODE_EXP).setCode(new CodeableConcept(new Coding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY))),
                                getStratifier().setCriteria(COND_CODE_EXP).setCode(new CodeableConcept(new Coding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY)))))
                        .setPopulation(List.of(new Measure.MeasureGroupPopulationComponent()
                                .setCode(new CodeableConcept(new Coding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY)))
                                .setCriteria(new Expression().setExpressionElement(new StringType(POPULATION_QUERY)))));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(2);
                assertThat(result.stratifierResults().get(0).counts()).isPresent();
                assertThat(result.stratifierResults().get(1).counts()).isPresent();
                assertThat(result.stratifierResults()).containsExactly(
                        new StratifierResult(Optional.of(Map.of(
                                Set.of(getStratifierKey(COND_DEF_CODE, COND_DEF_SYSTEM, COND_CODING, COND_SYSTEM)),
                                new PopulationsCount(new PopulationCount(new HashableCoding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY), 1)))),
                                new HashableCoding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY)),
                        new StratifierResult(Optional.of(Map.of(
                                Set.of(getStratifierKey(COND_DEF_CODE, COND_DEF_SYSTEM, COND_CODING, COND_SYSTEM)),
                                new PopulationsCount(new PopulationCount(new HashableCoding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY), 1)))),
                                new HashableCoding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY)));
            }

            @Test
            public void test_twoStratifierElements_oneResultValueEach() {
                when(dataStore.getPopulation(POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition()
                        .setClinicalStatus(new CodeableConcept(new Coding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY))))));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setStratifier(List.of(
                                getStratifier().setCriteria(COND_CODE_EXP).setCode(new CodeableConcept(new Coding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY))),
                                getStratifier().setCriteria(COND_STATUS_EXP).setCode(new CodeableConcept(new Coding(STATUS_DEF_SYSTEM, STATUS_DEF_CODE, SOME_DISPLAY)))))
                        .setPopulation(List.of(new Measure.MeasureGroupPopulationComponent()
                                .setCode(new CodeableConcept(new Coding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY)))
                                .setCriteria(new Expression().setExpressionElement(new StringType(POPULATION_QUERY)))));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(2);
                assertThat(result.stratifierResults().get(0).counts()).isPresent();
                assertThat(result.stratifierResults().get(1).counts()).isPresent();
                assertThat(result.stratifierResults()).containsExactly(
                        new StratifierResult(Optional.of(Map.of(
                                Set.of(getStratifierKey(COND_DEF_CODE, COND_DEF_SYSTEM, COND_CODING, COND_SYSTEM)),
                                new PopulationsCount(new PopulationCount(new HashableCoding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY), 1)))),
                                new HashableCoding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY)),
                        new StratifierResult(Optional.of(Map.of(
                                Set.of(getStratifierKey(STATUS_DEF_CODE, STATUS_DEF_SYSTEM, STATUS_VALUE_CODE, STATUS_VALUE_SYSTEM)),
                                new PopulationsCount(new PopulationCount(new HashableCoding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY), 1)))),
                                new HashableCoding(STATUS_DEF_SYSTEM, STATUS_DEF_CODE, SOME_DISPLAY)));
            }
        }

    }

    @Nested
    class StratifierOfMultipleComponents {
        @Nested
        class SingleStratifierInGroup {
            @Test
            public void test_oneStratifierElement_twoComponents_oneResultValue() {
                when(dataStore.getPopulation(POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition()
                        .setClinicalStatus(new CodeableConcept(new Coding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY))))));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setStratifier(List.of(getStratifier().setComponent(List.of(
                                        new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_EXP).setCode(
                                                new CodeableConcept(new Coding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY))),
                                        new Measure.MeasureGroupStratifierComponentComponent(COND_STATUS_EXP).setCode(
                                                new CodeableConcept(new Coding(STATUS_DEF_SYSTEM, STATUS_DEF_CODE, SOME_DISPLAY)))))
                                .setCode(new CodeableConcept(new Coding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY)))))
                        .setPopulation(List.of(new Measure.MeasureGroupPopulationComponent()
                                .setCode(new CodeableConcept(new Coding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY)))
                                .setCriteria(new Expression().setExpressionElement(new StringType(POPULATION_QUERY)))));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).counts()).isPresent();
                assertThat((result.stratifierResults().get(0))).isEqualTo(
                        new StratifierResult(Optional.of(Map.of(
                                Set.of(getStratifierKey(COND_DEF_CODE, COND_DEF_SYSTEM, COND_CODING, COND_SYSTEM),
                                        getStratifierKey(STATUS_DEF_CODE, STATUS_DEF_SYSTEM, STATUS_VALUE_CODE, STATUS_VALUE_SYSTEM)),
                                new PopulationsCount(new PopulationCount(new HashableCoding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY), 1)))),
                                new HashableCoding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY)));
            }

            @Test
            public void test_oneStratifierElement_twoComponents_twoSameResultValues() {
                //TODO
            }

            @Test
            public void test_oneStratifierElement_twoComponents_twoDifferentResultValues() {
                //TODO
            }


            @Nested
            class SomeResultsNotPresent {
                //TODO
            }
        }

        @Nested
        class MultipleStratifiersInGroup {
            //TODO

            @Nested
            class SomeResultsNotPresent {
                //TODO
            }
        }

    }

}
