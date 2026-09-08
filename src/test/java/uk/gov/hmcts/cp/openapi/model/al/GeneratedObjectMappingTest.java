package uk.gov.hmcts.cp.openapi.model.al;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedObjectMappingTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void json_should_map_to_AddressCandidate_object_with_all_fields() throws JsonProcessingException {
        String json = "{\n"
                + "  \"address1\": \"10\",\n"
                + "  \"address2\": \"Downing Street\",\n"
                + "  \"postcode\": \"SW1A 1AA\",\n"
                + "  \"uprn\": \"10033544886\",\n"
                + "  \"match\": 0.95,\n"
                + "  \"dpa\": { \"BUILDING_NUMBER\": \"10\" }\n"
                + "}";

        AddressCandidate candidate = mapper.readValue(json, AddressCandidate.class);
        assertThat(candidate.getAddress1()).isEqualTo("10");
        assertThat(candidate.getAddress2()).isEqualTo("Downing Street");
        assertThat(candidate.getPostcode()).isEqualTo("SW1A 1AA");
        assertThat(candidate.getUprn()).isEqualTo("10033544886");
        assertThat(candidate.getMatch()).isEqualByComparingTo("0.95");
        assertThat(candidate.getDpa()).containsEntry("BUILDING_NUMBER", "10");
    }

    @Test
    void json_should_map_to_AddressCandidate_object_with_only_required_fields() throws JsonProcessingException {
        String json = "{\n"
                + "  \"address1\": \"1\",\n"
                + "  \"postcode\": \"ZZ99 1AA\",\n"
                + "  \"uprn\": \"1\"\n"
                + "}";

        AddressCandidate candidate = mapper.readValue(json, AddressCandidate.class);
        assertThat(candidate.getAddress1()).isEqualTo("1");
        assertThat(candidate.getAddress2()).isNull();
        assertThat(candidate.getMatch()).isNull();
        assertThat(candidate.getDpa()).isNullOrEmpty();
    }

    @Test
    void json_should_map_to_AddressSearchResponse_with_results() throws JsonProcessingException {
        String json = "{\n"
                + "  \"results\": [\n"
                + "    { \"address1\": \"10\", \"address2\": \"Downing Street\", \"postcode\": \"SW1A 1AA\", \"uprn\": \"10033544886\" }\n"
                + "  ]\n"
                + "}";

        AddressSearchResponse response = mapper.readValue(json, AddressSearchResponse.class);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getPostcode()).isEqualTo("SW1A 1AA");
    }

    @Test
    void json_should_map_to_AddressSearchResponse_with_empty_results() throws JsonProcessingException {
        String json = "{ \"results\": [] }";

        AddressSearchResponse response = mapper.readValue(json, AddressSearchResponse.class);
        assertThat(response.getResults()).isEmpty();
    }

    @Test
    void json_should_map_to_DegradedResponse_object() throws JsonProcessingException {
        String json = "{\n"
                + "  \"degraded\": true,\n"
                + "  \"reason\": \"upstream-rate-limit\",\n"
                + "  \"retryAfterSeconds\": 30\n"
                + "}";

        DegradedResponse response = mapper.readValue(json, DegradedResponse.class);
        assertThat(response.getDegraded()).isTrue();
        assertThat(response.getReason()).isEqualTo(DegradedReason.UPSTREAM_RATE_LIMIT);
        assertThat(response.getRetryAfterSeconds()).isEqualTo(30);
    }

    @Test
    void json_should_map_to_DegradedResponse_without_retryAfterSeconds() throws JsonProcessingException {
        String json = "{ \"degraded\": true, \"reason\": \"circuit-open\" }";

        DegradedResponse response = mapper.readValue(json, DegradedResponse.class);
        assertThat(response.getReason()).isEqualTo(DegradedReason.CIRCUIT_OPEN);
        assertThat(response.getRetryAfterSeconds()).isNull();
    }

    @Test
    void json_should_map_to_ErrorResponse_object() throws JsonProcessingException {
        String json = "{\n"
                + "  \"error\": \"400\",\n"
                + "  \"message\": \"At least one of 'postcode' or 'firstLine' must be supplied.\",\n"
                + "  \"timestamp\": \"2025-01-01T11:11:11Z\",\n"
                + "  \"traceId\": \"a1b2c3d4e5f6g7h8\"\n"
                + "}";

        ErrorResponse errorResponse = mapper.readValue(json, ErrorResponse.class);
        assertThat(errorResponse.getError()).isEqualTo("400");
        assertThat(errorResponse.getMessage()).isEqualTo("At least one of 'postcode' or 'firstLine' must be supplied.");
        assertThat(toText(errorResponse.getTimestamp())).isEqualTo("2025-01-01T11:11:11Z");
        assertThat(errorResponse.getTraceId()).isEqualTo("a1b2c3d4e5f6g7h8");
    }

    private static String toText(Object value) {
        return value == null ? null : value.toString();
    }
}
