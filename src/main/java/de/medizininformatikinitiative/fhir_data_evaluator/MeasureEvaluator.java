package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MeasureEvaluator {

    private final GroupEvaluator groupEvaluator;

    public MeasureEvaluator(DataStore dataStore, FHIRPathEngine fhirPathEngine) {
        this.groupEvaluator = new GroupEvaluator(dataStore, fhirPathEngine);
    }

    public Mono<MeasureReport> evaluateMeasure(Measure measure) {
        return Flux.fromStream(measure.getGroup().stream()).flatMap(groupEvaluator::evaluateGroup)
                .map(GroupResult::toReportGroup).collectList()
                .map(measureReportGroup -> new MeasureReport().setGroup(measureReportGroup));
    }
}
