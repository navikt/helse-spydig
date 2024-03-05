package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import java.net.URI

class JsonSchemaValidator {

    private val schema = JsonSchemaFactory
        .getInstance(SpecVersion.VersionFlag.V202012)
        .getSchema(URI.create("https://raw.githubusercontent.com/navikt/helse/main/subsumsjon/json-schema-1.0.0.json"))

    fun errors(message: JsonNode): Set<ValidationMessage>? {
        return schema.validate(message).ifEmpty { null }
    }
}
