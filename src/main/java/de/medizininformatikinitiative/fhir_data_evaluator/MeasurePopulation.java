package de.medizininformatikinitiative.fhir_data_evaluator;

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
 * @param count      the number of members in the measure population
 * @param expression the expression to extract the member of the measure population from the initial population
 */
public record MeasurePopulation(int count, ExpressionNode expression) {

    /**
     * Makes a copy of the {@link MeasurePopulation} with a copied {@code count}, but the {@code expression} is not copied.
     * <p>
     */
    public MeasurePopulation shallowCopy() {
        return new MeasurePopulation(count, expression);
    }

    public Optional<Resource> evaluateResource(FHIRPathEngine fhirPathEngine, Resource resource) {
        List<Base> found = fhirPathEngine.evaluate(resource, expression);

        if (found.isEmpty())
            return Optional.empty();

        if (found.get(0) instanceof Resource r)
            return Optional.of(r);

        return Optional.empty();
    }

    public MeasurePopulation updateWithResource(Resource resource) {
        int newCount = resource == null ? this.count : this.count + 1;
        return new MeasurePopulation(newCount, expression);
    }

    public MeasurePopulation merge(MeasurePopulation other) {
        return new MeasurePopulation(count + other.count, expression);
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
