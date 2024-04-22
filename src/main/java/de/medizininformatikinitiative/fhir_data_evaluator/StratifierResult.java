package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds found values of one Stratifier.
 * <p>
 * At the beginning, it will usually hold one set of values found in one resource, with the Initial-Population count set to 1.
 * Then later multiple {@code StratifierResults} of the same Stratifier will be merged to a map that counts each found set of values.
 * <p>
 * In case the Stratifier does not consist of components but of criteria, the set will hold only one {@link ComponentKeyPair}.
 * <p>
 * In the {@code MeasureReport} a {@code StratifierResult} is the equivalent to an element in the {@code stratum} list.
 */
public record StratifierResult(Map<Set<ComponentKeyPair>, PopulationsCount> counts,
                               HashableCoding stratifierCode) {

    public static StratifierResult ofSingleKeyPair(ComponentKeyPair key, PopulationsCount populationsCount, HashableCoding stratifierCode) {
        return new StratifierResult(Map.of(Set.of(key), populationsCount), stratifierCode);
    }

    public static StratifierResult ofSingleSet(Set<ComponentKeyPair> set, PopulationsCount populationsCount, HashableCoding stratifierCode) {
        return new StratifierResult(Map.of(set, populationsCount), stratifierCode);
    }

    public StratifierResult merge(StratifierResult other) {
        assert this.stratifierCode.equals(other.stratifierCode);
        return new StratifierResult(mergeMaps(this.counts, other.counts), this.stratifierCode);
    }

    private Map<Set<ComponentKeyPair>, PopulationsCount> mergeMaps(Map<Set<ComponentKeyPair>, PopulationsCount> a, Map<Set<ComponentKeyPair>, PopulationsCount> b) {
        return Stream.concat(a.entrySet().stream(), b.entrySet().stream()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, PopulationsCount::merge));
    }

    public MeasureReport.MeasureReportGroupStratifierComponent toReport() {
        return new MeasureReport.MeasureReportGroupStratifierComponent()
                .setCode(List.of(new CodeableConcept(this.stratifierCode.toCoding())))
                .setStratum(this.counts.entrySet().stream().map(this::entryToReport).toList());
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
