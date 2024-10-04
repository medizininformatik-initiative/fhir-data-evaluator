package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;

@SpringBootApplication
public class FhirDataEvaluatorApplication {

    private static final Logger logger = LoggerFactory.getLogger(FhirDataEvaluatorApplication.class);

    private static final String REGISTRATION_ID = "openid-connect";

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
                               @Value("${maxInMemorySizeMib}") int maxInMemorySizeMib,
                               @Qualifier("oauth") ExchangeFilterFunction oauthExchangeFilterFunction) {
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
                return next.exchange(newReq);
            });
        }
        if (!user.isEmpty() && !password.isEmpty()) {
            builder = builder.filter(ExchangeFilterFunctions.basicAuthentication(user, password));
        }
        return builder.filter(oauthExchangeFilterFunction).build();
    }

    @Bean
    @Qualifier("oauth")
    ExchangeFilterFunction oauthExchangeFilterFunction(
            @Value("${fhir.oauth.issuer.uri}") String issuerUri,
            @Value("${fhir.oauth.client.id}") String clientId,
            @Value("${fhir.oauth.client.secret}") String clientSecret) {
        if (!issuerUri.isEmpty() && !clientId.isEmpty() && !clientSecret.isEmpty()) {
            logger.debug("Enabling OAuth2 authentication (issuer uri: '{}', client id: '{}').",
                    issuerUri, clientId);
            var clientRegistration = ClientRegistrations.fromIssuerLocation(issuerUri)
                    .registrationId(REGISTRATION_ID)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .authorizationGrantType(CLIENT_CREDENTIALS)
                    .build();
            var registrations = new InMemoryReactiveClientRegistrationRepository(clientRegistration);
            var clientService = new InMemoryReactiveOAuth2AuthorizedClientService(registrations);
            var authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                    registrations, clientService);
            var oAuthExchangeFilterFunction = new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                    authorizedClientManager);
            oAuthExchangeFilterFunction.setDefaultClientRegistrationId(REGISTRATION_ID);

            return oAuthExchangeFilterFunction;
        } else {
            logger.debug("Skipping OAuth2 authentication.");
            return (request, next) -> next.exchange(request);
        }
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

    private final static double NANOS_IN_SECOND = 1_000_000_000.0;

    @Value("${measureFile}")
    private String measureFilePath;
    @Value("${outputDir}")
    private String outputDirectory;

    private final MeasureEvaluator measureEvaluator;
    private final IParser parser;
    private final Quantity durationQuantity = new Quantity()
            .setCode("s")
            .setSystem("http://unitsofmeasure.org")
            .setUnit("u");

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

        long startTime = System.nanoTime();
        MeasureReport measureReport = measureEvaluator.evaluateMeasure(measure).block();
        double evaluationDuration = (double) (System.nanoTime() - startTime) / NANOS_IN_SECOND;
        assert measureReport != null;
        measureReport.addExtension(new Extension()
                .setUrl("http://fhir-evaluator/StructureDefinition/eval-duration")
                .setValue(durationQuantity.setValue(evaluationDuration)));

        String directoryAddition = args[0];

        try {
            FileWriter fileWriter = new FileWriter(outputDirectory + directoryAddition + "/measure-report.json");
            fileWriter.write(parser.encodeResourceToString(measureReport));
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
