package de.medizininformatikinitiative.fhir_data_evaluator;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public record CodingResult(Map<Set<StratifierCodingKey>, Integer> codingCounts) implements StratifierResult {

    public static CodingResult ofSingleKey(StratifierCodingKey key) {
        return new CodingResult(Collections.singletonMap(Set.of(key), 1));
    }

    public static CodingResult ofSingleSet(Set<StratifierCodingKey> set) {
        return new CodingResult(Map.of(set, 1));
    }

    @Override
    public StratifierResult merge(StratifierResult other) {
        if (!(other instanceof CodingResult otherCodingResult)) {
            // TODO error handling/ Either?
            return null;
        }
        return new CodingResult(Stream.concat(codingCounts.entrySet().stream(), otherCodingResult.codingCounts.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum)));
    }
}
