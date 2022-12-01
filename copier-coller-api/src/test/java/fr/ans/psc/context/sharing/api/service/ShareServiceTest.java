package fr.ans.psc.context.sharing.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ans.psc.context.sharing.api.ContextSharingApiApplication;
import fr.ans.psc.context.sharing.api.exception.PscCacheException;
import fr.ans.psc.context.sharing.api.exception.PscSchemaException;
import fr.ans.psc.context.sharing.api.model.PscContext;
import fr.ans.psc.context.sharing.api.repository.PscContextRepository;
import io.lettuce.core.RedisException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.util.AssertionErrors.fail;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(classes = ContextSharingApiApplication.class)
public class ShareServiceTest {

    @Autowired
    private ShareService shareService;

    @MockBean
    private PscContextRepository repository;

    private ObjectMapper mapper = new ObjectMapper();

    private final String PATIENT_INFO_SCHEMA = "patient-info";
    private final String UNKNOWN_SCHEMA = "unknown_schema";

    @Test
    @DisplayName("should accept valid json schema")
    public void shouldAcceptValidJsonSchemaTest() throws JsonProcessingException {
        JsonNode bag = mapper.readTree("{\"ps\":{\"nationalId\":\"123\"}}");
        PscContext pscContext = new PscContext("123", PATIENT_INFO_SCHEMA, bag);

        try {
            shareService.putPsContext(pscContext);
        } catch (PscSchemaException e) {
            e.printStackTrace();
            fail("Should not have thrown PscSchemaException");
        } catch (Exception ignored) { }
    }

    @Test
    @DisplayName("should reject invalid json schema")
    public void shouldRejectInvalidJsonSchemaTest() throws JsonProcessingException {
        JsonNode bag = mapper.readTree("{\"unknown_attribute\":\"value\"}");
        PscContext pscContext = new PscContext("123", PATIENT_INFO_SCHEMA, bag);

        assertThrows(PscSchemaException.class, () -> shareService.putPsContext(pscContext));
    }

    @Test
    @DisplayName("should reject unknown json schema")
    public void shouldRejectUnknownJsonSchemaTest() throws JsonProcessingException {
        JsonNode bag = mapper.readTree("{\"ps\":{\"nationalId\":\"123\"}}");
        PscContext pscContext = new PscContext("123", UNKNOWN_SCHEMA, bag);

        assertThrows(PscSchemaException.class, () -> shareService.putPsContext(pscContext));
    }

    @Test
    @DisplayName("should handle Redis Error")
    public void shouldHandleRedisErrorTest() throws JsonProcessingException {
        JsonNode bag = mapper.readTree("{\"ps\":{\"nationalId\":\"123\"}}");
        PscContext pscContext = new PscContext("123", PATIENT_INFO_SCHEMA, bag);

        Mockito.when(repository.save(pscContext)).thenThrow(RedisException.class);
        assertThrows(PscCacheException.class, () -> shareService.putPsContext(pscContext));
    }

}
