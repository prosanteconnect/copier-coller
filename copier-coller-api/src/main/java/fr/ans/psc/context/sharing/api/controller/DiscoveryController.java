package fr.ans.psc.context.sharing.api.controller;

import fr.ans.psc.context.sharing.api.model.PscContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/discovery")
public class DiscoveryController {

    @GetMapping
    public ResponseEntity<String> getAllRegisteredSchemas() {
        return null;
    }

    @GetMapping("/{schemaId}")
    public ResponseEntity<String> getSchemaById(@PathVariable String schemaId) {
        return null;
    }

    @PostMapping
    public ResponseEntity<String> createNewSchema(@RequestBody String schema) {
        return null;
    }

    @PutMapping
    public ResponseEntity<String> updateSchema(@RequestBody String schema) {
        return null;
    }
}
