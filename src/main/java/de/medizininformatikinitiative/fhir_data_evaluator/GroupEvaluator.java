package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.INITIAL_POPULATION_CODING;

public class GroupEvaluator {
    final String INITIAL_POPULATION_LANGUAGE = "text/x-fhir-query";

    private final DataStore dataStore;
    private final FHIRPathEngine fhirPathEngine;

    public GroupEvaluator(DataStore dataStore, FHIRPathEngine fhirPathEngine) {
        this.dataStore = dataStore;
        this.fhirPathEngine = fhirPathEngine;
    }


    public Mono<GroupResult> evaluateGroup(Measure.MeasureGroupComponent group) {
        var initialPopulation = getInitialPopulation(group);
        var population = dataStore.getPopulation("/" +
                initialPopulation.getCriteria().getExpressionElement());

        var parsedStratifiers = group.getStratifier().stream().map(fhirStratifier -> ParsedStratifier.fromFhirStratifier(fhirStratifier, fhirPathEngine)).toList();
        var groupReduceOp = new GroupReduceOp(parsedStratifiers.stream().map(s -> new StratifierReduceOp(fhirPathEngine, s)).toList());
        var emtpyStratifierResults = parsedStratifiers.stream().map(s -> new StratifierResult(s.coding(), new HashMap<>())).toList();

        return population.reduce(GroupResult.initial(emtpyStratifierResults), groupReduceOp);
    }

    private Measure.MeasureGroupPopulationComponent getInitialPopulation(Measure.MeasureGroupComponent group) {
        List<Measure.MeasureGroupPopulationComponent> foundInitialPopulations = new LinkedList<>();
        for (Measure.MeasureGroupPopulationComponent populationComponent : group.getPopulation()) {
            List<Coding> codings = populationComponent.getCode().getCoding();
            if (codings.size() != 1)
                throw new IllegalArgumentException("Population in Measure did not contain exactly one Coding");
            Coding coding = codings.get(0);

            if (coding.getSystem().equals(INITIAL_POPULATION_CODING.system()) && coding.getCode().equals(INITIAL_POPULATION_CODING.code()))
                foundInitialPopulations.add(populationComponent);
        }

        if (foundInitialPopulations.size() != 1)
            throw new IllegalArgumentException("Measure did not contain exactly one initial population");
        if (!foundInitialPopulations.get(0).getCriteria().getLanguage().equals(INITIAL_POPULATION_LANGUAGE))
            throw new IllegalArgumentException("Language of Initial Population was not equal to '%s'".formatted(INITIAL_POPULATION_LANGUAGE));
        return foundInitialPopulations.get(0);
    }


}
