package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import picocli.CommandLine;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;


@SpringBootApplication
public class FhirDataEvaluatorApplication {

    @Bean
    FhirContext context() {
        return FhirContext.forR4();
    }

    @Bean
    public IParser parser(FhirContext context) {
        return context.newJsonParser();
    }

    @Bean
    public FHIRPathEngine fhirPathEngine(FhirContext context) {
        final DefaultProfileValidationSupport validation = new DefaultProfileValidationSupport(context);
        final IWorkerContext worker = new HapiWorkerContext(context, validation);
        return new FHIRPathEngine(worker);
    }

    @Bean
    public MeasureEvaluator measureEvaluator(DataStore dataStore, FHIRPathEngine fhirPathEngine) {
        return new MeasureEvaluator(dataStore, fhirPathEngine);
    }

    @Bean
    public WebClient webClient() {
        ConnectionProvider provider = ConnectionProvider.builder("data-store")
                .maxConnections(4)
                .pendingAcquireMaxCount(500)
                .build();
        HttpClient httpClient = HttpClient.create(provider);
        WebClient.Builder builder = WebClient.builder()
                .baseUrl("http://localhost:8080/fhir")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/fhir+json");

        return builder.build();
    }

    @Bean
    public PrintStream outStream() {
        return System.out;
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FhirDataEvaluatorApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

}

@Profile("!test")
@Component
class EvaluationStarter implements CommandLineRunner {

    private final EvaluationCommand evaluationCommand;

    @Autowired
    public EvaluationStarter(EvaluationCommand evaluationCommand) {
        this.evaluationCommand = evaluationCommand;
    }

    @Override
    public void run(String... args) {
        int exitCode = new CommandLine(evaluationCommand).execute(args);
        System.exit(exitCode);
    }
}

@Component
@CommandLine.Command
class EvaluationCommand implements Runnable {
    private final MeasureEvaluator measureEvaluator;
    private final IParser parser;
    private final PrintStream outStream;


    @Autowired
    public EvaluationCommand(MeasureEvaluator measureEvaluator, IParser parser, PrintStream outStream) {
        this.measureEvaluator = measureEvaluator;
        this.parser = parser;
        this.outStream = outStream;
    }

    @CommandLine.Option(names = {"-mf", "--measure-file"}, required = true)
    private String measureFilePath;


    @Override
    public void run() {
        String readMeasure;
        try {
            readMeasure = Files.readString(Path.of(measureFilePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        outStream.println(getMeasureReport(readMeasure));
    }

    private String getMeasureReport(String measure) {
        return parser.encodeResourceToString(measureEvaluator.evaluateMeasure(parser.parseResource(Measure.class, measure)).block());
    }


}

