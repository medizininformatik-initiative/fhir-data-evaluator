package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.parser.DataFormatException;

public class BundleParsingException extends RuntimeException {
    public BundleParsingException(DataFormatException originalException, String idOfMalformedRes) {
        super("Failed parsing resource " + idOfMalformedRes, originalException);
    }
}
