package no.nav.helse

import com.networknt.schema.Error
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import tools.jackson.databind.JsonNode

class JsonSchemaValidator {
    private val schema =
        SchemaRegistry
            .withDefaultDialect(SpecificationVersion.DRAFT_2020_12) { builder ->
                // Fra json-schema-validator 2.0 hentes ikke remote-skjemaer lenger som standard
                builder.schemaLoader { it.fetchRemoteResources() }
            }.getSchema(SchemaLocation.of("https://raw.githubusercontent.com/navikt/helse/main/subsumsjon/json-schema-1.0.0.json"))

    fun errors(message: JsonNode): List<Error>? = schema.validate(message).ifEmpty { null }
}
