package de.medizininformatikinitiative.fhir_data_evaluator;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.PopulationI;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/**
 * Holds {@link PopulationI}s and {@link StratifierResult}s of one group.
 *
 * @param populations       the count of all resources of the group without any stratification
 * @param stratifierResults holds the results of each stratifier
 */
public record GroupResult<T extends PopulationI<T>>(T populations, List<StratifierResult<T>> stratifierResults) {

    public GroupResult {
        requireNonNull(populations);
        stratifierResults = List.copyOf(stratifierResults);
    }

    public static <T extends PopulationI<T>> GroupResult<T> initial(List<StratifierResult<T>> initialResults, T populations) {
        return new GroupResult<T>(populations, initialResults);
    }

    public GroupResult<T> applyResource(List<StratifierReduceOp<T>> stratifierOperations, Resource resource, T incrementPopulation) {
        assert stratifierResults.size() == stratifierOperations.size();
        var newPopulation = populations.merge(incrementPopulation);
        return new GroupResult<T>(newPopulation, applyEachStratifier(stratifierOperations, resource, incrementPopulation));
    }

    /**
     * This method assumes that the {@code stratifierOperation} at index {@code i} belongs to the {@code stratifierResult}
     * at index {@code i}.
     */
    private List<StratifierResult<T>> applyEachStratifier(List<StratifierReduceOp<T>> stratifierOperations, Resource resource, T incrementPopulation) {
        return IntStream.range(0, stratifierOperations.size()).mapToObj(i ->
                stratifierOperations.get(i).apply(stratifierResults.get(i), resource, incrementPopulation)).toList();
    }

    public MeasureReport.MeasureReportGroupComponent toReportGroup() {
        return populations.toReportGroupComponent()
                .setStratifier(stratifierResults.stream().map(StratifierResult::toReportGroupStratifier).toList());
    }
}
