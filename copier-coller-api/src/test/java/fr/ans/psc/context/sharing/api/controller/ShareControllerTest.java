package fr.ans.psc.context.sharing.api.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import fr.ans.psc.context.sharing.api.ContextSharingApiApplication;
import fr.ans.psc.context.sharing.api.TestRedisConfiguration;
import fr.ans.psc.context.sharing.api.model.PscContext;
import fr.ans.psc.context.sharing.api.repository.PscContextRepository;
import fr.ans.psc.context.sharing.api.service.PscAuthService;
import fr.ans.psc.context.sharing.api.service.ShareService;
import fr.ans.psc.context.sharing.api.utils.MemoryAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.AutoConfigureDataRedis;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestRedisConfiguration.class)
@AutoConfigureDataRedis
@AutoConfigureMockMvc
@DirtiesContext
@ActiveProfiles("test")
@ContextConfiguration(classes = ContextSharingApiApplication.class)
public class ShareControllerTest {
    protected MemoryAppender memoryAppender;
    protected final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PscContextRepository pscContextRepository;

    @Autowired
    private PscAuthService authService;

    private ObjectMapper mapper = new ObjectMapper();

    private final String ACCEPT_HEADER = "Accept";
    private final String APPLICATION_JSON = "application/json";
    private final String AUTHORIZATION_HEADER = "Authorization";
    private final String BEARER = "Bearer 574ef891";

