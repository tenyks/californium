/*******************************************************************************
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Achim Kraus (Bosch Software Innovations GmbH) - initial implementation.
 ******************************************************************************/
package org.eclipse.californium.interoperability.test.openssl;

import static org.eclipse.californium.interoperability.test.OpenSslUtil.CA_CERTIFICATES;
import static org.eclipse.californium.interoperability.test.OpenSslUtil.CA_RSA_CERTIFICATES;
import static org.eclipse.californium.interoperability.test.OpenSslUtil.CLIENT_CERTIFICATE;
import static org.eclipse.californium.interoperability.test.OpenSslUtil.SERVER_CERTIFICATE;
import static org.eclipse.californium.interoperability.test.OpenSslUtil.SERVER_RSA_CERTIFICATE;
import static org.eclipse.californium.interoperability.test.OpenSslUtil.TRUSTSTORE;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.californium.elements.util.StringUtil;
import org.eclipse.californium.interoperability.test.OpenSslUtil;
import org.eclipse.californium.interoperability.test.ProcessUtil;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;

/**
 * Test for openssl interoperability.
 * 
 * Requires external openssl installation, otherwise the tests are skipped. On
 * linux install just the openssl package (version 1.1.1). On windows you may
 * install git for windows, <a href="https://git-scm.com/download/win" target="_blank">git</a>
 * and add the extra tools to your path ("Git/mingw64/bin", may also be done
 * using a installation option). Alternatively you may install openssl for
 * windows on it's own <a href=
 * "https://bintray.com/vszakats/generic/download_file?file_path=openssl-1.1.1c-win64-mingw.zip" target="_blank">OpenSsl
 * for Windows</a> and add that to your path.
 * 
 * Note: the windows version 1.1.1a to 1.1.1k of the openssl s_server seems to
 * be broken. It starts only to accept, when the first message is entered.
 * Therefore the test are skipped on windows.
 */
public class OpenSslProcessUtil extends ProcessUtil {

	public enum AuthenticationMode {
		/**
		 * Use PSK.
		 */
		PSK,
		/**
		 * Send peer's certificate, trust all.
		 */
		CERTIFICATE,
		/**
		 * Send peer's certificate-chain, trust all.
		 */
		CHAIN,
		/**
		 * Send peer's certificate-chain, trust provided CAs.
		 */
		TRUST
	}

	public static final String DEFAULT_CURVES = "X25519:prime256v1";
	public static final String DEFAULT_SIGALGS = "ECDSA+SHA384:ECDSA+SHA256:RSA+SHA256";

	/**
	 * Create instance.
	 */
	public OpenSslProcessUtil() {
	}

	/**
	 * Get openssl version.
	 * 
	 * @param timeMillis timeout in milliseconds
	 * @return result of version command. {@code null}, if not available.
	 */
	public ProcessResult getOpenSslVersion(long timeMillis) {
		try {
			execute("openssl", "version");
			return waitResult(timeMillis);
		} catch (InterruptedException ex) {
			return null;
		} catch (IOException ex) {
			return null;
		}
	}

	public void assumeServerVersion() {
		String os = System.getProperty("os.name");
		if (os.startsWith("Windows")) {
			try {
				ProcessResult result = waitResult(2000);
				assumeFalse("Windows openssl server 1.1.1 seems to be broken!",
						result.contains("OpenSSL 1\\.1\\.1[a-k]"));
			} catch (InterruptedException ex) {
				assumeFalse("result for openssl version missing!", true);
			}
		}
	}

	public String startupClient(String destination, OpenSslProcessUtil.AuthenticationMode authMode,
			CipherSuite... ciphers) throws IOException, InterruptedException {
		return startupClient(destination, authMode, DEFAULT_CURVES, null, ciphers);
	}

	public String startupClient(String destination, OpenSslProcessUtil.AuthenticationMode authMode, String curves,
			String sigAlgs, CipherSuite... ciphers) throws IOException, InterruptedException {
		return startupClient(destination, authMode, curves, sigAlgs, CLIENT_CERTIFICATE, ciphers);
	}

