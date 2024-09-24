package de.medizininformatikinitiative.fhir_data_evaluator;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.Population;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.Individual;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/**
 * Holds {@link Population}s and {@link StratifierResult}s of one group.
 *
 * @param populations       the count of all resources of the group without any stratification
 * @param stratifierResults holds the results of each stratifier
 */
public record GroupResult<T extends Population<T, I>, I extends Individual<T>>(T populations,
                                                                               List<StratifierResult<T, I>> stratifierResults) {

    public GroupResult {
        requireNonNull(populations);
        stratifierResults = List.copyOf(stratifierResults);
    }

    public static <T extends Population<T, I>, I extends Individual<T>> GroupResult<T, I> initial(T populations,
                                                                                                  List<StratifierResult<T, I>> initialResults) {
        return new GroupResult<T, I>(populations, initialResults);
    }

    public GroupResult<T, I> applyResource(List<StratifierReduceOp<T, I>> stratifierOperations, Resource resource, I incrementIndividual) {
        assert stratifierResults.size() == stratifierOperations.size();
        var newPopulation = populations.increment(incrementIndividual);

        return new GroupResult<T, I>(newPopulation, applyEachStratifier(stratifierOperations, resource, incrementIndividual));
    }

    /**
     * This method assumes that the {@code stratifierOperation} at index {@code i} belongs to the {@code stratifierResult}
     * at index {@code i}.
     */
    private List<StratifierResult<T, I>> applyEachStratifier(List<StratifierReduceOp<T, I>> stratifierOperations,
                                                             Resource resource,
                                                             I incrementIndividual) {
        return IntStream.range(0, stratifierOperations.size()).mapToObj(i ->
                stratifierOperations.get(i).apply(stratifierResults.get(i), resource, incrementIndividual)).toList();
    }

    public MeasureReport.MeasureReportGroupComponent toReportGroup() {
        return populations.toReportGroupComponent()
                .setStratifier(stratifierResults.stream().map(StratifierResult::toReportGroupStratifier).toList());
    }
}
