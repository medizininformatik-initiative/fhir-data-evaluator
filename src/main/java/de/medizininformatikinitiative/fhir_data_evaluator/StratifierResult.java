package de.medizininformatikinitiative.fhir_data_evaluator;

import de.medizininformatikinitiative.fhir_data_evaluator.populations.Population;
import de.medizininformatikinitiative.fhir_data_evaluator.populations.individuals.Individual;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Holds found values of one stratifier.
 * <p>
 * In case the Stratifier does not consist of components but of criteria, a set will hold only one {@link StratumComponent}.
 * <p>
 * In the {@code MeasureReport} a {@link StratifierResult} is the equivalent to a {@link MeasureReport.MeasureReportGroupStratifierComponent stratifier}.
 * <p>
 * Note that this record is mutable through the {@link StratifierResult#mergeStratumComponents(Set, Individual)} method.
 *
 * @param code        the code of the stratifier if the stratifier consists of criteria and code
 * @param populations mutable map of the populations of each found set of values
 */
public record StratifierResult<T extends Population<T, I>, I extends Individual<T>>(
        Optional<HashableCoding> code,
        Map<Set<StratumComponent>, T> populations) {

    public StratifierResult {
        requireNonNull(code);
        requireNonNull(populations);
    }

    public static <T extends Population<T, I>, I extends Individual<T>> StratifierResult<T, I> initial(Measure.MeasureGroupStratifierComponent s, Class<T> type) {
        var code = s.hasCode() ? HashableCoding.ofFhirCoding(s.getCode().getCodingFirstRep()) : null;
        return new StratifierResult<T, I>(Optional.ofNullable(code), new HashMap<>());
    }

    /**
     * Merges {@code components} and the corresponding {@code individual} into the {@code StratifierResult} by
     * mutating it and then returns itself.
     *
     * @param components the key of the population to increment
     * @param individual the {@link Individual} used to initialize or increment the strata
     * @return the mutated {@link StratifierResult} itself
     */
    public StratifierResult<T, I> mergeStratumComponents(Set<StratumComponent> components, I individual) {
        populations.compute(components, (k, p) -> p == null ? individual.toPopulation() : p.increment(individual));
        return this;
    }

    public MeasureReport.MeasureReportGroupStratifierComponent toReportGroupStratifier() {
        var reportStratifier = new MeasureReport.MeasureReportGroupStratifierComponent()
                .setStratum(this.populations.entrySet().stream().map(StratifierResult::entryToReport).toList());

        code.ifPresent(c -> reportStratifier.setCode(List.of(c.toCodeableConcept())));

        return reportStratifier;
    }

    private static <T extends Population<T, I>, I extends Individual<T>> MeasureReport.StratifierGroupComponent entryToReport(Map.Entry<Set<StratumComponent>, T> entry) {

        MeasureReport.StratifierGroupComponent stratum = entry.getValue().toReportStratifierGroupComponent();

        if (entry.getKey().size() == 1) {
            stratum.setValue(new CodeableConcept(entry.getKey().iterator().next().value().toCoding()));
        } else {
            stratum.setComponent(entry.getKey().stream().map(StratumComponent::toReport).toList());
        }

        return stratum;
    }
}