	public String startupClient(String destination, OpenSslProcessUtil.AuthenticationMode authMode, String curves,
			String sigAlgs, String clientCert, CipherSuite... ciphers) throws IOException, InterruptedException {
		List<CipherSuite> list = Arrays.asList(ciphers);
		List<String> args = new ArrayList<String>();
		String openSslCiphers = OpenSslUtil.getOpenSslCipherSuites(ciphers);
		args.addAll(Arrays.asList("openssl", "s_client", "-dtls1_2", "-4", "-connect", destination, "-cipher",
				openSslCiphers));
		if (CipherSuite.containsPskBasedCipherSuite(list)) {
			args.add("-psk");
			args.add(StringUtil.byteArray2Hex(OpenSslUtil.OPENSSL_PSK_SECRET));
		}
		if (CipherSuite.containsCipherSuiteRequiringCertExchange(list)) {
			args.add("-cert");
			args.add(clientCert);
			add(args, authMode, CA_CERTIFICATES);
		}
		add(args, curves, sigAlgs);
		execute(args);
		return "(" + openSslCiphers.replace(":", "|") + ")";
	}

	public String startupServer(String accept, OpenSslProcessUtil.AuthenticationMode authMode, CipherSuite... ciphers)
			throws IOException, InterruptedException {
		return startupServer(accept, authMode, SERVER_CERTIFICATE, null, null, ciphers);
	}

	public String startupServer(String accept, OpenSslProcessUtil.AuthenticationMode authMode, String serverCertificate,
			String curves, String sigAlgs, CipherSuite... ciphers) throws IOException, InterruptedException {
		List<CipherSuite> list = Arrays.asList(ciphers);
		List<String> args = new ArrayList<String>();
		String openSslCiphers = OpenSslUtil.getOpenSslCipherSuites(ciphers);
		args.addAll(Arrays.asList("openssl", "s_server", "-4", "-dtls1_2", "-accept", accept, "-listen", "-verify", "5",
				"-cipher", openSslCiphers));
		if (CipherSuite.containsPskBasedCipherSuite(list)) {
			args.add("-psk");
			args.add(StringUtil.byteArray2Hex(OpenSslUtil.OPENSSL_PSK_SECRET));
		}
		if (CipherSuite.containsCipherSuiteRequiringCertExchange(list)) {
			args.add("-cert");
			args.add(serverCertificate);
			String chain = CA_CERTIFICATES;
			if (SERVER_RSA_CERTIFICATE.equals(serverCertificate)) {
				chain = CA_RSA_CERTIFICATES;
			}
			add(args, authMode, chain);
		}
		add(args, curves, sigAlgs);
		execute(args);
		// ensure, server is ready to ACCEPT messages
		assumeTrue(waitConsole("ACCEPT", TIMEOUT_MILLIS));
		return "(" + openSslCiphers.replace(":", "|") + ")";
	}

	public void add(List<String> args, String curves, String sigAlgs) throws IOException, InterruptedException {
		if (curves != null) {
			args.add("-curves");
			args.add(curves);
		}
		if (sigAlgs != null) {
			args.add("-sigalgs");
			args.add(sigAlgs);
		}
	}

	public void add(List<String> args, OpenSslProcessUtil.AuthenticationMode authMode, String chain)
			throws IOException, InterruptedException {
		switch (authMode) {
		case PSK:
			break;
		case CERTIFICATE:
			args.add("-no-CAfile");
			break;
		case CHAIN:
			args.add("-no-CAfile");
			args.add("-cert_chain");
			args.add(chain);
			break;
		case TRUST:
			args.add("-CAfile");
			args.add(TRUSTSTORE);
			args.add("-build_chain");
			break;
		}
	}

	public ProcessResult stop(long timeoutMillis) throws InterruptedException, IOException {
		sendln("Q");
		return waitResult(timeoutMillis);
	}

}
