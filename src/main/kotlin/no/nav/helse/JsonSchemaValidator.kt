
    package no.nav.helse

    import com.fasterxml.jackson.databind.JsonNode
    import com.networknt.schema.JsonSchema
    import com.networknt.schema.JsonSchemaFactory
    import com.networknt.schema.SpecVersion
    import com.networknt.schema.ValidationMessage

    class JsonSchemaValidator {

        val schema = JsonSchemaFactory
            .getInstance(SpecVersion.VersionFlag.V7)
            .getSchema("https://raw.githubusercontent.com/navikt/helse/main/subsumsjon/json-schema-1.0.0.json")

        //private val inntektsmeldingstatusSchema by lazy { "inntektsmeldingstatus".getSchema() }
        //private val vedtakSchema by lazy { "vedtak".getSchema() }

        fun JsonSchema.validateMessage(json: JsonNode) {
            val valideringsfeil = validate(json)
            if (emptySet<ValidationMessage>() == valideringsfeil) { logger.info("Fant en feil \n ${json.toPrettyString()}\n ") }
        }

        fun kaSomHelst(message: JsonNode) {
            schema.validateMessage(message)
        }
}