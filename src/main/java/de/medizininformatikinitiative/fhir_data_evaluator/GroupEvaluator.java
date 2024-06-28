package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.*;
import static java.util.Objects.requireNonNull;

public class GroupEvaluator {

    final String INITIAL_POPULATION_LANGUAGE = "text/x-fhir-query";
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
                new StratifierReduceOp(getComponentExpressions(fhirPathEngine, s), populationsTemplate, fhirPathEngine)).toList(),
                populationsTemplate, fhirPathEngine);

        var initialStratifierResults = group.getStratifier().stream().map(StratifierResult::initial).toList();

        return population.reduce(GroupResult.initial(initialStratifierResults, populationsTemplate.copy()), groupReduceOp);
    }

    private Populations createPopulationsTemplate(Measure.MeasureGroupComponent group) {
        var measurePopulation = findMeasurePopulation(group);
        var observationPopulation = findObservationPopulation(group);
        return new Populations(InitialPopulation.ZERO, measurePopulation, observationPopulation);
    }

    private Measure.MeasureGroupPopulationComponent findFhirInitialPopulation(Measure.MeasureGroupComponent group) {
        var foundInitialPopulations = findPopulationsByCode(group, INITIAL_POPULATION_CODING);

        if (foundInitialPopulations.size() != 1) {
            throw new IllegalArgumentException("Measure did not contain exactly one initial population");
        }

        var foundInitialPopulation = foundInitialPopulations.get(0);

        if (!foundInitialPopulation.getCriteria().getLanguage().equals(INITIAL_POPULATION_LANGUAGE)) {
            throw new IllegalArgumentException("Language of Initial Population was not equal to '%s'".formatted(INITIAL_POPULATION_LANGUAGE));
        }

        return foundInitialPopulation;
    }

    private Optional<MeasurePopulation> findMeasurePopulation(Measure.MeasureGroupComponent group) {
        var foundMeasurePopulation = findPopulationsByCode(group, MEASURE_POPULATION_CODING);
        if (foundMeasurePopulation.isEmpty()) {
            return Optional.empty();
        }

        if (foundMeasurePopulation.size() > 1) {
            throw new IllegalArgumentException("Measure did contain more than one measure population");
        }

        return Optional.of(new MeasurePopulation(0, fhirPathEngine.parse(foundMeasurePopulation.get(0).getCriteria().getExpression())));
    }

    private Optional<ObservationPopulation> findObservationPopulation(Measure.MeasureGroupComponent group) {
        var foundObservationPopulation = findPopulationsByCode(group, MEASURE_OBSERVATION_CODING);

        if (foundObservationPopulation.isEmpty()) {
            return Optional.empty();
        }

        if (foundObservationPopulation.size() > 1) {
            throw new IllegalArgumentException("Measure did contain more than one observation population");
        }

        var criteriaReferences = foundObservationPopulation.get(0).getExtensionsByUrl(CRITERIA_REFERENCE_URL);
        if (criteriaReferences.size() != 1)
            throw new IllegalArgumentException("Measure Observation Population did not contain exactly one aggregate method");
        if (!criteriaReferences.get(0).hasValue())
            throw new IllegalArgumentException("Criteria Reference of Measure Observation Population has no value");
        if (!criteriaReferences.get(0).getValue().toString().equals(CRITERIA_REFERENCE_VALUE))
            throw new IllegalArgumentException("Value of Criteria Reference of Measure Observation Population must be equal to %s".formatted(CRITERIA_REFERENCE_VALUE));

        var aggregateMethods = foundObservationPopulation.get(0).getExtensionsByUrl(AggregateUniqueCount.EXTENSION_URL);
        if (aggregateMethods.size() != 1)
            throw new IllegalArgumentException("Measure Observation Population did not contain exactly one aggregate method");
        if (!aggregateMethods.get(0).hasValue())
            throw new IllegalArgumentException("Aggregate Method of Measure Observation Population has no value");

        if (aggregateMethods.get(0).getValue() instanceof CodeType c) {
            if (!Objects.equals(c.getCode(), AggregateUniqueCount.EXTENSION_VALUE)) {
                throw new IllegalArgumentException("Aggregate Method of Measure Observation Population has not value %s".formatted(AggregateUniqueCount.EXTENSION_VALUE));
            }
            return Optional.of(new ObservationPopulation(0, fhirPathEngine.parse(foundObservationPopulation.get(0).getCriteria().getExpression()), new AggregateUniqueCount(new HashSet<>())));
        } else {
            throw new IllegalArgumentException("Value of aggregate method was not of type Coding");
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

    private static List<ComponentExpression> getComponentExpressions(FHIRPathEngine fhirPathEngine, Measure.MeasureGroupStratifierComponent fhirStratifier) {
        if (fhirStratifier.hasCriteria() && !fhirStratifier.hasComponent()) {
            return getComponentExpressionsFromCriteria(fhirPathEngine, fhirStratifier);
        }

        if (fhirStratifier.hasComponent() && !fhirStratifier.hasCriteria()) {
            return getComponentExpressionsFromComponents(fhirPathEngine, fhirStratifier);
        }

        throw new IllegalArgumentException("Stratifier did not contain either criteria or component exclusively");
    }

    private static List<ComponentExpression> getComponentExpressionsFromCriteria(FHIRPathEngine fhirPathEngine, Measure.MeasureGroupStratifierComponent fhirStratifier) {
        return List.of(ComponentExpression.fromCriteria(fhirPathEngine, fhirStratifier));
    }

    private static List<ComponentExpression> getComponentExpressionsFromComponents(FHIRPathEngine fhirPathEngine, Measure.MeasureGroupStratifierComponent fhirStratifier) {
        return fhirStratifier.getComponent().stream()
                .map(component -> ComponentExpression.fromComponent(fhirPathEngine, component))
                .toList();
    }
}
