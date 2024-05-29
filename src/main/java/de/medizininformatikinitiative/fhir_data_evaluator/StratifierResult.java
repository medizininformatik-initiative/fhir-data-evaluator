package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Holds found values of one stratifier.
 * <p>
 * For each resource, {@code counts} is mutated to either increase the count of a set of values or add a new set of
 * values with count 1.
 * <p>
 * In case the Stratifier does not consist of components but of criteria, a set will hold only one {@link StratumComponent}.
 * <p>
 * In the {@code MeasureReport} a {@link StratifierResult} is the equivalent to a {@link MeasureReport.MeasureReportGroupStratifierComponent stratifier}.
 *
 * @param code        the code of the stratifier
 * @param populations mutable map of the populations of each found set of values
 */
public record StratifierResult(HashableCoding code, Map<Set<StratumComponent>, Populations> populations) {

    public StratifierResult {
        requireNonNull(code);
        requireNonNull(populations);
    }

    public static StratifierResult initial(HashableCoding code) {
        return new StratifierResult(code, new HashMap<>());
    }

    /**
     * Increments the counts of the populations with {@code components}. Mutates the {@code StratifierResult} and
     * returns itself.
     *
     * @param components the key of the populations to increment
     * @return the mutated {@code StratifierResult} itself
     */
    public StratifierResult mergeStratumComponents(Set<StratumComponent> components) {
        populations.merge(components, Populations.ONE, Populations::merge);
        return this;
    }

    public MeasureReport.MeasureReportGroupStratifierComponent toReportGroupStratifier() {
        return new MeasureReport.MeasureReportGroupStratifierComponent()
                .setCode(List.of(code.toCodeableConcept()))
                .setStratum(this.populations.entrySet().stream().map(StratifierResult::entryToReport).toList());
    }

    private static MeasureReport.StratifierGroupComponent entryToReport(Map.Entry<Set<StratumComponent>, Populations> entry) {
        MeasureReport.StratifierGroupComponent stratum = new MeasureReport.StratifierGroupComponent()
                .setPopulation(entry.getValue().toReportStratifierPopulations());

        if (entry.getKey().size() == 1) {
            stratum.setValue(new CodeableConcept(entry.getKey().iterator().next().value().toCoding()));
        } else {
            stratum.setComponent(entry.getKey().stream().map(StratumComponent::toReport).toList());
        }

        return stratum;
    }
}
