package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.fhirpath.IFhirPath;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.atomic.AtomicInteger;

public class MeasureEvaluator {

    private final GroupEvaluator groupEvaluator;
    private final Scheduler SCHEDULER = Schedulers.parallel();
    private final int maxConcurrency;
    private AtomicInteger progressCounter = new AtomicInteger(0);
    private final Logger logger = LoggerFactory.getLogger(MeasureEvaluator.class);

    public MeasureEvaluator(DataStore dataStore, IFhirPath fhirPathEngine, int maxConcurrency) {
        this.groupEvaluator = new GroupEvaluator(dataStore, fhirPathEngine);
        this.maxConcurrency = maxConcurrency;
    }

    public Mono<MeasureReport> evaluateMeasure(Measure measure) {
        logger.info("Begin Evaluating Measure: for {} groups", measure.getGroup().size());

        return Flux.fromStream(measure.getGroup().stream()).parallel(maxConcurrency).runOn(SCHEDULER)
                .flatMap(groupEvaluator::evaluateGroup, false, 1)
                .doOnNext(response -> logger.info("Evaluated {} out of {} groups", progressCounter.incrementAndGet(), measure.getGroup().size()))
                .sequential().collectList()
                .map(measureReportGroup -> new MeasureReport().setGroup(measureReportGroup));
    }
}
