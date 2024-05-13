package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.FileWriter;
import java.io.IOException;
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
    public WebClient webClient(@Value("${fhir.server}") String fhirServer,
                               @Value("${fhir.user}") String user,
                               @Value("${fhir.password}") String password,
                               @Value("${fhir.maxConnections}") int maxConnections,
                               @Value("${fhir.maxQueueSize}") int maxQueueSize,
                               @Value("${fhir.bearerToken}") String bearerToken,
                               @Value("${maxInMemorySizeMib}") int maxInMemorySizeMib) {
        ConnectionProvider provider = ConnectionProvider.builder("data-store")
                .maxConnections(maxConnections)
                .pendingAcquireMaxCount(maxQueueSize)
                .build();
        HttpClient httpClient = HttpClient.create(provider);
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(fhirServer)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/fhir+json")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySizeMib * 1024 * 1024));
        if (!bearerToken.isEmpty()) {
            builder = builder.filter((request, next) -> {
                ClientRequest newReq = ClientRequest.from(request).header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken).build();
                return next.exchange(newReq);});
        }
        if (!user.isEmpty() && !password.isEmpty()) {
            builder = builder.filter(ExchangeFilterFunctions.basicAuthentication(user, password));
        }
        return builder.build();
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FhirDataEvaluatorApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

}

@Component
@Profile("!test")
class EvaluationExecutor implements CommandLineRunner {

    @Value("${measureFile}")
    private String measureFilePath;
    @Value("${outputDir}")
    private String outputDirectory;
    private final MeasureEvaluator measureEvaluator;
    private final IParser parser;

    public EvaluationExecutor(MeasureEvaluator measureEvaluator, IParser parser) {
        this.measureEvaluator = measureEvaluator;
        this.parser = parser;
    }

    private String getMeasureFile() {
        String readMeasure;
        try {
            readMeasure = Files.readString(Path.of(measureFilePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return readMeasure;
    }


    public void run(String... args) {
        String measureFile = getMeasureFile();
        Measure measure = parser.parseResource(Measure.class, measureFile);
        String measureReport = parser.encodeResourceToString(measureEvaluator.evaluateMeasure(measure).block());

        String directoryAddition = args[0];

        try {
            FileWriter fileWriter = new FileWriter(outputDirectory + directoryAddition + "/measure-report.json");
            fileWriter.write(measureReport);
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
