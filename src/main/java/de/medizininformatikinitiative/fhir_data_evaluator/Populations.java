package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Represents possibly multiple (merged) populations either on group or on stratifier level.
 *
 * @param initialPopulation     the initial population
 * @param measurePopulation     the measure population if present in the group
 * @param observationPopulation the observation population if present in the group
 */
public record Populations(InitialPopulation initialPopulation, Optional<MeasurePopulation> measurePopulation,
                          Optional<ObservationPopulation> observationPopulation) {

    public Populations {
        requireNonNull(initialPopulation);
        requireNonNull(measurePopulation);
        requireNonNull(observationPopulation);
    }

    public static final Populations INITIAL_ONE = ofInitial(InitialPopulation.ONE);

    public static Populations ofInitial(InitialPopulation initialPopulation) {
        return new Populations(initialPopulation, Optional.empty(), Optional.empty());
    }


    /**
     * Performs a deep copy of {@link Populations}, except for {@link org.hl7.fhir.r4.model.ExpressionNode}s within
     * {@link MeasurePopulation} and {@link ObservationPopulation}, which are copied shallowly.
     * <p>
     * This will also copy the set of aggregated values of the {@link ObservationPopulation} which can lead to a long
     * runtime. Currently, this is only used to copy empty {@link Populations}.
     */
    public Populations copy() {
        return new Populations(
                initialPopulation,
                measurePopulation.map(MeasurePopulation::shallowCopy),
                observationPopulation.map(ObservationPopulation::shallowCopyOf));
    }

    /**
     * Evaluates a resource and updates the {@link InitialPopulation}, {@link MeasurePopulation} and {@link ObservationPopulation}
     * accordingly.
     * <p>
     * This will mutate the set of aggregated values of the {@link AggregateUniqueCount} of the {@link ObservationPopulation}.
     *
     * @param resource the base resource to evaluate the {@link MeasurePopulation} on
     * @return the updated {@link Populations}
     */
    public Populations evaluatePopulations(Resource resource) {
        Optional<MeasurePopulation> evaluatedMeasurePop = Optional.empty();
        Optional<ObservationPopulation> evaluatedObsPop = Optional.empty();

        if (measurePopulation.isPresent()) {
            var measurePopResource = measurePopulation.get().evaluateResource(resource);
            evaluatedMeasurePop = measurePopResource.map(measurePopulation.get()::updateWithResource).or(() -> measurePopulation);

            if (observationPopulation.isPresent()) {
                evaluatedObsPop = measurePopResource.map(r -> observationPopulation.get().updateWithResource(r))
                        .or(() -> observationPopulation);
            }
        }

        return new Populations(initialPopulation.increaseCount(), evaluatedMeasurePop, evaluatedObsPop);
    }

    /**
     * Merges two {@link Populations}.
     * <p>
     * This merge operation might look computationally intensive because in the {@link ObservationPopulation#aggregateMethod()
     * aggregateMethod} of the {@link ObservationPopulation} two sets are merged. But currently this method is only called
     * where {@code other} is a "new" {@link Populations} of a single resource, which means that the set of that
     * {@link AggregateUniqueCount} can only contain one value, which is merged in O(1) for HashSets.
     *
     * @param other the other {@link Populations} to merge this population into
     * @return the merged {@link Populations}
     */
    public Populations merge(Populations other) {
        var mergedMeasurePop = measurePopulation.map(p1 -> other.measurePopulation.map(p1::merge).orElse(p1))
                .or(() -> other.measurePopulation);

        var mergedObsPop = observationPopulation.map(p1 -> other.observationPopulation.map(p1::merge).orElse(p1))
                .or(() -> other.observationPopulation);

        return new Populations(initialPopulation.merge(other.initialPopulation), mergedMeasurePop, mergedObsPop);
    }

    public List<MeasureReport.MeasureReportGroupPopulationComponent> toReportGroupPopulations() {
        var populations = new LinkedList<MeasureReport.MeasureReportGroupPopulationComponent>();

        populations.add(initialPopulation.toReportGroupPopulation());
        measurePopulation.ifPresent(mp -> populations.add(mp.toReportGroupPopulation()));
        observationPopulation.ifPresent(op -> populations.add(op.toReportGroupPopulation()));

        return populations;
    }

    public List<MeasureReport.StratifierGroupPopulationComponent> toReportStratifierPopulations() {
        var populations = new LinkedList<MeasureReport.StratifierGroupPopulationComponent>();

        populations.add(initialPopulation.toReportStratifierPopulation());
        measurePopulation.ifPresent(mp -> populations.add(mp.toReportStratifierPopulation()));
        observationPopulation.ifPresent(op -> populations.add(op.toReportStratifierPopulation()));

        return populations;
    }
}
