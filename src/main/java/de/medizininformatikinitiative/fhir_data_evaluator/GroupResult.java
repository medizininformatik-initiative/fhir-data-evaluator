package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.MeasureReport;

import java.util.List;
import java.util.stream.IntStream;

public record GroupResult(PopulationsCount populationsCount, List<StratifierResult> stratifierResults) {

    public GroupResult merge(GroupResult other) {
        return new GroupResult(this.populationsCount.merge(other.populationsCount),
                mergeResourceResults(this.stratifierResults, other.stratifierResults));
    }

    private List<StratifierResult> mergeResourceResults(List<StratifierResult> a, List<StratifierResult> b) {
        return IntStream.range(0, a.size()).mapToObj(i -> a.get(i).merge(b.get(i))).toList();
    }

    public MeasureReport.MeasureReportGroupComponent toReportGroup() {
        return new MeasureReport.MeasureReportGroupComponent()
                .setPopulation(populationsCount.toReportGroupPopulations())
                .setStratifier(stratifierResults.stream().map(StratifierResult::toReport).toList());
    }

}
