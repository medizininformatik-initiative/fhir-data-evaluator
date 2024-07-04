package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

/**
 * Holds {@link Populations} and {@link StratifierResult}s of one group.
 *
 * @param populations       the count of all resources of the group without any stratification
 * @param stratifierResults holds the results of each stratifier
 */
public record GroupResult(Populations populations, List<StratifierResult> stratifierResults) {

    public GroupResult {
        requireNonNull(populations);
        stratifierResults = List.copyOf(stratifierResults);
    }

    public static GroupResult initial(List<StratifierResult> initialResults) {
        return new GroupResult(Populations.ZERO, initialResults);
    }

    public GroupResult applyResource(List<StratifierReduceOp> stratifierOperations, Resource resource) {
        assert stratifierResults.size() == stratifierOperations.size();
        return new GroupResult(populations.increaseCounts(), applyEachStratifier(stratifierOperations, resource));
    }

    /**
     * This method assumes that the {@code stratifierOperation} at index {@code i} belongs to the {@code stratifierResult}
     * at index {@code i}.
     */
    private List<StratifierResult> applyEachStratifier(List<StratifierReduceOp> stratifierOperations, Resource resource) {
        return IntStream.range(0, stratifierOperations.size()).mapToObj(i -> stratifierOperations.get(i).apply(stratifierResults.get(i), resource)).toList();
    }

    public MeasureReport.MeasureReportGroupComponent toReportGroup() {
        return new MeasureReport.MeasureReportGroupComponent()
                .setPopulation(populations.toReportGroupPopulations())
                .setStratifier(stratifierResults.stream().map(StratifierResult::toReportGroupStratifier).toList());
    }
}
