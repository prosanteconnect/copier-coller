package fr.ans.psc.context.sharing.api.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ans.psc.context.sharing.api.ContextSharingApiApplication;
import fr.ans.psc.context.sharing.api.TestRedisConfiguration;
import fr.ans.psc.context.sharing.api.model.PscContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.AutoConfigureDataRedis;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;


import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestRedisConfiguration.class)
@AutoConfigureDataRedis
@ContextConfiguration(classes = ContextSharingApiApplication.class)
public class PsContextRepositoryTest {

    @Autowired
    private PscContextRepository ctxRepository;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("should save to Redis")
    public void shouldSavePscContext_toRedis() throws JsonProcessingException {
        JsonNode bag = mapper.readTree("{\"key\":\"value\"}");
        final PscContext pscContext = new PscContext("1", "schemaId", bag);
        final PscContext savedPscContext = ctxRepository.save(pscContext);

        assertNotNull(savedPscContext);
        assertEquals(pscContext.getBag(), savedPscContext.getBag());
        assertEquals("value", savedPscContext.getBag().get("key").asText());

        PscContext foundCtx = ctxRepository.findById("1").orElseThrow();
        assertEquals("value", foundCtx.getBag().get("key").asText());
    }

    @Test
    @DisplayName("should update context")
    public void shouldReplacePscContext() throws JsonProcessingException {
        JsonNode bag1 = mapper.readTree("{\"version\":\"1\"}");
        PscContext firstContext = new PscContext("1", "schemaName", bag1);
        PscContext firstSaved = ctxRepository.save(firstContext);

        JsonNode bag2 = mapper.readTree("{\"version\":\"2\"}");
        PscContext secondContext = new PscContext("1", "schema", bag2);
        PscContext secondSaved = ctxRepository.save(secondContext);

        assertNotEquals(firstSaved, secondSaved);
//        assertNotEquals(firstSaved.getBag(), secondSaved.getBag());
        assertEquals("2", secondSaved.getBag().get("version").asText());
        assertEquals("2", ctxRepository.findById("1").orElseThrow().getBag().get("version").asText());
    }

    // this test should only be enabled manually :
    // Spring Data Redis sets TimeToLive via @RedisHash annotation of model class
    //
    // I don't know how to override this value or set it dynamically
    // and I don't want to wait the current required duration
    @Test
    @DisplayName("should not be available after TTL")
    @Disabled
    public void shouldFlushAfterTtlTest() throws InterruptedException, JsonProcessingException {
        JsonNode bag = mapper.readTree("{\"key\":\"value\"}");
        final PscContext pscContext = new PscContext("1", "schema", bag);
        ctxRepository.save(pscContext);

        assertTrue(ctxRepository.existsById("1"));
        Thread.sleep(6000);
        assertFalse(ctxRepository.existsById("1"));
    }
}
