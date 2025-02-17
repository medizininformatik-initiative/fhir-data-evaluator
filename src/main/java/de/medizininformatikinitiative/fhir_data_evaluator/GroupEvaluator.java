package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.fhirpath.IFhirPath;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialAndMeasurePopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialPopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.InitialAndMeasureAndObsIndividual;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.InitialAndMeasureIndividual;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.InitialIndividual;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.mutable.AggregateUniqueCounter;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.mutable.InitialAndMeasureAndObsPopulation;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.INITIAL_POPULATION_CODING;
import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.MEASURE_OBSERVATION_CODING;
import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.MEASURE_POPULATION_CODING;
import static java.util.Objects.requireNonNull;

public class GroupEvaluator {

    final String FHIR_QUERY = "text/x-fhir-query";
    final String FHIR_PATH = "text/fhirpath";
    final String CRITERIA_REFERENCE_URL = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference";

    private final DataStore dataStore;
    private final IFhirPath fhirPathEngine;

    public GroupEvaluator(DataStore dataStore, IFhirPath fhirPathEngine) {
        this.dataStore = requireNonNull(dataStore);
        this.fhirPathEngine = requireNonNull(fhirPathEngine);
    }

    /**
     * Evaluates {@code group}.
     *
     * @param group the group to evaluate
     * @return a {@code Mono} of the {@link MeasureReport.MeasureReportGroupComponent}
     * @throws IllegalArgumentException if the group doesn't have exactly one initial population
     */
    public Mono<MeasureReport.MeasureReportGroupComponent> evaluateGroup(Measure.MeasureGroupComponent group) {
        var population = dataStore.getResources("/" +
                findFhirInitialPopulation(group).getCriteria().getExpressionElement());

        var measurePopulationExpression = findMeasurePopulationExpression(group);
        var observationPopulationExpression = findObservationPopulationExpression(group);
        if (measurePopulationExpression.isEmpty() && observationPopulationExpression.isPresent()) {
            throw new IllegalArgumentException("Group must not contain a Measure Observation without a Measure Population");
        }

        if (measurePopulationExpression.isEmpty()) {
            return evaluateGroupOfInitial(population, group);
        }
        if (observationPopulationExpression.isEmpty()) {
            return evaluateGroupOfInitialAndMeasure(population, group, measurePopulationExpression.get());
        }

        return evaluateGroupOfInitialAndMeasureAndObs(population, group, measurePopulationExpression.get(), observationPopulationExpression.get());
    }

    private Mono<MeasureReport.MeasureReportGroupComponent> evaluateGroupOfInitial(Flux<ResourceWithIncludes> population, Measure.MeasureGroupComponent group) {
        var groupReduceOp = new GroupReduceOpInitial(group.getStratifier().stream().map(s ->
                new StratifierReduceOp<InitialPopulation, InitialIndividual>(getComponentExpressions(s))).toList());

        List<StratifierResult<InitialPopulation, InitialIndividual>> initialStratifierResults = group.getStratifier().stream().map(s ->
                StratifierResult.initial(s, InitialPopulation.class)).toList();
        return population.reduce(new GroupResult<>(InitialPopulation.ZERO, initialStratifierResults), groupReduceOp)
                .map(GroupResult::toReportGroup);
    }

    private Mono<MeasureReport.MeasureReportGroupComponent> evaluateGroupOfInitialAndMeasure(Flux<ResourceWithIncludes> population,
                                                                                             Measure.MeasureGroupComponent group,
                                                                                             IFhirPath.IParsedExpression measurePopulationExpression) {
        var groupReduceOp = new GroupReduceOpMeasure(group.getStratifier().stream().map(s ->
                new StratifierReduceOp<InitialAndMeasurePopulation, InitialAndMeasureIndividual>(getComponentExpressions(s))).toList(),
                measurePopulationExpression);

        List<StratifierResult<InitialAndMeasurePopulation, InitialAndMeasureIndividual>> initialStratifierResults = group.getStratifier().stream().map(s ->
                StratifierResult.initial(s, InitialAndMeasurePopulation.class)).toList();
        return population.reduce(new GroupResult<>(InitialAndMeasurePopulation.ZERO, initialStratifierResults), groupReduceOp)
                .map(GroupResult::toReportGroup);
    }

    private Mono<MeasureReport.MeasureReportGroupComponent> evaluateGroupOfInitialAndMeasureAndObs(Flux<ResourceWithIncludes> population,
                                                                                                   Measure.MeasureGroupComponent group,
                                                                                                   IFhirPath.IParsedExpression measurePopulationExpression,
                                                                                                   IFhirPath.IParsedExpression observationPopulationExpression) {
        var groupReduceOp = new GroupReduceOpObservation(group.getStratifier().stream().map(s ->
                new StratifierReduceOp<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual>(getComponentExpressions(s))).toList(),
                measurePopulationExpression, observationPopulationExpression);

        List<StratifierResult<InitialAndMeasureAndObsPopulation, InitialAndMeasureAndObsIndividual>> initialStratifierResults = group.getStratifier().stream().map(s ->
                StratifierResult.initial(s, InitialAndMeasureAndObsPopulation.class)).toList();
        return population.reduce(new GroupResult<>(InitialAndMeasureAndObsPopulation.empty(), initialStratifierResults), groupReduceOp)
                .map(GroupResult::toReportGroup);
    }

