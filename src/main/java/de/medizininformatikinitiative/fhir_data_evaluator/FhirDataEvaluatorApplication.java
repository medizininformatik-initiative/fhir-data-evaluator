package de.medizininformatikinitiative.fhir_data_evaluator;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Quantity;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;

@SpringBootApplication
public class FhirDataEvaluatorApplication {

    private static final String REGISTRATION_ID = "openid-connect";

    @Bean
    FhirContext context() {
        return FhirContext.forR4();
    }

    @Bean
    public Logger logger() {
        return LoggerFactory.getLogger(FhirDataEvaluatorApplication.class);
    }

    @Bean
    public IFhirPath fhirPathEngine(FhirContext context) {
        return context.newFhirPath();
    }

    @Bean
    public DataStore sourceDataStore(WebClient sourceClient, @Value("${fhir.source.pageCount}") int sourcePageCount,
                                     FhirContext context, IFhirPath fhirPathEngine) {
        return new DataStore(sourceClient, sourcePageCount, context, fhirPathEngine);
    }

    @Bean
    public DataStore reportDataStore(WebClient reportClient, FhirContext context, IFhirPath fhirPathEngine) {
        return new DataStore(reportClient, 100, context, fhirPathEngine);
    }

    @Bean
    public MeasureEvaluator measureEvaluator(DataStore sourceDataStore, IFhirPath fhirPathEngine,
                                             @Value("${fhir.source.maxConnections}") int maxConnections) {
        return new MeasureEvaluator(sourceDataStore, fhirPathEngine, maxConnections);
    }

    @Bean
    public WebClient sourceClient(@Value("${fhir.source.server}") String fhirServer,
                               @Value("${fhir.source.user}") String user,
                               @Value("${fhir.source.password}") String password,
                               @Value("${fhir.source.maxConnections}") int maxConnections,
                               @Value("${fhir.source.bearerToken}") String bearerToken,
                               @Value("${maxInMemorySizeMib}") int maxInMemorySizeMib,
                               @Qualifier("sourceOauth") ExchangeFilterFunction oauthExchangeFilterFunction) {
        return getWebClient(fhirServer, user, password, maxConnections, bearerToken, maxInMemorySizeMib, oauthExchangeFilterFunction);
    }

    @Bean
    @Qualifier("sourceOauth")
    ExchangeFilterFunction sourceOauthExchangeFilterFunction(
            @Value("${fhir.source.oauth.issuer.uri}") String issuerUri,
            @Value("${fhir.source.oauth.client.id}") String clientId,
            @Value("${fhir.source.oauth.client.secret}") String clientSecret) {
        return getExchangeFilterFunction(issuerUri, clientId, clientSecret);
    }

    @Bean
    public WebClient reportClient(@Value("${fhir.report.server}") String fhirServer,
                                  @Value("${fhir.report.user}") String user,
                                  @Value("${fhir.report.password}") String password,
                                  @Value("${fhir.report.maxConnections}") int maxConnections,
                                  @Value("${fhir.report.bearerToken}") String bearerToken,
                                  @Value("${maxInMemorySizeMib}") int maxInMemorySizeMib,
                                  @Qualifier("reportOauth") ExchangeFilterFunction oauthExchangeFilterFunction) {
        return getWebClient(fhirServer, user, password, maxConnections, bearerToken, maxInMemorySizeMib, oauthExchangeFilterFunction);
    }

    @Bean
    @Qualifier("reportOauth")
    ExchangeFilterFunction reportOauthExchangeFilterFunction(
            @Value("${fhir.report.oauth.issuer.uri}") String issuerUri,
            @Value("${fhir.report.oauth.client.id}") String clientId,
            @Value("${fhir.report.oauth.client.secret}") String clientSecret) {
        return getExchangeFilterFunction(issuerUri, clientId, clientSecret);
    }

