package de.medizininformatikinitiative.fhir_data_evaluator.populations;

import ca.uhn.fhir.fhirpath.IFhirPath;
import de.medizininformatikinitiative.fhir_data_evaluator.ResourceWithIncludes;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;

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

    /**
     * Increments the count of the measure population.
     */
    public MeasurePopulation increment() {
        return new MeasurePopulation(count + 1);
    }

    public static Optional<ResourceWithIncludes> evaluateMeasurePopResource(ResourceWithIncludes resource, IFhirPath.IParsedExpression expression) {
        List<Base> found = resource.fhirPathEngine().evaluate(resource.mainResource(), expression, Base.class);

        if (found.isEmpty())
            return Optional.empty();

        if (found.size() > 1)
            throw new IllegalArgumentException("Measure population evaluated into more than one entity");

        if (found.get(0) instanceof Resource r)
            return Optional.of(new ResourceWithIncludes(r, resource.includes(), resource.fhirPathEngine()));

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