    private Measure.MeasureGroupPopulationComponent findFhirInitialPopulation(Measure.MeasureGroupComponent group) {
        var foundInitialPopulations = findPopulationsByCode(group, INITIAL_POPULATION_CODING);

        if (foundInitialPopulations.size() != 1) {
            throw new IllegalArgumentException("Measure did not contain exactly one initial population");
        }

        var foundInitialPopulation = foundInitialPopulations.get(0);

        if (!foundInitialPopulation.getCriteria().getLanguage().equals(FHIR_QUERY)) {
            throw new IllegalArgumentException("Language of Initial Population was not equal to '%s'".formatted(FHIR_QUERY));
        }

        return foundInitialPopulation;
    }

    private Optional<IFhirPath.IParsedExpression> findMeasurePopulationExpression(Measure.MeasureGroupComponent group) {
        var foundMeasurePopulations = findPopulationsByCode(group, MEASURE_POPULATION_CODING);
        if (foundMeasurePopulations.isEmpty()) {
            return Optional.empty();
        }

        if (foundMeasurePopulations.size() > 1) {
            throw new IllegalArgumentException("Measure did contain more than one measure population");
        }

        var foundMeasurePopulation = foundMeasurePopulations.get(0);
        if (!foundMeasurePopulation.getCriteria().getLanguage().equals(FHIR_PATH)) {
            throw new IllegalArgumentException("Language of Measure Population was not equal to '%s'".formatted(FHIR_PATH));
        }

        try {
            return Optional.of(fhirPathEngine.parse(foundMeasurePopulation.getCriteria().getExpression()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<IFhirPath.IParsedExpression> findObservationPopulationExpression(Measure.MeasureGroupComponent group) {
        var foundObservationPopulations = findPopulationsByCode(group, MEASURE_OBSERVATION_CODING);

        if (foundObservationPopulations.isEmpty()) {
            return Optional.empty();
        }

        if (foundObservationPopulations.size() > 1) {
            throw new IllegalArgumentException("Measure did contain more than one observation population");
        }

        var foundObservationPopulation = foundObservationPopulations.get(0);
        if (!foundObservationPopulation.getCriteria().getLanguage().equals(FHIR_PATH)) {
            throw new IllegalArgumentException("Language of Measure Observation was not equal to '%s'".formatted(FHIR_PATH));
        }

        var criteriaReferences = foundObservationPopulation.getExtensionsByUrl(CRITERIA_REFERENCE_URL);
        if (criteriaReferences.size() != 1)
            throw new IllegalArgumentException("Measure Observation Population did not contain exactly one criteria reference");
        if (!criteriaReferences.get(0).hasValue())
            throw new IllegalArgumentException("Criteria Reference of Measure Observation Population has no value");

        var measurePopulations = findPopulationsByCode(group, MEASURE_POPULATION_CODING);
        if (!measurePopulations.isEmpty()) {
            if (!criteriaReferences.get(0).getValue().toString().equals(measurePopulations.get(0).getId()))
                throw new IllegalArgumentException("Value of Criteria Reference of Measure Observation Population must be equal to the ID of the Measure Population");
        }

        var aggregateMethods = foundObservationPopulation.getExtensionsByUrl(AggregateUniqueCounter.EXTENSION_URL);
        if (aggregateMethods.size() != 1)
            throw new IllegalArgumentException("Measure Observation Population did not contain exactly one aggregate method");
        if (!aggregateMethods.get(0).hasValue())
            throw new IllegalArgumentException("Aggregate Method of Measure Observation Population has no value");

        if (!aggregateMethods.get(0).getValue().toString().equals(AggregateUniqueCounter.EXTENSION_VALUE)) {
            throw new IllegalArgumentException("Aggregate Method of Measure Observation Population has not value '%s'".formatted(AggregateUniqueCounter.EXTENSION_VALUE));
        }

        try {
            return Optional.of(fhirPathEngine.parse(foundObservationPopulation.getCriteria().getExpression()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Measure.MeasureGroupPopulationComponent> findPopulationsByCode(Measure.MeasureGroupComponent group, HashableCoding code) {
        return group.getPopulation().stream().filter(population -> {
            var codings = population.getCode().getCoding();

            if (codings.size() != 1) {
                throw new IllegalArgumentException("Population in Measure did not contain exactly one Coding");
            }

            return code.equals(HashableCoding.ofFhirCoding(codings.get(0)));
        }).toList();
    }

    private List<ComponentExpression> getComponentExpressions(Measure.MeasureGroupStratifierComponent fhirStratifier) {
        if (fhirStratifier.hasCriteria() && !fhirStratifier.hasComponent()) {
            return getComponentExpressionsFromCriteria(fhirStratifier);
        }

        if (fhirStratifier.hasComponent() && !fhirStratifier.hasCriteria()) {
            return getComponentExpressionsFromComponents(fhirStratifier);
        }

        throw new IllegalArgumentException("Stratifier did not contain either criteria or component exclusively");
    }

    private List<ComponentExpression> getComponentExpressionsFromCriteria(Measure.MeasureGroupStratifierComponent fhirStratifier) {
        try {
            return List.of(ComponentExpression.fromCriteria(fhirPathEngine, fhirStratifier));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<ComponentExpression> getComponentExpressionsFromComponents(Measure.MeasureGroupStratifierComponent fhirStratifier) {
        return fhirStratifier.getComponent().stream()
                .map(component -> ComponentExpression.fromComponent(fhirPathEngine, component)).toList();
    }
}
