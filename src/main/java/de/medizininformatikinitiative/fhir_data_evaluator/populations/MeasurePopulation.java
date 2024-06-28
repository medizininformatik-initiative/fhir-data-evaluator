package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.ExpressionNode;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import java.util.List;
import java.util.Optional;

import static de.medizininformatikinitiative.fhir_data_evaluator.HashableCoding.MEASURE_POPULATION_CODING;

/**
 * Represents a measure population either on group or on stratifier level.
 *
 * @param count the number of members in the measure population
 */
public record MeasurePopulation(int count) {

    public static MeasurePopulation ZERO = new MeasurePopulation(0);
    public static MeasurePopulation ONE = new MeasurePopulation(1);

    public MeasurePopulation merge(MeasurePopulation other) {
        return new MeasurePopulation(count + other.count);
    }

    public static Optional<Resource> evaluateMeasurePopResource(Resource resource, ExpressionNode expression, FHIRPathEngine fhirPathEngine) {
        List<Base> found = fhirPathEngine.evaluate(resource, expression);

        if (found.isEmpty())
            return Optional.empty();

        if (found.size() > 1)
            throw new IllegalArgumentException("Measure population evaluated into more than one entity");

        if (found.get(0) instanceof Resource r)
            return Optional.of(r);

        throw new IllegalArgumentException("Measure population evaluated into different type than 'Resource'");
    }

    public MeasureReport.MeasureReportGroupPopulationComponent toReportGroupPopulation() {
        return new MeasureReport.MeasureReportGroupPopulationComponent()
                .setCode(MEASURE_POPULATION_CODING.toCodeableConcept())
                .setCount(count);
    }

    public MeasureReport.StratifierGroupPopulationComponent toReportStratifierPopulation() {
        return new MeasureReport.StratifierGroupPopulationComponent()
                .setCode(MEASURE_POPULATION_CODING.toCodeableConcept())
                .setCount(count);
    }
}
