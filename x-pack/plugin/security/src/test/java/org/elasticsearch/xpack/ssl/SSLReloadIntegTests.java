/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.ssl;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.CheckedRunnable;
import org.elasticsearch.common.settings.MockSecureSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.env.TestEnvironment;
import org.elasticsearch.test.SecurityIntegTestCase;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.xpack.core.ssl.SSLConfiguration;
import org.elasticsearch.xpack.core.ssl.SSLService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for SSL reloading support
 */
public class SSLReloadIntegTests extends SecurityIntegTestCase {

    private Path nodeKeyPath;
    private Path nodeCertPath;
    private Path clientCertPath;
    private Path updateableCertPath;

    @Override
    public Settings nodeSettings(int nodeOrdinal) {
        // Nodes start trusting testnode.crt and testclient.crt
        Path origKeyPath = getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode.pem");
        Path origCertPath = getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode.crt");
        Path origClientCertPath = getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testclient.crt");
        Path tempDir = createTempDir();
        try {
            if (nodeKeyPath == null) {
                nodeKeyPath = tempDir.resolve("testnode.pem");
                Files.copy(origKeyPath, nodeKeyPath);
            }
            if (nodeCertPath == null) {
                nodeCertPath = tempDir.resolve("testnode.crt");
                Files.copy(origCertPath, nodeCertPath);
            }
            if (clientCertPath == null) {
                clientCertPath = tempDir.resolve("testclient.crt");
                Files.copy(origClientCertPath, clientCertPath);
            }
            // Placeholder trusted certificate that will be updated later on
            if (updateableCertPath == null) {
                updateableCertPath = tempDir.resolve("updateable.crt");
                Files.copy(origCertPath, updateableCertPath);
            }
        } catch (IOException e) {
            throw new ElasticsearchException("failed to copy key or certificate", e);
        }

        Settings settings = super.nodeSettings(nodeOrdinal);
        Settings.Builder builder = Settings.builder()
                .put(settings.filter((s) -> s.startsWith("xpack.security.transport.ssl.") == false));
        builder.put("path.home", createTempDir())
            .put("xpack.security.transport.ssl.key", nodeKeyPath)
            .put("xpack.security.transport.ssl.key_passphrase", "testnode")
            .put("xpack.security.transport.ssl.certificate", nodeCertPath)
            .putList("xpack.security.transport.ssl.certificate_authorities",
                Arrays.asList(nodeCertPath.toString(), clientCertPath.toString(), updateableCertPath.toString()))
            .put("resource.reload.interval.high", "1s");
        //builder.put("xpack.security.transport.ssl.enabled", true);
        Settings withTransportSSL = builder.build();
        withTransportSSL
            .filter(s -> s.startsWith("xpack.security.transport.ssl"))
            .keySet()
            .forEach(key -> {
                if (key.endsWith("certificate_authorities")) {
                    builder.putList(key.replace("xpack.security.transport.ssl.", "xpack.security.http.ssl."),
                        withTransportSSL.getAsList(key));
                } else {
                    builder.put(key.replace("xpack.security.transport.ssl.", "xpack.security.http.ssl."), withTransportSSL.get(key));
                }
            });
        builder.put("xpack.security.http.ssl.enabled", true);
        return builder.build();
    }

    @Override
    protected boolean transportSSLEnabled() {
        return false;
    }

    public void testThatSSLConfigurationReloadsOnModification() throws Exception {
        Path keyPath = createTempDir().resolve("testnode_updated.pem");
        Path certPath = createTempDir().resolve("testnode_updated.crt");
        Files.copy(getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode_updated.pem"), keyPath);
        Files.copy(getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode_updated.crt"), certPath);
        MockSecureSettings secureSettings = new MockSecureSettings();
        secureSettings.setString("xpack.security.transport.ssl.secure_key_passphrase", "testnode");
        Settings settings = Settings.builder()
            .put("path.home", createTempDir())
            .put("xpack.security.transport.ssl.key", keyPath)
            .put("xpack.security.transport.ssl.certificate", certPath)
            .putList("xpack.security.transport.ssl.certificate_authorities",
                Arrays.asList(nodeCertPath.toString(), clientCertPath.toString(), updateableCertPath.toString()))
            .setSecureSettings(secureSettings)
            .build();
        String node = randomFrom(internalCluster().getNodeNames());
        SSLService sslService = new SSLService(settings, TestEnvironment.newEnvironment(settings));
        SSLConfiguration sslConfiguration = sslService.getSSLConfiguration("xpack.security.transport.ssl");
        SSLSocketFactory sslSocketFactory = sslService.sslSocketFactory(sslConfiguration);
        TransportAddress address = internalCluster()
            .getInstance(Transport.class, node).boundAddress().publishAddress();
        // Fails as our nodes do not trust testnode_updated.crt
        try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(address.getAddress(), address.getPort())) {
            assertThat(socket.isConnected(), is(true));
            socket.startHandshake();
            fail("handshake should not have been successful!");
        } catch (SSLException | SocketException expected) {
            logger.trace("expected exception", expected);
        }
        // Copy testnode_updated.crt to the placeholder updateable.crt so that the nodes will start trusting it now
        try {
            Files.move(certPath, updateableCertPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(certPath, updateableCertPath, StandardCopyOption.REPLACE_EXISTING);
        }
        CountDownLatch latch = new CountDownLatch(1);
        assertBusy(() -> {
            try (SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(address.getAddress(), address.getPort())) {
                logger.info("opened socket for reloading [{}]", socket);
                socket.addHandshakeCompletedListener(event -> {
                    try {
                        assertThat(event.getPeerPrincipal().getName(), containsString("Test Node"));
                        logger.info("ssl handshake completed on port [{}]", event.getSocket().getLocalPort());
                        latch.countDown();
                    } catch (Exception e) {
                        fail("caught exception in listener " + e.getMessage());
                    }
                });
                socket.startHandshake();

            } catch (Exception e) {
                fail("caught exception " + e.getMessage());
            }
        });
        latch.await();
    }

    public void testHttpClientDoesntTrustServer() throws Exception {
        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream is =
                 Files.newInputStream(getDataPath("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testnode_updated.jks"))) {
            store.load(is, "testnode".toCharArray());
        }
        SSLContext context = SSLContext.getInstance("TLSv1.3");
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(store, "testnode".toCharArray());
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(store);
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        assertThat(Arrays.asList(context.getDefaultSSLParameters().getProtocols()).contains("TLSv1.3"), is(true));

        try (CloseableHttpClient client = HttpClients.custom().setSSLContext(context).build()) {
            SSLHandshakeException sslException = expectThrows(SSLHandshakeException.class, () ->
                privilegedConnect(() -> client.execute(new HttpGet(getHttpURL())).close()));
            assertThat(sslException.getCause().getMessage(), containsString("PKIX path validation failed"));
        }
    }

    private static void privilegedConnect(CheckedRunnable<Exception> runnable) throws Exception {
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
                runnable.run();
                return null;
            });
        } catch (PrivilegedActionException e) {
            throw (Exception) e.getCause();
        }
    }
}
