package dev.zhulidov.cash_tracker.app.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmailsTest {

    @Test
    void normalize_lowercasesAndTrims() {
        assertEquals("alex@test.com", Emails.normalize("  Alex@Test.COM \n"));
    }

    @Test
    void normalize_leavesNormalizedEmailUntouched() {
        assertEquals("alex@test.com", Emails.normalize("alex@test.com"));
    }

    @Test
    void normalize_whenNull_returnsNull() {
        assertNull(Emails.normalize(null));
    }

    @Test
    void normalize_isLocaleIndependent() {
        // в турецкой локали "I".toLowerCase() давал бы "ı"; нормализация должна использовать Locale.ROOT
        java.util.Locale previous = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
            assertEquals("info@test.com", Emails.normalize("INFO@test.com"));
        } finally {
            java.util.Locale.setDefault(previous);
        }
    }
}
