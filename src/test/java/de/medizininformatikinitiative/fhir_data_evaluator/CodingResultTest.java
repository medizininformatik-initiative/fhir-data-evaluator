package de.medizininformatikinitiative.fhir_data_evaluator;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CodingResultTest {
    String SYS_1 = "sys1";
    String SYS_2 = "sys2";
    String CODE_1 = "code1";
    String CODE_2 = "code2";

    HashableCoding CODING_1 = new HashableCoding(SYS_1, CODE_1);
    HashableCoding CODING_2 = new HashableCoding(SYS_2, CODE_2);
    StratifierCodingKey KEY_1 = new StratifierCodingKey(CODING_1, CODING_2);
    Set<StratifierCodingKey> KEY_SET_1 = Set.of(KEY_1);
    int VALUE_1 = 2;
    int VALUE_2 = 4;

    @Test
    void testMergeSimple() {
        CodingResult result1 = new CodingResult(Map.of(KEY_SET_1, VALUE_1));
        CodingResult result2 = new CodingResult(Map.of(KEY_SET_1, VALUE_2));

        CodingResult merged = (CodingResult) result1.merge(result2);

        assertThat(merged.codingCounts().get(KEY_SET_1)).isEqualTo(VALUE_1 + VALUE_2);
    }

}
