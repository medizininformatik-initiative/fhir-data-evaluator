package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record StratifierResult(Optional<Map<Set<ComponentKeyPair>, PopulationsCount>> counts,
                               HashableCoding stratifierCode) {

    public static StratifierResult ofSingleKeyPair(ComponentKeyPair key, PopulationsCount populationsCount, HashableCoding stratifierCode) {
        return new StratifierResult(Optional.of(Map.of(Set.of(key), populationsCount)), stratifierCode);
    }

    public static StratifierResult ofSingleSet(Set<ComponentKeyPair> set, PopulationsCount populationsCount, HashableCoding stratifierCode) {
        return new StratifierResult(Optional.of(Map.of(set, populationsCount)), stratifierCode);
    }

    public static StratifierResult empty(HashableCoding stratifierCode) {
        return new StratifierResult(Optional.empty(), stratifierCode);
    }

    public StratifierResult merge(StratifierResult other) {
        return this.counts
                .flatMap(thisCounts -> other.counts.map(otherCounts ->
                        new StratifierResult(Optional.of(mergeMaps(thisCounts, otherCounts)), this.stratifierCode)))
                .orElse(other);
    }

    private Map<Set<ComponentKeyPair>, PopulationsCount> mergeMaps(Map<Set<ComponentKeyPair>, PopulationsCount> a, Map<Set<ComponentKeyPair>, PopulationsCount> b) {
        return Stream.concat(a.entrySet().stream(), b.entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, PopulationsCount::merge));
    }

    public MeasureReport.MeasureReportGroupStratifierComponent toReport() {

        return new MeasureReport.MeasureReportGroupStratifierComponent()
                .setCode(List.of(new CodeableConcept(stratifierCode.toFhirCoding())))
                .setStratum(counts
                        .map(presentCounts -> presentCounts.entrySet().stream().map(this::entryToReport).toList())
                        .orElse(null));
    }

    private MeasureReport.StratifierGroupComponent entryToReport(Map.Entry<Set<ComponentKeyPair>, PopulationsCount> entry) {
        MeasureReport.StratifierGroupComponent stratumElement = new MeasureReport.StratifierGroupComponent()
                .setPopulation(entry.getValue().toReportStratifierPopulations());

        if (entry.getKey().size() == 1) {
            stratumElement.setValue(new CodeableConcept(entry.getKey().iterator().next().valueCode().toCoding()));
        } else {
            stratumElement.setComponent(entry.getKey().stream().map(ComponentKeyPair::toReport).toList());
        }

        return stratumElement;
    }
}
