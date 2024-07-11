package de.medizininformatikinitiative.fhir_data_evaluator;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.junit.jupiter.api.DisplayName;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupEvaluatorTest {

    static final String CONDITION_QUERY = "Condition?_profile=https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose";
    static final Expression COND_CODE_PATH = expressionOfPath("Condition.code.coding");
    static final Expression COND_STATUS_PATH = expressionOfPath("Condition.clinicalStatus.coding");
    static final String COND_VALUE_CODE = "cond-value-code";
    static final String COND_VALUE_CODE_1 = "cond-val-1";
    static final String COND_VALUE_CODE_2 = "cond-val-2";
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
    public static final StratumComponent COND_VALUE_KEYPAIR = new StratumComponent(
            HashableCoding.ofFhirCoding(COND_DEF_CODING),
            new HashableCoding(COND_VALUE_SYSTEM, COND_VALUE_CODE, SOME_DISPLAY));
    public static final StratumComponent COND_VALUE_KEYPAIR_1 = new StratumComponent(
            HashableCoding.ofFhirCoding(COND_DEF_CODING),
            new HashableCoding(COND_VALUE_SYSTEM, COND_VALUE_CODE_1, SOME_DISPLAY));
    public static final StratumComponent COND_VALUE_KEYPAIR_2 = new StratumComponent(
            HashableCoding.ofFhirCoding(COND_DEF_CODING),
            new HashableCoding(COND_VALUE_SYSTEM, COND_VALUE_CODE_2, SOME_DISPLAY));
    public static final StratumComponent STATUS_VALUE_KEYPAIR = new StratumComponent(
            new HashableCoding(STATUS_DEF_SYSTEM, STATUS_DEF_CODE, SOME_DISPLAY),
            new HashableCoding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY));
    static final String INITIAL_POPULATION_CODE = "initial-population";
    static final String MEASURE_POPULATION_CODE = "measure-population";
    static final String OBSERVATION_POPULATION_CODE = "measure-observation";
    static final String POPULATION_SYSTEM = "http://terminology.hl7.org/CodeSystem/measure-population";
    static final FHIRPathEngine pathEngine = createPathEngine();
    public static final String INITIAL_POPULATION_LANGUAGE = "text/x-fhir-query";
    public static final String FHIR_PATH = "text/fhirpath";
    public static final String MEASURE_POPULATION_ID = "measure-population-identifier";
    static final String CRITERIA_REFERENCE_URL = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference";
    static final String MEASURE_POPULATION_PATH = "Condition";
    static final String OBSERVATION_POPULATION_PATH = "Condition.subject.reference";
    final String CRITERIA_REFERENCE_VALUE = "measure-population-identifier";

    @Mock
    DataStore dataStore;

    private static Expression expressionOfPath(String expStr) {
        Expression expression = new Expression();
        return expression.setExpression(expStr).setLanguage(FHIR_PATH);
    }

    public static Condition getCondition() {
        Coding condCoding = new Coding().setSystem(COND_VALUE_SYSTEM).setCode(COND_VALUE_CODE);
        CodeableConcept condConcept = new CodeableConcept().addCoding(condCoding);

        return new Condition().setCode(condConcept);
    }

    public static Condition getConditionWithSubject(String subjectID) {
        return getCondition().setSubject(new Reference().setReference(subjectID));
    }

    public static Patient getPatient(AdministrativeGender gender) {
        return new Patient().setGender(gender);
    }

    public static Observation getObservation(String quantityCode) {
        return new Observation().setValue(new Quantity().setCode(quantityCode));
    }

    public static FHIRPathEngine createPathEngine() {
        final FhirContext context = FhirContext.forR4();
        final DefaultProfileValidationSupport validation = new DefaultProfileValidationSupport(context);
        final IWorkerContext worker = new HapiWorkerContext(context, validation);
        return new FHIRPathEngine(worker);
    }

    public static Measure.MeasureGroupPopulationComponent getInitialPopulation(String query) {
        return new Measure.MeasureGroupPopulationComponent()
                .setCriteria(new Expression().setExpression(query).setLanguage(INITIAL_POPULATION_LANGUAGE))
                .setCode(new CodeableConcept(new Coding().setSystem(POPULATION_SYSTEM).setCode(INITIAL_POPULATION_CODE)));
    }

    public static Measure.MeasureGroupPopulationComponent getMeasurePopulation(String fhirpath) {
        return (Measure.MeasureGroupPopulationComponent) new Measure.MeasureGroupPopulationComponent()
                .setCriteria(new Expression().setExpression(fhirpath).setLanguage(FHIR_PATH))
                .setCode(new CodeableConcept(new Coding().setSystem(POPULATION_SYSTEM).setCode(MEASURE_POPULATION_CODE)))
                .setId(MEASURE_POPULATION_ID);
    }

    public static Measure.MeasureGroupPopulationComponent getObservationPopulation(String fhirpath) {
        return (Measure.MeasureGroupPopulationComponent) new Measure.MeasureGroupPopulationComponent()
                .setCriteria(new Expression().setExpression(fhirpath).setLanguage(FHIR_PATH))
                .setCode(new CodeableConcept(new Coding().setSystem(POPULATION_SYSTEM).setCode(OBSERVATION_POPULATION_CODE)))
                .setExtension(List.of(
                        new Extension(AggregateUniqueCount.EXTENSION_URL).setValue(new CodeType(AggregateUniqueCount.EXTENSION_VALUE)),
                        new Extension(CRITERIA_REFERENCE_URL).setValue(new CodeType(MEASURE_POPULATION_ID))));
    }

    public static Measure.MeasureGroupComponent getMeasureGroup() {
        Measure.MeasureGroupComponent measureGroup = new Measure.MeasureGroupComponent();

        return measureGroup.setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
    }

    @Nested
    class ExceptionTests {

        @Test
        public void test_twoCodingsInPopulation() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setPopulation(List.of(
                    getInitialPopulation(CONDITION_QUERY).setCode(new CodeableConcept()
                            .addCoding(new Coding().setSystem(POPULATION_SYSTEM).setCode(INITIAL_POPULATION_CODE))
                            .addCoding(new Coding().setSystem(POPULATION_SYSTEM).setCode(INITIAL_POPULATION_CODE)))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Population in Measure did not contain exactly one Coding");
        }

        @Test
        public void test_twoInitialPopulations() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setPopulation(List.of(getInitialPopulation(CONDITION_QUERY), getInitialPopulation(CONDITION_QUERY)));
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
                    getInitialPopulation(CONDITION_QUERY)
                            .setCriteria(new Expression().setExpressionElement(new StringType(CONDITION_QUERY))
                                    .setLanguage("some-wrong-language"))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Language of Initial Population was not equal to '%s'".formatted(INITIAL_POPULATION_LANGUAGE));
        }

        @Test
        public void test_componentWithoutCoding() {
            when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of
                            (new Measure.MeasureGroupStratifierComponent()
                                    .setComponent(List.of(new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(null)))
                                    .setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Stratifier component did not contain exactly one coding");
        }

        @Test
        public void test_componentWithMultipleCodings() {
            when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent()
                                    .setComponent(List.of(new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(
                                            new CodeableConcept()
                                                    .addCoding(COND_DEF_CODING)
                                                    .addCoding(COND_DEF_CODING))))
                                    .setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Stratifier component did not contain exactly one coding");
        }

        @Test
        public void test_componentWithWrongLanguage() {
            when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setComponent(List.of(
                                            new Measure.MeasureGroupStratifierComponentComponent(expressionOfPath(COND_CODE_PATH.getExpression()).setLanguage("some-other-language"))
                                                    .setCode(new CodeableConcept().addCoding(COND_DEF_CODING))))
                                    .setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Language of stratifier component was not equal to '%s'".formatted(FHIR_PATH));
        }

        @Test
        public void test_multipleMeasurePopulations() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            getMeasurePopulation(MEASURE_POPULATION_PATH)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Measure did contain more than one measure population");
        }

        @Test
        public void test_measurePopulation_withWrongLanguage() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH)
                                    .setCriteria(new Expression().setExpression(MEASURE_POPULATION_PATH).setLanguage("some-other-language"))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Language of Measure Population was not equal to '%s'".formatted(FHIR_PATH));
        }

        @Test
        public void test_observationPopulationWithoutMeasurePopulation() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getObservationPopulation(OBSERVATION_POPULATION_PATH)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Group must not contain a Measure Observation without a Measure Population");
        }

        @Test
        public void test_multipleObservationPopulations() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            getObservationPopulation(OBSERVATION_POPULATION_PATH),
                            getObservationPopulation(OBSERVATION_POPULATION_PATH)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Measure did contain more than one observation population");
        }

        @Test
        public void test_observationPopulation_withWrongLanguage() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            getObservationPopulation(OBSERVATION_POPULATION_PATH)
                                    .setCriteria(new Expression().setExpression(MEASURE_POPULATION_PATH).setLanguage("some-other-language"))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Language of Measure Observation was not equal to '%s'".formatted(FHIR_PATH));
        }

        @Test
        public void test_observationPopulation_withoutCriteriaReference() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            (Measure.MeasureGroupPopulationComponent) getObservationPopulation(OBSERVATION_POPULATION_PATH)
                                    .setExtension(List.of(
                                            new Extension(AggregateUniqueCount.EXTENSION_URL).setValue(new CodeType(AggregateUniqueCount.EXTENSION_VALUE))))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Measure Observation Population did not contain exactly one criteria reference");
        }

        @Test
        public void test_observationPopulation_withTooManyCriteriaReferences() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            (Measure.MeasureGroupPopulationComponent) getObservationPopulation(OBSERVATION_POPULATION_PATH)
                                    .setExtension(List.of(
                                            new Extension(AggregateUniqueCount.EXTENSION_URL).setValue(new CodeType(AggregateUniqueCount.EXTENSION_VALUE)),
                                            new Extension(CRITERIA_REFERENCE_URL).setValue(new CodeType(MEASURE_POPULATION_ID)),
                                            new Extension(CRITERIA_REFERENCE_URL).setValue(new CodeType(MEASURE_POPULATION_ID))))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Measure Observation Population did not contain exactly one criteria reference");
        }

        @Test
        public void test_observationPopulation_criteriaReferenceWithNoValue() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            (Measure.MeasureGroupPopulationComponent) getObservationPopulation(OBSERVATION_POPULATION_PATH)
                                    .setExtension(List.of(
                                            new Extension(AggregateUniqueCount.EXTENSION_URL).setValue(new CodeType(AggregateUniqueCount.EXTENSION_VALUE)),
                                            new Extension(CRITERIA_REFERENCE_URL)))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Criteria Reference of Measure Observation Population has no value");
        }

        @Test
        public void test_obesrvationPopulation_criteriaReferenceWithWrongValue() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            (Measure.MeasureGroupPopulationComponent) getObservationPopulation(OBSERVATION_POPULATION_PATH)
                                    .setExtension(List.of(
                                            new Extension(AggregateUniqueCount.EXTENSION_URL).setValue(new CodeType(AggregateUniqueCount.EXTENSION_VALUE)),
                                            new Extension(CRITERIA_REFERENCE_URL).setValue(new CodeType("some-other-value"))))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Value of Criteria Reference of Measure Observation Population must be equal to '%s'".formatted(CRITERIA_REFERENCE_VALUE));
        }

        @Test
        public void test_observationPopulation_withoutAggregateMethod() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            (Measure.MeasureGroupPopulationComponent) getObservationPopulation(OBSERVATION_POPULATION_PATH)
                                    .setExtension(List.of(
                                            new Extension(CRITERIA_REFERENCE_URL).setValue(new CodeType(MEASURE_POPULATION_ID))))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Measure Observation Population did not contain exactly one aggregate method");
        }

        @Test
        public void test_observationPopulation_withTooManyAggregateMethods() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            (Measure.MeasureGroupPopulationComponent) getObservationPopulation(OBSERVATION_POPULATION_PATH)
                                    .setExtension(List.of(
                                            new Extension(AggregateUniqueCount.EXTENSION_URL).setValue(new CodeType(AggregateUniqueCount.EXTENSION_VALUE)),
                                            new Extension(AggregateUniqueCount.EXTENSION_URL).setValue(new CodeType(AggregateUniqueCount.EXTENSION_VALUE)),
                                            new Extension(CRITERIA_REFERENCE_URL).setValue(new CodeType(MEASURE_POPULATION_ID))))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Measure Observation Population did not contain exactly one aggregate method");
        }

        @Test
        public void test_observationPopulation_aggregateMethodWithoutValue() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            (Measure.MeasureGroupPopulationComponent) getObservationPopulation(OBSERVATION_POPULATION_PATH)
                                    .setExtension(List.of(
                                            new Extension(AggregateUniqueCount.EXTENSION_URL),
                                            new Extension(CRITERIA_REFERENCE_URL).setValue(new CodeType(MEASURE_POPULATION_ID))))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Aggregate Method of Measure Observation Population has no value");
        }

        @Test
        public void test_observationPopulation_aggregateMethodWithWrongValue() {
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            (Measure.MeasureGroupPopulationComponent) getObservationPopulation(OBSERVATION_POPULATION_PATH)
                                    .setExtension(List.of(
                                            new Extension(AggregateUniqueCount.EXTENSION_URL).setValue(new CodeType("some-value")),
                                            new Extension(CRITERIA_REFERENCE_URL).setValue(new CodeType(MEASURE_POPULATION_ID))))));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            assertThatThrownBy(() -> groupEvaluator.evaluateGroup(measureGroup).block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Aggregate Method of Measure Observation Population has not value '%s'".formatted(AggregateUniqueCount.EXTENSION_VALUE));
        }
    }

    @Nested
    @DisplayName("Test a wide range of scenarios using type Coding")
    class CodingTypeComplex {

        @Nested
        class StratifierOfSingleCriteria {
            @Nested
            class SingleStratifierInGroup_withSingleCriteria {

                @Test
                public void test_oneStratifierElement_oneResultValue_ignoreOtherPopulations() {
                    when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup().setStratifier(List.of(new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH)
                                    .setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(
                                    getInitialPopulation(CONDITION_QUERY),
                                    getInitialPopulation(CONDITION_QUERY).setCode(new CodeableConcept(new Coding().setCode("some-other-population").setSystem("some-system")))));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                    assertThat((result.stratifierResults().get(0)))
                            .isEqualTo(
                                    new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                            Set.of(COND_VALUE_KEYPAIR),
                                            Populations.INITIAL_ONE)
                                    ));
                }

                @Test
                public void test_oneStratifierElement_oneResultValue() {
                    when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                    assertThat((result.stratifierResults().get(0)))
                            .isEqualTo(
                                    new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                            Set.of(COND_VALUE_KEYPAIR),
                                            Populations.INITIAL_ONE)
                                    ));
                }

                @Test
                public void test_oneStratifierElement_twoSameResultValues() {
                    when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                            getCondition(),
                            getCondition())));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populations().initialPopulation().count()).isEqualTo(2);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                    assertThat(result.stratifierResults().get(0))
                            .isEqualTo(
                                    new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                            Set.of(COND_VALUE_KEYPAIR),
                                            Populations.ofInitial(new InitialPopulation(2)))
                                    ));

                }

                @Test
                public void test_oneStratifierElement_twoDifferentResultValues() {
                    when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                            getCondition().setCode(new CodeableConcept(new Coding().setSystem(COND_VALUE_SYSTEM).setCode(COND_VALUE_CODE_1))),
                            getCondition().setCode(new CodeableConcept(new Coding().setSystem(COND_VALUE_SYSTEM).setCode(COND_VALUE_CODE_2))))));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populations().initialPopulation().count()).isEqualTo(2);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                    assertThat(result.stratifierResults().get(0))
                            .isEqualTo(
                                    new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                            Set.of(COND_VALUE_KEYPAIR_1),
                                            Populations.INITIAL_ONE,
                                            Set.of(COND_VALUE_KEYPAIR_2),
                                            Populations.INITIAL_ONE)
                                    ));
                }

                @Nested
                class FailTests {
                    @Test
                    public void test_oneStratifierElement_noValue() {
                        when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(new Condition())));
                        Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                                .setStratifier(List.of(
                                        new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                                .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                        GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                        var result = groupEvaluator.evaluateGroup(measureGroup).block();

                        assertThat(result).isNotNull();
                        assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                        assertThat(result.stratifierResults().size()).isEqualTo(1);
                        assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                        assertThat((result.stratifierResults().get(0)))
                                .isEqualTo(
                                        new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                                Set.of(StratumComponent.ofFailedNoValueFound(COND_VALUE_KEYPAIR.code())),
                                                Populations.INITIAL_ONE)
                                        ));
                    }

                    @Test
                    public void test_oneStratifierElement_tooManyValues() {
                        Coding condCoding = new Coding().setSystem(COND_VALUE_SYSTEM).setCode(COND_VALUE_CODE);
                        when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(new Condition().setCode(new CodeableConcept()
                                .addCoding(condCoding)
                                .addCoding(condCoding)))));
                        Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                                .setStratifier(List.of(
                                        new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                                .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                        GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                        var result = groupEvaluator.evaluateGroup(measureGroup).block();

                        assertThat(result).isNotNull();
                        assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                        assertThat(result.stratifierResults().size()).isEqualTo(1);
                        assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                        assertThat((result.stratifierResults().get(0)))
                                .isEqualTo(
                                        new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                                Set.of(StratumComponent.ofFailedTooManyValues(COND_VALUE_KEYPAIR.code())),
                                                Populations.INITIAL_ONE)
                                        ));
                    }

                    @Test
                    public void test_oneStratifierElement_invalidType() {
                        when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                        Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                                .setStratifier(List.of(
                                        new Measure.MeasureGroupStratifierComponent().setCriteria(expressionOfPath("Condition.code")).setCode(new CodeableConcept(COND_DEF_CODING))))
                                .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                        GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                        var result = groupEvaluator.evaluateGroup(measureGroup).block();

                        assertThat(result).isNotNull();
                        assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                        assertThat(result.stratifierResults().size()).isEqualTo(1);
                        assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                        assertThat((result.stratifierResults().get(0)))
                                .isEqualTo(
                                        new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                                Set.of(StratumComponent.ofFailedInvalidType(COND_VALUE_KEYPAIR.code())),
                                                Populations.INITIAL_ONE)
                                        ));
                    }

                    @Test
                    public void test_oneStratifierElement_missingSystem() {
                        when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(new Condition().setCode(new CodeableConcept(new Coding().setCode(COND_VALUE_CODE))))));
                        Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                                .setStratifier(List.of(
                                        new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                                .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                        GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                        var result = groupEvaluator.evaluateGroup(measureGroup).block();

                        assertThat(result).isNotNull();
                        assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                        assertThat(result.stratifierResults().size()).isEqualTo(1);
                        assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                        assertThat((result.stratifierResults().get(0)))
                                .isEqualTo(
                                        new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                                Set.of(StratumComponent.ofFailedMissingFields(COND_VALUE_KEYPAIR.code())),
                                                Populations.INITIAL_ONE)
                                        ));
                    }

                    @Test
                    public void test_oneStratifierElement_missingCode() {
                        when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(new Condition().setCode(new CodeableConcept(new Coding().setSystem(COND_VALUE_SYSTEM))))));
                        Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                                .setStratifier(List.of(
                                        new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                                .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                        GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                        var result = groupEvaluator.evaluateGroup(measureGroup).block();

                        assertThat(result).isNotNull();
                        assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                        assertThat(result.stratifierResults().size()).isEqualTo(1);
                        assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                        assertThat((result.stratifierResults().get(0)))
                                .isEqualTo(
                                        new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                                Set.of(StratumComponent.ofFailedMissingFields(COND_VALUE_KEYPAIR.code())),
                                                Populations.INITIAL_ONE)
                                        ));
                    }
                }
            }

            @Nested
            class MultipleStratifiersInGroup_withSingleCriteria {

                @Test
                public void test_twoSameStratifierElements() {
                    when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                    assertThat(result.stratifierResults().size()).isEqualTo(2);
                    assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                    assertThat(result.stratifierResults().get(1).populations()).isNotNull();
                    assertThat(result.stratifierResults())
                            .containsExactly(
                                    new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                            Set.of(COND_VALUE_KEYPAIR),
                                            Populations.INITIAL_ONE)
                                    ),
                                    new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                            Set.of(COND_VALUE_KEYPAIR),
                                            Populations.INITIAL_ONE)
                                    ));
                }

                @Test
                public void test_twoStratifierElements_oneResultValueEach() {
                    when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition()
                            .setClinicalStatus(new CodeableConcept(new Coding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY))))));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_STATUS_PATH).setCode(new CodeableConcept(STATUS_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                    assertThat(result.stratifierResults().size()).isEqualTo(2);
                    assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                    assertThat(result.stratifierResults().get(1).populations()).isNotNull();
                    assertThat(result.stratifierResults())
                            .containsExactly(
                                    new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(Set.of(COND_VALUE_KEYPAIR),
                                            Populations.INITIAL_ONE)
                                    ),
                                    new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(STATUS_DEF_CODING)), Map.of(
                                            Set.of(STATUS_VALUE_KEYPAIR),
                                            Populations.INITIAL_ONE)
                                    ));
                }
            }

        }

        @Nested
        class StratifierOfMultipleComponents {
            @Nested
            class SingleStratifierInGroup {
                @Test
                public void test_oneStratifierElement_twoDifferentComponents_oneDifferentResultValueEach() {
                    when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                            getCondition().setClinicalStatus(new CodeableConcept(new Coding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY))))));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent()
                                            .setComponent(List.of(
                                                    new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                                    new Measure.MeasureGroupStratifierComponentComponent(COND_STATUS_PATH).setCode(new CodeableConcept(STATUS_DEF_CODING))))
                                            .setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                    assertThat((result.stratifierResults().get(0)))
                            .isEqualTo(
                                    new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                            Set.of(COND_VALUE_KEYPAIR, STATUS_VALUE_KEYPAIR),
                                            Populations.INITIAL_ONE)
                                    ));
                }

                @Test
                public void test_oneStratifierElement_twoDifferentComponents_oneSameResultValueEach() {
                    when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent()
                                            .setComponent(List.of(
                                                    new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                                    new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(new CodeableConcept(new Coding(COND_DEF_SYSTEM, "some-other-code", SOME_DISPLAY)))))
                                            .setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                    assertThat((result.stratifierResults().get(0)))
                            .isEqualTo(
                                    new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                            Set.of(
                                                    COND_VALUE_KEYPAIR,
                                                    new StratumComponent(
                                                            new HashableCoding(COND_DEF_SYSTEM, "some-other-code", SOME_DISPLAY),
                                                            COND_VALUE_KEYPAIR.value())),
                                            Populations.INITIAL_ONE)
                                    ));
                }

                @Test
                public void test_oneStratifierElement_twoSameComponents() { // TODO this is actually undefined behaviour I think
                    when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent()
                                            .setComponent(List.of(
                                                    new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                                    new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                                            .setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                    assertThat((result.stratifierResults().get(0)))
                            .isEqualTo(
                                    new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                            Set.of(COND_VALUE_KEYPAIR),
                                            Populations.INITIAL_ONE)
                                    ));
                }

                @Test
                public void test_oneStratifierElement_twoDifferentComponents_twoDifferentResultValuesEach() {
                    final CodeableConcept condCoding1 = new CodeableConcept().addCoding(new Coding().setSystem(COND_VALUE_SYSTEM).setCode("cond-code-value-1"));
                    final CodeableConcept condCoding2 = new CodeableConcept().addCoding(new Coding().setSystem(COND_VALUE_SYSTEM).setCode("cond-code-value-2"));
                    final CodeableConcept statusCoding2 = new CodeableConcept().addCoding(new Coding().setSystem(STATUS_VALUE_SYSTEM).setCode("status-value-1"));
                    final CodeableConcept statusCoding1 = new CodeableConcept().addCoding(new Coding().setSystem(STATUS_VALUE_SYSTEM).setCode("status-value-2"));
                    final StratumComponent condValueKeypair_1 = new StratumComponent(
                            new HashableCoding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY),
                            new HashableCoding(COND_VALUE_SYSTEM, "cond-code-value-1", SOME_DISPLAY));
                    final StratumComponent condValueKeypair_2 = new StratumComponent(
                            new HashableCoding(COND_DEF_SYSTEM, COND_DEF_CODE, SOME_DISPLAY),
                            new HashableCoding(COND_VALUE_SYSTEM, "cond-code-value-2", SOME_DISPLAY));
                    final StratumComponent statusValueKeypair_1 = new StratumComponent(
                            new HashableCoding(STATUS_DEF_SYSTEM, STATUS_DEF_CODE, SOME_DISPLAY),
                            new HashableCoding(STATUS_VALUE_SYSTEM, "status-value-1", SOME_DISPLAY));
                    final StratumComponent statusValueKeypair_2 = new StratumComponent(
                            new HashableCoding(STATUS_DEF_SYSTEM, STATUS_DEF_CODE, SOME_DISPLAY),
                            new HashableCoding(STATUS_VALUE_SYSTEM, "status-value-2", SOME_DISPLAY));

                    when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                            getCondition().setCode(condCoding1).setClinicalStatus(statusCoding1),
                            getCondition().setCode(condCoding1).setClinicalStatus(statusCoding2),
                            getCondition().setCode(condCoding2).setClinicalStatus(statusCoding1),
                            getCondition().setCode(condCoding2).setClinicalStatus(statusCoding2))));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent()
                                            .setComponent(List.of(
                                                    new Measure.MeasureGroupStratifierComponentComponent(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                                    new Measure.MeasureGroupStratifierComponentComponent(COND_STATUS_PATH).setCode(new CodeableConcept(STATUS_DEF_CODING))))))
                            .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(result.populations().initialPopulation().count()).isEqualTo(4);
                    assertThat(result.stratifierResults().size()).isEqualTo(1);
                    assertThat(result.stratifierResults().get(0).populations()).isNotNull();

                    assertThat((result.stratifierResults().get(0)))
                            .isEqualTo(
                                    new StratifierResult(Optional.empty(), Map.of(
                                            Set.of(condValueKeypair_1, statusValueKeypair_1), Populations.INITIAL_ONE,
                                            Set.of(condValueKeypair_1, statusValueKeypair_2), Populations.INITIAL_ONE,
                                            Set.of(condValueKeypair_2, statusValueKeypair_1), Populations.INITIAL_ONE,
                                            Set.of(condValueKeypair_2, statusValueKeypair_2), Populations.INITIAL_ONE)
                                    ));
                }


            }

        }
    }

    @Nested
    @DisplayName("Simple tests to validate functionality of type Code")
    class CodeTypeSimple {
        static final String PATIENT_QUERY = "Patient";
        static final AdministrativeGender GENDER = AdministrativeGender.FEMALE;
        static final Expression GENDER_PATH = expressionOfPath("Patient.gender");
        static final String GENDER_DEF_SYSTEM = "gender-def-system";
        static final String GENDER_DEF_CODE = "gender-evaluation-code";
        public static final Coding GENDER_DEF_CODING = new Coding(GENDER_DEF_SYSTEM, GENDER_DEF_CODE, SOME_DISPLAY);
        public static final StratumComponent GENDER_VALUE_KEYPAIR = new StratumComponent(
                HashableCoding.ofFhirCoding(GENDER_DEF_CODING),
                HashableCoding.ofSingleCodeValue(GENDER.toCode()));


        @Nested
        @DisplayName("Test code that is of type CodeType in hapi")
        class Code_ofType_CodeType {
            static final String OBSERVATION_QUERY = "Observation";
            static final String NG_ML = "ng/mL";
            static final Expression VALUE_PATH = expressionOfPath("Observation.value.code");
            static final String QUANTITY_DEF_SYSTEM = "quantity-def-system";
            static final String QUANTITY_DEF_CODE = "quantity-evaluation-code";
            public static final Coding QUANTITY_DEF_CODING = new Coding(QUANTITY_DEF_SYSTEM, QUANTITY_DEF_CODE, SOME_DISPLAY);
            public static final StratumComponent QUANTITY_VALUE_KEYPAIR = new StratumComponent(
                    HashableCoding.ofFhirCoding(QUANTITY_DEF_CODING),
                    HashableCoding.ofSingleCodeValue(NG_ML));

            @Test
            public void test_quantityCode() {
                when(dataStore.getPopulation("/" + OBSERVATION_QUERY)).thenReturn(Flux.fromIterable(List.of(getObservation(NG_ML))));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                        .setStratifier(List.of(
                                new Measure.MeasureGroupStratifierComponent().setCriteria(VALUE_PATH).setCode(new CodeableConcept(QUANTITY_DEF_CODING))))
                        .setPopulation(List.of(getInitialPopulation(OBSERVATION_QUERY)));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                assertThat((result.stratifierResults().get(0)))
                        .isEqualTo(
                                new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(QUANTITY_DEF_CODING)), Map.of(
                                        Set.of(QUANTITY_VALUE_KEYPAIR),
                                        Populations.INITIAL_ONE)
                                ));
            }

        }

        @Nested
        @DisplayName("Test code that is of type Enumeration in hapi")
        class Code_ofType_Enumeration {
            @Test
            public void test_gender() {
                when(dataStore.getPopulation("/" + PATIENT_QUERY)).thenReturn(Flux.fromIterable(List.of(getPatient(GENDER))));
                Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                        .setStratifier(List.of(
                                new Measure.MeasureGroupStratifierComponent().setCriteria(GENDER_PATH).setCode(new CodeableConcept(GENDER_DEF_CODING))))
                        .setPopulation(List.of(getInitialPopulation(PATIENT_QUERY)));
                GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                var result = groupEvaluator.evaluateGroup(measureGroup).block();

                assertThat(result).isNotNull();
                assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
                assertThat(result.stratifierResults().size()).isEqualTo(1);
                assertThat(result.stratifierResults().get(0).populations()).isNotNull();
                assertThat((result.stratifierResults().get(0)))
                        .isEqualTo(
                                new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(GENDER_DEF_CODING)), Map.of(
                                        Set.of(GENDER_VALUE_KEYPAIR),
                                        Populations.INITIAL_ONE)
                                ));
            }
        }


        @Test
        public void test_code_no_value() {
            when(dataStore.getPopulation("/" + PATIENT_QUERY)).thenReturn(Flux.fromIterable(List.of(getPatient(null))));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(GENDER_PATH).setCode(new CodeableConcept(GENDER_DEF_CODING))))
                    .setPopulation(List.of(getInitialPopulation(PATIENT_QUERY)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            var result = groupEvaluator.evaluateGroup(measureGroup).block();

            assertThat(result).isNotNull();
            assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
            assertThat(result.stratifierResults().size()).isEqualTo(1);
            assertThat(result.stratifierResults().get(0).populations()).isNotNull();
            assertThat((result.stratifierResults().get(0)))
                    .isEqualTo(
                            new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(GENDER_DEF_CODING)), Map.of(
                                    Set.of(new StratumComponent(HashableCoding.ofFhirCoding(GENDER_DEF_CODING),
                                            HashableCoding.FAIL_NO_VALUE_FOUND)),
                                    Populations.INITIAL_ONE)
                            ));
        }
    }

    @Nested
    @DisplayName("Simple tests to validate functionality of type Boolean")
    class BooleanTypeSimple {
        static final Expression COND_CODE_EXISTS_PATH = expressionOfPath("Condition.code.exists()");
        static final StratumComponent COND_CODE_EXISTS_TRUE = new StratumComponent(
                HashableCoding.ofFhirCoding(COND_DEF_CODING),
                HashableCoding.ofSingleCodeValue("true"));
        static final StratumComponent COND_CODE_EXISTS_FALSE = new StratumComponent(
                HashableCoding.ofFhirCoding(COND_DEF_CODING),
                HashableCoding.ofSingleCodeValue("false"));

        @Test
        public void test_code_exists() {
            when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition())));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_EXISTS_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            var result = groupEvaluator.evaluateGroup(measureGroup).block();

            assertThat(result).isNotNull();
            assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
            assertThat(result.stratifierResults().size()).isEqualTo(1);
            assertThat(result.stratifierResults().get(0).populations()).isNotNull();
            assertThat((result.stratifierResults().get(0)))
                    .isEqualTo(
                            new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                    Set.of(COND_CODE_EXISTS_TRUE),
                                    Populations.INITIAL_ONE)
                            ));
        }

        @Test
        public void test_code_exists_not() {
            when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition().setCode(null))));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_EXISTS_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            var result = groupEvaluator.evaluateGroup(measureGroup).block();

            assertThat(result).isNotNull();
            assertThat(result.populations().initialPopulation().count()).isEqualTo(1);
            assertThat(result.stratifierResults().size()).isEqualTo(1);
            assertThat(result.stratifierResults().get(0).populations()).isNotNull();
            assertThat((result.stratifierResults().get(0)))
                    .isEqualTo(
                            new StratifierResult(Optional.of(HashableCoding.ofFhirCoding(COND_DEF_CODING)), Map.of(
                                    Set.of(COND_CODE_EXISTS_FALSE),
                                    Populations.INITIAL_ONE)
                            ));
        }
    }

    @Nested
    @DisplayName("Test Unique Count")
    class UniqueCount {

        static final String UNIQUE_VAL_1 = "val-1";
        static final String UNIQUE_VAL_2 = "val-2";


        @Test
        @DisplayName("Two same values resulting in unique count '1'")
        public void test_twoSameValues() {
            when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                    getConditionWithSubject(UNIQUE_VAL_1),
                    getConditionWithSubject(UNIQUE_VAL_1))));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            getObservationPopulation(OBSERVATION_POPULATION_PATH)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            var result = groupEvaluator.evaluateGroup(measureGroup).block();

            assertThat(result).isNotNull();
            assertThat(result.populations().initialPopulation().count()).isEqualTo(2);
            assertThat(result.populations().measurePopulation()).isPresent();
            assertThat(result.populations().measurePopulation().get().count()).isEqualTo(2);
            assertThat(result.populations().observationPopulation()).isPresent();
            assertThat(result.populations().observationPopulation().get().count()).isEqualTo(2);
            assertThat(result.populations().observationPopulation().get().aggregateMethod().getScore()).isEqualTo(1);
            assertThat(result.stratifierResults().size()).isEqualTo(1);
            assertThat(result.stratifierResults().get(0).populations()).isNotNull();

            var firstStratum = result.stratifierResults().get(0).populations().entrySet().iterator().next();
            assertThat(firstStratum.getKey()).isEqualTo(Set.of(COND_VALUE_KEYPAIR));
            assertThat(firstStratum.getValue().initialPopulation().count()).isEqualTo(2);
            assertThat(firstStratum.getValue().measurePopulation()).isPresent();
            assertThat(firstStratum.getValue().measurePopulation().get().count()).isEqualTo(2);
            assertThat(firstStratum.getValue().observationPopulation()).isPresent();
            assertThat(firstStratum.getValue().observationPopulation().get().count()).isEqualTo(2);
            assertThat(firstStratum.getValue().observationPopulation().get().aggregateMethod().getScore()).isEqualTo(1);
        }

        @Test
        @DisplayName("Two different values resulting in unique count '2'")
        public void test_twoDifferentValues() {
            when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                    getConditionWithSubject(UNIQUE_VAL_1),
                    getConditionWithSubject(UNIQUE_VAL_2))));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            getObservationPopulation(OBSERVATION_POPULATION_PATH)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            var result = groupEvaluator.evaluateGroup(measureGroup).block();

            assertThat(result).isNotNull();
            assertThat(result.populations().initialPopulation().count()).isEqualTo(2);
            assertThat(result.populations().measurePopulation()).isPresent();
            assertThat(result.populations().measurePopulation().get().count()).isEqualTo(2);
            assertThat(result.populations().observationPopulation()).isPresent();
            assertThat(result.populations().observationPopulation().get().count()).isEqualTo(2);
            assertThat(result.populations().observationPopulation().get().aggregateMethod().getScore()).isEqualTo(2);
            assertThat(result.stratifierResults().size()).isEqualTo(1);
            assertThat(result.stratifierResults().get(0).populations()).isNotNull();

            var firstStratum = result.stratifierResults().get(0).populations().entrySet().iterator().next();
            assertThat(firstStratum.getKey()).isEqualTo(Set.of(COND_VALUE_KEYPAIR));
            assertThat(firstStratum.getValue().initialPopulation().count()).isEqualTo(2);
            assertThat(firstStratum.getValue().measurePopulation()).isPresent();
            assertThat(firstStratum.getValue().measurePopulation().get().count()).isEqualTo(2);
            assertThat(firstStratum.getValue().observationPopulation()).isPresent();
            assertThat(firstStratum.getValue().observationPopulation().get().count()).isEqualTo(2);
            assertThat(firstStratum.getValue().observationPopulation().get().aggregateMethod().getScore()).isEqualTo(2);

        }

        @Test
        @DisplayName("Two same values part of Measure Population and one different value not part of Measure Population")
        public void test_twoSameValues_oneDifferentValue_withDifferentMeasurePopulation() {
            when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                    getConditionWithSubject(UNIQUE_VAL_1).setClinicalStatus(new CodeableConcept(new Coding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY))),
                    getConditionWithSubject(UNIQUE_VAL_1).setClinicalStatus(new CodeableConcept(new Coding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY))),
                    getConditionWithSubject(UNIQUE_VAL_2))));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation("Condition.where(clinicalStatus.exists())"),
                            getObservationPopulation(OBSERVATION_POPULATION_PATH)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            var result = groupEvaluator.evaluateGroup(measureGroup).block();

            assertThat(result).isNotNull();
            assertThat(result.populations().initialPopulation().count()).isEqualTo(3);
            assertThat(result.populations().measurePopulation()).isPresent();
            assertThat(result.populations().measurePopulation().get().count()).isEqualTo(2);
            assertThat(result.populations().observationPopulation()).isPresent();
            assertThat(result.populations().observationPopulation().get().count()).isEqualTo(2);
            assertThat(result.populations().observationPopulation().get().aggregateMethod().getScore()).isEqualTo(1);
            assertThat(result.stratifierResults().size()).isEqualTo(1);
            assertThat(result.stratifierResults().get(0).populations()).isNotNull();

            var firstStratum = result.stratifierResults().get(0).populations().entrySet().iterator().next();
            assertThat(firstStratum.getKey()).isEqualTo(Set.of(COND_VALUE_KEYPAIR));
            assertThat(firstStratum.getValue().initialPopulation().count()).isEqualTo(3);
            assertThat(firstStratum.getValue().measurePopulation()).isPresent();
            assertThat(firstStratum.getValue().measurePopulation().get().count()).isEqualTo(2);
            assertThat(firstStratum.getValue().observationPopulation()).isPresent();
            assertThat(firstStratum.getValue().observationPopulation().get().count()).isEqualTo(2);
            assertThat(firstStratum.getValue().observationPopulation().get().aggregateMethod().getScore()).isEqualTo(1);
        }


        @Test
        @DisplayName("Two Conditions with same value and one Condition with no value, leading to a different Measure Observation Population")
        public void test_twoSameValues_withDifferentObservationPopulation() {
            when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                    getConditionWithSubject(UNIQUE_VAL_1),
                    getConditionWithSubject(UNIQUE_VAL_1),
                    getCondition())));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            getObservationPopulation(OBSERVATION_POPULATION_PATH)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            var result = groupEvaluator.evaluateGroup(measureGroup).block();

            assertThat(result).isNotNull();
            assertThat(result.populations().initialPopulation().count()).isEqualTo(3);
            assertThat(result.populations().measurePopulation()).isPresent();
            assertThat(result.populations().measurePopulation().get().count()).isEqualTo(3);
            assertThat(result.populations().observationPopulation()).isPresent();
            assertThat(result.populations().observationPopulation().get().count()).isEqualTo(2);
            assertThat(result.populations().observationPopulation().get().aggregateMethod().getScore()).isEqualTo(1);
            assertThat(result.stratifierResults().size()).isEqualTo(1);
            assertThat(result.stratifierResults().get(0).populations()).isNotNull();

            var firstStratum = result.stratifierResults().get(0).populations().entrySet().iterator().next();
            assertThat(firstStratum.getKey()).isEqualTo(Set.of(COND_VALUE_KEYPAIR));
            assertThat(firstStratum.getValue().initialPopulation().count()).isEqualTo(3);
            assertThat(firstStratum.getValue().measurePopulation()).isPresent();
            assertThat(firstStratum.getValue().measurePopulation().get().count()).isEqualTo(3);
            assertThat(firstStratum.getValue().observationPopulation()).isPresent();
            assertThat(firstStratum.getValue().observationPopulation().get().count()).isEqualTo(2);
            assertThat(firstStratum.getValue().observationPopulation().get().aggregateMethod().getScore()).isEqualTo(1);
        }

        @Test
        @DisplayName("Two Conditions with different coding but with same reference resulting in two stratums with count '1' each, " +
                    "and group as a whole has also unique-count '1'")
        public void test_twoDifferentStratumValues_withSameUniqueValue() {

            when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(
                    getConditionWithSubject(UNIQUE_VAL_1)
                            .setCode(new CodeableConcept(new Coding().setSystem(COND_VALUE_SYSTEM).setCode(COND_VALUE_CODE_1))),
                    getConditionWithSubject(UNIQUE_VAL_1)
                            .setCode(new CodeableConcept(new Coding().setSystem(COND_VALUE_SYSTEM).setCode(COND_VALUE_CODE_2))))));
            Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                    .setStratifier(List.of(
                            new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING))))
                    .setPopulation(List.of(
                            getInitialPopulation(CONDITION_QUERY),
                            getMeasurePopulation(MEASURE_POPULATION_PATH),
                            getObservationPopulation(OBSERVATION_POPULATION_PATH)));
            GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

            var result = groupEvaluator.evaluateGroup(measureGroup).block();

            assertThat(result).isNotNull();
            assertThat(result.populations().initialPopulation().count()).isEqualTo(2);
            assertThat(result.populations().measurePopulation()).isPresent();
            assertThat(result.populations().measurePopulation().get().count()).isEqualTo(2);
            assertThat(result.populations().observationPopulation()).isPresent();
            assertThat(result.populations().observationPopulation().get().count()).isEqualTo(2);
            assertThat(result.populations().observationPopulation().get().aggregateMethod().getScore()).isEqualTo(1);
            assertThat(result.stratifierResults().size()).isEqualTo(1);
            assertThat(result.stratifierResults().get(0).populations().size()).isEqualTo(2);

            var firstStratum = result.stratifierResults().get(0).populations().entrySet().iterator().next();
            assertThat(firstStratum.getKey()).isEqualTo(Set.of(COND_VALUE_KEYPAIR_1));
            assertThat(firstStratum.getValue().initialPopulation().count()).isEqualTo(1);
            assertThat(firstStratum.getValue().measurePopulation()).isPresent();
            assertThat(firstStratum.getValue().measurePopulation().get().count()).isEqualTo(1);
            assertThat(firstStratum.getValue().observationPopulation()).isPresent();
            assertThat(firstStratum.getValue().observationPopulation().get().count()).isEqualTo(1);
            assertThat(firstStratum.getValue().observationPopulation().get().aggregateMethod().getScore()).isEqualTo(1);

            var secondStratum = result.stratifierResults().get(0).populations().entrySet().stream().skip(1).iterator().next();
            assertThat(secondStratum.getKey()).isEqualTo(Set.of(COND_VALUE_KEYPAIR_2));
            assertThat(secondStratum.getValue().initialPopulation().count()).isEqualTo(1);
            assertThat(secondStratum.getValue().measurePopulation()).isPresent();
            assertThat(secondStratum.getValue().measurePopulation().get().count()).isEqualTo(1);
            assertThat(secondStratum.getValue().observationPopulation()).isPresent();
            assertThat(secondStratum.getValue().observationPopulation().get().count()).isEqualTo(1);
            assertThat(secondStratum.getValue().observationPopulation().get().aggregateMethod().getScore()).isEqualTo(1);
        }
    }
}
