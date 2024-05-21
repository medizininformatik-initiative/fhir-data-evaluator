package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class MeasureEvaluator {

    private final GroupEvaluator groupEvaluator;
    private final Scheduler SCHEDULER;

    public MeasureEvaluator(DataStore dataStore, FHIRPathEngine fhirPathEngine, int groupThreadCount) {
        this.groupEvaluator = new GroupEvaluator(dataStore, fhirPathEngine);
        this.SCHEDULER = Schedulers.newParallel("group-scheduler", groupThreadCount);
    }

    public Mono<MeasureReport> evaluateMeasure(Measure measure) {
        return Flux.fromStream(measure.getGroup().stream()).parallel().runOn(SCHEDULER).flatMap(groupEvaluator::evaluateGroup)
                .map(GroupResult::toReportGroup).sequential().collectList()
                .map(measureReportGroup -> new MeasureReport().setGroup(measureReportGroup));
    }
}
