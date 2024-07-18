package de.medizininformatikinitiative.fhir_data_evaluator;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.AggregateUniqueCount;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialAndMeasureAndObsPopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialAndMeasurePopulation;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.InitialPopulation;
import org.hl7.fhir.r4.model.ExpressionNode;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.*;
import static java.util.Objects.requireNonNull;

public class GroupEvaluator {

    final String FHIR_QUERY = "text/x-fhir-query";
    final String FHIR_PATH = "text/fhirpath";
    final String CRITERIA_REFERENCE_URL = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference";
    final String CRITERIA_REFERENCE_VALUE = "measure-population-identifier";

    private final DataStore dataStore;
    private final FHIRPathEngine fhirPathEngine;

    public GroupEvaluator(DataStore dataStore, FHIRPathEngine fhirPathEngine) {
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
        var population = dataStore.getPopulation("/" +
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
        // TODO java docs (especially for different GroupReduceOps)

        return evaluateGroupOfInitialAndMeasureAndObs(population, group, measurePopulationExpression.get(), observationPopulationExpression.get());
    }

    private Mono<MeasureReport.MeasureReportGroupComponent> evaluateGroupOfInitial(Flux<Resource> population, Measure.MeasureGroupComponent group) {
        var groupReduceOp = new GroupReduceOpInitial(group.getStratifier().stream().map(s ->
                new StratifierReduceOp<InitialPopulation>(getComponentExpressions(s))).toList());

        List<StratifierResult<InitialPopulation>> initialStratifierResults = group.getStratifier().stream().map(s ->
                StratifierResult.initial(s, InitialPopulation.class)).toList();
        return population.reduce(new GroupResult<>(InitialPopulation.ZERO, initialStratifierResults), groupReduceOp)
                .map(GroupResult::toReportGroup);
    }
    private Mono<MeasureReport.MeasureReportGroupComponent> evaluateGroupOfInitialAndMeasure(Flux<Resource> population,
                                                                                             Measure.MeasureGroupComponent group,
                                                                                             ExpressionNode measurePopulationExpression) {
        var groupReduceOp = new GroupReduceOpMeasure(group.getStratifier().stream().map(s ->
                new StratifierReduceOp<InitialAndMeasurePopulation>(getComponentExpressions(s))).toList(),
                measurePopulationExpression, fhirPathEngine);

        List<StratifierResult<InitialAndMeasurePopulation>> initialStratifierResults = group.getStratifier().stream().map(s ->
                StratifierResult.initial(s, InitialAndMeasurePopulation.class)).toList();
        return population.reduce(new GroupResult<>(InitialAndMeasurePopulation.ZERO, initialStratifierResults), groupReduceOp)
                .map(GroupResult::toReportGroup);
    }

    private Mono<MeasureReport.MeasureReportGroupComponent> evaluateGroupOfInitialAndMeasureAndObs(Flux<Resource> population,
                                                                                                   Measure.MeasureGroupComponent group,
                                                                                                   ExpressionNode measurePopulationExpression,
                                                                                                   ExpressionNode observationPopulationExpression) {
        var groupReduceOp = new GroupReduceOpObservation(group.getStratifier().stream().map(s ->
                new StratifierReduceOp<InitialAndMeasureAndObsPopulation>(getComponentExpressions(s))).toList(),
                measurePopulationExpression, observationPopulationExpression, fhirPathEngine);

        List<StratifierResult<InitialAndMeasureAndObsPopulation>> initialStratifierResults = group.getStratifier().stream().map(s ->
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

    private Optional<ExpressionNode> findMeasurePopulationExpression(Measure.MeasureGroupComponent group) {
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

        return Optional.of(fhirPathEngine.parse(foundMeasurePopulation.getCriteria().getExpression()));
    }

    private Optional<ExpressionNode> findObservationPopulationExpression(Measure.MeasureGroupComponent group) {
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
        if (!criteriaReferences.get(0).getValue().toString().equals(CRITERIA_REFERENCE_VALUE))
            throw new IllegalArgumentException("Value of Criteria Reference of Measure Observation Population must be equal to '%s'".formatted(CRITERIA_REFERENCE_VALUE));

        var aggregateMethods = foundObservationPopulation.getExtensionsByUrl(AggregateUniqueCount.EXTENSION_URL);
        if (aggregateMethods.size() != 1)
            throw new IllegalArgumentException("Measure Observation Population did not contain exactly one aggregate method");
        if (!aggregateMethods.get(0).hasValue())
            throw new IllegalArgumentException("Aggregate Method of Measure Observation Population has no value");

        if (!aggregateMethods.get(0).getValue().toString().equals(AggregateUniqueCount.EXTENSION_VALUE)) {
            throw new IllegalArgumentException("Aggregate Method of Measure Observation Population has not value '%s'".formatted(AggregateUniqueCount.EXTENSION_VALUE));
        }

        return Optional.of(fhirPathEngine.parse(foundObservationPopulation.getCriteria().getExpression()));
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
        return List.of(ComponentExpression.fromCriteria(fhirPathEngine, fhirStratifier));
    }

    private List<ComponentExpression> getComponentExpressionsFromComponents(Measure.MeasureGroupStratifierComponent fhirStratifier) {
        return fhirStratifier.getComponent().stream()
                .map(component -> ComponentExpression.fromComponent(fhirPathEngine, component))
                .toList();
    }
}
