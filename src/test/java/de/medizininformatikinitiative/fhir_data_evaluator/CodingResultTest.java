package de.medizininformatikinitiative.fhir_data_evaluator;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CodingResultTest {
    String SOME_DISPLAY = "some-display";
    String SYS_1 = "sys1";
    String SYS_2 = "sys2";
    String CODE_1 = "code1";
    String CODE_2 = "code2";

    HashableCoding CODING_1 = new HashableCoding(SYS_1, CODE_1, SOME_DISPLAY);
    HashableCoding CODING_2 = new HashableCoding(SYS_2, CODE_2, SOME_DISPLAY);
    ComponentKeyPair KEY_1 = new ComponentKeyPair(CODING_1, CODING_2);
    Set<ComponentKeyPair> KEY_SET_1 = Set.of(KEY_1);
    HashableCoding POPULATION_CODE = new HashableCoding("population-system", "population-code", "population-display");
    HashableCoding STRTIFIER_CODE = new HashableCoding("stratifier-system", "stratifier-code", "stratifier-display");
    int VALUE_1 = 2;
    int VALUE_2 = 4;

    @Test
    void testMergeSimple() {
        StratifierResult result1 = new StratifierResult(Map.of(KEY_SET_1, new PopulationsCount(new PopulationCount(POPULATION_CODE, VALUE_1))), STRTIFIER_CODE);
        StratifierResult result2 = new StratifierResult(Map.of(KEY_SET_1, new PopulationsCount(new PopulationCount(POPULATION_CODE, VALUE_2))), STRTIFIER_CODE);

        StratifierResult merged = result1.merge(result2);

        assertThat(merged.counts().get(KEY_SET_1).initialPopulation().count()).isEqualTo(VALUE_1 + VALUE_2);
    }

}
