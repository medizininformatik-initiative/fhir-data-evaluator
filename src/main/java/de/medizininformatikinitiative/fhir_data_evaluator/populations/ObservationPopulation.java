package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import org.hl7.fhir.r4.model.MeasureReport;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.MEASURE_OBSERVATION_CODING;
import static java.util.Objects.requireNonNull;

/**
 * Represents a measure observation population either on group or on stratifier level.
 *
 * @param count           the number of members in the measure population
 * @param aggregateMethod the method to aggregate the members of this population
 */
public record ObservationPopulation(int count, AggregateUniqueCount aggregateMethod) {

    public ObservationPopulation {
        requireNonNull(aggregateMethod);
    }

    public static ObservationPopulation empty() {
        return new ObservationPopulation(0, AggregateUniqueCount.empty());
    }

    public static ObservationPopulation initialWithValue(String value) {
        return new ObservationPopulation(1, AggregateUniqueCount.withValue(value));
    }

    public ObservationPopulation merge(ObservationPopulation other) {
        return new ObservationPopulation(count + other.count, aggregateMethod.merge(other.aggregateMethod));
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
