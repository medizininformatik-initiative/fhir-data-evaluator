package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Measure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@CommandLine.Command
public class FhirDataEvaluator implements Runnable {
    private final MeasureEvaluator measureEvaluator;
    private final IParser parser;


    @Autowired
    public FhirDataEvaluator(MeasureEvaluator measureEvaluator, IParser parser) {
        this.measureEvaluator = measureEvaluator;
        this.parser = parser;
    }

    @CommandLine.Option(names = {"-mf", "--measure-file"}, required = true)
    private String measureFilePath;


    @Override
    public void run() {
        String measureString;
        try {
            measureString = Files.readString(Path.of(measureFilePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Measure measure = parser.parseResource(Measure.class, measureString);
        String reportString = parser.encodeResourceToString(measureEvaluator.evaluateMeasure(measure).block());

        System.out.println(reportString);
    }
}
