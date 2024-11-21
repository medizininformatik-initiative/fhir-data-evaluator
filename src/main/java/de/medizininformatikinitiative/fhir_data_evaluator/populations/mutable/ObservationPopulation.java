package de.medizininformatikinitiative.fhir_data_evaluator.populations.mutable;

import ca.uhn.fhir.fhirpath.IFhirPath;
import de.medizininformatikinitiative.fhir_data_evaluator.ResourceWithIncludes;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.StringType;

import java.util.List;
import java.util.Optional;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.MEASURE_OBSERVATION_CODING;
import static java.util.Objects.requireNonNull;

/**
 * Represents a measure observation population either on group or on stratifier level.
 * <p>
 * This record is mutable because it holds a mutable {@link AggregateUniqueCounter}.
 *
 * @param count           the number of members in the observation population
 * @param aggregateMethod the method to aggregate the members of this population
 */
public record ObservationPopulation(int count, AggregateUniqueCounter aggregateMethod) {

    public ObservationPopulation {
        requireNonNull(aggregateMethod);
    }

    public static ObservationPopulation empty() {
        return new ObservationPopulation(0, AggregateUniqueCounter.of());
    }

    public static ObservationPopulation initialWithValue(String value) {
        return new ObservationPopulation(1, AggregateUniqueCounter.of(value));
    }

    /**
     * Increments the count of the observation population and adds a value to the aggregate method.
     *
     * @param value the value to add to the aggregate method
     */
    public ObservationPopulation increment(String value) {
        return new ObservationPopulation(count + 1, aggregateMethod.addValue(requireNonNull(value)));
    }

    public static Optional<String> evaluateObservationPop(ResourceWithIncludes resource, IFhirPath.IParsedExpression expression) {
        List<Base> found = resource.fhirPathEngine().evaluate(resource.mainResource(), expression, Base.class);

        if (found.isEmpty())
            return Optional.empty();

        if (found.size() > 1)
            throw new IllegalArgumentException("Measure observation population evaluated into more than one entity");

        if (found.get(0) instanceof StringType s)
            return Optional.of(s.getValue());

        throw new IllegalArgumentException("Measure observation population evaluated into different type than 'String'");
    }

    public MeasureReport.MeasureReportGroupPopulationComponent toReportGroupPopulation() {
        return new MeasureReport.MeasureReportGroupPopulationComponent()
                .setCode(MEASURE_OBSERVATION_CODING.toCodeableConcept())
                .setCount(count);
    }

    public MeasureReport.StratifierGroupPopulationComponent toReportStratifierPopulation() {
        return new MeasureReport.StratifierGroupPopulationComponent()
                .setCode(MEASURE_OBSERVATION_CODING.toCodeableConcept())
                .setCount(count);
    }
}
