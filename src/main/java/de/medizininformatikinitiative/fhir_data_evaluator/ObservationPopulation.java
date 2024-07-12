package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;
import java.util.Optional;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.MEASURE_OBSERVATION_CODING;
import static java.util.Objects.requireNonNull;

/**
 * Represents a measure observation population either on group or on stratifier level.
 *
 * @param fhirPathEngine  the engine used to evaluate the expression
 * @param count           the number of members in the measure population
 * @param expression      the expression to extract the member of the observation population from the measure population
 * @param aggregateMethod the method to aggregate the members of this population
 */
public record ObservationPopulation(FHIRPathEngine fhirPathEngine, int count, ExpressionNode expression,
                                    AggregateUniqueCount aggregateMethod) {

    public ObservationPopulation {
        requireNonNull(fhirPathEngine);
        requireNonNull(expression);
        requireNonNull(aggregateMethod);
    }

    /**
     * Makes a copy of the {@link ObservationPopulation} with a copied {@code count} and {@code aggregateMethod},
     * but the {@code expression} is not copied.
     * <p>
     */
    public ObservationPopulation shallowCopyOf() {
        return new ObservationPopulation(fhirPathEngine, count, expression, aggregateMethod.copy());
    }

    public ObservationPopulation updateWithResource(Resource resource) {
        Optional<String> value = evaluateResource(resource);

        return value
                .map(v -> new ObservationPopulation(fhirPathEngine, count + 1, expression, aggregateMethod.aggregateValue(v)))
                .orElse(this);
    }

    private Optional<String> evaluateResource(Resource resource) {
        List<Base> found = fhirPathEngine.evaluate(resource, expression);

        if (found.isEmpty()) {
            return Optional.empty();
        }

        if (found.get(0) instanceof StringType s) {
            return Optional.of(s.getValue());
        }

        return Optional.empty();
    }

    public ObservationPopulation merge(ObservationPopulation other) {
        return new ObservationPopulation(fhirPathEngine, count + other.count, expression, aggregateMethod.merge(other.aggregateMethod));
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
