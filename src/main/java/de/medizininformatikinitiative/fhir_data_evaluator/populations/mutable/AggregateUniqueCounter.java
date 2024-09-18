package de.medizininformatikinitiative.fhir_data_evaluator.populations.mutable;

import org.hl7.fhir.r4.model.Quantity;

import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Holds a set of unique {@link String}s.
 * <p>
 * This record is mutable because it holds a mutable {@link HashSet}, which is necessary to efficiently store and add
 * many values without copying.
 *
 * @param aggregatedValues the set of unique aggregated values
 */
public record AggregateUniqueCounter(Set<String> aggregatedValues) {
    public static final String EXTENSION_URL = "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-aggregateMethod";
    public static final String EXTENSION_VALUE = "unique-count";

    public AggregateUniqueCounter {
        aggregatedValues = new HashSet<>(aggregatedValues);
    }

    public static AggregateUniqueCounter of() {
        return new AggregateUniqueCounter(Set.of());
    }

    public static AggregateUniqueCounter of(String value) {
        return new AggregateUniqueCounter(Set.of(value));
    }

    /**
     * Mutates {@code aggregatedValues} to add a new value.
     *
     * @param val the value to add to the aggregated values
     * @return itself with the mutated {@code aggregatedValues}
     */
    public AggregateUniqueCounter addValue(String val) {
        aggregatedValues.add(requireNonNull(val));
        return this;
    }

    public Quantity score() {
        return new Quantity(aggregatedValues.size());
    }
}
