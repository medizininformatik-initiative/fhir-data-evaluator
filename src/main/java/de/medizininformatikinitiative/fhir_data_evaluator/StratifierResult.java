package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Holds found values of one stratifier.
 * <p>
 * For each resource, {@code counts} is mutated to either increase the count of a set of values or add a new set of
 * values with count 1.
 * <p>
 * In case the Stratifier does not consist of components but of criteria, a set will hold only one {@link StratumComponent}.
 * <p>
 * In the {@code MeasureReport} a {@link StratifierResult} is the equivalent to an element in the {@code stratum} list.
 *
 * @param code the code of the stratifier
 * @param counts mutable map of the counts of each found set of values
 */
public record StratifierResult(HashableCoding code, Map<Set<StratumComponent>, Populations> counts) {

    public void mergeResourceResult(Set<StratumComponent> r) {
        counts.merge(r, Populations.INITIAL_ONE, Populations::merge);
    }

    public MeasureReport.MeasureReportGroupStratifierComponent toReport() {
        return new MeasureReport.MeasureReportGroupStratifierComponent()
                .setCode(List.of(code.toCodeableConcept()))
                .setStratum(this.counts.entrySet().stream().map(this::entryToReport).toList());
    }

    private MeasureReport.StratifierGroupComponent entryToReport(Map.Entry<Set<StratumComponent>, Populations> entry) {
        MeasureReport.StratifierGroupComponent stratumElement = new MeasureReport.StratifierGroupComponent()
                .setPopulation(entry.getValue().toReportStratifierPopulations());

        if (entry.getKey().size() == 1) {
            stratumElement.setValue(new CodeableConcept(entry.getKey().iterator().next().value().toCoding()));
        } else {
            stratumElement.setComponent(entry.getKey().stream().map(StratumComponent::toReport).toList());
        }

        return stratumElement;
    }
}
