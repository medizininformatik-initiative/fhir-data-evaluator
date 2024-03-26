package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Bundle;
import reactor.core.publisher.Flux;

public class DataStore {
    public Flux<Bundle.BundleEntryComponent> getPopulation(String populationQuery) {
        // TODO
        return Flux.empty();
    }
}
