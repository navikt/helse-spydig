package no.nav.helse

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import java.net.URI

class JsonSchemaValidator {

    private val schema = JsonSchemaFactory
        .getInstance(SpecVersion.VersionFlag.V7)
        .getSchema(URI.create("https://raw.githubusercontent.com/navikt/helse/main/subsumsjon/json-schema-1.0.0.json"))

    private fun JsonSchema.validateMessage(json: JsonNode): Boolean {
        val valideringsfeil = validate(json)
        return if (valideringsfeil.isNotEmpty()) {
            logger.info("Fant en feil:\n $valideringsfeil \n det var ${json["kilde"].asText()} som var synderen ")
            false
        } else {
            true
        }
    }

    fun isJSONvalid(message: JsonNode): Boolean {
        return schema.validateMessage(message)
    }
}