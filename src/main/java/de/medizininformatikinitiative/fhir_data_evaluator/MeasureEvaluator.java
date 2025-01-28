package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.fhirpath.IFhirPath;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class MeasureEvaluator {

    private final GroupEvaluator groupEvaluator;
    private final Scheduler SCHEDULER = Schedulers.parallel();

    public MeasureEvaluator(DataStore dataStore, IFhirPath fhirPathEngine) {
        this.groupEvaluator = new GroupEvaluator(dataStore, fhirPathEngine);
    }

    public Mono<MeasureReport> evaluateMeasure(Measure measure) {
        return Flux.fromStream(measure.getGroup().stream()).parallel().runOn(SCHEDULER).flatMap(groupEvaluator::evaluateGroup)
                .sequential().collectList()
                .map(measureReportGroup -> new MeasureReport().setGroup(measureReportGroup));
    }
}
