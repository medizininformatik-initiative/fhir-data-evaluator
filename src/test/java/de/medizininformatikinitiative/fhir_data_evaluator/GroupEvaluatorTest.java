package de.medizininformatikinitiative.fhir_data_evaluator;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.AggregateUniqueCount;
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

import java.math.BigDecimal;
import java.util.List;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    public static final HashableCoding STATUS_DEF_CODING = new HashableCoding(STATUS_DEF_SYSTEM, STATUS_DEF_CODE, SOME_DISPLAY);
    public static final HashableCoding COND_VALUE_CODING = new HashableCoding(COND_VALUE_SYSTEM, COND_VALUE_CODE, SOME_DISPLAY);
    public static final HashableCoding COND_VALUE_CODING_1 = new HashableCoding(COND_VALUE_SYSTEM, COND_VALUE_CODE_1, SOME_DISPLAY);
    public static final HashableCoding COND_VALUE_CODING_2 = new HashableCoding(COND_VALUE_SYSTEM, COND_VALUE_CODE_2, SOME_DISPLAY);
    public static final HashableCoding STATUS_VALUE_CODING = new HashableCoding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY);
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

    private static MeasureReport.MeasureReportGroupPopulationComponent findPopulationByCode(MeasureReport.MeasureReportGroupComponent group, HashableCoding code) {
        return group.getPopulation().stream().filter(population -> {
            var codings = population.getCode().getCoding();

            return code.equals(HashableCoding.ofFhirCoding(codings.get(0)));
        }).toList().get(0);
    }

    private static MeasureReport.StratifierGroupPopulationComponent findPopulationByCode(MeasureReport.StratifierGroupComponent stratum, HashableCoding code) {
        return stratum.getPopulation().stream().filter(population -> {
            var codings = population.getCode().getCoding();

            return code.equals(HashableCoding.ofFhirCoding(codings.get(0)));
        }).toList().get(0);
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
                    assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                    assertThat(result.getStratifier().size()).isEqualTo(1);
                    assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                            .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(COND_VALUE_CODING);
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);
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
                    assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                    assertThat(result.getStratifier().size()).isEqualTo(1);
                    assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                            .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(COND_VALUE_CODING);
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);
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
                    assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(2);
                    assertThat(result.getStratifier().size()).isEqualTo(1);
                    assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                            .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(COND_VALUE_CODING);
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(2);

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
                    assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(2);
                    assertThat(result.getStratifier().size()).isEqualTo(1);
                    assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(2);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                            .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(COND_VALUE_CODING_1);
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(1).getValue().getCodingFirstRep()))
                            .isEqualTo(COND_VALUE_CODING_2);
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(1), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);
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
                        assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                        assertThat(result.getStratifier().size()).isEqualTo(1);
                        assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);


                        assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                                .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                        assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep()))
                                .isEqualTo(FAIL_NO_VALUE_FOUND);
                        assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                                .isEqualTo(1);
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
                        assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                        assertThat(result.getStratifier().size()).isEqualTo(1);
                        assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

                        assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                                .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                        assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep()))
                                .isEqualTo(FAIL_TOO_MANY_VALUES);
                        assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                                .isEqualTo(1);
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
                        assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                        assertThat(result.getStratifier().size()).isEqualTo(1);
                        assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

                        assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                                .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                        assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep()))
                                .isEqualTo(FAIL_INVALID_TYPE);
                        assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                                .isEqualTo(1);
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
                        assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                        assertThat(result.getStratifier().size()).isEqualTo(1);
                        assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

                        assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                                .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                        assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep()))
                                .isEqualTo(FAIL_MISSING_FIELDS);
                        assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                                .isEqualTo(1);
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
                        assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                        assertThat(result.getStratifier().size()).isEqualTo(1);
                        assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

                        assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                                .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                        assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep()))
                                .isEqualTo(FAIL_MISSING_FIELDS);
                        assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                                .isEqualTo(1);
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
                    assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                    assertThat(result.getStratifier().size()).isEqualTo(2);
                    assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);
                    assertThat(result.getStratifier().get(1).getStratum().size()).isEqualTo(1);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                            .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(COND_VALUE_CODING);
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(1).getCode().get(0).getCodingFirstRep()))
                            .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(1).getStratum().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(COND_VALUE_CODING);
                    assertThat(findPopulationByCode(result.getStratifier().get(1).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);
                }

                @Test
                public void test_twoStratifierElements_oneResultValueEach() {
                    when(dataStore.getPopulation("/" + CONDITION_QUERY)).thenReturn(Flux.fromIterable(List.of(getCondition()
                            .setClinicalStatus(new CodeableConcept(new Coding(STATUS_VALUE_SYSTEM, STATUS_VALUE_CODE, SOME_DISPLAY))))));
                    Measure.MeasureGroupComponent measureGroup = getMeasureGroup()
                            .setStratifier(List.of(
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_CODE_PATH).setCode(new CodeableConcept(COND_DEF_CODING)),
                                    new Measure.MeasureGroupStratifierComponent().setCriteria(COND_STATUS_PATH).setCode(new CodeableConcept(STATUS_DEF_CODING.toCoding()))))
                            .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                    assertThat(result.getStratifier().size()).isEqualTo(2);
                    assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);
                    assertThat(result.getStratifier().get(1).getStratum().size()).isEqualTo(1);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                            .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(COND_VALUE_CODING);
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(1).getCode().get(0).getCodingFirstRep()))
                            .isEqualTo(HashableCoding.ofFhirCoding(STATUS_DEF_CODING.toCoding()));
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(1).getStratum().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(STATUS_VALUE_CODING);
                    assertThat(findPopulationByCode(result.getStratifier().get(1).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);
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
                                                    new Measure.MeasureGroupStratifierComponentComponent(COND_STATUS_PATH).setCode(new CodeableConcept(STATUS_DEF_CODING.toCoding()))))
                                            .setCode(new CodeableConcept(COND_DEF_CODING))))
                            .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                    assertThat(result.getStratifier().size()).isEqualTo(1);
                    assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);
                    assertThat(result.getStratifier().get(0).getStratum().get(0).getComponent().size()).isEqualTo(2);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getComponent().get(0).getCode().getCodingFirstRep()))
                            .isEqualTo(STATUS_DEF_CODING);
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getComponent().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(STATUS_VALUE_CODING);
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getComponent().get(1).getCode().getCodingFirstRep()))
                            .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getComponent().get(1).getValue().getCodingFirstRep()))
                            .isEqualTo(COND_VALUE_CODING);
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);
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
                    assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                    assertThat(result.getStratifier().size()).isEqualTo(1);
                    assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);
                    assertThat(result.getStratifier().get(0).getStratum().get(0).getComponent().size()).isEqualTo(2);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getComponent().get(0).getCode().getCodingFirstRep()))
                            .isEqualTo(new HashableCoding(COND_DEF_SYSTEM, "some-other-code", SOME_DISPLAY));
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getComponent().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(COND_VALUE_CODING);
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getComponent().get(1).getCode().getCodingFirstRep()))
                            .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getComponent().get(1).getValue().getCodingFirstRep()))
                            .isEqualTo(COND_VALUE_CODING);
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);
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
                                                    new Measure.MeasureGroupStratifierComponentComponent(COND_STATUS_PATH).setCode(new CodeableConcept(STATUS_DEF_CODING.toCoding()))))))
                            .setPopulation(List.of(getInitialPopulation(CONDITION_QUERY)));
                    GroupEvaluator groupEvaluator = new GroupEvaluator(dataStore, pathEngine);

                    var result = groupEvaluator.evaluateGroup(measureGroup).block();

                    assertThat(result).isNotNull();
                    assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(4);
                    assertThat(result.getStratifier().size()).isEqualTo(1);
                    assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(4);
                    assertThat(result.getStratifier().get(0).getStratum().get(0).getComponent().size()).isEqualTo(2);
                    assertThat(result.getStratifier().get(0).getStratum().get(1).getComponent().size()).isEqualTo(2);
                    assertThat(result.getStratifier().get(0).getStratum().get(2).getComponent().size()).isEqualTo(2);
                    assertThat(result.getStratifier().get(0).getStratum().get(3).getComponent().size()).isEqualTo(2);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getComponent().get(0).getCode().getCodingFirstRep()))
                            .isEqualTo(statusValueKeypair_1.code());
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getComponent().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(statusValueKeypair_1.value());
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getComponent().get(1).getCode().getCodingFirstRep()))
                            .isEqualTo(condValueKeypair_2.code());
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getComponent().get(1).getValue().getCodingFirstRep()))
                            .isEqualTo(condValueKeypair_2.value());
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(1).getComponent().get(0).getCode().getCodingFirstRep()))
                            .isEqualTo(statusValueKeypair_2.code());
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(1).getComponent().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(statusValueKeypair_2.value());
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(1).getComponent().get(1).getCode().getCodingFirstRep()))
                            .isEqualTo(condValueKeypair_1.code());
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(1).getComponent().get(1).getValue().getCodingFirstRep()))
                            .isEqualTo(condValueKeypair_1.value());
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(2).getComponent().get(0).getCode().getCodingFirstRep()))
                            .isEqualTo(statusValueKeypair_2.code());
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(2).getComponent().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(statusValueKeypair_2.value());
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(2).getComponent().get(1).getCode().getCodingFirstRep()))
                            .isEqualTo(condValueKeypair_2.code());
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(2).getComponent().get(1).getValue().getCodingFirstRep()))
                            .isEqualTo(condValueKeypair_2.value());
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);

                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(3).getComponent().get(0).getCode().getCodingFirstRep()))
                            .isEqualTo(statusValueKeypair_1.code());
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(3).getComponent().get(0).getValue().getCodingFirstRep()))
                            .isEqualTo(statusValueKeypair_1.value());
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(3).getComponent().get(1).getCode().getCodingFirstRep()))
                            .isEqualTo(condValueKeypair_1.code());
                    assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(3).getComponent().get(1).getValue().getCodingFirstRep()))
                            .isEqualTo(condValueKeypair_1.value());
                    assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                            .isEqualTo(1);
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
                assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                assertThat(result.getStratifier().size()).isEqualTo(1);
                assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

                assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                        .isEqualTo(HashableCoding.ofFhirCoding(QUANTITY_DEF_CODING));
                assertThat(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep().getCode())
                        .isEqualTo(QUANTITY_VALUE_KEYPAIR.value().code());
                assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                        .isEqualTo(1);

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
                assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
                assertThat(result.getStratifier().size()).isEqualTo(1);
                assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

                assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                        .isEqualTo(HashableCoding.ofFhirCoding(GENDER_DEF_CODING));
                assertThat(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep().getCode())
                        .isEqualTo(GENDER_VALUE_KEYPAIR.value().code());
                assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                        .isEqualTo(1);
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
            assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
            assertThat(result.getStratifier().size()).isEqualTo(1);
            assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

            assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                    .isEqualTo(HashableCoding.ofFhirCoding(GENDER_DEF_CODING));
            assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep()))
                    .isEqualTo(FAIL_NO_VALUE_FOUND);
            assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                    .isEqualTo(1);
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
            assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
            assertThat(result.getStratifier().size()).isEqualTo(1);
            assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

            assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                    .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
            assertThat(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep().getCode())
                    .isEqualTo(COND_CODE_EXISTS_TRUE.value().code());
            assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                    .isEqualTo(1);

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
            assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
            assertThat(result.getStratifier().size()).isEqualTo(1);
            assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

            assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                    .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
            assertThat(result.getStratifier().get(0).getStratum().get(0).getValue().getCodingFirstRep().getCode())
                    .isEqualTo(COND_CODE_EXISTS_FALSE.value().code());
            assertThat(findPopulationByCode(result.getStratifier().get(0).getStratum().get(0), INITIAL_POPULATION_CODING).getCount())
                    .isEqualTo(1);
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
            assertThat(result.getStratifier().size()).isEqualTo(1);
            assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

            assertThat(result).isNotNull();
            assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(2);
            assertThat(findPopulationByCode(result, MEASURE_POPULATION_CODING).getCount()).isEqualTo(2);
            assertThat(findPopulationByCode(result, MEASURE_OBSERVATION_CODING).getCount()).isEqualTo(2);
            assertThat(result.getMeasureScore().getValue()).isEqualTo(new BigDecimal(1));

            assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                    .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
            var firstStratum = result.getStratifier().get(0).getStratum().get(0);
            assertThat(HashableCoding.ofFhirCoding(firstStratum.getValue().getCodingFirstRep())).isEqualTo(COND_VALUE_CODING);
            assertThat(findPopulationByCode(firstStratum, INITIAL_POPULATION_CODING).getCount()).isEqualTo(2);
            assertThat(findPopulationByCode(firstStratum, MEASURE_POPULATION_CODING).getCount()).isEqualTo(2);
            assertThat(findPopulationByCode(firstStratum, MEASURE_OBSERVATION_CODING).getCount()).isEqualTo(2);
            assertThat(firstStratum.getMeasureScore().getValue()).isEqualTo(new BigDecimal(1));
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
            assertThat(result.getStratifier().size()).isEqualTo(1);
            assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

            assertThat(result).isNotNull();
            assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(2);
            assertThat(findPopulationByCode(result, MEASURE_POPULATION_CODING).getCount()).isEqualTo(2);
            assertThat(findPopulationByCode(result, MEASURE_OBSERVATION_CODING).getCount()).isEqualTo(2);
            assertThat(result.getMeasureScore().getValue()).isEqualTo(new BigDecimal(2));

            assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                    .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
            var firstStratum = result.getStratifier().get(0).getStratum().get(0);
            assertThat(HashableCoding.ofFhirCoding(firstStratum.getValue().getCodingFirstRep())).isEqualTo(COND_VALUE_CODING);
            assertThat(findPopulationByCode(firstStratum, INITIAL_POPULATION_CODING).getCount()).isEqualTo(2);
            assertThat(findPopulationByCode(firstStratum, MEASURE_POPULATION_CODING).getCount()).isEqualTo(2);
            assertThat(findPopulationByCode(firstStratum, MEASURE_OBSERVATION_CODING).getCount()).isEqualTo(2);
            assertThat(firstStratum.getMeasureScore().getValue()).isEqualTo(new BigDecimal(2));
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
            assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(3);
            assertThat(findPopulationByCode(result, MEASURE_POPULATION_CODING).getCount()).isEqualTo(2);
            assertThat(findPopulationByCode(result, MEASURE_OBSERVATION_CODING).getCount()).isEqualTo(2);
            assertThat(result.getMeasureScore().getValue()).isEqualTo(new BigDecimal(1));

            assertThat(result.getStratifier().size()).isEqualTo(1);
            assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

            assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                    .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
            var firstStratum = result.getStratifier().get(0).getStratum().get(0);
            assertThat(HashableCoding.ofFhirCoding(firstStratum.getValue().getCodingFirstRep())).isEqualTo(COND_VALUE_CODING);
            assertThat(findPopulationByCode(firstStratum, INITIAL_POPULATION_CODING).getCount()).isEqualTo(3);
            assertThat(findPopulationByCode(firstStratum, MEASURE_POPULATION_CODING).getCount()).isEqualTo(2);
            assertThat(findPopulationByCode(firstStratum, MEASURE_OBSERVATION_CODING).getCount()).isEqualTo(2);
            assertThat(firstStratum.getMeasureScore().getValue()).isEqualTo(new BigDecimal(1));
        }


        @Test
        @DisplayName("Two Conditions with same value and one Condition with no value, leading to a different Measure Observation Population count")
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
            assertThat(result.getStratifier().size()).isEqualTo(1);
            assertThat(result.getStratifier().get(0).getStratum().size()).isEqualTo(1);

            assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(3);
            assertThat(findPopulationByCode(result, MEASURE_POPULATION_CODING).getCount()).isEqualTo(3);
            assertThat(findPopulationByCode(result, MEASURE_OBSERVATION_CODING).getCount()).isEqualTo(2);
            assertThat(result.getMeasureScore().getValue()).isEqualTo(new BigDecimal(1));

            assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                    .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
            var firstStratum = result.getStratifier().get(0).getStratum().get(0);
            assertThat(HashableCoding.ofFhirCoding(firstStratum.getValue().getCodingFirstRep())).isEqualTo(COND_VALUE_CODING);
            assertThat(findPopulationByCode(firstStratum, INITIAL_POPULATION_CODING).getCount()).isEqualTo(3);
            assertThat(findPopulationByCode(firstStratum, MEASURE_POPULATION_CODING).getCount()).isEqualTo(3);
            assertThat(findPopulationByCode(firstStratum, MEASURE_OBSERVATION_CODING).getCount()).isEqualTo(2);
            assertThat(firstStratum.getMeasureScore().getValue()).isEqualTo(new BigDecimal(1));
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
            assertThat(findPopulationByCode(result, INITIAL_POPULATION_CODING).getCount()).isEqualTo(2);
            assertThat(findPopulationByCode(result, MEASURE_POPULATION_CODING).getCount()).isEqualTo(2);
            assertThat(findPopulationByCode(result, MEASURE_OBSERVATION_CODING).getCount()).isEqualTo(2);
            assertThat(result.getMeasureScore().getValue()).isEqualTo(new BigDecimal(1));

            assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                    .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
            var firstStratum = result.getStratifier().get(0).getStratum().get(0);
            assertThat(HashableCoding.ofFhirCoding(firstStratum.getValue().getCodingFirstRep())).isEqualTo(COND_VALUE_CODING_1);
            assertThat(findPopulationByCode(firstStratum, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
            assertThat(findPopulationByCode(firstStratum, MEASURE_POPULATION_CODING).getCount()).isEqualTo(1);
            assertThat(findPopulationByCode(firstStratum, MEASURE_OBSERVATION_CODING).getCount()).isEqualTo(1);
            assertThat(firstStratum.getMeasureScore().getValue()).isEqualTo(new BigDecimal(1));

            assertThat(HashableCoding.ofFhirCoding(result.getStratifier().get(0).getCode().get(0).getCodingFirstRep()))
                    .isEqualTo(HashableCoding.ofFhirCoding(COND_DEF_CODING));
            var secondStratum = result.getStratifier().get(0).getStratum().get(1);
            assertThat(HashableCoding.ofFhirCoding(secondStratum.getValue().getCodingFirstRep())).isEqualTo(COND_VALUE_CODING_2);
            assertThat(findPopulationByCode(secondStratum, INITIAL_POPULATION_CODING).getCount()).isEqualTo(1);
            assertThat(findPopulationByCode(secondStratum, MEASURE_POPULATION_CODING).getCount()).isEqualTo(1);
            assertThat(findPopulationByCode(secondStratum, MEASURE_OBSERVATION_CODING).getCount()).isEqualTo(1);
            assertThat(firstStratum.getMeasureScore().getValue()).isEqualTo(new BigDecimal(1));
        }
    }
}
