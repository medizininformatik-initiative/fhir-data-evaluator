package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;

/**
 * Counts the populations of the Measure either on group-level or on stratifier-level.
 * <p>
 * Currently, the only accepted population is the Initial-Population.
 */
public record PopulationsCount(PopulationCount initialPopulation) {

    static public PopulationsCount ofInitialPopulation(Measure.MeasureGroupPopulationComponent populationComponent) {
        return new PopulationsCount(new PopulationCount(HashableCoding.ofFhirCoding(populationComponent.getCode().getCodingFirstRep()), 0));
    }

    public PopulationsCount evaluateOnResource(Resource resource) {
        return evaluateInitialPopulation(resource);
    }

    private PopulationsCount evaluateInitialPopulation(Resource resource) {
        return resource == null ?
                new PopulationsCount(new PopulationCount(this.initialPopulation().code(), 0)) :
                new PopulationsCount(new PopulationCount(this.initialPopulation().code(), 1));
    }

    public PopulationsCount merge(PopulationsCount other) {
        return mergeInitialPopulation(other);
    }

    private PopulationsCount mergeInitialPopulation(PopulationsCount other) {
        assert this.initialPopulation.code().equals(other.initialPopulation.code());
        return new PopulationsCount(new PopulationCount(this.initialPopulation.code(), this.initialPopulation.count() + other.initialPopulation.count()));
    }

    public List<MeasureReport.StratifierGroupPopulationComponent> toReportStratifierPopulations() {
        return List.of(initialPopulation.toReportStratifierPopulation());
    }

    public List<MeasureReport.MeasureReportGroupPopulationComponent> toReportGroupPopulations() {
        return List.of(initialPopulation.toReportGroupPopulation());
    }
}
