package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import picocli.CommandLine;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;


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
    public WebClient webClient(@Value("${fhir.server}")String fhirServer,
                               @Value("${fhir.user}") String user,
                               @Value("${fhir.password}") String password,
                               @Value("${fhir.maxConnections}") int maxConnections,
                               @Value("${fhir.maxQueueSize}") int maxQueueSize) throws Exception {
        ConnectionProvider provider = ConnectionProvider.builder("data-store")
                .maxConnections(4)
                .pendingAcquireMaxCount(500)
                .build();
        HttpClient httpClient = HttpClient.create(provider);
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(fhirServer)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/fhir+json");
        if (!user.isEmpty() && !password.isEmpty()) {
            builder = builder.filter(ExchangeFilterFunctions.basicAuthentication(user, password));
        }
        return builder.build();
    }

    @Bean
    public PrintStream outStream() {
        return System.out;
    }

}

@Component
@Profile("!test")
class EvaluationExecutor implements InitializingBean {

    @Value("${measureFile}")
    private String measureFilePath;
    private final MeasureEvaluator measureEvaluator;
    private final PrintStream outStream;
    private final IParser parser;
    public EvaluationExecutor(MeasureEvaluator measureEvaluator, PrintStream outStream, IParser parser) {
        this.measureEvaluator = measureEvaluator;
        this.outStream = outStream;
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


    @Override
    public void afterPropertiesSet() {
        String measureReport = parser.encodeResourceToString(measureEvaluator.evaluateMeasure(parser.parseResource(Measure.class, getMeasureFile())).block());
        outStream.println(measureReport);
    }
}

@CommandLine.Command
class EvaluationCommand implements Runnable {

    @CommandLine.Option(names = {"-f", "--measure-file"}, required = true)
    private String measureFilePath;
    @CommandLine.Option(names = {"-s", "--fhir-server"}, required = true)
    private String fhirServer;
    @CommandLine.Option(names = {"--fhir-user"}, required = false)
    private String fhirUser;
    @CommandLine.Option(names = {"--fhir-password"}, required = false)
    private String fhirPassword;
    @CommandLine.Option(names = {"--fhir-max-connections"}, required = false)
    private String fhirMaxConnections;
    @CommandLine.Option(names = {"--fhir-max-queue-size"}, required = false)
    private String fhirMaxQueueSize;


    @Override
    public void run() {
        System.setProperty("measureFile", measureFilePath);
        System.setProperty("fhir.server", fhirServer);
        if(fhirUser != null)
            System.setProperty("fhir.user", fhirUser);
        if(fhirPassword != null)
            System.setProperty("fhir.password", fhirPassword);
        if(fhirMaxConnections != null)
            System.setProperty("fhir.maxConnections", fhirMaxConnections);
        if(fhirMaxQueueSize != null)
            System.setProperty("fhir.maxQueueSize", fhirMaxQueueSize);

        SpringApplication app = new SpringApplication(FhirDataEvaluatorApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run();
    }

    public static void main(String[] args) {
        new CommandLine(new EvaluationCommand()).execute(args);
    }


}

