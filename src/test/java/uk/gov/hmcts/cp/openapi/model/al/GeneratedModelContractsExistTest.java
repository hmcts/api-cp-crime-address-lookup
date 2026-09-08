package uk.gov.hmcts.cp.openapi.model.al;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedModelContractsExistTest {

    @Test
    void generated_models_should_exist() {
        List<String> models = List.of(
                "uk.gov.hmcts.cp.openapi.model.al.AddressCandidate",
                "uk.gov.hmcts.cp.openapi.model.al.AddressSearchResponse",
                "uk.gov.hmcts.cp.openapi.model.al.AddressResponseInclude",
                "uk.gov.hmcts.cp.openapi.model.al.DegradedResponse",
                "uk.gov.hmcts.cp.openapi.model.al.DegradedReason",
                "uk.gov.hmcts.cp.openapi.model.al.ErrorResponse"
        );

        for (String fqcn : models) {
            assertTrue(classExists(fqcn), "Missing generated model: " + fqcn);
        }
    }

    @Test
    void addressCandidate_should_have_expected_accessors() throws Exception {
        Class<?> cls = Class.forName("uk.gov.hmcts.cp.openapi.model.al.AddressCandidate");
        assertHasGetter(cls, "getAddress1");
        assertHasGetter(cls, "getAddress2");
        assertHasGetter(cls, "getAddress3");
        assertHasGetter(cls, "getAddress4");
        assertHasGetter(cls, "getAddress5");
        assertHasGetter(cls, "getPostcode");
        assertHasGetter(cls, "getUprn");
        assertHasGetter(cls, "getMatch");
        assertHasGetter(cls, "getDpa");
    }

    @Test
    void addressSearchResponse_should_have_expected_accessors() throws Exception {
        Class<?> cls = Class.forName("uk.gov.hmcts.cp.openapi.model.al.AddressSearchResponse");
        assertHasGetter(cls, "getResults");
    }

    @Test
    void degradedResponse_should_have_expected_accessors() throws Exception {
        Class<?> cls = Class.forName("uk.gov.hmcts.cp.openapi.model.al.DegradedResponse");
        assertHasGetter(cls, "getDegraded");
        assertHasGetter(cls, "getReason");
        assertHasGetter(cls, "getRetryAfterSeconds");
    }

    @Test
    void errorResponse_should_have_expected_accessors() throws Exception {
        Class<?> cls = Class.forName("uk.gov.hmcts.cp.openapi.model.al.ErrorResponse");
        assertHasGetter(cls, "getError");
        assertHasGetter(cls, "getMessage");
        assertHasGetter(cls, "getTimestamp");
        assertHasGetter(cls, "getTraceId");
    }

    @Test
    void degradedReason_enum_has_expected_values() throws Exception {
        Class<?> enumCls = Class.forName("uk.gov.hmcts.cp.openapi.model.al.DegradedReason");
        assertTrue(enumCls.isEnum(), "DegradedReason should be an enum");

        var names = enumValues(enumCls);
        assertTrue(names.containsAll(List.of(
                "upstream-timeout", "upstream-rate-limit", "upstream-auth", "upstream-contract", "circuit-open"
        )), "DegradedReason must contain all documented degraded reasons. Was: " + names);
    }

    @Test
    void addressResponseInclude_enum_has_expected_values() throws Exception {
        Class<?> enumCls = Class.forName("uk.gov.hmcts.cp.openapi.model.al.AddressResponseInclude");
        assertTrue(enumCls.isEnum(), "AddressResponseInclude should be an enum");
        assertTrue(enumValues(enumCls).contains("dpa"));
    }

    // ---------- helpers ----------

    private static boolean classExists(String fqcn) {
        try {
            Class.forName(fqcn);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static void assertHasGetter(Class<?> type, String methodName) {
        boolean found = Arrays.stream(type.getMethods())
                .map(Method::getName)
                .anyMatch(methodName::equals);
        assertTrue(found, () -> "Expected getter '" + methodName + "' on " + type.getName());
    }

    /**
     * openapi-generator emits enum constants with a {@code getValue()} accessor holding the
     * literal wire value (e.g. "upstream-rate-limit"), since Java identifiers can't contain
     * hyphens - the enum constant name itself is a sanitised identifier, not the wire value.
     */
    private static List<String> enumValues(Class<?> enumClass) throws Exception {
        Method getValue = enumClass.getMethod("getValue");
        return Arrays.stream(enumClass.getEnumConstants())
                .map(constant -> {
                    try {
                        return String.valueOf(getValue.invoke(constant));
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .toList();
    }
}
