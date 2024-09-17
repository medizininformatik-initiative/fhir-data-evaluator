package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import java.util.HashSet;
import java.util.Set;

/**
 * Holds a set of unique {@link String}s.
 *
 * @param aggregatedValues the set of unique aggregated values
 */
public record AggregateUniqueCount(HashSet<String> aggregatedValues) {
    public static final String EXTENSION_URL = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-aggregateMethod";
    public static final String EXTENSION_VALUE = "unique-count";

    public AggregateUniqueCount {
        aggregatedValues = new HashSet<>(aggregatedValues);
    }

    public static AggregateUniqueCount empty() {
        return new AggregateUniqueCount(new HashSet<>());
    }

    public static AggregateUniqueCount withValue(String value) {
        return new AggregateUniqueCount(new HashSet<>(Set.of(value)));
    }

    public AggregateUniqueCount deepCopy() {
        return new AggregateUniqueCount(new HashSet<>(aggregatedValues));
    }

    /**
     * Mutates {@code aggregatedValues} to merge the {@code aggregatedValues} of another {@link AggregateUniqueCount}
     * into itself.
     *
     * @param a the {@link AggregateUniqueCount} to merge
     * @return itself with the mutated {@code aggregatedValues}
     */
    public AggregateUniqueCount merge(AggregateUniqueCount a) {
        aggregatedValues.addAll(a.aggregatedValues);
        return this;
    }

    public int getScore() {
        return aggregatedValues.size();
    }
}
