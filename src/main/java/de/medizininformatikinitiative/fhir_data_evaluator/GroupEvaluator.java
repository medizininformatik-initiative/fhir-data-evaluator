package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import reactor.core.publisher.Mono;

import java.util.List;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.INITIAL_POPULATION_CODING;
import static java.util.Objects.requireNonNull;

public class GroupEvaluator {

    final String INITIAL_POPULATION_LANGUAGE = "text/x-fhir-query";

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
        var initialPopulation = findInitialPopulation(group);
        var population = dataStore.getPopulation("/" +
                initialPopulation.getCriteria().getExpressionElement());

        var groupReduceOp = new GroupReduceOp(group.getStratifier().stream().map(s ->
                new StratifierReduceOp(getComponentExpressions(fhirPathEngine, s))).toList());

        var initialStratifierResults = group.getStratifier().stream().map(s ->
                        StratifierResult.initial(s.hasCode() ? HashableCoding.ofFhirCoding(s.getCode().getCodingFirstRep()) : null))
                .toList();

        return population.reduce(GroupResult.initial(initialStratifierResults), groupReduceOp);
    }

    private Measure.MeasureGroupPopulationComponent findInitialPopulation(Measure.MeasureGroupComponent group) {
        var foundInitialPopulations = group.getPopulation().stream().filter(population -> {
            var codings = population.getCode().getCoding();

            if (codings.size() != 1) {
                throw new IllegalArgumentException("Population in Measure did not contain exactly one Coding");
            }

            return INITIAL_POPULATION_CODING.equals(HashableCoding.ofFhirCoding(codings.get(0)));
        }).toList();

        if (foundInitialPopulations.size() != 1) {
            throw new IllegalArgumentException("Measure did not contain exactly one initial population");
        }

        var foundInitialPopulation = foundInitialPopulations.get(0);

        if (!foundInitialPopulation.getCriteria().getLanguage().equals(INITIAL_POPULATION_LANGUAGE)) {
            throw new IllegalArgumentException("Language of Initial Population was not equal to '%s'".formatted(INITIAL_POPULATION_LANGUAGE));
        }

        return foundInitialPopulation;
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
