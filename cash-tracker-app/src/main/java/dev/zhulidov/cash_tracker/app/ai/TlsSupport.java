package dev.zhulidov.cash_tracker.app.ai;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Серверы GigaChat используют сертификаты национального удостоверяющего центра Минцифры, которых нет в
 * стандартном хранилище Java. Здесь к обычным доверенным сертификатам добавляются указанные файлы.
 * Проверка сертификата и имени хоста остаётся включённой: подменить сервер нельзя.
 */
final class TlsSupport {

    private TlsSupport() {
    }

    /** SSL-контекст, доверяющий стандартным корневым сертификатам Java и дополнительным из PEM-файлов. */
    static SSLContext sslContext(List<Path> extraCertificates) {
        try {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init(trustStoreWith(extraCertificates));
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, factory.getTrustManagers(), null);
            return context;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot build SSL context: " + e.getMessage(), e);
        }
    }

    /** Хранилище доверия: стандартное хранилище Java плюс все сертификаты из переданных файлов. */
    static KeyStore trustStoreWith(List<Path> extraCertificates) {
        try {
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(null, null);

            int index = 0;
            for (X509Certificate certificate : defaultTrustedCertificates()) {
                store.setCertificateEntry("default-" + index++, certificate);
            }
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            for (Path path : extraCertificates) {
                Collection<? extends Certificate> certificates = read(certificateFactory, path);
                for (Certificate certificate : certificates) {
                    store.setCertificateEntry("extra-" + index++, certificate);
                }
            }
            return store;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Cannot build trust store: " + e.getMessage(), e);
        }
    }

    private static Collection<? extends Certificate> read(CertificateFactory factory, Path path)
            throws IOException, GeneralSecurityException {
        if (!Files.isReadable(path)) {
            throw new IllegalStateException("CA certificate file is not readable: " + path);
        }
        try (InputStream in = Files.newInputStream(path)) {
            Collection<? extends Certificate> certificates = factory.generateCertificates(in);
            if (certificates.isEmpty()) {
                throw new IllegalStateException("No certificates found in " + path + " (a PEM or DER X.509 file is expected)");
            }
            return certificates;
        }
    }

    private static List<X509Certificate> defaultTrustedCertificates() throws GeneralSecurityException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        return Arrays.stream(factory.getTrustManagers())
                .filter(X509TrustManager.class::isInstance)
                .map(X509TrustManager.class::cast)
                .flatMap(manager -> Arrays.stream(manager.getAcceptedIssuers()))
                .toList();
    }
}