    @RegisterExtension
    static WireMockExtension httpMockServer = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .usingFilesUnderClasspath("wiremock/api"))
            .configureStaticDsl(true).build();

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry propertiesRegistry) {
        propertiesRegistry.add("psc.url.introspection", () -> httpMockServer.baseUrl() + "/token/introspect");
        propertiesRegistry.add("psc.url.userinfo", () -> httpMockServer.baseUrl() + "/userinfo");
        propertiesRegistry.add("psc.clientID", () -> "client-id");
        propertiesRegistry.add("psc.clientSecret", () -> "client-secret");
    }

    @BeforeEach
    public void setUp(WebApplicationContext context) {
        // LOG APPENDER
        Logger controllerLogger = (Logger) LoggerFactory.getLogger(ShareController.class);
        Logger authServiceLogger = (Logger) LoggerFactory.getLogger(PscAuthService.class);
        Logger shareServiceLogger = (Logger) LoggerFactory.getLogger(ShareService.class);
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        controllerLogger.setLevel(Level.DEBUG);
        controllerLogger.addAppender(memoryAppender);
        authServiceLogger.setLevel(Level.DEBUG);
        authServiceLogger.addAppender(memoryAppender);
        shareServiceLogger.setLevel(Level.DEBUG);
        shareServiceLogger.addAppender(memoryAppender);
        memoryAppender.start();

        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @DisplayName("put context should be rejected if token absent")
    public void failedWithoutTokenTest() throws Exception {
        ResultActions reqWithoutToken = mockMvc.perform(put("/share")
                .header(ACCEPT_HEADER, APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .content("{\"schemaId\":\"patient-info\",\"bag\":\"{\\\"nationalId\\\":\\\"123\\\"}\"}")
                .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().is(HttpStatus.FORBIDDEN.value()));

        assertThat(memoryAppender.contains("No access token provided", Level.ERROR)).isTrue();
    }

    @Test
    @DisplayName("invalid token should failed")
    public void invalidTokenFailedTest() throws Exception {
        httpMockServer.stubFor(WireMock.post("/token/introspect").willReturn(aResponse().withStatus(200).withBody("{\"active\":\"false\"}")));
        ResultActions invalidToken = mockMvc.perform(put("/share")
                .header(ACCEPT_HEADER, APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER, BEARER)
                .contentType(APPLICATION_JSON)
                .content("{\"schemaId\":\"patient-info\",\"bag\":\"{\\\"nationalId\\\":\\\"123\\\"}\"}")
                .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));

        assertThat(memoryAppender.contains("The provided token is not valid", Level.ERROR)).isTrue();
    }

    @Test
    @DisplayName("provider response without active field failed")
    public void invalidFieldProviderResponseTest() throws Exception {
        httpMockServer.stubFor(WireMock.post("/token/introspect").willReturn(aResponse().withStatus(200).withBody("{\"other_field\":\"false\"}")));
        ResultActions invalidActiveField = mockMvc.perform(put("/share")
                .header(ACCEPT_HEADER, APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER, BEARER)
                .contentType(APPLICATION_JSON)
                .content("{\"schemaId\":\"patient-info\",\"bag\":\"{\\\"nationalId\\\":\\\"123\\\"}\"}")
                .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));

        assertThat(memoryAppender.contains("Invalid PSC introspection response : active field not found", Level.ERROR)).isTrue();
    }

    @Test
    @DisplayName("invalid provider json response failed")
    public void invalidProviderJsonResponseTest() throws Exception {
        httpMockServer.stubFor(WireMock.post("/token/introspect").willReturn(aResponse().withStatus(200).withBody("{active:\"false\"}")));
        ResultActions invalidJsonResponse = mockMvc.perform(put("/share")
                .header(ACCEPT_HEADER, APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER, BEARER)
                .contentType(APPLICATION_JSON)
                .content("{\"schemaId\":\"patient-info\",\"bag\":\"{\\\"nationalId\\\":\\\"123\\\"}\"}")
                .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));

        assertThat(memoryAppender.contains("Technical error during PSC introspect parsing", Level.ERROR)).isTrue();
    }

    @Test
    @DisplayName("put PscContext OK")
    public void putContextOkTest() throws Exception {
        // mock ProSanteConnect calls
        String userInfosJson = Files.readString(new ClassPathResource("userInfos.json").getFile().toPath());
        httpMockServer.stubFor(WireMock.post("/token/introspect").willReturn(aResponse().withStatus(200).withBody("{\"active\":\"true\"}")));
        httpMockServer.stubFor(WireMock.get("/userinfo").willReturn(aResponse().withStatus(200).withBody(userInfosJson)));

        // mock API calls
        String requestContentJson = "{\"schemaId\":\"patient-info\",\"bag\":{\"ps\":{\"nationalId\":\"123\"}}}";
        String responseContentJson = "{\"psId\":\"899700218896\",\"schemaId\":\"patient-info\",\"bag\":{\"ps\":{\"nationalId\":\"123\"}}}";

        // put request
        ResultActions putRequest = mockMvc.perform(put("/share")
                .header(ACCEPT_HEADER, APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER, BEARER)
                .contentType(APPLICATION_JSON)
                .content(requestContentJson)
                .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().is(200))
                .andExpect(content().json(responseContentJson));
    }

    @Test
    @DisplayName("put PscContext OK")
    public void putContextDeeperTest() throws Exception {
        // mock ProSanteConnect calls
        String userInfosJson = Files.readString(new ClassPathResource("userInfos.json").getFile().toPath());
        httpMockServer.stubFor(WireMock.post("/token/introspect").willReturn(aResponse().withStatus(200).withBody("{\"active\":\"true\"}")));
        httpMockServer.stubFor(WireMock.get("/userinfo").willReturn(aResponse().withStatus(200).withBody(userInfosJson)));

        // mock API calls
        String requestContentJson = "{\"schemaId\":\"alt\",\"bag\":{\"ps\":{\"nationalId\":\"123\",\"health_structure\":{\"structureTechnicalId\":\"456\"}}}}";
        String responseContentJson = "{\"psId\":\"899700218896\",\"schemaId\":\"alt\",\"bag\":{\"ps\":{\"nationalId\":\"123\",\"health_structure\":{\"structureTechnicalId\":\"456\"}}}}";

        // put request
        ResultActions putRequest = mockMvc.perform(put("/share")
                .header(ACCEPT_HEADER, APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER, BEARER)
                .contentType(APPLICATION_JSON)
                .content(requestContentJson)
                .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().is(200))
                .andExpect(content().json(responseContentJson));
    }

    @Test
    @DisplayName("get PscContext OK")
    public void getPscContextTest() throws Exception {
        // mock ProSanteConnect calls
        String userInfosJson = Files.readString(new ClassPathResource("userInfos.json").getFile().toPath());
        httpMockServer.stubFor(WireMock.post("/token/introspect").willReturn(aResponse().withStatus(200).withBody("{\"active\":\"true\"}")));
        httpMockServer.stubFor(WireMock.get("/userinfo").willReturn(aResponse().withStatus(200).withBody(userInfosJson)));

        // populate Redis
        JsonNode bag = mapper.readTree("{\"ps\":{\"nationalId\":\"899700218896\"}}");
        PscContext storedContext = new PscContext("899700218896", "patient-info", bag);

        pscContextRepository.save(storedContext);
        String responseContentJson = mapper.writeValueAsString(storedContext);

        // get request
        ResultActions getRequest = mockMvc.perform(get("/share")
                .header(ACCEPT_HEADER, APPLICATION_JSON)
                .header(AUTHORIZATION_HEADER, BEARER)
                .contentType(APPLICATION_JSON)
                .characterEncoding(StandardCharsets.UTF_8))
                .andExpect(status().is(200))
                .andExpect(content().json(responseContentJson));
    }
}
