package fr.ans.psc.context.sharing.api.model;

public class PscContextSchema {

    private String schemaId;

    private String schema;

    public PscContextSchema() {
    }

    public PscContextSchema(String schemaId, String schema) {
        this.schemaId = schemaId;
        this.schema = schema;
    }

    public String getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(String schemaId) {
        this.schemaId = schemaId;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }
}