    private ExchangeFilterFunction getExchangeFilterFunction(String issuerUri, String clientId, String clientSecret) {
        if (!issuerUri.isEmpty() && !clientId.isEmpty() && !clientSecret.isEmpty()) {
            logger().debug("Enabling OAuth2 authentication (issuer uri: '{}', client id: '{}').",
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
            logger().debug("Skipping OAuth2 authentication.");
            return (request, next) -> next.exchange(request);
        }
    }

    private WebClient getWebClient(String fhirServer, String user, String password, int maxConnections,
                                   String bearerToken, int maxInMemorySizeMib, ExchangeFilterFunction oauthExchangeFilterFunction) {
        ConnectionProvider provider = ConnectionProvider.builder("data-store")
                .maxConnections(maxConnections)
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
    private final DataStore reportDataStore;
    private final Logger logger = LoggerFactory.getLogger(EvaluationExecutor.class);

    @Value("${measureFile}")
    private String measureFilePath;
    @Value("${outputDir}")
    private String outputDirectory;
    @Value("${sendReportToServer}")
    private boolean sendReportToServer;
    @Value("${createObfuscatedReport}")
    private boolean createObfuscatedReport;
    @Value("${authorIdentifierSystem}")
    private String authorIdentifierSystem;
    @Value("${authorIdentifierValue}")
    private String authorIdentifierValue;
    @Value("${projectIdentifierSystem}")
    private String projectIdentifierSystem;
    @Value("${projectIdentifierValue}")
    private String projectIdentifierValue;
    @Value("${projectIdentifierSystemObfuscatedReport}")
    private String projectIdentifierSystemObfuscated;
    @Value("${projectIdentifierValueObfuscatedReport}")
    private String projectIdentifierValueObfuscated;
    @Value("${fhir.report.server}")
    private String reportServer;
    @Value("${obfuscationCount}")
    private int obfuscationCount;
    private final String TRANSACTION_BUNDLE_TEMPLATE_FILE = "/transaction-bundle-template.json";

    private final MeasureEvaluator measureEvaluator;
    private final FhirContext context;
    private final Quantity durationQuantity = new Quantity()
            .setCode("s")
            .setSystem("http://unitsofmeasure.org")
            .setUnit("u");

    public EvaluationExecutor(MeasureEvaluator measureEvaluator, FhirContext context, DataStore reportDataStore) {
        this.measureEvaluator = measureEvaluator;
        this.context = context;
        this.reportDataStore = reportDataStore;
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

    private String readFromInputStream(InputStream inputStream)
            throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    private String getBundleTemplate() {
        try {
            return readFromInputStream(EvaluationExecutor.class.getResourceAsStream(TRANSACTION_BUNDLE_TEMPLATE_FILE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getDocRefId(String projectIdentifierSystem, String projectIdentifierValue) {
        var documentReferences = reportDataStore.getResources(reportServer + "/DocumentReference")
                .map(r -> (DocumentReference)r.mainResource()).collectList().block();

        var refsWithSameProjId = documentReferences.stream()
                .filter(r -> r.getMasterIdentifier() != null
                        && projectIdentifierSystem.equals(r.getMasterIdentifier().getSystem())
                        && projectIdentifierValue.equals(r.getMasterIdentifier().getValue())).toList();

        if (refsWithSameProjId.size() > 1) {
            throw new RuntimeException(String.format("Multiple DocumentReferences exist for masterIdentifier " +
                            "{system: '%s', code: '%s'} on FHIR server - please delete old DocumentReferences for transfer.",
                    projectIdentifierSystem, projectIdentifierValue));
        } else if (refsWithSameProjId.size() == 1) {
            return refsWithSameProjId.get(0).getIdPart();
        } else {
            return UUID.randomUUID().toString();
        }
    }

    private String createTransactionBundle(String date, String report, String projectIdentifierSystem, String projectIdentifierValue) {
        var docRefId = getDocRefId(projectIdentifierSystem, projectIdentifierValue);

        JSONObject jo = new JSONObject(getBundleTemplate());

        var authorIdent = jo.getJSONArray("entry").getJSONObject(0).getJSONObject("resource")
                .getJSONArray("author").getJSONObject(0).getJSONObject("identifier");
        var masterIdent = jo.getJSONArray("entry").getJSONObject(0).getJSONObject("resource")
                .getJSONObject("masterIdentifier");

        authorIdent.put("system", authorIdentifierSystem);
        authorIdent.put("value", authorIdentifierValue);
        masterIdent.put("system", projectIdentifierSystem);
        masterIdent.put("value", projectIdentifierValue);

        jo.getJSONArray("entry").getJSONObject(0).getJSONObject("resource").put("date", date);

        var docRefUrl = "urn:uuid:" + UUID.randomUUID();
        var reportUrl = "urn:uuid:" + UUID.randomUUID();
        jo.getJSONArray("entry").getJSONObject(0).getJSONObject("resource").getJSONArray("content")
                .getJSONObject(0).getJSONObject("attachment").put("url", reportUrl);
        jo.getJSONArray("entry").getJSONObject(0).put("fullUrl", docRefUrl);
        jo.getJSONArray("entry").getJSONObject(0).getJSONObject("request").put("url", "DocumentReference/" + docRefId);
        jo.getJSONArray("entry").getJSONObject(0).getJSONObject("resource").put("id", docRefId);

        jo.getJSONArray("entry").getJSONObject(1).put("fullUrl", reportUrl);
        jo.getJSONArray("entry").getJSONObject(1).put("resource", new JSONObject(report));
        return jo.toString();
    }

    private int obfuscateCount(int count) {
        return count >= 1 && count <= obfuscationCount ? obfuscationCount : count;
    }

    private BigDecimal obfuscateCount(BigDecimal count) {
        return new BigDecimal(String.valueOf(obfuscateCount(count.intValueExact())));
    }

    private Quantity obfuscateCount(Quantity quantity) {
        return quantity.copy().setValue(obfuscateCount(quantity.getValue()));
    }

    private MeasureReport obfuscateReport(MeasureReport r) {
        MeasureReport obfuscatedReport = r.copy();
        obfuscatedReport.getGroup().forEach(g -> {
            g.getPopulation().forEach(gp -> {
                gp.setCount(obfuscateCount(gp.getCount()));
                if (g.getMeasureScore().getValue() != null) {
                    g.setMeasureScore(obfuscateCount(g.getMeasureScore()));
                }
            });
            g.getStratifier().forEach(s -> s.getStratum().forEach(stratum ->{
                    stratum.getPopulation().forEach(sp -> sp.setCount(obfuscateCount(sp.getCount())));
                    if (stratum.getMeasureScore().getValue() != null) {
                        stratum.setMeasureScore(obfuscateCount(stratum.getMeasureScore()));
                    }
        }));});

        return obfuscatedReport;
    }

    private void writeFile(String path, String data) {
        try {
            FileWriter fileWriter = new FileWriter(path);
            fileWriter.write(data);
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run(String... args) {
        String measureFile = getMeasureFile();
    Measure measure = context.newJsonParser().parseResource(Measure.class, measureFile);

        long startTime = System.nanoTime();
        MeasureReport measureReport = measureEvaluator.evaluateMeasure(measure).block();
        double evaluationDuration = (double) (System.nanoTime() - startTime) / NANOS_IN_SECOND;
        assert measureReport != null;
        measureReport.addExtension(new Extension()
                .setUrl("http://fhir-evaluator/StructureDefinition/eval-duration")
                .setValue(durationQuantity.setValue(evaluationDuration)));
        Optional<MeasureReport> obfuscatedReport = createObfuscatedReport ? Optional.of(obfuscateReport(measureReport)) : Optional.empty();

        String parsedReport = context.newJsonParser().encodeResourceToString(measureReport);
        Optional<String> parsedObfuscatedReport = obfuscatedReport.map(r -> context.newJsonParser().encodeResourceToString(r));

        String directoryAddition = args[0];
        String dateForBundle = args[1];
        if(sendReportToServer) {
            logger.info("Uploading MeasureReport to FHIR Report server at {}", reportServer);
            reportDataStore.postReport(createTransactionBundle(dateForBundle, parsedReport, projectIdentifierSystem, projectIdentifierValue))
                    .doOnSuccess(v -> logger.info("Successfully uploaded MeasureReport to FHIR Report server"))
                    .block();
            parsedObfuscatedReport.ifPresent(r -> {
                reportDataStore.postReport(createTransactionBundle(dateForBundle, r, projectIdentifierSystemObfuscated, projectIdentifierValueObfuscated))
                        .doOnSuccess(v -> logger.info("Successfully uploaded obfuscated MeasureReport to FHIR Report server"))
                        .block();
            });
        } else {
            writeFile(outputDirectory + directoryAddition + "/measure-report.json", parsedReport);
            parsedObfuscatedReport.ifPresent(r -> {
                    writeFile(outputDirectory + directoryAddition + "/measure-report-obfuscated.json", r);
            });
        }
    }
}
