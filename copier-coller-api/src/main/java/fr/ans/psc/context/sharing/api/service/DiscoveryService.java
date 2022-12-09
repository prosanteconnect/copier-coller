package fr.ans.psc.context.sharing.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import fr.ans.psc.context.sharing.api.exception.PscSchemaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class DiscoveryService {

    @Value("${schemas.file.repository}")
    private String schemasFileRepository;

    public List<String> getAllSchemas() throws PscSchemaException {

        File jsonSchemasRepository = new File(schemasFileRepository);
        File[] jsonSchemaFiles = jsonSchemasRepository.listFiles();
        List<String> schemas = new ArrayList<>();

        try {
            for (File file : Objects.requireNonNull(jsonSchemaFiles)) {
                String schema = Files.readString(Path.of(file.getPath()));
                schemas.add(schema);
            }
            return schemas;
        } catch (IOException e) {
            log.error("error occurred when reading json schemas");
            throw new PscSchemaException();
        }
    }

    public String getPscJsonContextSchema(String schemaId) throws PscSchemaException {
        File jsonSchemaFile = new File(schemasFileRepository, schemaId + ".json");

        try {
            return Files.readString(Path.of(jsonSchemaFile.getPath()));
        } catch (FileNotFoundException e) {
            log.error("Unknown schema submitted");
            throw new PscSchemaException();
        } catch (IOException e) {
            log.error("could not read schema {}", schemaId);
            throw new PscSchemaException();
        }
    }

    public void createPscContextJsonSchema(String schemaString) {
    }

    public void updatePscContextJsonSchema(String schemaString) {
    }
}
