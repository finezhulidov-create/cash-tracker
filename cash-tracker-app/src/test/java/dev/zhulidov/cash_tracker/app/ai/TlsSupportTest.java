package dev.zhulidov.cash_tracker.app.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TlsSupportTest {

    @TempDir
    Path temp;

    private Path testCertificate() throws Exception {
        return Path.of(getClass().getResource("/ai/test-ca.pem").toURI());
    }

    private static boolean containsSubject(KeyStore store, String commonName) throws Exception {
        for (String alias : Collections.list(store.aliases())) {
            if (store.getCertificate(alias) instanceof X509Certificate certificate
                    && certificate.getSubjectX500Principal().getName().contains(commonName)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void addsTheGivenCertificateToTheStandardOnes() throws Exception {
        KeyStore withExtra = TlsSupport.trustStoreWith(List.of(testCertificate()));
        KeyStore standardOnly = TlsSupport.trustStoreWith(List.of());

        assertTrue(containsSubject(withExtra, "Cash Tracker Test CA"));
        assertFalse(containsSubject(standardOnly, "Cash Tracker Test CA"));
        assertEquals(standardOnly.size() + 1, withExtra.size());
        assertTrue(standardOnly.size() > 10, "the standard Java trust store should be kept: " + standardOnly.size());
    }

    @Test
    void severalFilesAndBundlesAreSupported() throws Exception {
        String pem = Files.readString(testCertificate());
        Path bundle = Files.writeString(temp.resolve("bundle.pem"), pem + "\n" + pem);
        Path single = Files.writeString(temp.resolve("single.pem"), pem);

        int base = TlsSupport.trustStoreWith(List.of()).size();

        assertEquals(base + 2, TlsSupport.trustStoreWith(List.of(bundle)).size());
        assertEquals(base + 3, TlsSupport.trustStoreWith(List.of(bundle, single)).size());
    }

    @Test
    void buildsAnSslContext() throws Exception {
        SSLContext context = TlsSupport.sslContext(List.of(testCertificate()));

        assertEquals("TLS", context.getProtocol());
        assertNotNull(context.getSocketFactory());
    }

    @Test
    void aMissingFileIsAnErrorWithThePath() {
        Path missing = temp.resolve("nope.pem");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> TlsSupport.trustStoreWith(List.of(missing)));

        assertTrue(error.getMessage().contains("nope.pem"), error.getMessage());
    }

    @Test
    void aFileWithoutCertificatesIsAnError() throws IOException {
        Path garbage = Files.writeString(temp.resolve("garbage.pem"), "this is not a certificate");

        assertThrows(IllegalStateException.class, () -> TlsSupport.trustStoreWith(List.of(garbage)));
    }
}
