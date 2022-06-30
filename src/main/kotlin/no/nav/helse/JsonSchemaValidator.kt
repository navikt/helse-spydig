
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

        fun JsonSchema.validateMessage(json: JsonNode) {
            val valideringsfeil = validate(json)
            if (valideringsfeil.isNotEmpty()) { logger.info("Fant en feil:\n $valideringsfeil") }
            else {logger.info("Fant ikke en feil :)) ")}
        }

        fun validateJSON(message: JsonNode) {
            schema.validateMessage(message)
        }
}