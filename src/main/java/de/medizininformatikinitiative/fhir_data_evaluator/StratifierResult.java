package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Quantity;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Holds found values of one stratifier.
 * <p>
 * In case the Stratifier does not consist of components but of criteria, a set will hold only one {@link StratumComponent}.
 * <p>
 * In the {@code MeasureReport} a {@link StratifierResult} is the equivalent to a {@link MeasureReport.MeasureReportGroupStratifierComponent stratifier}.
 *
 * @param code        the code of the stratifier if the stratifier consists of criteria and code
 * @param populations mutable map of the populations of each found set of values
 */
public record StratifierResult(Optional<HashableCoding> code, Map<Set<StratumComponent>, Populations> populations) {

    public StratifierResult {
        requireNonNull(code);
        requireNonNull(populations);
    }

    public static StratifierResult initial(Measure.MeasureGroupStratifierComponent s) {
        var code = s.hasCode() ? HashableCoding.ofFhirCoding(s.getCode().getCodingFirstRep()) : null;
        return new StratifierResult(Optional.ofNullable(code), new HashMap<>());
    }

    /**
     * Merges {@code components} and the corresponding {@code newPopulations} into the {@code StratifierResult} by
     * mutating it and then returns itself.
     *
     * @param components the key of the populations to increment
     * @return the mutated {@code StratifierResult} itself
     */
    public StratifierResult mergeStratumComponents(Set<StratumComponent> components, Populations newPopulations) {
        populations.merge(components, newPopulations, Populations::merge);
        return this;
    }

    public MeasureReport.MeasureReportGroupStratifierComponent toReportGroupStratifier() {
        var reportStratifier = new MeasureReport.MeasureReportGroupStratifierComponent()
                .setStratum(this.populations.entrySet().stream().map(StratifierResult::entryToReport).toList());

        code.ifPresent(c -> reportStratifier.setCode(List.of(c.toCodeableConcept())));

        return reportStratifier;
    }

    private static MeasureReport.StratifierGroupComponent entryToReport(Map.Entry<Set<StratumComponent>, Populations> entry) {
        var populations = entry.getValue();
        MeasureReport.StratifierGroupComponent stratum = new MeasureReport.StratifierGroupComponent()
                .setPopulation(populations.toReportStratifierPopulations());

        populations.observationPopulation().ifPresent(op -> stratum.setMeasureScore(new Quantity(op.aggregateMethod().getScore())));

        if (entry.getKey().size() == 1) {
            stratum.setValue(new CodeableConcept(entry.getKey().iterator().next().value().toCoding()));
        } else {
            stratum.setComponent(entry.getKey().stream().map(StratumComponent::toReport).toList());
        }

        return stratum;
    }
}
