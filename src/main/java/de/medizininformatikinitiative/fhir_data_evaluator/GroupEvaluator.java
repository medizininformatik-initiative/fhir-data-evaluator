package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import reactor.core.publisher.Mono;

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

        var parsedStratifiers = group.getStratifier().stream().map(fhirStratifier -> ParsedStratifier.fromFhirStratifier(fhirPathEngine, fhirStratifier)).toList();
        var groupReduceOp = new GroupReduceOp(parsedStratifiers.stream().map(s -> new StratifierReduceOp(s.componentExpressions())).toList());
        var stratifierCodes = parsedStratifiers.stream().map(ParsedStratifier::code).toList();

        return population.reduce(GroupResult.initial(stratifierCodes), groupReduceOp);
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
}
