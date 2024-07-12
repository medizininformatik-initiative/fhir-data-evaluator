package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import reactor.core.publisher.Mono;

import java.util.HashSet;
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
     * @return a {@code Mono} of the {@code GroupResult}
     * @throws IllegalArgumentException if the group doesn't have exactly one initial population
     */
    public Mono<GroupResult> evaluateGroup(Measure.MeasureGroupComponent group) {
        var population = dataStore.getPopulation("/" +
                findFhirInitialPopulation(group).getCriteria().getExpressionElement());

        var populationsTemplate = createPopulationsTemplate(group);
        var groupReduceOp = new GroupReduceOp(group.getStratifier().stream().map(s ->
                new StratifierReduceOp(getComponentExpressions(s), populationsTemplate)).toList(),
                populationsTemplate);

        var initialStratifierResults = group.getStratifier().stream().map(StratifierResult::initial).toList();

        return population.reduce(GroupResult.initial(initialStratifierResults, populationsTemplate.copy()), groupReduceOp);
    }

    private Populations createPopulationsTemplate(Measure.MeasureGroupComponent group) {
        var measurePopulation = findMeasurePopulation(group);
        var observationPopulation = findObservationPopulation(group);

        if (measurePopulation.isEmpty() && observationPopulation.isPresent()) {
            throw new IllegalArgumentException("Group must not contain a Measure Observation without a Measure Population");
        }

        return new Populations(InitialPopulation.ZERO, measurePopulation, observationPopulation);
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

    private Optional<MeasurePopulation> findMeasurePopulation(Measure.MeasureGroupComponent group) {
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

        return Optional.of(new MeasurePopulation(fhirPathEngine, 0, fhirPathEngine.parse(foundMeasurePopulation.getCriteria().getExpression())));
    }

    private Optional<ObservationPopulation> findObservationPopulation(Measure.MeasureGroupComponent group) {
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

        return Optional.of(new ObservationPopulation(fhirPathEngine, 0, fhirPathEngine.parse(foundObservationPopulation.getCriteria().getExpression()), new AggregateUniqueCount(new HashSet<>())));
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
