package de.medizininformatikinitiative.fhir_data_evaluator;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

/**
 * A key that can be used as one part of a {@link ComponentKeyPair key pair}.
 * <p>
 * Classes implementing this interface should implement the {@code hashCode()} and {@code equals()} methods.
 */

public interface ComponentKey {

    Coding toCoding();

    CodeableConcept toCodeableConcept();
}
