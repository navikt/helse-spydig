
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

        fun JsonSchema.validateMessage(json: JsonNode): Boolean {
            val valideringsfeil = validate(json)
            if (valideringsfeil.isNotEmpty()) {
                logger.info("Fant en feil:\n $valideringsfeil")
                return false
            }
            else {
                return true
            }
        }

        fun isJSONvalid(message: JsonNode): Boolean {
            return schema.validateMessage(message)
        }
}