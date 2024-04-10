package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import picocli.CommandLine;


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
        WebClient.Builder builder = WebClient.builder()
                .baseUrl("http://localhost:8080/fhir/")
                .defaultHeader("Accept", "application/fhir+json");

        return builder.build();
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FhirDataEvaluatorApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

}

@Component
class EvaluationStarter implements CommandLineRunner {

    private final FhirDataEvaluator evaluationCommand;

    @Autowired
    public EvaluationStarter(FhirDataEvaluator evaluationCommand) {
        this.evaluationCommand = evaluationCommand;
    }

    @Override
    public void run(String... args) {
        int exitCode = new CommandLine(evaluationCommand).execute(args);
        System.exit(exitCode);
    }
}
