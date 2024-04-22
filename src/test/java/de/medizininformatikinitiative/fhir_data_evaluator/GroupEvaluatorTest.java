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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupEvaluatorTest {

    static final String POPULATION_QUERY = "Condition?_profile=https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose";
    static final Expression COND_CODE_PATH = expressionOfExpression("Condition.code.coding");
    static final Expression COND_STATUS_PATH = expressionOfExpression("Condition.clinicalStatus.coding");
    static final String COND_VALUE_CODE = "cond-value-code";
    static final String COND_VALUE_SYSTEM = "http://fhir.de/CodeSystem/bfarm/icd-10-gm";
    static final String COND_DEF_CODE = "cond-def-code";
    static final String COND_DEF_SYSTEM = "cond-def-sys";
    static final String SOME_DISPLAY = "some-display";
    public static final Coding COND_DEF_CODING = new Coding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY);
    static final String STATUS_VALUE_CODE = "active";
    static final String STATUS_VALUE_SYSTEM = "http://terminology.hl7.org/CodeSystem/condition-clinical";
    static final String STATUS_DEF_CODE = "clinical-status";
    static final String STATUS_DEF_SYSTEM = "http://fhir-evaluator/strat/system";
    public static final Coding STATUS_DEF_CODING = new Coding(STATUS_DEF_SYSTEM, STATUS_DEF_CODE, SOME_DISPLAY);
    public static final ComponentKeyPair COND_VALUE_KEYPAIR = new ComponentKeyPair(
            HashableCoding.ofFhirCoding(COND_DEF_CODING),
            new HashableCoding(COND_VALUE_SYSTEM, COND_VALUE_CODE, SOME_DISPLAY));
    public static final ComponentKeyPair STATUS_VALUE_KEYPAIR = new ComponentKeyPair(
            new HashableCoding(STATUS_DEF_SYSTEM, STATUS_DEF_CODE, SOME_DISPLAY),
            new HashableCoding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY));
    static final String INITIAL_POPULATION_CODE = "initial-population";
    static final String INITIAL_POPULATION_SYSTEM = "http://terminology.hl7.org/CodeSystem/measure-population";
    public static final HashableCoding INITIAL_POPULATION_CODING = new HashableCoding(INITIAL_POPULATION_SYSTEM, INITIAL_POPULATION_CODE, SOME_DISPLAY);
    static final FHIRPathEngine pathEngine = createPathEngine();
    public static final String INITIAL_POPULATION_LANGUAGE = "text/x-fhir-query";
    public static final String STRATIFIER_LANGUAGE = "text/fhirpath";

    @Mock
    DataStore dataStore;

    private static Expression expressionOfExpression(String expStr) {
        Expression expression = new Expression();
        return expression.setExpression(expStr).setLanguage(STRATIFIER_LANGUAGE);
    }

    public static Condition getCondition() {
        Coding condCoding = new Coding().setSystem(COND_VALUE_SYSTEM).setCode(COND_VALUE_CODE);
        CodeableConcept condConcept = new CodeableConcept().addCoding(condCoding);

        return new Condition().setCode(condConcept);
    }

    public static FHIRPathEngine createPathEngine() {
        final FhirContext context = FhirContext.forR4();
        final DefaultProfileValidationSupport validation = new DefaultProfileValidationSupport(context);
        final IWorkerContext worker = new HapiWorkerContext(context, validation);
        return new FHIRPathEngine(worker);
    }

    public static Measure.MeasureGroupPopulationComponent getInitialPopulation() {
        return new Measure.MeasureGroupPopulationComponent()
                .setCriteria(new Expression().setExpression(POPULATION_QUERY).setLanguage(INITIAL_POPULATION_LANGUAGE))
                .setCode(new CodeableConcept(new Coding().setSystem(INITIAL_POPULATION_SYSTEM).setCode(INITIAL_POPULATION_CODE)));
    }

    public static Measure.MeasureGroupComponent getMeasureGroup() {
        Measure.MeasureGroupComponent measureGroup = new Measure.MeasureGroupComponent();

        return measureGroup.setPopulation(List.of(getInitialPopulation()));
    }

    @Nested
    class ExceptionTests {
        @Test
        public void test_twoCodingsInPopulation() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setPopulation(List.of(
                    getInitialPopulation().setCode(new CodeableConcept()
                            .addCoding(new Coding().setSystem(INITIAL_POPULATION_SYSTEM).setCode(INITIAL_POPULATION_CODE))
                            .addCoding(new Coding().setSystem(INITIAL_POPULATION_SYSTEM).setCode(INITIAL_POPULATION_CODE)))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Population in Measure did not contain exactly one Coding");
        }

        @Test
        public void test_twoInitialPopulations() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setPopulation(List.of(getInitialPopulation(), getInitialPopulation()));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Measure did not contain exactly one initial population");
        }

        @Test
        public void test_noInitialPopulations() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setPopulation(List.of());
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Measure did not contain exactly one initial population");
        }

        @Test
        public void test_wrongInitialPopulationLanguage() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setPopulation(List.of(
                    getInitialPopulation()
                            .setCriteria(new Expression().setExpressionElement(new StringType(POPULATION_QUERY))
                                    .setLanguage("some-wrong-language"))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Language of Initial Population was not equal to '%s'".formatted(INITIAL_POPULATION_LANGUAGE));
        }

        @Test
        public void test_stratifierWithoutCoding() {
            when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH)))
                    .setCode(null)
                    .setPopulation(List.of(getInitialPopulation()));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Stratifier did not contain exactly one coding");
        }

        @Test
        public void test_stratifierWithMultipleCodings() {
            when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH)))
                    .setCode(new CodeableConcept()
                            .addCoding(COND_DEF_CODING)
                            .addCoding(COND_DEF_CODING))
                    .setPopulation(List.of(getInitialPopulation()));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Stratifier did not contain exactly one coding");
        }

        @Test
        public void test_stratifierWithWrongLanguage() {
            when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(new Measure.MeasureGroupStratifierComponent().setCriteria(expressionOfExpression(COND_CODE_PATH.getExpression()).setLanguage("some-other-language"))))
                    .setCode(new CodeableConcept(COND_DEF_CODING))
                    .setPopulation(List.of(getInitialPopulation()));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Stratifier did not contain exactly one coding");
        }

        @Test
        public void test_componentWithoutCoding() {
            when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of
                            (new Measure.MeasureGroupStratifierComponent()
                                    .setComponent(List.of(new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(null)))
                                    .setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(getInitialPopulation()));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Stratifier component did not contain exactly one coding");
        }

        @Test
        public void test_componentWithMultipleCodings() {
            when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent()
                                    .setComponent(List.of(new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(
                                            new CodeableConcept()
                                                    .addCoding(COND_DEF_CODING)
                                                    .addCoding(COND_DEF_CODING))))
                                    .setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(getInitialPopulation()));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Stratifier component did not contain exactly one coding");
        }

        @Test
        public void test_componentWithWrongLanguage() {
            when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setComponent(List.of(
                                            new Measure.MeasureGroupStratifierComponentComponent(expressionOfExpression(COND_CODE_PATH.getExpression()).setLanguage("some-other-language"))
                                                    .setCode(new CodeableConcept().addCoding(COND_DEF_CODING))))
                                    .setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(getInitialPopulation()));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Language of stratifier component was not equal to '%s'".formatted(STRATIFIER_LANGUAGE));
        }
    }


    @Nested
    class StratifierOfSingleCriteria {
        @Nested
        class SingleStratifierInGroup_withSingleCriteria {

            @Test
            public void test_oneStratifierElement_oneResultValue_ignoreOtherPopulations() {
                when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setStratifier(List.of(new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH)
                                .setCode(new CodeableConcept(COND_DEF_CODING))))
                        .setPopulation(List.of(
                                getInitialPopulation(),
                                getInitialPopulation().setCode(new CodeableConcept(new Coding().setCode("some-other-population").setSystem("some-system")))));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                assertThat((result.stratifierResults().get(0)))
                        .isEqualTo(
                                new StratifierResult(Map.of(
                                        Set.of(COND_VALUE_KEYPAIR),
                                        new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                        HashableCoding.ofFhirCoding(COND_DEF_CODING)));
            }

            @Test
            public void test_oneStratifierElement_oneResultValue() {
                when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                        .setStratifier(List.of(
                                new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                        .setPopulation(List.of(getInitialPopulation()));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                assertThat((result.stratifierResults().get(0)))
                        .isEqualTo(
                                new StratifierResult(Map.of(
                                        Set.of(COND_VALUE_KEYPAIR),
                                        new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                        HashableCoding.ofFhirCoding(COND_DEF_CODING)));
            }

            @Test
            public void test_oneStratifierElement_twoSameResultValues() {
                when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                        getCondition(),
                        getCondition())));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                        .setStratifier(List.of(
                                new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                        .setPopulation(List.of(getInitialPopulation()));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(2);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                assertThat(result.stratifierResults().get(0))
                        .isEqualTo(
                                new StratifierResult(Map.of(
                                        Set.of(COND_VALUE_KEYPAIR),
                                        new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 2))),
                                        HashableCoding.ofFhirCoding(COND_DEF_CODING)));

            }

            @Test
            public void test_oneStratifierElement_twoDifferentResultValues() {
                when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                        getCondition(),
                        getCondition().setCode(new CodeableConcept(new Coding().setSystem(COND_VALUE_SYSTEM).setCode("some-other-value"))))));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                        .setStratifier(List.of(
                                new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                        .setPopulation(List.of(getInitialPopulation()));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(2);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                assertThat(result.stratifierResults().get(0))
                        .isEqualTo(
                                new StratifierResult(Map.of(
                                        Set.of(COND_VALUE_KEYPAIR),
                                        new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1)),
                                        Set.of(new ComponentKeyPair(
                                                HashableCoding.ofFhirCoding(COND_DEF_CODING),
                                                new HashableCoding(COND_VALUE_SYSTEM, "some-other-value", SOME_DISPLAY))),
                                        new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                        HashableCoding.ofFhirCoding(COND_DEF_CODING)));
            }

            @Nested
            class FailTests {
                @Test
                public void test_oneStratifierElement_noValue() {
                    when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(new Condition())));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation()));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                    assertThat((result.stratifierResults().get(0)))
                            .isEqualTo(
                                    new StratifierResult(Map.of(
                                            Set.of(ComponentKeyPair.ofFailedNoValueFound(COND_VALUE_KEYPAIR.definitionCode())),
                                            new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                            HashableCoding.ofFhirCoding(COND_DEF_CODING)));
                }

                @Test
                public void test_oneStratifierElement_tooManyValues() {
                    Coding condCoding = new Coding().setSystem(COND_VALUE_SYSTEM).setCode(COND_VALUE_CODE);
                    when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(new Condition().setCode(new CodeableConcept()
                            .addCoding(condCoding)
                            .addCoding(condCoding)))));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation()));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                    assertThat((result.stratifierResults().get(0)))
                            .isEqualTo(
                                    new StratifierResult(Map.of(
                                            Set.of(ComponentKeyPair.ofFailedTooManyValues(COND_VALUE_KEYPAIR.definitionCode())),
                                            new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                            HashableCoding.ofFhirCoding(COND_DEF_CODING)));
                }

                @Test
                public void test_oneStratifierElement_invalidType() {
                    when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(expressionOfExpression("Condition.code")).setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation()));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                    assertThat((result.stratifierResults().get(0)))
                            .isEqualTo(
                                    new StratifierResult(Map.of(
                                            Set.of(ComponentKeyPair.ofFailedInvalidType(COND_VALUE_KEYPAIR.definitionCode())),
                                            new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                            HashableCoding.ofFhirCoding(COND_DEF_CODING)));
                }

                @Test
                public void test_oneStratifierElement_missingSystem() {
                    when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(new Condition().setCode(new CodeableConcept(new Coding().setCode(COND_VALUE_CODE))))));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation()));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                    assertThat((result.stratifierResults().get(0)))
                            .isEqualTo(
                                    new StratifierResult(Map.of(
                                            Set.of(ComponentKeyPair.ofFailedMissingFields(COND_VALUE_KEYPAIR.definitionCode())),
                                            new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                            HashableCoding.ofFhirCoding(COND_DEF_CODING)));
                }

                @Test
                public void test_oneStratifierElement_missingCode() {
                    when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(new Condition().setCode(new CodeableConcept(new Coding().setSystem(COND_VALUE_SYSTEM))))));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation()));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                    assertThat((result.stratifierResults().get(0)))
                            .isEqualTo(
                                    new StratifierResult(Map.of(
                                            Set.of(ComponentKeyPair.ofFailedMissingFields(COND_VALUE_KEYPAIR.definitionCode())),
                                            new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                            HashableCoding.ofFhirCoding(COND_DEF_CODING)));
                }
            }
        }

        @Nested
        class MultipleStratifiersInGroup_withSingleCriteria {

            @Test
            public void test_twoSameStratifierElements() {
                when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                        .setStratifier(List.of(
                                new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                        .setPopulation(List.of(getInitialPopulation()));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(2);
                assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                assertThat(result.stratifierResults().get(1).counts()).isNotNull();
                assertThat(result.stratifierResults())
                        .containsExactly(
                                new StratifierResult(Map.of(
                                        Set.of(COND_VALUE_KEYPAIR),
                                        new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                        HashableCoding.ofFhirCoding(COND_DEF_CODING)),
                                new StratifierResult(Map.of(
                                        Set.of(COND_VALUE_KEYPAIR),
                                        new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                        HashableCoding.ofFhirCoding(COND_DEF_CODING)));
            }

            @Test
            public void test_twoStratifierElements_oneResultValueEach() {
                when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition()
                        .setClinicalStatus(new CodeableConcept(new Coding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY))))));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                        .setStratifier(List.of(
                                new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                new Measure.MeasureGroupStratifierComponent().setCriteria(COND_STATUS_PATH).setCode(new CodeableConcept(STATUS_DEF_CODING))))
                        .setPopulation(List.of(getInitialPopulation()));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(2);
                assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                assertThat(result.stratifierResults().get(1).counts()).isNotNull();
                assertThat(result.stratifierResults())
                        .containsExactly(
                                new StratifierResult(Map.of(Set.of(COND_VALUE_KEYPAIR),
                                        new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                        HashableCoding.ofFhirCoding(COND_DEF_CODING)),
                                new StratifierResult(Map.of(
                                        Set.of(STATUS_VALUE_KEYPAIR),
                                        new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                        HashableCoding.ofFhirCoding(STATUS_DEF_CODING)));
            }
        }

    }

    @Nested
    class StratifierOfMultipleComponents {
        @Nested
        class SingleStratifierInGroup {
            @Test
            public void test_oneStratifierElement_twoDifferentComponents_oneDifferentResultValueEach() {
                when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                        getCondition().setClinicalStatus(new CodeableConcept(new Coding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY))))));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                        .setStratifier(List.of(
                                new Measure.MeasureGroupStratifierComponent()
                                        .setComponent(List.of(
                                                new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                                new Measure.MeasureGroupStratifierComponentComponent(COND_STATUS_PATH).setCode(new CodeableConcept(STATUS_DEF_CODING))))
                                        .setCode(new CodeableConcept(COND_DEF_CODING))))
                        .setPopulation(List.of(getInitialPopulation()));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                assertThat((result.stratifierResults().get(0)))
                        .isEqualTo(
                                new StratifierResult(Map.of(
                                        Set.of(COND_VALUE_KEYPAIR, STATUS_VALUE_KEYPAIR),
                                        new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                        HashableCoding.ofFhirCoding(COND_DEF_CODING)));
            }

            @Test
            public void test_oneStratifierElement_twoDifferentComponents_oneSameResultValueEach() {
                when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                        .setStratifier(List.of(
                                new Measure.MeasureGroupStratifierComponent()
                                        .setComponent(List.of(
                                                new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                                new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(new CodeableConcept(new Coding(COND_DEF_SYSTEM, "some-other-code", SOME_DISPLAY)))))
                                        .setCode(new CodeableConcept(COND_DEF_CODING))))
                        .setPopulation(List.of(getInitialPopulation()));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                assertThat((result.stratifierResults().get(0)))
                        .isEqualTo(
                                new StratifierResult(Map.of(
                                        Set.of(
                                                COND_VALUE_KEYPAIR,
                                                new ComponentKeyPair(
                                                        new HashableCoding(COND_DEF_SYSTEM, "some-other-code", SOME_DISPLAY),
                                                        COND_VALUE_KEYPAIR.valueCode())),
                                        new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                        HashableCoding.ofFhirCoding(COND_DEF_CODING)));
            }

            @Test
            public void test_oneStratifierElement_twoSameComponents() { // TODO this is actually undefined behaviour I think
                when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                        .setStratifier(List.of(
                                new Measure.MeasureGroupStratifierComponent()
                                        .setComponent(List.of(
                                                new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                                new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                                        .setCode(new CodeableConcept(COND_DEF_CODING))))
                        .setPopulation(List.of(getInitialPopulation()));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).counts()).isNotNull();
                assertThat((result.stratifierResults().get(0)))
                        .isEqualTo(
                                new StratifierResult(Map.of(
                                        Set.of(COND_VALUE_KEYPAIR),
                                        new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                        HashableCoding.ofFhirCoding(COND_DEF_CODING)));
            }

            @Test
            public void test_oneStratifierElement_twoDifferentComponents_twoDifferentResultValuesEach() {
                final CodeableConcept condCoding1 = new CodeableConcept().addCoding(new Coding().setSystem(COND_VALUE_SYSTEM).setCode("cond-code-value-1"));
                final CodeableConcept condCoding2 = new CodeableConcept().addCoding(new Coding().setSystem(COND_VALUE_SYSTEM).setCode("cond-code-value-2"));
                final CodeableConcept statusCoding2 = new CodeableConcept().addCoding(new Coding().setSystem(STATUS_VALUE_SYSTEM).setCode("status-value-1"));
                final CodeableConcept statusCoding1 = new CodeableConcept().addCoding(new Coding().setSystem(STATUS_VALUE_SYSTEM).setCode("status-value-2"));
                final ComponentKeyPair condValueKeypair_1 = new ComponentKeyPair(
                        new HashableCoding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY),
                        new HashableCoding(COND_VALUE_SYSTEM, "cond-code-value-1", SOME_DISPLAY));
                final ComponentKeyPair condValueKeypair_2 = new ComponentKeyPair(
                        new HashableCoding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY),
                        new HashableCoding(COND_VALUE_SYSTEM, "cond-code-value-2", SOME_DISPLAY));
                final ComponentKeyPair statusValueKeypair_1 = new ComponentKeyPair(
                        new HashableCoding(STATUS_DEF_SYSTEM, STATUS_DEF_CODE, SOME_DISPLAY),
                        new HashableCoding(STATUS_VALUE_SYSTEM, "status-value-1", SOME_DISPLAY));
                final ComponentKeyPair statusValueKeypair_2 = new ComponentKeyPair(
                        new HashableCoding(STATUS_DEF_SYSTEM, STATUS_DEF_CODE, SOME_DISPLAY),
                        new HashableCoding(STATUS_VALUE_SYSTEM, "status-value-2", SOME_DISPLAY));

                when(dataStore.getPopulation("/" + POPULATION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                        getCondition().setCode(condCoding1).setClinicalStatus(statusCoding1),
                        getCondition().setCode(condCoding1).setClinicalStatus(statusCoding2),
                        getCondition().setCode(condCoding2).setClinicalStatus(statusCoding1),
                        getCondition().setCode(condCoding2).setClinicalStatus(statusCoding2))));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                        .setStratifier(List.of(
                                new Measure.MeasureGroupStratifierComponent()
                                        .setComponent(List.of(
                                                new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                                new Measure.MeasureGroupStratifierComponentComponent(COND_STATUS_PATH).setCode(new CodeableConcept(STATUS_DEF_CODING))))
                                        .setCode(new CodeableConcept(COND_DEF_CODING))))
                        .setPopulation(List.of(getInitialPopulation()));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populationsCount().initialPopulation().count()).isEqualTo(4);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).counts()).isNotNull();

                assertThat((result.stratifierResults().get(0)))
                        .isEqualTo(
                                new StratifierResult(Map.of(
                                        Set.of(condValueKeypair_1, statusValueKeypair_1), new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1)),
                                        Set.of(condValueKeypair_1, statusValueKeypair_2), new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1)),
                                        Set.of(condValueKeypair_2, statusValueKeypair_1), new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1)),
                                        Set.of(condValueKeypair_2, statusValueKeypair_2), new PopulationsCount(new PopulationCount(INITIAL_POPULATION_CODING, 1))),
                                        HashableCoding.ofFhirCoding(COND_DEF_CODING)));
            }


        }

    }

}
