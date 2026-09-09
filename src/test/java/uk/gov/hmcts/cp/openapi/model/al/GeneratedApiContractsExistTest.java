package uk.gov.hmcts.cp.openapi.model.al;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.config.OpenAPIConfigurationLoader;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedApiContractsExistTest {

    @Test
    void openAPI_bean_should_have_expected_properties() {
        OpenAPI openAPI = new OpenAPIConfigurationLoader().openAPI();
        Info info = openAPI.getInfo();

        String expectedVersion = System.getProperty("API_SPEC_VERSION", "0.0.0");
        String expectedUrl = "https://api-cp-crime-address-lookup.net/{version}";
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo(expectedUrl);

        assertThat(info.getTitle()).isEqualTo("Address Lookup API");
        assertThat(info.getDescription()).contains("OS Places");
        assertThat(info.getDescription()).contains("HMCTS Common Platform");
        assertThat(info.getDescription()).contains("OpenAPI contract for this API");
        assertThat(info.getDescription()).contains("dataset=DPA");
        assertThat(info.getDescription()).contains("LPI-only records");

        assertThat(info.getVersion()).isEqualTo(expectedVersion);

        assertThat(info.getLicense().getName()).isEqualTo("MIT");
        assertThat(info.getLicense().getUrl()).isEqualTo("https://opensource.org/licenses/MIT");

        assertThat(info.getContact().getEmail()).isEqualTo("no-reply@hmcts.com");
    }

    @Test
    void addressCandidate_schema_should_declare_required_fields_and_correct_formats() {
        OpenAPI openAPI = new OpenAPIConfigurationLoader().openAPI();
        Schema<?> schema = openAPI.getComponents().getSchemas().get("AddressCandidate");

        assertThat(schema).isNotNull();
        assertThat(schema.getRequired()).containsExactlyInAnyOrder("address1", "postcode", "uprn");
        assertThat(schema.getAdditionalProperties()).isEqualTo(Boolean.FALSE);

        Map<String, Schema> properties = schema.getProperties();
        assertThat(properties).containsKeys(
                "address1", "address2", "address3", "address4", "address5", "postcode", "uprn", "match", "dpa"
        );

        assertThat(properties.get("address1").getType()).isEqualTo("string");
        assertThat(properties.get("address1").getMaxLength()).isEqualTo(35);

        assertThat(properties.get("postcode").getType()).isEqualTo("string");
        assertThat(properties.get("postcode").getMaxLength()).isEqualTo(8);
        assertThat(properties.get("postcode").getPattern()).isEqualTo("^[A-Z]{1,2}[0-9][A-Z0-9]? [0-9][A-Z]{2}$");

        assertThat(properties.get("uprn").getType()).isEqualTo("string");
        assertThat(properties.get("uprn").getPattern()).isEqualTo("^[0-9]{1,12}$");

        assertThat(properties.get("match").getType()).isEqualTo("number");
        assertThat(properties.get("match").getMinimum()).isEqualByComparingTo("0");
        assertThat(properties.get("match").getMaximum()).isEqualByComparingTo("1");
    }

    @Test
    void addressSearchResponse_schema_should_declare_results_array() {
        OpenAPI openAPI = new OpenAPIConfigurationLoader().openAPI();
        Schema<?> schema = openAPI.getComponents().getSchemas().get("AddressSearchResponse");

        assertThat(schema).isNotNull();
        assertThat(schema.getRequired()).containsExactly("results");

        Map<String, Schema> properties = schema.getProperties();
        assertThat(properties).containsKey("results");
        assertThat(properties.get("results").getType()).isEqualTo("array");
        assertThat(properties.get("results").getItems()).isNotNull();
    }

    @Test
    void degradedResponse_schema_should_declare_required_fields() {
        OpenAPI openAPI = new OpenAPIConfigurationLoader().openAPI();
        Schema<?> schema = openAPI.getComponents().getSchemas().get("DegradedResponse");

        assertThat(schema).isNotNull();
        assertThat(schema.getRequired()).containsExactlyInAnyOrder("degraded", "reason");
        assertThat(schema.getAdditionalProperties()).isEqualTo(Boolean.FALSE);

        Map<String, Schema> properties = schema.getProperties();
        assertThat(properties).containsKeys("degraded", "reason", "retryAfterSeconds");
        assertThat(properties.get("degraded").getType()).isEqualTo("boolean");
        assertThat(properties.get("retryAfterSeconds").getType()).isEqualTo("integer");
    }

    @Test
    void degradedReason_enum_schema_should_declare_expected_values() {
        OpenAPI openAPI = new OpenAPIConfigurationLoader().openAPI();
        Schema<?> schema = openAPI.getComponents().getSchemas().get("DegradedReason");

        assertThat(schema).isNotNull();
        List<String> enumValues = schema.getEnum().stream().map(String::valueOf).toList();
        assertThat(enumValues).containsExactlyInAnyOrder(
                "upstream-timeout", "upstream-rate-limit", "upstream-auth", "upstream-contract", "circuit-open"
        );
    }

    @Test
    void addressResponseInclude_enum_schema_should_declare_expected_values() {
        OpenAPI openAPI = new OpenAPIConfigurationLoader().openAPI();

        Schema<?> includeSchema = openAPI.getComponents().getSchemas().get("AddressResponseInclude");
        assertThat(includeSchema).isNotNull();
        assertThat(includeSchema.getEnum().stream().map(String::valueOf).toList()).containsExactly("dpa");
    }

    @Test
    void errorResponse_schema_should_declare_required_fields() {
        OpenAPI openAPI = new OpenAPIConfigurationLoader().openAPI();
        Schema<?> schema = openAPI.getComponents().getSchemas().get("ErrorResponse");

        assertThat(schema).isNotNull();
        assertThat(schema.getRequired()).containsExactlyInAnyOrder("error", "message", "timestamp", "traceId");

        Map<String, Schema> properties = schema.getProperties();
        Schema<?> timestampSchema = properties.get("timestamp");
        assertThat(timestampSchema.getType()).isEqualTo("string");
        assertThat(timestampSchema.getFormat()).isEqualTo("date-time");
    }

    @Test
    void addresses_postcode_path_should_declare_postcode_query_parameters() {
        OpenAPI openAPI = new OpenAPIConfigurationLoader().openAPI();
        var operation = openAPI.getPaths().get("/addresses/postcode").getGet();

        assertThat(operation.getOperationId()).isEqualTo("searchByPostcode");
        List<String> paramNames = operation.getParameters().stream()
                .map(io.swagger.v3.oas.models.parameters.Parameter::getName)
                .toList();
        assertThat(paramNames).containsExactlyInAnyOrder("postcode", "include");

        var postcodeParam = operation.getParameters().stream()
                .filter(p -> p.getName().equals("postcode"))
                .findFirst()
                .orElseThrow();
        assertThat(postcodeParam.getRequired()).isTrue();

        var includeParam = operation.getParameters().stream()
                .filter(p -> p.getName().equals("include"))
                .findFirst()
                .orElseThrow();
        assertThat(includeParam.getRequired()).isNotEqualTo(Boolean.TRUE);
    }

    @Test
    void addresses_path_should_declare_free_text_search_query_parameters() {
        OpenAPI openAPI = new OpenAPIConfigurationLoader().openAPI();
        var operation = openAPI.getPaths().get("/addresses").getGet();

        assertThat(operation.getOperationId()).isEqualTo("searchAddresses");
        List<String> paramNames = operation.getParameters().stream()
                .map(io.swagger.v3.oas.models.parameters.Parameter::getName)
                .toList();
        assertThat(paramNames).containsExactlyInAnyOrder("address", "include");

        var addressParam = operation.getParameters().stream()
                .filter(p -> p.getName().equals("address"))
                .findFirst()
                .orElseThrow();
        assertThat(addressParam.getRequired()).isTrue();
    }

    @Test
    void addresses_find_path_should_declare_match_query_parameters() {
        OpenAPI openAPI = new OpenAPIConfigurationLoader().openAPI();
        var operation = openAPI.getPaths().get("/addresses/find").getGet();

        assertThat(operation.getOperationId()).isEqualTo("findAddress");
        var addressParam = operation.getParameters().stream()
                .filter(p -> p.getName().equals("address"))
                .findFirst()
                .orElseThrow();
        assertThat(addressParam.getRequired()).isTrue();

        var minMatchParam = operation.getParameters().stream()
                .filter(p -> p.getName().equals("minMatch"))
                .findFirst()
                .orElseThrow();
        assertThat(minMatchParam.getRequired()).isNotEqualTo(Boolean.TRUE);
    }
}
