package de.medizininformatikinitiative.fhir_data_evaluator;

import java.util.HashSet;

/**
 * Holds a set of unique {@link String}s .
 *
 * @param aggregatedValues the set of unique aggregated values
 */
public record AggregateUniqueCount(HashSet<String> aggregatedValues) {
    static final String EXTENSION_URL = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-aggregateMethod";
    static final String EXTENSION_VALUE = "unique-count";

    /**
     * Makes a copy of the {@link AggregateUniqueCount} with a copied set of {@code aggregatedValues}.
     */
    public AggregateUniqueCount copy() {
        return new AggregateUniqueCount(new HashSet<>(aggregatedValues));
    }

    /**
     * Mutates {@code aggregatedValues} to add a value to it if the value is not already present in the set.
     *
     * @param value the value to add to the {@code aggregatedValues}
     * @return itself with the mutated {@code aggregatedValues}
     */
    public AggregateUniqueCount aggregateValue(String value) {
        aggregatedValues.add(value);
        return this;
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
