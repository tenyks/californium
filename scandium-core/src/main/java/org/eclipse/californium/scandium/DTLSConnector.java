/*******************************************************************************
 * Copyright (c) 2015, 2018 Institute for Pervasive Computing, ETH Zurich and others.
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
 *    Matthias Kovatsch - creator and main architect
 *    Stefan Jucker - DTLS implementation
 *    Julien Vermillard - Sierra Wireless
 *    Kai Hudalla (Bosch Software Innovations GmbH) - add duplicate record detection
 *    Kai Hudalla (Bosch Software Innovations GmbH) - fix bug 462463
 *    Kai Hudalla (Bosch Software Innovations GmbH) - re-factor configuration
 *    Kai Hudalla (Bosch Software Innovations GmbH) - fix bug 464383
 *    Kai Hudalla (Bosch Software Innovations GmbH) - add support for stale
 *                                                    session expiration (466554)
 *    Kai Hudalla (Bosch Software Innovations GmbH) - replace SessionStore with ConnectionStore
 *                                                    keeping all information about the connection
 *                                                    to a peer in a single place
 *    Kai Hudalla (Bosch Software Innovations GmbH) - fix bug 472196
 *    Achim Kraus, Kai Hudalla (Bosch Software Innovations GmbH) - fix bug 478538
 *    Kai Hudalla (Bosch Software Innovations GmbH) - derive max datagram size for outbound messages
 *                                                    from network MTU
 *    Kai Hudalla (Bosch Software Innovations GmbH) - fix bug 483371
 *    Benjamin Cabe - fix typos in logger
 *    Kai Hudalla (Bosch Software Innovations GmbH) - use SessionListener to trigger sending of pending
 *                                                    APPLICATION messages
 *    Bosch Software Innovations GmbH - set correlation context on sent/received messages
 *                                      (fix GitHub issue #1)
 *    Achim Kraus (Bosch Software Innovations GmbH) - use CorrelationContextMatcher
 *                                                    for outgoing messages
 *                                                    (fix GitHub issue #104)
 *    Achim Kraus (Bosch Software Innovations GmbH) - introduce synchronized getSocket()
 *                                                    as pair to synchronized releaseSocket().
 *    Achim Kraus (Bosch Software Innovations GmbH) - restart internal executor
 *    Achim Kraus (Bosch Software Innovations GmbH) - processing retransmission of flight
 *                                                    after last flight was sent.
 *    Achim Kraus (Bosch Software Innovations GmbH) - add onSent() and onError(). 
 *                                                    issue #305
 *    Achim Kraus (Bosch Software Innovations GmbH) - Change RetransmitTask to
 *                                                    schedule a "stripped job"
 *                                                    instead of executing 
 *                                                    handleTimeout directly.
 *                                                    cancel flight only, if they
 *                                                    should not be retransmitted
 *                                                    anymore.
 *    Achim Kraus (Bosch Software Innovations GmbH) - call handshakeFailed on 
 *                                                    terminateOngoingHandshake,
 *                                                    processAlertRecord, 
 *                                                    handleTimeout,
 *                                                    and add error callback in
 *                                                    newDeferredMessageSender.
 *    Achim Kraus (Bosch Software Innovations GmbH) - reuse receive buffer and packet. 
 *    Achim Kraus (Bosch Software Innovations GmbH) - use socket's reuseAddress only
 *                                                    if bindAddress determines a port
 *    Achim Kraus (Bosch Software Innovations GmbH) - introduce protocol,
 *                                                    remove scheme
 *    Achim Kraus (Bosch Software Innovations GmbH) - check for cancelled retransmission
 *                                                    before sending.
 *    Achim Kraus (Bosch Software Innovations GmbH) - move application handler call
 *                                                    out of synchronized block
 *    Achim Kraus (Bosch Software Innovations GmbH) - move creation of endpoint context
 *                                                    to DTLSSession
 *    Bosch Software Innovations GmbH - migrate to SLF4J
 *    Achim Kraus (Bosch Software Innovations GmbH) - add automatic resumption
 *    Achim Kraus (Bosch Software Innovations GmbH) - change receiver thread to
 *                                                    daemon
 *    Achim Kraus (Bosch Software Innovations GmbH) - response with alert, if 
 *                                                    connection store is exhausted.
 *    Achim Kraus (Bosch Software Innovations GmbH) - fix double incrementing
 *                                                    pending outgoing message downcounter.
 *    Achim Kraus (Bosch Software Innovations GmbH) - update dtls session timestamp only,
 *                                                    if access is validated with the MAC 
 *    Achim Kraus (Bosch Software Innovations GmbH) - fix session resumption with session cache
 *                                                    issue #712
 *                                                    execute jobs after shutdown to ensure, 
 *                                                    onError is called for all pending messages. 
 *    Achim Kraus (Bosch Software Innovations GmbH) - fix issues #716 and #717
 *                                                    change scopes to protected to support
 *                                                    subclass specific implementations.
 *    Achim Kraus (Bosch Software Innovations GmbH) - use session ticket when sending messages
 *                                                    over a connection marked for resumption.
 *    Achim Kraus (Bosch Software Innovations GmbH) - issue 744: use handshaker as 
 *                                                    parameter for session listener.
 *                                                    Move session listener callback out of sync
 *                                                    block of processApplicationDataRecord.
 *    Achim Kraus (Bosch Software Innovations GmbH) - add handshakeFlightRetransmitted
 *    Achim Kraus (Bosch Software Innovations GmbH) - add onConnecting and onDtlsRetransmission
 *    Achim Kraus (Bosch Software Innovations GmbH) - redesign connection session listener to
 *                                                    ensure, that the session listener methods
 *                                                    are called via the handshaker.
 *                                                    Move handshakeCompleted out on synchronized block.
 *                                                    When handshaker replaced, called handshakeFailed
 *                                                    on old to trigger sent error for pending messages.
 *                                                    Reuse ongoing handshaker instead of creating a new
 *                                                    one.
 *    Achim Kraus (Bosch Software Innovations GmbH) - add multiple receiver threads.
 *                                                    move default thread numbers to configuration.
 *    Achim Kraus (Bosch Software Innovations GmbH) - add cause to handshake failure.
 *    Achim Kraus (Bosch Software Innovations GmbH) - remove HELLO_VERIFY_REQUEST
 *                                                    from resumption handshakes
 *    Achim Kraus (Bosch Software Innovations GmbH) - extend deferred processed messages to
 *                                                    limited number of incoming and outgoing messages
 *                                                    extend executor names with specific prefix.
 *    Achim Kraus (Bosch Software Innovations GmbH) - fix reuse of already stopped serial
 *                                                    executors.
 *    Achim Kraus (Bosch Software Innovations GmbH) - remove unused RecordLayer.sendRecord
 *    Achim Kraus (Bosch Software Innovations GmbH) - redesign DTLSFlight and RecordLayer
 *                                                    add timeout for handshakes
 *    Achim Kraus (Bosch Software Innovations GmbH) - move serial executor into connection
 *                                                    process new CLIENT_HELLOs without
 *                                                    serial executor.
 ******************************************************************************/
package org.eclipse.californium.scandium;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.PortUnreachableException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.eclipse.californium.elements.Connector;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.EndpointContextMatcher;
import org.eclipse.californium.elements.PersistentConnector;
import org.eclipse.californium.elements.exception.EndpointMismatchException;
import org.eclipse.californium.elements.exception.EndpointUnconnectedException;
import org.eclipse.californium.elements.exception.MulticastNotSupportedException;
import org.eclipse.californium.elements.RawData;
import org.eclipse.californium.elements.RawDataChannel;
import org.eclipse.californium.elements.util.Bytes;
import org.eclipse.californium.elements.util.ClockUtil;
import org.eclipse.californium.elements.util.DaemonThreadFactory;
import org.eclipse.californium.elements.util.DatagramReader;
import org.eclipse.californium.elements.util.ExecutorsUtil;
import org.eclipse.californium.elements.util.LeastRecentlyUsedCache;
import org.eclipse.californium.elements.util.NamedThreadFactory;
import org.eclipse.californium.elements.util.NetworkInterfacesUtil;
import org.eclipse.californium.elements.util.NoPublicAPI;
import org.eclipse.californium.elements.util.SerialExecutor;
import org.eclipse.californium.elements.util.StringUtil;
import org.eclipse.californium.elements.util.WipAPI;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.ApplicationMessage;
import org.eclipse.californium.scandium.dtls.AddressVerification;
import org.eclipse.californium.scandium.dtls.ClientHandshaker;
import org.eclipse.californium.scandium.dtls.ClientHello;
import org.eclipse.californium.scandium.dtls.Connection;
import org.eclipse.californium.scandium.dtls.ConnectionEvictedException;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.ConnectionIdGenerator;
import org.eclipse.californium.scandium.dtls.HandshakeResult;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.ContentType;
import org.eclipse.californium.scandium.dtls.DTLSConnectionState;
import org.eclipse.californium.scandium.dtls.DTLSContext;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.DtlsException;
import org.eclipse.californium.scandium.dtls.ExtendedMasterSecretMode;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeMessage;
import org.eclipse.californium.scandium.dtls.Handshaker;
import org.eclipse.californium.scandium.dtls.HelloVerifyRequest;
import org.eclipse.californium.scandium.dtls.InMemoryConnectionStore;
import org.eclipse.californium.scandium.dtls.MaxFragmentLengthExtension;
import org.eclipse.californium.scandium.dtls.ProtocolVersion;
import org.eclipse.californium.scandium.dtls.Record;
import org.eclipse.californium.scandium.dtls.RecordLayer;
import org.eclipse.californium.scandium.dtls.ResumingClientHandshaker;
import org.eclipse.californium.scandium.dtls.ResumingServerHandshaker;
import org.eclipse.californium.scandium.dtls.ResumptionSupportingConnectionStore;
import org.eclipse.californium.scandium.dtls.ServerHandshaker;
import org.eclipse.californium.scandium.dtls.ServerNameExtension;
import org.eclipse.californium.scandium.dtls.SessionAdapter;
import org.eclipse.californium.scandium.dtls.SessionStore;
import org.eclipse.californium.scandium.dtls.SessionId;
import org.eclipse.californium.scandium.dtls.SessionListener;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedPskStore;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.SecretUtil;
import org.eclipse.californium.scandium.util.ServerNames;

/**
 * A {@link Connector} using <em>Datagram TLS</em> (DTLS) as specified in
 * <a href="http://tools.ietf.org/html/rfc6347" target="_blank">RFC 6347</a> for securing data
 * exchanged between networked clients and a server application.
 * 
 * Note: using IPv6 interfaces with multiple addresses including permanent and
 * temporary (with potentially several different prefixes) currently causes
 * issues on the server side. The outgoing traffic in response to incoming may
 * select a different source address than the incoming destination address. To
 * overcome this, please ensure that the 'any address' is not used on the server
 * side and a separate Connector is created for each address to receive incoming
 * traffic.
 */
public class DTLSConnector implements Connector, PersistentConnector, RecordLayer {

	/**
	 * The {@code EndpointContext} key used to store the host name indicated by a
	 * client in an SNI hello extension.
	 */
	public static final String KEY_TLS_SERVER_HOST_NAME = "TLS_SERVER_HOST_NAME";

	private static final Logger LOGGER = LoggerFactory.getLogger(DTLSConnector.class);
	private static final Logger DROP_LOGGER = LoggerFactory.getLogger(LOGGER.getName() + ".drops");
	private static final int MAX_PLAINTEXT_FRAGMENT_LENGTH = 16384; // max. DTLSPlaintext.length (2^14 bytes)
	private static final int MAX_CIPHERTEXT_EXPANSION = CipherSuite.getOverallMaxCiphertextExpansion();
	private static final int MAX_DATAGRAM_BUFFER_SIZE = MAX_PLAINTEXT_FRAGMENT_LENGTH
			+ Record.DTLS_HANDSHAKE_HEADER_LENGTH
			+ MAX_CIPHERTEXT_EXPANSION;

	/**
	 * Additional padding used by the new record type introduced with the
	 * connection id. May be randomized to obfuscate the payload length. Due to
	 * the ongoing discussion in draft-ietf-tls-dtls-connection-id, currently
	 * only a fixed value.
	 */
	private static final int TLS12_CID_PADDING = 0;

	private static final long CLIENT_HELLO_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(60);

	/**
	 * Indicates, that MDC support is available.
	 * 
	 * @see MDC
	 */
	public static final boolean MDC_SUPPORT;

	static {
		boolean mdc = false;
		try {
			MDC.clear();
			mdc = true;
		} catch (Throwable ex) {
		}
		MDC_SUPPORT = mdc;
	}

	/** all the configuration options for the DTLS connector */ 
	protected final DtlsConnectorConfig config;

	private final ResumptionSupportingConnectionStore connectionStore;

	/**
	 * Queue with recent successful handshakes.
	 * 
	 * @since 3.0
	 */
	private final Queue<Connection> recentHandshakes = new ConcurrentLinkedQueue<>();

	/**
	 * General auto resumption timeout in milliseconds. {@code null}, if auto
	 * resumption is not used.
	 */
	private final Long autoResumptionTimeoutMillis;

	private final int thresholdHandshakesWithoutVerifiedPeer;
	private final AtomicInteger pendingHandshakesWithoutVerifiedPeer = new AtomicInteger();
	protected final DtlsHealth health;

	private final boolean serverOnly;
	private final String defaultHandshakeMode;
	/**
	 * Apply record filter only for records within the receive window.
	 */
	private final int useExtendedWindowFilter;
	/**
	 * Apply record filter.
	 */
	private final boolean useFilter;
	/**
	 * Apply address update only for newer records based on epoch/sequence_number.
	 */
	private final boolean useCidUpdateAddressOnNewerRecordFilter;

	/**
	 * (Down-)counter for pending outbound messages. Initialized with
	 * {@link DtlsConnectorConfig#getOutboundMessageBufferSize()}.
	 */
	private final AtomicInteger pendingOutboundMessagesCountdown = new AtomicInteger();

	private final List<Thread> receiverThreads = new LinkedList<Thread>();

	/**
	 * Configure connection id generator. May be {@code null}, if connection id
	 * should not be supported.
	 */
	protected final ConnectionIdGenerator connectionIdGenerator;
	/**
	 * Protocol version to use for sending a hello verify request. Default
	 * {@code null} to reply the client's version.
	 * 
	 * @since 2.5
	 */
	private final ProtocolVersion protocolVersionForHelloVerifyRequests;

	private ScheduledFuture<?> recentHandshakeCleaner;

	private ScheduledFuture<?> statusLogger;

	private InetSocketAddress lastBindAddress;
	/**
	 * Provided or configured maximum transmission unit.
	 */
	private Integer maximumTransmissionUnit;
	/**
	 * IPv4 maximum transmission unit.
	 * @since 2.4
	 */
	private int ipv4Mtu = DEFAULT_IPV4_MTU;
	/**
	 * IPv6 maximum transmission unit.
	 * @since 2.4
	 */
	private int ipv6Mtu = DEFAULT_IPV6_MTU;
	protected int inboundDatagramBufferSize = MAX_DATAGRAM_BUFFER_SIZE;

	private final CookieGenerator cookieGenerator = new CookieGenerator();

	private volatile DatagramSocket socket;

	/** The timer daemon to schedule retransmissions. */
	protected ScheduledExecutorService timer;

	/** Indicates whether the connector has started and not stopped yet */
	private AtomicBoolean running = new AtomicBoolean(false);

	/**
	 * Endpoint context matcher for outgoing messages.
	 * 
	 * @see #setEndpointContextMatcher(EndpointContextMatcher)
	 * @see #getEndpointContextMatcher()
	 * @see #sendMessage(RawData, Connection)
	 * @see #sendMessage(RawData, Connection, DTLSSession)
	 */
	private volatile EndpointContextMatcher endpointContextMatcher;

	private volatile RawDataChannel messageHandler;
	private volatile AlertHandler alertHandler;
	private final SessionListener sessionListener;
	private final ConnectionListener connectionListener;
	private ExecutorService executorService;
	private boolean hasInternalExecutor;

	/**
	 * Creates a DTLS connector from a given configuration object using the
	 * standard in-memory {@code ConnectionStore}.
	 * 
	 * @param configuration the configuration options
	 * @throws NullPointerException if the configuration is {@code null}
	 */
	public DTLSConnector(DtlsConnectorConfig configuration) {
		this(configuration, createConnectionStore(configuration));
	}

	/**
	 * Create and initialize default connection store.
	 * 
	 * @param configuration configuration for initialization
	 * @return connection store
	 * @since 3.0 (moved SessionCache from parameter to configuration)
	 */
	protected static ResumptionSupportingConnectionStore createConnectionStore(DtlsConnectorConfig configuration) {
		return new InMemoryConnectionStore(configuration.getMaxConnections(),
				configuration.getStaleConnectionThreshold(), configuration.getSessionStore()).setTag(configuration.getLoggingTag());

	}

	/**
	 * Creates a DTLS connector for a given set of configuration options.
	 * <p>
	 * The connection store must use the same connection id generator as
	 * configured in the provided configuration. The current implementation synchronize on the connection store,
	 * therefore it is important not to use the connection store within a
	 * different synchronization scope.
	 * </p>
	 * 
	 * @param configuration The configuration options.
	 * @param connectionStore The registry to use for managing connections to
	 *            peers.
	 * @throws NullPointerException if any of the parameters is {@code null}.
	 * @throws IllegalArgumentException if the connection store uses a different
	 *             cid generator than the configuration.
	 */
	protected DTLSConnector(final DtlsConnectorConfig configuration, final ResumptionSupportingConnectionStore connectionStore) {
		if (configuration == null) {
			throw new NullPointerException("Configuration must not be null");
		} else if (connectionStore == null) {
			throw new NullPointerException("Connection store must not be null");
		} else {
			this.config = configuration;
			this.connectionIdGenerator = config.getConnectionIdGenerator();
			this.protocolVersionForHelloVerifyRequests = config.getProtocolVersionForHelloVerifyRequests();
			this.pendingOutboundMessagesCountdown.set(config.getOutboundMessageBufferSize());
			this.autoResumptionTimeoutMillis = config.getAutoResumptionTimeoutMillis();
			this.serverOnly = config.isServerOnly();
			this.defaultHandshakeMode = config.getDefaultHandshakeMode();
			this.useExtendedWindowFilter = config.useExtendedWindowFilter();
			this.useFilter = config.useAntiReplayFilter() || useExtendedWindowFilter != 0;
			this.useCidUpdateAddressOnNewerRecordFilter = config.useCidUpdateAddressOnNewerRecordFilter();
			this.connectionStore = connectionStore;
			this.connectionStore.attach(connectionIdGenerator);
			this.connectionStore.setConnectionListener(config.getConnectionListener());
			this.connectionListener = config.getConnectionListener();
			HandshakeResultHandler handler = new HandshakeResultHandler() {

				@Override
				public void apply(HandshakeResult connectionResult) {
					processAsynchronousHandshakeResult(connectionResult);
				}
			};
			AdvancedPskStore advancedPskStore = config.getAdvancedPskStore();
			if (advancedPskStore != null) {
				advancedPskStore.setResultHandler(handler);
			}
			NewAdvancedCertificateVerifier certificateVerifier = config.getAdvancedCertificateVerifier();
			if (certificateVerifier != null) {
				certificateVerifier.setResultHandler(handler);
			}
			DtlsHealth healthHandler = config.getHealthHandler();
			Integer healthStatusInterval = config.getHealthStatusInterval();
			// this is a useful health metric
			// that could later be exported to some kind of monitoring interface
			if (healthHandler == null && healthStatusInterval != null && healthStatusInterval > 0) {
				healthHandler = createDefaultHealthHandler(config);
				if (!healthHandler.isEnabled()) {
					healthHandler = null;
				}
			}
			this.health = healthHandler;
			this.sessionListener = new SessionAdapter() {

				@Override
				public void contextEstablished(Handshaker handshaker, DTLSContext establishedContext) {
					DTLSConnector.this.contextEstablished(handshaker);
				}

				@Override
				public void handshakeCompleted(final Handshaker handshaker) {
					if (health != null) {
						health.endHandshake(true);
					}
					final Connection connection = handshaker.getConnection();
					if (connection.getStartNanos() != null) {
						recentHandshakes.add(connection);
					}
				}

				@Override
				public void handshakeFailed(Handshaker handshaker, Throwable error) {
					if (health != null) {
						health.endHandshake(false);
					}
					List<RawData> listOut = handshaker.takeDeferredApplicationData();
					if (!listOut.isEmpty()) {
						LOGGER.debug("Handshake with [{}] failed, report error to deferred {} messages",
								handshaker.getPeerAddress(), listOut.size());
						for (RawData message : listOut) {
							message.onError(error);
						}
					}
					Connection connection = handshaker.getConnection();
					if (handshaker.isRemovingConnection()) {
						connectionStore.remove(connection, false);
					} else if (handshaker.isProbing()) {
						LOGGER.debug("Handshake with [{}] failed within probe!", handshaker.getPeerAddress());
					} else if (connection.getEstablishedDtlsContext() == handshaker.getDtlsContext()) {
						if (error instanceof HandshakeException) {
							AlertMessage alert = ((HandshakeException)error).getAlert();
							if (alert != null && alert.getDescription() == AlertDescription.CLOSE_NOTIFY) {
								LOGGER.debug("Handshake with [{}] closed after session was established!",
										handshaker.getPeerAddress());
							} else {
								LOGGER.warn("Handshake with [{}] failed after session was established! {}",
										handshaker.getPeerAddress(), alert);
							}
						} else {
							// failure after established (last FINISH),
							// but before completed (first data)
							if (error instanceof ConnectionEvictedException) {
								LOGGER.debug("Handshake with [{}] never get APPLICATION_DATA",
										handshaker.getPeerAddress(), error);
							} else {
								LOGGER.warn("Handshake with [{}] failed after session was established!",
										handshaker.getPeerAddress(), error);
							}
						}
					} else if (connection.hasEstablishedDtlsContext()) {
						LOGGER.warn("Handshake with [{}] failed, but has an established session!",
								handshaker.getPeerAddress());
					} else {
						LOGGER.warn("Handshake with [{}] failed, connection preserved!", handshaker.getPeerAddress());
					}
				}
			};
			int maxConnections = config.getMaxConnections();
			// calculate absolute threshold from relative.
			long thresholdInPercent = config.getVerifyPeersOnResumptionThreshold();
			long threshold = (((long) maxConnections * thresholdInPercent) + 50L) / 100L;
			if (threshold == 0 && thresholdInPercent > 0) {
				threshold = 1;
			}
			this.thresholdHandshakesWithoutVerifiedPeer = (int) threshold;
		}
	}

	/**
	 * Create default health handler.
	 * 
	 * @param configuration configuration
	 * @return default health handler.
	 * @since 2.5
	 */
	protected DtlsHealth createDefaultHealthHandler(DtlsConnectorConfig configuration) {
		return new DtlsHealthLogger(configuration.getLoggingTag());
	}

	/**
	 * Initialize new create handshaker.
	 * 
	 * Add {@link #sessionListener}.
	 * 
	 * @param handshaker new create handshaker
	 */
	private final void initializeHandshaker(final Handshaker handshaker) {
		if (sessionListener != null) {
			handshaker.addSessionListener(sessionListener);
			if (health != null) {
				health.startHandshake();
			}
		}
		onInitializeHandshaker(handshaker);
	}

	/**
	 * Called after initialization of new create handshaker.
	 * 
	 * Intended to be used for subclass specific handshaker initialization.
	 * 
	 * @param handshaker new create handshaker
	 */
	protected void onInitializeHandshaker(final Handshaker handshaker) {
	}

	/**
	 * Process context established.
	 * 
	 * Called from
	 * {@link SessionListener#contextEstablished(Handshaker, DTLSContext)} to
	 * store {@link DTLSSession} and process deferred messages.
	 * 
	 * @param handshaker handshaker
	 * @since 3.0 (was sessionEstablished).
	 */
	private final void contextEstablished(Handshaker handshaker) {
		try {
			final Connection connection = handshaker.getConnection();
			connectionStore.putEstablishedSession(connection);
			final SerialExecutor serialExecutor = connection.getExecutor();
			List<RawData> listOut = handshaker.takeDeferredApplicationData();
			if (!listOut.isEmpty()) {
				LOGGER.trace("DTLS context with [{}] established, now process deferred {} outgoing messages",
						handshaker.getPeerAddress(), listOut.size());
				for (RawData message : listOut) {
					final RawData rawData = message;
					serialExecutor.execute(new Runnable() {

						@Override
						public void run() {
							sendMessage(rawData, connection);
						}
					});
				}
			}
			List<Record> listIn = handshaker.takeDeferredRecordsOfNextEpoch();
			if (!listIn.isEmpty()) {
				LOGGER.trace("DTLS context with [{}] established, now process deferred {} incoming messages",
						handshaker.getPeerAddress(), listIn.size());
				for (Record message : listIn) {
					final Record record = message;
					serialExecutor.execute(new Runnable() {

						@Override
						public void run() {
							processRecord(record, connection);
						}
					});
				}
			}
		} catch (RejectedExecutionException ex) {
			LOGGER.debug("stopping.");
		}
	}

	/**
	 * Calculate start time of recent handshakes, which starting client hellos are
	 * expired.
	 * 
	 * To prevent starting handshakes accidentally from repeated client hellos,
	 * the client's random is used to filter that for
	 * {@link #CLIENT_HELLO_TIMEOUT_MILLIS}.
	 * 
	 * @return system nanoseconds.
	 * @since 3.0
	 */
	private long calculateRecentHandshakeExpires() {
		return ClockUtil.nanoRealtime() - TimeUnit.MILLISECONDS.toNanos(CLIENT_HELLO_TIMEOUT_MILLIS);
	}

	/**
	 * Cleanup recent handshakes.
	 * 
	 * Remove starting hello client, if expired.
	 * 
	 * @since 3.0
	 */
	private void cleanupRecentHandshakes() {
		int count = 0;
		int size = 0;
		long expires = calculateRecentHandshakeExpires();
		while (true) {
			size = recentHandshakes.size();
			Connection connection = recentHandshakes.peek();
			if (connection == null) {
				break;
			}
			Long startNanos = connection.getStartNanos();
			if (startNanos == null || (expires - startNanos) >= 0) {
				connection.startByClientHello(null);
				recentHandshakes.poll();
				++count;
			} else {
				break;
			}
		}
		if (count > 0 || size > 0) {
			LOGGER.warn("Cleanup {} recent handshakes, left {}!", count, size);
		}
	}

	/**
	 * Sets the executor to be used for processing records.
	 * <p>
	 * If this property is not set before invoking the {@linkplain #start()
	 * start method}, a new {@link ExecutorService} is created with a thread
	 * pool of {@linkplain DtlsConnectorConfig#getConnectionThreadCount() size}.
	 * 
	 * This helps with performing multiple handshakes in parallel, in particular if the key exchange
	 * requires a look up of identities, e.g. in a database or using a web service.
	 * <p>
	 * If this method is used to set an executor, the executor will <em>not</em> be shut down
	 * by the {@linkplain #stop() stop method}.
	 * 
	 * @param executor The executor.
	 * @throws IllegalStateException if a new executor is set and this connector is already running.
	 */
	public final synchronized void setExecutor(ExecutorService executor) {
		if (this.executorService != executor) {
			if (running.get()) {
				throw new IllegalStateException("cannot set new executor while connector is running");
			} else {
				this.executorService = executor;
			}
		}
	}

	/**
	 * Closes a connection with a given peer.
	 * 
	 * The connection is gracefully shut down, i.e. a final
	 * <em>CLOSE_NOTIFY</em> alert message is sent to the peer.
	 * 
	 * @param peerAddress the address of the peer to close the connection to
	 * @throws RejectedExecutionException is connector is stopping or stopped.
	 */
	public final void close(InetSocketAddress peerAddress) {
		final Connection connection = getConnection(peerAddress, null, false);
		if (connection != null && connection.hasEstablishedDtlsContext()) {
			SerialExecutor serialExecutor = connection.getExecutor();
			serialExecutor.execute(new Runnable() {

				@Override
				public void run() {
					closeConnection(connection);
				}
			});
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final synchronized void start() throws IOException {
		start(config.getAddress());
	}

	/**
	 * Re-starts the connector binding to the same IP address and port as
	 * on the previous start.
	 * 
	 * Note: intended for unit tests only!
	 * 
	 * @throws IOException if the connector cannot be bound to the previous
	 *            IP address and port
	 */
	final synchronized void restart() throws IOException {
		if (lastBindAddress != null) {
			start(lastBindAddress);
		} else {
			throw new IllegalStateException("Connector has never been started before");
		}
	}

	private synchronized ExecutorService getExecutorService() {
		return executorService;
	}

	/**
	 * Start connector.
	 * 
	 * @param bindAddress address to bind socket.
	 * @throws IOException I/O error
	 */
	protected void start(InetSocketAddress bindAddress) throws IOException {
		if (running.get()) {
			return;
		}
		init(bindAddress, new DatagramSocket(null), config.getMaxTransmissionUnit());
	}

	/**
	 * Initialize socket ad start connector.
	 * 
	 * @param bindAddress address to bind socket
	 * @param socket socket
	 * @param mtu mtu of socket, or {@code null}, if socket implementation
	 *            doesn't use a special mtu.
	 * @throws IOException I/O error
	 * @since 2.1
	 */
	protected void init(InetSocketAddress bindAddress, DatagramSocket socket, Integer mtu) throws IOException {
		this.socket = socket;
		pendingOutboundMessagesCountdown.set(config.getOutboundMessageBufferSize());

		if (bindAddress.getPort() != 0 && config.isAddressReuseEnabled()) {
			// make it easier to stop/start a server consecutively without delays
			LOGGER.info("Enable address reuse for socket!");
			socket.setReuseAddress(true);
			if (!socket.getReuseAddress()) {
				LOGGER.warn("Enable address reuse for socket failed!");
			}
		}

		Integer size = config.getSocketReceiveBufferSize();
		try {
			if (size != null && size != 0) {
				socket.setReceiveBufferSize(size);
			}
			size = config.getSocketSendBufferSize();
			if (size != null && size != 0) {
				socket.setSendBufferSize(size);
			}
		} catch (IllegalArgumentException ex) {
			LOGGER.error("failed to apply {}", size, ex);
		}
		// don't try to access the buffer sizes,
		// when receive may already lock the socket!
		int recvBuffer = socket.getReceiveBufferSize();
		int sendBuffer = socket.getSendBufferSize();

		if (!socket.isBound()) {
			socket.bind(bindAddress);
		}
		InetSocketAddress actualBindAddress = new InetSocketAddress(socket.getLocalAddress(), socket.getLocalPort());
		if (lastBindAddress != null && !actualBindAddress.equals(lastBindAddress)) {
			connectionStore.markAllAsResumptionRequired();
		}

		if (config.getMaxFragmentLengthCode() != null) {
			MaxFragmentLengthExtension.Length lengthCode = MaxFragmentLengthExtension.Length.fromCode(
					config.getMaxFragmentLengthCode());
			// reduce inbound buffer size accordingly
			inboundDatagramBufferSize = lengthCode.length()
					+ MAX_CIPHERTEXT_EXPANSION
					+ Record.DTLS_HANDSHAKE_HEADER_LENGTH; // 12 bytes DTLS message headers, 13 bytes DTLS record headers
		}

		if (config.getMaxTransmissionUnit() != null) {
			this.maximumTransmissionUnit = config.getMaxTransmissionUnit();
			LOGGER.info("Configured MTU [{}]", this.maximumTransmissionUnit);
		} else if (mtu != null) {
			this.maximumTransmissionUnit = mtu;
			LOGGER.info("Forced MTU [{}]", this.maximumTransmissionUnit);
		} else {
			InetAddress localInterfaceAddress = bindAddress.getAddress();
			if (localInterfaceAddress.isAnyLocalAddress()) {
				ipv4Mtu = NetworkInterfacesUtil.getIPv4Mtu();
				ipv6Mtu = NetworkInterfacesUtil.getIPv6Mtu();
				LOGGER.info("multiple network interfaces, using smallest MTU [IPv4 {}, IPv6 {}]", ipv4Mtu, ipv6Mtu);
			} else {
				NetworkInterface ni = NetworkInterface.getByInetAddress(localInterfaceAddress);
				boolean ipv6 = localInterfaceAddress instanceof Inet6Address;
				if (ni != null && ni.getMTU() > 0) {
					if (ipv6) {
						ipv6Mtu = ni.getMTU();
					} else {
						ipv4Mtu = ni.getMTU();
					}
				} else if (ipv6) {
					ipv6Mtu = NetworkInterfacesUtil.getIPv6Mtu();
					LOGGER.info("Cannot determine MTU of network interface, using minimum MTU [{}] of IPv6 instead", ipv6Mtu);
				} else {
					ipv4Mtu = NetworkInterfacesUtil.getIPv4Mtu();
					LOGGER.info("Cannot determine MTU of network interface, using minimum MTU [{}] of IPv4 instead", ipv4Mtu);
				}
			}
			if (inboundDatagramBufferSize > config.getMaxTransmissionUnitLimit()) {
				if (ipv4Mtu > config.getMaxTransmissionUnitLimit()) {
					ipv4Mtu = config.getMaxTransmissionUnitLimit();
					LOGGER.info("Limit MTU IPv4[{}]", ipv4Mtu);
				}
				if (ipv6Mtu > config.getMaxTransmissionUnitLimit()) {
					ipv6Mtu = config.getMaxTransmissionUnitLimit();
					LOGGER.info("Limit MTU IPv6[{}]", ipv6Mtu);
				}
			} else {
				if (ipv4Mtu > inboundDatagramBufferSize) {
					ipv4Mtu = inboundDatagramBufferSize;
					LOGGER.info("Buffersize MTU IPv4[{}]", ipv4Mtu);
				}
				if (ipv6Mtu > inboundDatagramBufferSize) {
					ipv6Mtu = inboundDatagramBufferSize;
					LOGGER.info("Buffersize MTU IPv6[{}]", ipv6Mtu);
				}
			}
		}

		lastBindAddress = actualBindAddress;

		if (executorService instanceof ScheduledExecutorService) {
			timer = (ScheduledExecutorService) executorService;
		} else {
			timer = ExecutorsUtil.newSingleThreadScheduledExecutor(new DaemonThreadFactory(
					"DTLS-Timer-" + lastBindAddress + "#", NamedThreadFactory.SCANDIUM_THREAD_GROUP)); //$NON-NLS-1$
		}

		if (executorService == null) {
			int threadCount = config.getConnectionThreadCount();
			if (threadCount > 1) {
				executorService = ExecutorsUtil.newFixedThreadPool(threadCount - 1, new DaemonThreadFactory(
						"DTLS-Worker-" + lastBindAddress + "#", NamedThreadFactory.SCANDIUM_THREAD_GROUP)); //$NON-NLS-1$
			} else {
				executorService = timer;
			}
			this.hasInternalExecutor = true;
		}
		// prepare restored connections.
		long expires = calculateRecentHandshakeExpires();
		int recentCounter = 0;
		List<Connection> recent = new ArrayList<>();
		Iterator<Connection> iterator = connectionStore.iterator();
		while (iterator.hasNext()) {
			Connection connection = iterator.next();
			if (connection.hasEstablishedDtlsContext()) {
				if (!connection.isExecuting()) {
					connection.setConnectorContext(executorService, connectionListener);
				}
				Long start = connection.getStartNanos();
				if (start != null) {
					++recentCounter;
					if ((expires - start) < 0) {
						recent.add(connection);
					} else {
						connection.startByClientHello(null);
					}
				}
			}
		}
		if (recentCounter > 0) {
			LOGGER.warn("Restore {} recent handshakes!", recent.size());
			if (!recent.isEmpty()) {
				Collections.sort(recent, new Comparator<Connection>() {

					@Override
					public int compare(Connection o1, Connection o2) {
						Long time1 = o1.getStartNanos();
						Long time2 = o2.getStartNanos();
						if (time1 != null && time2 != null) {
							long diff = time1 - time2;
							if (diff > 0) {
								return 1;
							} else if (diff < 0) {
								return -1;
							}
						} else if (time1 == null) {
							return -1;
						} else if (time2 == null) {
							return 1;
						}
						return 0;
					}
				});
				recentHandshakes.addAll(recent);
				cleanupRecentHandshakes();
			}
		}
		running.set(true);

		int receiverThreadCount = config.getReceiverThreadCount();
		for (int i = 0; i < receiverThreadCount; i++) {
			Worker receiver = new Worker("DTLS-Receiver-" + i + "-" + lastBindAddress) {

				private final byte[] receiverBuffer = new byte[inboundDatagramBufferSize];
				private final DatagramPacket packet = new DatagramPacket(receiverBuffer, inboundDatagramBufferSize);

				@Override
				public void doWork() throws Exception {
					if (MDC_SUPPORT) {
						MDC.clear();
					}
					packet.setData(receiverBuffer);
					receiveNextDatagramFromNetwork(packet);
				}
			};
			receiver.setDaemon(true);
			receiver.start();
			receiverThreads.add(receiver);
		}

		String mtuDescription = maximumTransmissionUnit != null ? maximumTransmissionUnit.toString() : "IPv4 " + ipv4Mtu + " / IPv6 " + ipv6Mtu;
		LOGGER.info("DTLSConnector listening on {}, recv buf = {}, send buf = {}, recv packet size = {}, MTU = {}",
				lastBindAddress, recvBuffer, sendBuffer, inboundDatagramBufferSize, mtuDescription);

		// this is a useful health metric
		// that could later be exported to some kind of monitoring interface
		if (health != null && health.isEnabled()) {
			final Integer healthStatusInterval = config.getHealthStatusInterval();
			if (healthStatusInterval != null) {
				statusLogger = timer.scheduleAtFixedRate(new Runnable() {

					@Override
					public void run() {
						health.dump(config.getLoggingTag(), config.getMaxConnections(), connectionStore.remainingCapacity(), pendingHandshakesWithoutVerifiedPeer.get());
					}

				}, healthStatusInterval, healthStatusInterval, TimeUnit.SECONDS);
			}
		}

		recentHandshakeCleaner = timer.scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() {
				try {
					cleanupRecentHandshakes();
				} catch (Throwable t) {
					LOGGER.warn("Cleanup recent handshakes failed!", t);
				}
			}

		}, 5000, 5000, TimeUnit.MILLISECONDS);
	}

	/**
	 * Force connector to an abbreviated handshake. See <a href="https://tools.ietf.org/html/rfc5246#section-7.3" target="_blank">RFC 5246</a>.
	 * 
	 * The abbreviated handshake will be done next time data will be sent with {@link #send(RawData)}.
	 * @param peer the peer for which we will force to do an abbreviated handshake
	 */
	public final synchronized void forceResumeSessionFor(InetSocketAddress peer) {
		Connection peerConnection = connectionStore.get(peer);
		if (peerConnection != null && peerConnection.hasEstablishedDtlsContext()) {
			peerConnection.setResumptionRequired(true);
		}
	}

	/**
	 * Marks all established sessions currently maintained by this connector to be resumed by means
	 * of an <a href="https://tools.ietf.org/html/rfc5246#section-7.3" target="_blank">abbreviated handshake</a> the
	 * next time a message is being sent to the corresponding peer using {@link #send(RawData)}.
	 * <p>
	 * This method's execution time is proportional to the number of connections this connector maintains.
	 */
	public final synchronized void forceResumeAllSessions() {
		connectionStore.markAllAsResumptionRequired();
	}

	/**
	 * Clears all connection state this connector maintains for peers.
	 * <p>
	 * After invoking this method a new connection needs to be established with a peer using a 
	 * full handshake in order to exchange messages with it again.
	 */
	public final synchronized void clearConnectionState() {
		connectionStore.clear();
	}

	private final DatagramSocket getSocket() {
		return socket;
	}

	@Override
	public void stop() {
		ExecutorService shutdownTimer = null;
		ExecutorService shutdown = null;
		List<Runnable> pending = new ArrayList<>();
		boolean stop;
		synchronized (this) {
			stop = running.compareAndSet(true, false);
			if (stop) {
				LOGGER.debug("DTLS connector on [{}] stopping ...", lastBindAddress);
				if (statusLogger != null) {
					statusLogger.cancel(false);
					statusLogger = null;
				}
				if (recentHandshakeCleaner != null) {
					recentHandshakeCleaner.cancel(false);
					recentHandshakeCleaner = null;
				}
				// recent handshakes will be restored from connection store,
				recentHandshakes.clear();
				for (Thread t : receiverThreads) {
					t.interrupt();
				}
				if (socket != null) {
					socket.close();
					socket = null;
				}
				maximumTransmissionUnit = null;
				ipv4Mtu = DEFAULT_IPV4_MTU;
				ipv6Mtu = DEFAULT_IPV6_MTU;
				connectionStore.stop(pending);
				if (executorService != timer) {
					pending.addAll(timer.shutdownNow());
					shutdownTimer = timer;
					timer = null;
				}
				if (hasInternalExecutor) {
					pending.addAll(executorService.shutdownNow());
					shutdown = executorService;
					executorService = null;
					hasInternalExecutor = false;
				}
				for (Thread t : receiverThreads) {
					t.interrupt();
					try {
						t.join(500);
					} catch (InterruptedException e) {
					}
				}
				receiverThreads.clear();
			}
		}
		if (shutdownTimer != null) {
			try {
				if (!shutdownTimer.awaitTermination(500, TimeUnit.MILLISECONDS)) {
					LOGGER.warn("Shutdown DTLS connector on [{}] timer not terminated in time!", lastBindAddress);
				}
			} catch (InterruptedException e) {
			}
		}
		if (shutdown != null) {
			try {
				if (!shutdown.awaitTermination(500, TimeUnit.MILLISECONDS)) {
					LOGGER.warn("Shutdown DTLS connector on [{}] executor not terminated in time!", lastBindAddress);
				}
			} catch (InterruptedException e) {
			}
		}
		for (Runnable job : pending) {
			try {
				job.run();
			} catch (Exception e) {
				LOGGER.warn("Shutdown DTLS connector:", e);
			}
		}
		if (stop) {
			LOGGER.debug("DTLS connector on [{}] stopped.", lastBindAddress);
		}
	}

	/**
	 * Destroys the connector.
	 * <p>
	 * This method invokes {@link #stop()} and clears the <code>ConnectionStore</code>
	 * used to manage connections to peers. Thus, contrary to the behavior specified
	 * for {@link Connector#destroy()}, this connector can be re-started using the
	 * {@link #start()} method but subsequent invocations of the {@link #send(RawData)}
	 * method will trigger the establishment of a new connection to the corresponding peer.
	 * </p>
	 */
	@Override
	public synchronized void destroy() {
		stop();
		connectionStore.clear();
		messageHandler = null;
	}

	@WipAPI
	@Override
	public int saveConnections(OutputStream out, long maxQuietPeriodInSeconds) throws IOException {
		if (isRunning()) {
			throw new IllegalStateException("Connector is running, save not possible!");
		}
		return connectionStore.saveConnections(out, maxQuietPeriodInSeconds);
	}

	@WipAPI
	@Override
	public int loadConnections(InputStream in, long delta) throws IOException {
		return connectionStore.loadConnections(in, delta);
	}

	@WipAPI
	public boolean restoreConnection(Connection connection) {
		return connectionStore.restore(connection);
	}

	/**
	 * Start to terminate connections related to the provided principals.
	 * 
	 * Note: if {@link SessionStore} is used, it's not possible to remove a
	 * cache entry, if no related connection is in the connection store.
	 * 
	 * @param principal principal, which connections are to terminate
	 * @return future to cancel or wait for completion
	 */
	public Future<Void> startDropConnectionsForPrincipal(final Principal principal) {
		if (principal == null) {
			throw new NullPointerException("principal must not be null!");
		}
		LeastRecentlyUsedCache.Predicate<Principal> handler = new LeastRecentlyUsedCache.Predicate<Principal>() {

			@Override
			public boolean accept(Principal connectionPrincipal) {
				return principal.equals(connectionPrincipal);
			}
		};
		return startTerminateConnectionsForPrincipal(handler);
	}

	/**
	 * Start to terminate connections applying the provided handler to the
	 * principals of all connections.
	 * 
	 * Note: if {@link SessionStore} is used, it's not possible to remove a
	 * cache entry, if no related connection is in the connection store. All
	 * available connections will be removed from that session cache as well.
	 * 
	 * @param principalHandler handler to be called within the serial execution
	 *            of the related connection. If {@code true} is returned, the
	 *            related connection is terminated and the session is removed
	 *            from the session cache.
	 * @return future to cancel or wait for completion
	 * @see #startTerminateConnectionsForPrincipal(org.eclipse.californium.elements.util.LeastRecentlyUsedCache.Predicate,
	 *      boolean)
	 */
	public Future<Void> startTerminateConnectionsForPrincipal(
			LeastRecentlyUsedCache.Predicate<Principal> principalHandler) {
		return startTerminateConnectionsForPrincipal(principalHandler, true);
	}

	/**
	 * Start to terminate connections applying the provided handler to the
	 * principals of all connections.
	 * 
	 * Note: if {@link SessionStore} is used, it's not possible to remove a
	 * cache entry, if no related connection is in the connection store.
	 * 
	 * @param principalHandler handler to be called within the serial execution
	 *            of the related connection. If {@code true} is returned, the
	 *            related connection is terminated
	 * @param removeFromSessionCache {@code true} if the session of the
	 *            connection should be removed from the session cache,
	 *            {@code false}, otherwise
	 * @return future to cancel or wait for completion
	 * @see #startTerminateConnectionsForPrincipal(org.eclipse.californium.elements.util.LeastRecentlyUsedCache.Predicate)
	 * @since 2.6
	 */
	public Future<Void> startTerminateConnectionsForPrincipal(
			final LeastRecentlyUsedCache.Predicate<Principal> principalHandler, final boolean removeFromSessionCache) {
		if (principalHandler == null) {
			throw new NullPointerException("principal handler must not be null!");
		}
		LeastRecentlyUsedCache.Predicate<Connection> connectionHandler = new LeastRecentlyUsedCache.Predicate<Connection>() {

			@Override
			public boolean accept(Connection connection) {
				Principal peer = null;
				DTLSSession session = connection.getSession();
				if (session != null) {
					peer = session.getPeerIdentity();
					if (peer != null && principalHandler.accept(peer)) {
						connectionStore.remove(connection, removeFromSessionCache);
					}
				}
				return false;
			}
		};
		return startForEach(connectionHandler);
	}

	/**
	 * Start applying provided handler to all connections.
	 * 
	 * @param handler handler to be called within the serial execution of the
	 *            passed in connection. If {@code true} is returned, iterating
	 *            is stopped.
	 * @return future to cancel or wait for completion
	 */
	public Future<Void> startForEach(LeastRecentlyUsedCache.Predicate<Connection> handler) {
		if (handler == null) {
			throw new NullPointerException("handler must not be null!");
		}
		ForEachFuture result = new ForEachFuture();
		nextForEach(connectionStore.iterator(), handler, result);
		return result;
	}

	/**
	 * Calls provided handler for each connection returned be the provided
	 * iterator.
	 * 
	 * @param iterator iterator over connections
	 * @param handler handler to be called for all connections returned by the
	 *            iterator. Iteration is stopped, when handler returns
	 *            {@code true}
	 * @param result future to get cancelled or signal completion
	 */
	private void nextForEach(final Iterator<Connection> iterator,
			final LeastRecentlyUsedCache.Predicate<Connection> handler, final ForEachFuture result) {

		if (!result.isStopped() && iterator.hasNext()) {
			final Connection next = iterator.next();
			try {
				next.getExecutor().execute(new Runnable() {

					@Override
					public void run() {
						boolean done = true;
						try {
							if (!result.isStopped() && !handler.accept(next)) {
								done = false;
								nextForEach(iterator, handler, result);
							}
						} catch (Exception exception) {
							result.failed(exception);
						} finally {
							if (done) {
								result.done();
							}
						}
					}
				});
				return;
			} catch (RejectedExecutionException ex) {
				if (!handler.accept(next)) {
					while (iterator.hasNext()) {
						if (handler.accept(iterator.next())) {
							break;
						}
						if (result.isStopped()) {
							break;
						}
					}
				}
			}
		}
		result.done();
	}

	/**
	 * Get connection to communication with peer.
	 * 
	 * @param peerAddress socket address of peer
	 * @param cid connection id. {@code null}, if cid extension is not used
	 * @param create {@code true}, create new connection, if connection is not
	 *            available.
	 * @return connection to communication with peer. {@code null}, if store is
	 *         exhausted or if the connection is not available and the provided
	 *         parameter create is {@code false}.
	 */
	private final Connection getConnection(InetSocketAddress peerAddress, ConnectionId cid, boolean create) {
		ExecutorService executor = getExecutorService();
		synchronized (connectionStore) {
			Connection connection;
			if (cid != null) {
				connection = connectionStore.get(cid);
			} else {
				connection = connectionStore.get(peerAddress);
				if (connection == null && create) {
					LOGGER.trace("create new connection for {}", peerAddress);
					Connection newConnection = new Connection(peerAddress);
					newConnection.setConnectorContext(executor, connectionListener);
					if (running.get()) {
						// only add, if connector is running!
						if (!connectionStore.put(newConnection)) {
							return null;
						}
					}
					return newConnection;
				}
			}
			if (connection == null) {
				LOGGER.trace("no connection available for {},{}", peerAddress, cid);
			} else if (!connection.isExecuting() && running.get()) {
				LOGGER.trace("revive connection for {},{}", peerAddress, cid);
				connection.setConnectorContext(executor, connectionListener);
			} else {
				LOGGER.trace("connection available for {},{}", peerAddress, cid);
			}
			return connection;
		}
	}

	/**
	 * Receive the next datagram from network.
	 * 
	 * Potentially called by multiple threads.
	 * 
	 * @param packet datagram the be read from network
	 * @throws IOException if an io- error occurred
	 * @see #processDatagram(DatagramPacket, InetSocketAddress)
	 */
	protected void receiveNextDatagramFromNetwork(DatagramPacket packet) throws IOException {

		DatagramSocket currentSocket = getSocket();
		if (currentSocket == null) {
			// very unlikely race condition.
			return;
		}

		currentSocket.receive(packet);

		if (packet.getLength() == 0) {
			// nothing to do
			return;
		}

		processDatagram(packet, null);
	}

	@Override
	public void processDatagram(DatagramPacket datagram) {
		processDatagram(datagram, null);
	}

	/**
	 * Process received datagram.
	 * 
	 * Potentially called by multiple threads.
	 * 
	 * @param packet received message
	 * @param router router address, {@code null}, if no router is used.
	 * @since 2.5
	 */
	protected void processDatagram(DatagramPacket packet, InetSocketAddress router) {
		InetSocketAddress peerAddress = (InetSocketAddress) packet.getSocketAddress();
		if (MDC_SUPPORT) {
			MDC.put("PEER", StringUtil.toString(peerAddress));
		}
		if (health != null) {
			health.receivingRecord(false);
		}
		long timestamp = ClockUtil.nanoRealtime();

		DatagramReader reader = new DatagramReader(packet.getData(), packet.getOffset(), packet.getLength());
		List<Record> records = Record.fromReader(reader, connectionIdGenerator, timestamp);
		LOGGER.trace("Received {} DTLS records from {} using a {} byte datagram buffer", records.size(),
				StringUtil.toLog(peerAddress), inboundDatagramBufferSize);

		if (records.isEmpty()) {
			DROP_LOGGER.trace("Discarding malicious record with {} bytes from [{}]", packet.getLength(),
					StringUtil.toLog(peerAddress));
			if (health != null) {
				health.receivingRecord(true);
			}
			return;
		}

		if (!running.get()) {
			DROP_LOGGER.trace("Discarding {} records, startting with {} from [{}] on shutdown", records.size(),
					records.get(0).getType(), StringUtil.toLog(peerAddress));
			LOGGER.debug("Execution shutdown while processing incoming records from peer: {}",
					StringUtil.toLog(peerAddress));
			if (health != null) {
				health.receivingRecord(true);
			}
			return;
		}

		final Record firstRecord = records.get(0);

		if (records.size() == 1 && firstRecord.isNewClientHello()) {
			firstRecord.setAddress(peerAddress, router);
			executorService.execute(new Runnable() {

				@Override
				public void run() {
					if (MDC_SUPPORT) {
						MDC.put("PEER", StringUtil.toString(firstRecord.getPeerAddress()));
					}
					processNewClientHello(firstRecord);
					if (MDC_SUPPORT) {
						MDC.clear();
					}
				}
			});
			return;
		}

		final ConnectionId connectionId = firstRecord.getConnectionId();
		final Connection connection = getConnection(peerAddress, connectionId, false);

		if (connection == null) {
			if (health != null) {
				health.receivingRecord(true);
			}
			if (connectionId == null) {
				DROP_LOGGER.trace("Discarding {} records from [{}] received without existing connection",
						records.size(), StringUtil.toLog(peerAddress));
			} else {
				DROP_LOGGER.trace("Discarding {} records from [{},{}] received without existing connection",
						records.size(), StringUtil.toLog(peerAddress), connectionId);
			}
			return;
		}

		SerialExecutor serialExecutor = connection.getExecutor();

		for (final Record record : records) {
			try {
				record.setAddress(peerAddress, router);
				serialExecutor.execute(new Runnable() {

					@Override
					public void run() {
						if (running.get()) {
							processRecord(record, connection);
						}
					}
				});
			} catch (RejectedExecutionException e) {
				// dont't terminate connection on shutdown!
				LOGGER.debug("Execution rejected while processing record [type: {}, peer: {}]",
						record.getType(), StringUtil.toLog(peerAddress), e);
				break;
			} catch (RuntimeException e) {
				LOGGER.warn("Unexpected error occurred while processing record [type: {}, peer: {}]",
						record.getType(), StringUtil.toLog(peerAddress), e);
				terminateConnectionWithInternalError(connection);
				break;
			}
		}
	}

	/**
	 * Process received record.
	 * 
	 * @param record received record.
	 * @param connection connection to process record.
	 */
	@Override
	public void processRecord(Record record, Connection connection) {

		try {
			// ensure, that connection is still related to record 
			// and not changed by processing an other record before 
			if (record.getConnectionId() == null && !connection.equalsPeerAddress(record.getPeerAddress())) {
				long delay = TimeUnit.NANOSECONDS.toMillis(ClockUtil.nanoRealtime() - record.getReceiveNanos());
				DROP_LOGGER.debug("Drop received record {}, connection changed address {} => {}! (shift {}ms)",
						record.getType(), StringUtil.toLog(record.getPeerAddress()),
						StringUtil.toLog(connection.getPeerAddress()), delay);
				if (health != null) {
					health.receivingRecord(true);
				}
				return;
			}

			int epoch = record.getEpoch();
			LOGGER.trace("Received DTLS record of type [{}], length: {}, [epoche:{},rseqn:{}]", 
					record.getType(), record.getFragmentLength(), epoch, record.getSequenceNumber());

			Handshaker handshaker = connection.getOngoingHandshake();
			if (handshaker != null && handshaker.isExpired()) {
				// handshake expired during Android / OS "deep sleep"
				// on receiving, fail to remove connection, if session is not established 
				handshaker.handshakeFailed(new Exception("handshake already expired!"));
				if (connectionStore.get(connection.getConnectionId()) != connection) {
					// connection removed, then drop record
					DROP_LOGGER.debug(
							"Discarding {} record [epoch {}, rseqn {}] received from peer [{}], handshake expired!",
							record.getType(), epoch, record.getSequenceNumber(),
							StringUtil.toLog(record.getPeerAddress()));
					if (health != null) {
						health.receivingRecord(true);
					}
					return;
				}
				handshaker = null;
			}

			final DTLSContext context = connection.getDtlsContext(epoch);

			if (context == null) {
				if (handshaker != null && handshaker.getDtlsContext().getReadEpoch() == 0 && epoch == 1) {
					// future records, apply session after handshake finished.
					handshaker.addRecordsOfNextEpochForDeferredProcessing(record);
				} else {
					DROP_LOGGER.debug("Discarding {} record [epoch {}, rseqn {}] received from peer [{}] without an active dtls context",
							record.getType(), epoch, record.getSequenceNumber(), StringUtil.toLog(record.getPeerAddress()));
					if (health != null) {
						health.receivingRecord(true);
					}
				}
				return;
			}

			// The DTLS 1.2 spec (section 4.1.2.6) advises to do replay detection
			// before MAC validation based on the record's sequence numbers
			// see http://tools.ietf.org/html/rfc6347#section-4.1.2.6
			boolean closed = connection.isClosed();
			boolean discard = (useFilter || closed)
					&& !context.isRecordProcessable(epoch, record.getSequenceNumber(), useExtendedWindowFilter);
			if (discard) {
				if (closed) {
					DROP_LOGGER.debug("Discarding {} record [epoch {}, rseqn {}] received from closed peer [{}]", record.getType(),
							epoch, record.getSequenceNumber(), StringUtil.toLog(record.getPeerAddress()));
				} else {
					DROP_LOGGER.debug("Discarding duplicate {} record [epoch {}, rseqn {}] received from peer [{}]",
							record.getType(), epoch, record.getSequenceNumber(), StringUtil.toLog(record.getPeerAddress()));
				}
				if (health != null) {
					health.receivingRecord(true);
				}
				return;
			}

			if (record.getType() == ContentType.TLS12_CID) {
				// !useCid already dropped in Record.fromByteArray
				if (epoch == 0) {
					DROP_LOGGER.debug("Discarding TLS_CID record received from peer [{}] during handshake",
							StringUtil.toLog(record.getPeerAddress()));
					if (health != null) {
						health.receivingRecord(true);
					}
					return;
				}
			} else if (epoch > 0 && connection.expectCid()) {
				DROP_LOGGER.debug("Discarding record received from peer [{}], CID required!",
						StringUtil.toLog(record.getPeerAddress()));
				if (health != null) {
					health.receivingRecord(true);
				}
				return;
			}

			if (!record.isDecoded() || record.getType() != ContentType.APPLICATION_DATA) {
				// application data may be deferred again until the session is really established
				record.decodeFragment(context.getReadState());
			}

			if (handshaker != null && handshaker.isProbing()) {
				// received record, probe successful
				connectionStore.removeFromEstablishedSessions(connection);
				connection.resetContext();
				handshaker.resetProbing();
				LOGGER.trace("handshake probe successful {}", StringUtil.toLog(connection.getPeerAddress()));
			}

			switch (record.getType()) {
			case APPLICATION_DATA:
				processApplicationDataRecord(record, connection);
				break;
			case ALERT:
				processAlertRecord(record, connection, context);
				break;
			case CHANGE_CIPHER_SPEC:
				processChangeCipherSpecRecord(record, connection);
				break;
			case HANDSHAKE:
				processHandshakeRecord(record, connection, context);
				break;
			default:
				DROP_LOGGER.debug("Discarding record of unsupported type [{}] from peer [{}]",
					record.getType(), StringUtil.toLog(record.getPeerAddress()));
			}
		} catch (RuntimeException e) {
			if (health != null) {
				health.receivingRecord(true);
			}
			LOGGER.warn("Unexpected error occurred while processing record from peer [{}]",
					StringUtil.toLog(record.getPeerAddress()), e);
			terminateConnectionWithInternalError(connection);
		} catch (GeneralSecurityException e) {
			DTLSContext dtlsContext = connection.getEstablishedDtlsContext();
			if (dtlsContext != null) {
				dtlsContext.incrementMacErrors();
				if (connectionListener != null) {
					if (connectionListener.onConnectionMacError(connection)) {
						closeConnection(connection);
					}
				}
			}
			DROP_LOGGER.debug("Discarding {} received from peer [{}] caused by {}",
					record.getType(), StringUtil.toLog(record.getPeerAddress()), e.getMessage());
			if (health != null) {
				health.receivingRecord(true);
			}
			LOGGER.debug("error occurred while processing record from peer [{}]",
					StringUtil.toLog(record.getPeerAddress()), e);
		} catch (HandshakeException e) {
			LOGGER.debug("error occurred while processing record from peer [{}]",
					StringUtil.toLog(record.getPeerAddress()), e);
		}
	}

	private void closeConnection(Connection connection) {
		DTLSContext context = connection.getEstablishedDtlsContext();
		if (context != null) {
			LOGGER.trace("Closing connection with peer [{}]", connection.getPeerAddress());
			sendAlert(connection, context, new AlertMessage(AlertLevel.WARNING, AlertDescription.CLOSE_NOTIFY));
			connection.setResumptionRequired(true);
		}
	}

	/**
	 * Terminate connection with internal error.
	 * 
	 * Send alert, if connection has dtls context. Remove connection from store.
	 * 
	 * @param connection connection to terminate
	 * @since 3.0
	 */
	private void terminateConnectionWithInternalError(Connection connection) {
		DTLSContext context = connection.getDtlsContext();
		if (context != null) {
			LOGGER.trace("Terminating connection with peer [{}], Internal Error", StringUtil.toLog(connection.getPeerAddress()));
			sendAlert(connection, context, new AlertMessage(AlertLevel.FATAL, AlertDescription.INTERNAL_ERROR));
			// clear session & (pending) handshaker
		}
		connectionStore.remove(connection, true);
	}

	/**
	 * Process application data record.
	 * 
	 * @param record application data record
	 * @param connection connection to process the received record
	 */
	private void processApplicationDataRecord(final Record record, final Connection connection) {
		final Handshaker ongoingHandshake = connection.getOngoingHandshake();
		final DTLSContext dtlsContext = connection.getEstablishedDtlsContext();
		if (dtlsContext != null && !connection.isResumptionRequired()) {
			// APPLICATION_DATA can only be processed within the context of
			// an established, i.e. fully negotiated, session
			ApplicationMessage message = (ApplicationMessage) record.getFragment();

			updateConnectionAddress(record, connection);

			final RawDataChannel channel = messageHandler;
			// finally, forward de-crypted message to application layer
			if (channel != null) {
				// context
				DtlsEndpointContext endpointContext = connection.getReadContext(record.getPeerAddress());
				LOGGER.trace("Received APPLICATION_DATA for {}", endpointContext);
				// create application message.
				RawData receivedApplicationMessage = RawData.inbound(message.getData(), endpointContext, false,
						record.getReceiveNanos(), lastBindAddress);
				channel.receiveData(receivedApplicationMessage);
			}
		} else if (ongoingHandshake != null) {
			// wait for FINISH
			// the record is already decoded, so adding it for deferred processing
			// requires to protect it from applying the session again in processRecord!
			ongoingHandshake.addRecordsOfNextEpochForDeferredProcessing(record);
		} else {
			DROP_LOGGER.debug("Discarding APPLICATION_DATA record received from peer [{}]",
					StringUtil.toLog(record.getPeerAddress()));
		}
	}

	/**
	 * Process alert record.
	 * 
	 * @param record alert record
	 * @param connection connection to process the received record
	 * @param epochContext session applied to decode record
	 */
	private void processAlertRecord(Record record, Connection connection, DTLSContext epochContext) {
		AlertMessage alert = (AlertMessage) record.getFragment();
		Handshaker handshaker = connection.getOngoingHandshake();
		HandshakeException error = null;
		LOGGER.trace("Processing {} ALERT from [{}]: {}", alert.getLevel(),
				StringUtil.toLog(connection.getPeerAddress()), alert.getDescription());
		if (AlertDescription.CLOSE_NOTIFY.equals(alert.getDescription())) {
			// according to section 7.2.1 of the TLS 1.2 spec
			// (http://tools.ietf.org/html/rfc5246#section-7.2.1)
			// we need to respond with a CLOSE_NOTIFY alert and
			// then close and remove the connection immediately
			if (connection.hasEstablishedDtlsContext()) {
				updateConnectionAddress(record, connection);
			} else {
				error = new HandshakeException("Received 'close notify'", alert);
				if (handshaker != null) {
					handshaker.setFailureCause(error);
				}
			}
			if (!connection.isResumptionRequired()) {
				if (connection.getPeerAddress() != null) {
					sendAlert(connection, epochContext, new AlertMessage(AlertLevel.WARNING, AlertDescription.CLOSE_NOTIFY));
				}
				if (connection.hasEstablishedDtlsContext()) {
					connection.close(record);
				} else {
					connectionStore.remove(connection, false);
				}
			}
		} else if (AlertLevel.FATAL.equals(alert.getLevel())) {
			// according to section 7.2 of the TLS 1.2 spec
			// (http://tools.ietf.org/html/rfc5246#section-7.2)
			// the connection needs to be terminated immediately
			error = new HandshakeException("Received 'fatal alert/" + alert.getDescription() + "'", alert);
			if (handshaker != null) {
				handshaker.setFailureCause(error);
			}
			connectionStore.remove(connection, true);
		} else {
			// non-fatal alerts do not require any special handling
		}
		reportAlertInternal(connection, alert);
		if (null != error && null != handshaker) {
			handshaker.handshakeFailed(error);
		}
	}

	/**
	 * Update connection address.
	 * 
	 * @param record received record.
	 * @param connection connection of received record
	 * @since 3.0 (removed DTLS context parameter)
	 */
	private void updateConnectionAddress(Record record, Connection connection) {
		InetSocketAddress newAddress = null;
		if (connection.markRecordAsRead(record) || !useCidUpdateAddressOnNewerRecordFilter) {
			// address update, it's a newer record!
			connection.setRouter(record.getRouter());
			newAddress = record.getPeerAddress();
		}
		connectionStore.update(connection, newAddress);
		final Handshaker ongoingHandshake = connection.getOngoingHandshake();
		if (ongoingHandshake != null) {
			// the handshake has been completed successfully
			ongoingHandshake.handshakeCompleted();
		}
		if (connectionListener != null) {
			if (connectionListener.onConnectionUpdatesSequenceNumbers(connection, false)) {
				closeConnection(connection);
			}
		}
	}

	/**
	 * Process change cipher spec record.
	 * 
	 * @param record change cipher spec record
	 * @param connection connection to process the received record
	 */
	private void processChangeCipherSpecRecord(Record record, Connection connection) {
		Handshaker ongoingHandshaker = connection.getOngoingHandshake();
		if (ongoingHandshaker != null) {
			// processing a CCS message does not result in any additional flight to be sent
			try {
				ongoingHandshaker.processMessage(record);
			} catch (HandshakeException e) {
				processExceptionDuringHandshake(record, connection, e);
			}
		} else {
			// change cipher spec can only be processed within the
			// context of an existing handshake -> ignore record
			DROP_LOGGER.debug("Received CHANGE_CIPHER_SPEC record from peer [{}] with no handshake going on",
					StringUtil.toLog(record.getPeerAddress()));
		}
	}

	/**
	 * Process handshake record.
	 * 
	 * @param record handshake record
	 * @param connection connection to process the record.
	 * @param dtlsContext dtls context of the record.
	 */
	private void processHandshakeRecord(Record record, Connection connection, DTLSContext dtlsContext) {
		LOGGER.trace("Received {} record from peer [{}]", record.getType(), StringUtil.toLog(record.getPeerAddress()));
		if (record.isNewClientHello()) {
			throw new IllegalArgumentException("new CLIENT_HELLO must be processed by processClientHello!");
		}
		try {
			Handshaker handshaker = connection.getOngoingHandshake();
			HandshakeMessage handshakeMessage = (HandshakeMessage) record.getFragment();
			switch (handshakeMessage.getMessageType()) {
			case CLIENT_HELLO:
				// We do not support re-negotiation as recommended in :
				// https://tools.ietf.org/html/rfc7925#section-17
				DROP_LOGGER.debug("Reject re-negotiation from peer [{}]", StringUtil.toLog(record.getPeerAddress()));
				sendAlert(connection, dtlsContext,
						new AlertMessage(AlertLevel.WARNING, AlertDescription.NO_RENEGOTIATION));
				break;
			case HELLO_REQUEST:
				if (handshaker != null) {
					// TLS 1.2, Section 7.4.1.1 advises to ignore HELLO_REQUEST
					// messages during an ongoing handshake
					// (http://tools.ietf.org/html/rfc5246#section-7.4.1.1)
					DROP_LOGGER.debug("Ignore HELLO_REQUEST received from peer [{}] during ongoing handshake",
							StringUtil.toLog(connection.getPeerAddress()));
				} else {
					// TLS 1.2, Section 7.4.1.1 allows to reject HELLO_REQUEST
					// messages
					// (http://tools.ietf.org/html/rfc5246#section-7.4.1.1.)
					DROP_LOGGER.debug("Reject HELLO_REQUEST received from peer [{}]", StringUtil.toLog(connection.getPeerAddress()));
					// We do not support re-negotiation as recommended in :
					// https://tools.ietf.org/html/rfc7925#section-17
					sendAlert(connection, dtlsContext,
							new AlertMessage(AlertLevel.WARNING, AlertDescription.NO_RENEGOTIATION));
				}
				break;
			default:
				if (handshaker != null) {
					handshaker.processMessage(record);
				} else {
					DROP_LOGGER.debug("Discarding HANDSHAKE message [epoch={}] from peer [{}], no ongoing handshake!",
							record.getEpoch(), StringUtil.toLog(record.getPeerAddress()));
				}
				break;
			}
		} catch (HandshakeException e) {
			processExceptionDuringHandshake(record, connection, e);
		}
	}

	/**
	 * Process new CLIENT_HELLO message.
	 * 
	 * Executed outside the serial execution. Checks for either a valid session
	 * id or a valid cookie. If the check is passed successfully, check next, if
	 * a connection for that CLIENT_HELLO already exists using the client random
	 * contained in the CLIENT_HELLO message. If the connection already exists,
	 * take that, otherwise create a new one and pass the execution to the
	 * serial execution of that connection.
	 * 
	 * @param record record of CLIENT_HELLO message
	 */
	private void processNewClientHello(final Record record) {
		InetSocketAddress peerAddress = record.getPeerAddress();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Processing new CLIENT_HELLO from peer [{}]:{}{}", StringUtil.toLog(peerAddress),
					StringUtil.lineSeparator(), record);
		}
		try {
			// CLIENT_HELLO with epoch 0 is not encrypted, so use DTLSConnectionState.NULL 
			record.decodeFragment(DTLSConnectionState.NULL);
			final ClientHello clientHello = (ClientHello) record.getFragment();

			byte[] expectedCookie = cookieGenerator.generateCookie(peerAddress, clientHello);

			// before starting a new handshake or resuming an established
			// session we need to make sure that the peer is in possession of
			// the IP address indicated in the client hello message
			AddressVerification addressVerification = isClientInControlOfSourceIpAddress(clientHello, record, expectedCookie);
			if (addressVerification != null) {
				Connection connection;
				synchronized (connectionStore) {
					connection = connectionStore.get(peerAddress);
					if (connection != null && !connection.isStartedByClientHello(clientHello)) {
						if (!addressVerification.hasMatchingCookie()) {
							SessionId establishedSessionId = connection.getEstablishedSessionIdentifier();
							boolean sameSession = Bytes.equals(establishedSessionId, clientHello.getSessionId());
							if (!sameSession) {
								// don't overwrite the current connection, first verify address
								// protection for spoofed client_hello, with valid session id
								// and spoofed ip-address of valid other peer.
								addressVerification = null;
							}
						}
						if (addressVerification != null) {
							final Handshaker handshaker = connection.getOngoingHandshake();
							if (handshaker != null) {
								DTLSContext dtlsContext = connection.getEstablishedDtlsContext();
								if (dtlsContext == null || dtlsContext != handshaker.getDtlsContext()) {
									final DtlsException cause = new DtlsException("Received new CLIENT_HELLO from "
											+ StringUtil.toDisplayString(peerAddress));
									handshaker.setFailureCause(cause);
									connection.getExecutor().execute(new Runnable() {

										@Override
										public void run() {
											if (running.get()) {
												handshaker.handshakeFailed(cause);
											}
										}
									});
								}
							}
							connection = null;
						}
					}
					if (connection == null) {
						connection = new Connection(peerAddress);
						connection.setConnectorContext(getExecutorService(), connectionListener);
						connection.startByClientHello(clientHello);
						if (!connectionStore.put(connection)) {
							return;
						}
					}
				}
				if (addressVerification != null) {
					try {
						final Connection clientConnection = connection;
						final DTLSSession session = addressVerification.getMatchingSession();
						connection.getExecutor().execute(new Runnable() {

							@Override
							public void run() {
								if (running.get()) {
									processClientHello(clientHello, record, clientConnection, session);
								}
							}
						});
					} catch (RejectedExecutionException e) {
						// dont't terminate connection on shutdown!
						LOGGER.debug("Execution rejected while processing record [type: {}, peer: {}]",
								record.getType(), StringUtil.toLog(peerAddress), e);
					} catch (RuntimeException e) {
						LOGGER.warn("Unexpected error occurred while processing record [type: {}, peer: {}]",
								record.getType(), StringUtil.toLog(peerAddress), e);
						terminateConnectionWithInternalError(connection);
					}
					return;
				}
			}
			// sender's address not verified => send hello verify request for verification
			sendHelloVerify(clientHello, record, expectedCookie);
		} catch (HandshakeException e) {
			LOGGER.debug("Processing new CLIENT_HELLO from peer [{}] failed!",
					StringUtil.toLog(record.getPeerAddress()), e);
		} catch (GeneralSecurityException e) {
			DROP_LOGGER.debug("Processing new CLIENT_HELLO from peer [{}] failed!",
					StringUtil.toLog(record.getPeerAddress()), e);
		} catch (RuntimeException e) {
			LOGGER.warn("Processing new CLIENT_HELLO from peer [{}] failed!", StringUtil.toLog(record.getPeerAddress()),
					e);
		}
	}

	/**
	 * Process CLIENT_HELLO message.
	 * 
	 * @param clientHello CLIENT_HELLO message
	 * @param record record of CLIENT_HELLO message
	 * @param resumptionSession available connections to process handshake message
	 */
	private void processClientHello(ClientHello clientHello, Record record, Connection connection, DTLSSession resumptionSession) {
		if (connection == null) {
			throw new NullPointerException("connection must not be null!");
		} else if (!connection.equalsPeerAddress(record.getPeerAddress())) {
			DROP_LOGGER.info("Drop received CLIENT_HELLO, changed address {} => {}!",
					StringUtil.toLog(record.getPeerAddress()), StringUtil.toLog(connection.getPeerAddress()));
			return;
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Processing CLIENT_HELLO from peer [{}]:{}{}", StringUtil.toLog(record.getPeerAddress()),
					StringUtil.lineSeparator(), record);
		}

		try {
			if (connection.hasEstablishedDtlsContext() || connection.hasOngoingHandshake()) {
				DROP_LOGGER.debug("Discarding received duplicate CLIENT_HELLO message [epoch={}] from peer [{}]!", record.getEpoch(),
						StringUtil.toLog(record.getPeerAddress()));
			} else if (resumptionSession != null) {
				// client wants to resume a cached session
				resumeExistingSession(clientHello, record, connection, resumptionSession);
			} else {
				// At this point the client has demonstrated reachability by completing a cookie exchange
				// so we terminate the previous connection and start a new handshake
				// (see section 4.2.8 of RFC 6347 (DTLS 1.2))
				startNewHandshake(clientHello, record, connection);
			}
		} catch (HandshakeException e) {
			processExceptionDuringHandshake(record, connection, e);
		}
	}

	/**
	 * Checks whether the peer is able to receive data on the IP address
	 * indicated in its client hello message.
	 * <p>
	 * The check is done by means of comparing the cookie contained in the
	 * client hello message with the cookie computed for the request using the
	 * {@code generateCookie} method.
	 * </p>
	 * <p>
	 * If a matching session id is contained, but no cookie, it depends on the
	 * number of pending resumption handshakes, if a
	 * <em>HELLO_VERIFY_REQUEST</em> is send to the peer, of a resumption
	 * handshake is started without.
	 * </p>
	 * Executed outside the connection's serial execution.
	 * 
	 * @param clientHello the peer's client hello method including the cookie to
	 *            verify
	 * @param record the received record
	 * @param expectedCookie expected cookie
	 * @return address verification level, if at least the cookie or the session
	 *         is is matching, {@code null}, neither the cookie nor the session
	 *         id is matching.
	 * @since 3.0 (adapted parameter list and return type)
	 */
	private AddressVerification isClientInControlOfSourceIpAddress(ClientHello clientHello, Record record,
			byte[] expectedCookie) {
		// verify client's ability to respond on given IP address
		// by exchanging a cookie as described in section 4.2.1 of the DTLS 1.2 spec
		// see http://tools.ietf.org/html/rfc6347#section-4.2.1
		boolean cookie = false;
		byte[] providedCookie = clientHello.getCookie();
		if (providedCookie.length > 0) {
			// check, if cookie of the current period matches
			if (MessageDigest.isEqual(expectedCookie, providedCookie)) {
				cookie = true;
			} else {
				try {
					// check, if cookie of the past period matches
					byte[] pastCookie = cookieGenerator.generatePastCookie(record.getPeerAddress(), clientHello);
					if (pastCookie != null && MessageDigest.isEqual(pastCookie, providedCookie)) {
						cookie = true;
					}
				} catch (GeneralSecurityException ex) {
					LOGGER.debug("failed to generate past cookie.", ex);
				}
			}
			if (!cookie && LOGGER.isDebugEnabled()) {
				LOGGER.debug("provided cookie must {} match {}. Send verify request to {}",
						StringUtil.byteArray2HexString(providedCookie, StringUtil.NO_SEPARATOR, 6),
						StringUtil.byteArray2HexString(expectedCookie, StringUtil.NO_SEPARATOR, 6),
						StringUtil.toLog(record.getPeerAddress()));
			}
		}

		DTLSSession session = null;
		SessionId sessionId = clientHello.getSessionId();
		if (!sessionId.isEmpty()) {
			boolean lookup = cookie;
			if (!lookup && 0 < thresholdHandshakesWithoutVerifiedPeer) {
				// use short resumption (without verify request)
				// only, if the number of the pending short
				// resumption handshakes is below the threshold
				int pending = pendingHandshakesWithoutVerifiedPeer.get();
				LOGGER.trace("pending fast resumptions [{}], threshold [{}]", pending,
						thresholdHandshakesWithoutVerifiedPeer);
				lookup = pending < thresholdHandshakesWithoutVerifiedPeer;
			}
			if (lookup) {
				session = connectionStore.find(sessionId);
			}
		}
		if (cookie || session != null) {
			return new AddressVerification(cookie, session);
		} else {
			return null;
		}
	}

	/**
	 * Start a new handshake.
	 * 
	 * @param clientHello CLIENT_HELLO message.
	 * @param record record containing the CLIENT_HELLO message.
	 * @param connection connection to start handshake.
	 * @throws HandshakeException if the parameters provided in the client hello message
	 *           cannot be used to start a handshake with the peer
	 */
	private void startNewHandshake(final ClientHello clientHello, final Record record, final Connection connection) throws HandshakeException {
		// use the record sequence number from CLIENT_HELLO as initial sequence number
		// for records sent to the client (see section 4.2.1 of RFC 6347 (DTLS 1.2))
		// initialize handshaker based on CLIENT_HELLO (this accounts
		// for the case that multiple cookie exchanges have taken place)
		Handshaker handshaker = new ServerHandshaker(record.getSequenceNumber(), clientHello.getMessageSeq(), this, timer, connection, config);
		initializeHandshaker(handshaker);
		handshaker.processMessage(record);
	}

	/**
	 * Resume existing session.
	 * 
	 * @param clientHello CLIENT_HELLO message.
	 * @param record record containing the CLIENT_HELLO message.
	 * @param connection connection to resume
	 * @param resumptionSession session to resume
	 * @throws HandshakeException if the session cannot be resumed based on the
	 *             parameters provided in the client hello message
	 * @throws NullPointerException if the connection or resumption session is
	 *             {@code null}
	 */
	private void resumeExistingSession(ClientHello clientHello, Record record, Connection connection,
			DTLSSession resumptionSession) throws HandshakeException {
		InetSocketAddress peerAddress = record.getPeerAddress();
		LOGGER.trace("Client [{}] wants to resume session with ID [{}]", StringUtil.toLog(peerAddress),
				clientHello.getSessionId());

		if (resumptionSession == null) {
			throw new NullPointerException("Resumption session must not be null!");
		}
		if (connection == null) {
			throw new NullPointerException("connection must not be null!");
		}

		boolean ok = true;
		if (ok && config.isSniEnabled()) {
			ServerNames serverNames1 = resumptionSession.getServerNames();
			ServerNames serverNames2 = null;
			ServerNameExtension extension = clientHello.getServerNameExtension();
			if (extension != null) {
				serverNames2 = extension.getServerNames();
			}
			if (serverNames1 != null) {
				ok = serverNames1.equals(serverNames2);
			} else if (serverNames2 != null) {
				// invalidate ticket, server names mismatch
				ok = false;
			}
		}
		if (ok && config.getExtendedMasterSecretMode() != ExtendedMasterSecretMode.NONE) {
			// https://tools.ietf.org/html/rfc7627#section-5.3
			if (!resumptionSession.useExtendedMasterSecret() && clientHello.hasExtendedMasterSecret()) {
				// If the original session did not use the
				// "extended_master_secret" extension but the new
				// ClientHello contains the extension, then the
				// server MUST NOT perform the abbreviated handshake.
				// Instead, it SHOULD continue with a full handshake (as
				// described in Section 5.2) to negotiate a new session.
				ok = false;
			}
			// aborting handshakes is done in ResumingServerHandshaker
		}
		if (ok) {
			// session has been found in cache, resume it
			final Handshaker handshaker = new ResumingServerHandshaker(record.getSequenceNumber(),
					clientHello.getMessageSeq(), resumptionSession, this, timer, connection, config);
			initializeHandshaker(handshaker);

			// client wants to resume a session that has been negotiated by this node

			if (clientHello.getCookie().length == 0) {
				// short resumption without verify request
				pendingHandshakesWithoutVerifiedPeer.incrementAndGet();
				handshaker.addSessionListener(new SessionAdapter() {

					@Override
					public void contextEstablished(Handshaker currentHandshaker, DTLSContext establishedContext)
							throws HandshakeException {
						pendingHandshakesWithoutVerifiedPeer.decrementAndGet();
					}

					@Override
					public void handshakeFailed(Handshaker handshaker, Throwable error) {
						pendingHandshakesWithoutVerifiedPeer.decrementAndGet();
					}

				});
			}

			// process message
			handshaker.processMessage(record);
		} else {
			SecretUtil.destroy(resumptionSession);
			LOGGER.trace(
					"Client [{}] tries to resume non-existing session [ID={}], performing full handshake instead ...",
					StringUtil.toLog(peerAddress), clientHello.getSessionId());
			startNewHandshake(clientHello, record, connection);
		}
	}

	private void sendHelloVerify(ClientHello clientHello, Record record, byte[] expectedCookie) throws GeneralSecurityException {
		if (expectedCookie == null) {
			throw new NullPointerException("Cookie must not be null!");
		}
		// send CLIENT_HELLO_VERIFY with cookie in order to prevent
		// DOS attack as described in DTLS 1.2 spec
		LOGGER.trace("Verifying client IP address [{}] using HELLO_VERIFY_REQUEST", StringUtil.toLog(record.getPeerAddress()));
		ProtocolVersion version = protocolVersionForHelloVerifyRequests;
		if (version == null) {
			// no fixed version configured, reply the client's version.
			version = clientHello.getClientVersion();
			if (version.compareTo(ProtocolVersion.VERSION_DTLS_1_0) < 0) {
				version = ProtocolVersion.VERSION_DTLS_1_0;
			} else if (version.compareTo(ProtocolVersion.VERSION_DTLS_1_2) > 0) {
				version = ProtocolVersion.VERSION_DTLS_1_2;
			}
		}
		// according RFC 6347, 4.2.1. Denial-of-Service Countermeasures, the HelloVerifyRequest should use version 1.0
		HelloVerifyRequest msg = new HelloVerifyRequest(version, expectedCookie);
		// because we do not have a handshaker in place yet that
		// manages message_seq numbers, we need to set it explicitly
		// use message_seq from CLIENT_HELLO in order to allow for
		// multiple consecutive cookie exchanges with a client
		msg.setMessageSeq(clientHello.getMessageSeq());
		// use epoch 0 and sequence no from CLIENT_HELLO record as
		// mandated by section 4.2.1 of the DTLS 1.2 spec
		// see http://tools.ietf.org/html/rfc6347#section-4.2.1
		Record helloVerify = new Record(ContentType.HANDSHAKE, version, record.getSequenceNumber(), msg);
		helloVerify.setAddress(record.getPeerAddress(), null);
		try {
			sendRecord(helloVerify);
		} catch (IOException e) {
			// already logged ...
		}
	}

	/**
	 * Handle a exception occurring during the handshake.
	 * 
	 * Suppress {@link AlertDescription#UNKNOWN_PSK_IDENTITY} and none
	 * {@link AlertLevel#FATAL} alerts. Other alerts during an ongoing handshake
	 * are send to the other peer and terminates that handshake with that peer
	 * immediately.
	 * 
	 * That includes:
	 * <ul>
	 * <li>canceling any pending retransmissions to the peer</li>
	 * <li>destroying any state for an ongoing handshake with the peer</li>
	 * </ul>
	 * 
	 * @param record related received record. Since 2.3, this may be
	 *            {@code null} in order to support exception during processing
	 *            of a asynchronous master secret result.
	 * @param connection connection
	 * @param cause the exception that is the cause for terminating the
	 *            handshake
	 * @since 3.0
	 */
	private void processExceptionDuringHandshake(Record record, Connection connection, HandshakeException cause) {
		AlertMessage alert = cause.getAlert();
		// discard none fatal alert exception
		if (!AlertLevel.FATAL.equals(alert.getLevel())) {
			if (record != null) {
				discardRecord(record, cause);
			}
			reportAlertInternal(connection, alert);
			return;
		}

		// "Unknown identity" and "bad PSK" should be both handled in a same way.
		// Generally "bad PSK" means invalid MAC on FINISHED message.
		// In production both should be silently ignored : https://bugs.eclipse.org/bugs/show_bug.cgi?id=533258
		if (AlertDescription.UNKNOWN_PSK_IDENTITY.equals(alert.getDescription())) {
			if (record != null) {
				discardRecord(record, cause);
			}
			reportAlertInternal(connection, alert);
			return;
		}

		// in other cases terminate handshake
		Handshaker handshaker = connection.getOngoingHandshake();
		if (handshaker != null) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Aborting handshake with peer [{}]:", StringUtil.toLog(connection.getPeerAddress()),
						cause);
			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Aborting handshake with peer [{}]: {}", StringUtil.toLog(connection.getPeerAddress()),
						cause.getMessage());
			}
			handshaker.setFailureCause(cause);
			DTLSContext handshakeContext = handshaker.getDtlsContext();
			DTLSContext connectionContext = connection.getEstablishedDtlsContext();
			// keep established session intact and only terminate ongoing handshake
			// failure after established (last FINISH), but before completed (first data)
			if (connectionContext == handshakeContext) {
				if (alert.getDescription() == AlertDescription.CLOSE_NOTIFY) {
					LOGGER.debug("Handshake with [{}] closed after session was established!",
							StringUtil.toLog(handshaker.getPeerAddress()));
				} else {
					LOGGER.warn("Handshake with [{}] failed after session was established! {}",
							StringUtil.toLog(handshaker.getPeerAddress()), alert);
				}
			} else if (connectionContext != null) {
				LOGGER.warn("Handshake with [{}] failed, but has an established session!",
						StringUtil.toLog(handshaker.getPeerAddress()));
			}
			sendAlert(connection, handshakeContext, alert);
			handshaker.handshakeFailed(cause);
			reportAlertInternal(connection, alert);
		}
	}

	/**
	 * Handle alert internally.
	 * 
	 * Keeps first reported alert as root cause and reports that to the
	 * {@link AlertHandler}, if available.
	 * @param connection connection affected by that alert
	 * @param alert received alert or detected alert
	 * 
	 * @since 3.0 (was handleAlertInternal)
	 */
	private void reportAlertInternal(Connection connection, AlertMessage alert) {
		if (connection.setRootCause(alert)) {
			AlertHandler handler = alertHandler;
			if (handler != null) {
				handler.onAlert(connection.getPeerAddress(), alert);
			}
		}
	}

	void sendAlert(Connection connection, DTLSContext context, AlertMessage alert) {
		if (connection == null) {
			throw new NullPointerException("Connection must not be null");
		} else if (context == null) {
			throw new NullPointerException("DTLS Context must not be null");
		} else if (alert == null) {
			throw new NullPointerException("Alert must not be null");
		}
		if (connection.isResumptionRequired()) {
			return;
		}
		try {
			LOGGER.trace("send ALERT {} for peer {}.", alert, StringUtil.toLog(connection.getPeerAddress()));
			Record record;
			boolean useCid = context.getWriteEpoch() > 0;
			if (useCid || alert.getProtocolVersion() == null) {
				record = new Record(ContentType.ALERT, context.getWriteEpoch(), alert, context,
						useCid, TLS12_CID_PADDING);
			} else {
				record = new Record(ContentType.ALERT, alert.getProtocolVersion(), context.getNextSequenceNumber(), alert);
			}
			record.setAddress(connection.getPeerAddress(), connection.getRouter());
			sendRecord(record);
			if (connectionListener != null) {
				connectionListener.onConnectionUpdatesSequenceNumbers(connection, true);
			}
		} catch (IOException e) {
			// already logged ...
		} catch (GeneralSecurityException e) {
			DROP_LOGGER.warn("Cannot create ALERT message for peer [{}]", StringUtil.toLog(connection.getPeerAddress()),
					e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void send(final RawData message) {
		if (message == null) {
			throw new NullPointerException("Message must not be null");
		}
		if (health != null) {
			health.sendingRecord(false);
		}
		if (message.isMulticast()) {
			DROP_LOGGER.warn("DTLSConnector drops {} outgoing bytes to multicast {}", message.getSize(),
					StringUtil.toLog(message.getInetSocketAddress()));
			message.onError(new MulticastNotSupportedException("DTLS doesn't support multicast!"));
			if (health != null) {
				health.sendingRecord(true);
			}
			return;
		}
		final Connection connection;
		RuntimeException error = null;

		if (!running.get()) {
			connection = null;
			error = new IllegalStateException("connector must be started before sending messages is possible");
		} else if (message.getSize() > MAX_PLAINTEXT_FRAGMENT_LENGTH) {
			connection = null;
			error = new IllegalArgumentException(
					"Message data must not exceed " + MAX_PLAINTEXT_FRAGMENT_LENGTH + " bytes");
		} else {
			boolean create = !serverOnly;
			if (create) {
				create = !getEffectiveHandshakeMode(message).equals(DtlsEndpointContext.HANDSHAKE_MODE_NONE);
			}
			connection = getConnection(message.getInetSocketAddress(), null, create);
			if (connection == null) {
				if (create) {
					error = new IllegalStateException("connection store is exhausted!");
				} else {
					if (serverOnly) {
						message.onError(new EndpointUnconnectedException("server only, connection missing!"));
					} else {
						message.onError(new EndpointUnconnectedException("connection missing!"));
					}
					DROP_LOGGER.debug("DTLSConnector drops {} outgoing bytes to {}, connection missing!",
							message.getSize(), StringUtil.toLog(message.getInetSocketAddress()));
					if (health != null) {
						health.sendingRecord(true);
					}
					return;
				}
			}
		}
		if (error != null) {
			DROP_LOGGER.debug("DTLSConnector drops {} outgoing bytes to {}, {}!", message.getSize(),
					StringUtil.toLog(message.getInetSocketAddress()), error.getMessage());
			message.onError(error);
			if (health != null) {
				health.sendingRecord(true);
			}
			throw error;
		}

		final long now = ClockUtil.nanoRealtime();
		if (pendingOutboundMessagesCountdown.decrementAndGet() >= 0) {
			try {
				SerialExecutor executor = connection.getExecutor();
				if (executor == null) {
					throw new NullPointerException("missing executor for connection! " + connection.getPeerAddress());
				}
				executor.execute(new Runnable() {

					@Override
					public void run() {
						try {
							if (running.get()) {
								sendMessage(now, message, connection);
							} else {
								DROP_LOGGER.trace("DTLSConnector drops {} outgoing bytes to {}, connector not running!",
										message.getSize(), StringUtil.toLog(message.getInetSocketAddress()));
								message.onError(new InterruptedIOException("Connector is not running."));
								if (health != null) {
									health.sendingRecord(true);
								}
							}
						} catch (Exception e) {
							if (running.get()) {
								LOGGER.warn("Exception thrown by executor thread [{}]",
										Thread.currentThread().getName(), e);
							}
							DROP_LOGGER.trace("DTLSConnector drops {} outgoing bytes to {} {}", message.getSize(),
									StringUtil.toLog(message.getInetSocketAddress()), e.getMessage());
							if (health != null) {
								health.sendingRecord(true);
							}
							message.onError(e);
						} finally {
							pendingOutboundMessagesCountdown.incrementAndGet();
						}
					}
				});
			} catch (RejectedExecutionException e) {
				LOGGER.debug("Execution rejected while sending application record [peer: {}]",
						StringUtil.toLog(message.getInetSocketAddress()), e);
				DROP_LOGGER.trace("DTLSConnector drops {} outgoing bytes to {}, {}", message.getSize(),
						StringUtil.toLog(message.getInetSocketAddress()), e.getMessage());
				message.onError(new InterruptedIOException("Connector is not running."));
				if (health != null) {
					health.sendingRecord(true);
				}
			}
		} else {
			pendingOutboundMessagesCountdown.incrementAndGet();
			DROP_LOGGER.warn("Outbound message overflow! Dropping outbound message to peer [{}]",
					StringUtil.toLog(message.getInetSocketAddress()));
			message.onError(new IllegalStateException("Outbound message overflow!"));
			if (health != null) {
				health.sendingRecord(true);
			}
		}
	}

	/**
	 * Sends a raw message to a peer.
	 * <p>
	 * This method encrypts and sends the bytes contained in the message using an
	 * already established session with the peer. If no session exists yet, a
	 * new handshake with the peer is initiated and the sending of the message is
	 * deferred to after the handshake has been completed and a session is established.
	 * </p>
	 * 
	 * @param nanos system nanoseconds of receiving the data
	 * @param message the data to send to the peer
	 * @param connection connection of the peer
	 * @throws HandshakeException if starting the handshake fails
	 */
	private void sendMessage(final long nanos, final RawData message, final Connection connection) throws HandshakeException {

		if (connection.getPeerAddress() == null) {
			long delay = TimeUnit.NANOSECONDS.toMillis(ClockUtil.nanoRealtime() - nanos);
			DROP_LOGGER.info("Drop outgoing record with {} bytes, connection lost address {}! (shift {}ms)", message.getSize(),
					StringUtil.toLog(message.getInetSocketAddress()), delay);
			message.onError(new EndpointUnconnectedException("connection not longer assigned to address!"));
			if (health != null) {
				health.sendingRecord(true);
			}
			return;
		}
		LOGGER.trace("Sending application layer message to [{}]", message.getEndpointContext());

		Handshaker handshaker = connection.getOngoingHandshake();
		if (handshaker != null && !handshaker.hasContextEstablished()) {
			if (handshaker.isExpired()) {
				// handshake expired during Android / OS "deep sleep"
				// on sending, abort, keep connection for new handshake
				handshaker.handshakeAborted(new Exception("handshake already expired!"));
			} else if (handshaker.isProbing()) {
				if (checkOutboundEndpointContext(message, null)) {
					message.onConnecting();
					handshaker.addApplicationDataForDeferredProcessing(message);
				}
				return;
			}
		}

		if (connection.isActive() && !connection.isClosed()) {
			sendMessageWithSession(message, connection);
		} else {
			sendMessageWithoutSession(message, connection);
		}
	}

	/**
	 * Send message without session.
	 * 
	 * Starts handshake, if not already pending, and queue message.
	 * 
	 * @param message message to send after handshake completes
	 * @param connection connection to send message
	 * @throws HandshakeException If exception occurred starting the handshake
	 * @since 2.1
	 */
	private void sendMessageWithoutSession(final RawData message, final Connection connection)
			throws HandshakeException {

		if (!checkOutboundEndpointContext(message, null)) {
			return;
		}
		Handshaker handshaker = connection.getOngoingHandshake();
		if (handshaker == null) {
			if (serverOnly) {
				DROP_LOGGER.trace("DTLSConnector drops {} outgoing bytes to {}, server only, connection missing!",
						message.getSize(), StringUtil.toLog(message.getInetSocketAddress()));
				message.onError(new EndpointUnconnectedException("server only, connection missing!"));
				if (health != null) {
					health.sendingRecord(true);
				}
				return;
			}
			boolean none = getEffectiveHandshakeMode(message).contentEquals(DtlsEndpointContext.HANDSHAKE_MODE_NONE);
			if (none) {
				DROP_LOGGER.trace("DTLSConnector drops {} outgoing bytes to {}, connection missing!", message.getSize(),
						StringUtil.toLog(message.getInetSocketAddress()));
				message.onError(new EndpointUnconnectedException("connection missing!"));
				if (health != null) {
					health.sendingRecord(true);
				}
				return;
			}
			String hostname = message.getEndpointContext().getVirtualHost();
			// no session with peer established nor handshaker started yet,
			// create new empty session & start handshake
			ClientHandshaker clientHandshaker = new ClientHandshaker(hostname, this, timer, connection, config, false);
			initializeHandshaker(clientHandshaker);
			message.onConnecting();
			clientHandshaker.addApplicationDataForDeferredProcessing(message);
			clientHandshaker.startHandshake(); // may fail with IOException!
		} else {
			message.onConnecting();
			handshaker.addApplicationDataForDeferredProcessing(message);
		}
	}

	/**
	 * Send message with session.
	 * 
	 * Starts handshake, if requested by resumption or {@link DtlsEndpointContext#KEY_HANDSHAKE_MODE}.
	 * 
	 * @param message message to send
	 * @param connection connection to send message
	 * @throws HandshakeException If exception occurred starting the handshake
	 * @since 2.1
	 */
	private void sendMessageWithSession(final RawData message, final Connection connection) throws HandshakeException {

		DTLSContext context = connection.getEstablishedDtlsContext();
		boolean markedAsClosed = context != null && context.isMarkedAsClosed();
		String handshakeMode = getEffectiveHandshakeMode(message);
		boolean none = DtlsEndpointContext.HANDSHAKE_MODE_NONE.equals(handshakeMode);
		if (none) {
			if (markedAsClosed || connection.isResumptionRequired()) {
				DROP_LOGGER.trace("DTLSConnector drops {} outgoing bytes to {}, resumption required!", message.getSize(), StringUtil.toLog(message.getInetSocketAddress()));
				message.onError(new EndpointUnconnectedException("resumption required!"));
				if (health != null) {
					health.sendingRecord(true);
				}
				return;
			}
		} else {
			boolean probing = DtlsEndpointContext.HANDSHAKE_MODE_PROBE.equals(handshakeMode);
			boolean full = DtlsEndpointContext.HANDSHAKE_MODE_FORCE_FULL.equals(handshakeMode);
			boolean force = probing || full || DtlsEndpointContext.HANDSHAKE_MODE_FORCE.equals(handshakeMode);
			if (force || markedAsClosed || connection.isAutoResumptionRequired(getAutoResumptionTimeout(message))) {
				// create the session to resume from the previous one.
				if (serverOnly) {
					DROP_LOGGER.trace(
							"DTLSConnector drops {} outgoing bytes to {}, server only, resumption requested failed!",
							message.getSize(), StringUtil.toLog(message.getInetSocketAddress()));
					message.onError(new EndpointUnconnectedException("server only, resumption requested failed!"));
					if (health != null) {
						health.sendingRecord(true);
					}
					return;
				}
				message.onConnecting();
				String hostname = message.getEndpointContext().getVirtualHost();
				Handshaker previousHandshaker = connection.getOngoingHandshake();
				DTLSSession newSession = null;
				if (!full && context != null) {
					DTLSSession resume = context.getSession();
					full = resume.getSessionIdentifier().isEmpty();
					if (!full && config.getExtendedMasterSecretMode().is(ExtendedMasterSecretMode.ENABLED)) {
						// https://tools.ietf.org/html/rfc7627#section-5.3
						// The client SHOULD NOT offer an abbreviated handshake
						// to resume a session that does not use an extended
						// master secret. Instead, it SHOULD offer a full
						// handshake.
						full = !resume.useExtendedMasterSecret();
					}
					if (!full) {
						newSession = new DTLSSession(resume);
						newSession.setHostName(hostname);
					}
				}
				if (context != null) {
					if (!probing) {
						connectionStore.removeFromEstablishedSessions(connection);
					}
				} else {
					probing = false;
				}
				if (probing) {
					// Only reset the resumption trigger, but keep the session for now
					// the session will be reseted with the first received data
					connection.setResumptionRequired(false);
				} else {
					connection.resetContext();
				}
				ClientHandshaker newHandshaker;
				if (full) {
					// server may use a empty session id to indicate,
					// that resumption is not supported
					// https://tools.ietf.org/html/rfc5246#section-7.4.1.3
					newHandshaker = new ClientHandshaker(hostname, this, timer, connection, config, probing);
				} else {
					newHandshaker = new ResumingClientHandshaker(newSession, this, timer, connection, config, probing);
				}
				initializeHandshaker(newHandshaker);
				if (previousHandshaker != null) {
					newHandshaker.takeDeferredApplicationData(previousHandshaker);
					// abort, keep connection
					previousHandshaker.handshakeAborted(new Exception("handshake replaced!"));
				}
				newHandshaker.addApplicationDataForDeferredProcessing(message);
				newHandshaker.startHandshake();
				return;
			}
		}
		// session with peer has already been established,
		// use it to send encrypted message
		sendMessage(message, connection);
	}

	private void sendMessage(final RawData message, final Connection connection) {
		try {
			DTLSContext dltsContext = connection.getEstablishedDtlsContext();
			LOGGER.trace("send {}-{} using {}", connection.getConnectionId(),
					StringUtil.toLog(connection.getPeerAddress()), dltsContext.getSession().getSessionIdentifier());
			final DtlsEndpointContext context = connection.getWriteContext();
			if (!checkOutboundEndpointContext(message, context)) {
				return;
			}

			message.onContextEstablished(context);
			Record record = new Record(
					ContentType.APPLICATION_DATA,
					dltsContext.getWriteEpoch(),
					new ApplicationMessage(message.getBytes()),
					dltsContext,
					true, TLS12_CID_PADDING);
			record.setAddress(connection.getPeerAddress(), connection.getRouter());
			sendRecord(record);
			message.onSent();
			connectionStore.update(connection, null);
			if (connectionListener != null) {
				if (connectionListener.onConnectionUpdatesSequenceNumbers(connection, true)) {
					closeConnection(connection);
				}
			}
		} catch (IOException e) {
			message.onError(e);
		} catch (GeneralSecurityException e) {
			DROP_LOGGER.warn("Cannot send APPLICATION record to peer [{}]",
					StringUtil.toLog(message.getInetSocketAddress()), e);
			message.onError(e);
		}
	}

	/**
	 * Check, if the endpoint context match for outgoing messages using
	 * {@link #endpointContextMatcher}.
	 * 
	 * @param message message to be checked
	 * @param connectionContext endpoint context of the connection. May be
	 *            {@code null}, if not established.
	 * @return {@code true}, if outgoing message matches, {@code false}, if not
	 *         and should NOT be send.
	 * @see EndpointContextMatcher#isToBeSent(EndpointContext, EndpointContext)
	 */
	private boolean checkOutboundEndpointContext(final RawData message, final EndpointContext connectionContext) {
		final EndpointContextMatcher endpointMatcher = getEndpointContextMatcher();
		if (null != endpointMatcher && !endpointMatcher.isToBeSent(message.getEndpointContext(), connectionContext)) {
			if (DROP_LOGGER.isInfoEnabled()) {
				DROP_LOGGER.info("DTLSConnector ({}) drops {} bytes outgoing, {} != {}", this, message.getSize(),
						endpointMatcher.toRelevantState(message.getEndpointContext()),
						endpointMatcher.toRelevantState(connectionContext));
			}
			message.onError(new EndpointMismatchException("DTLS sending"));
			if (health != null) {
				health.sendingRecord(true);
			}
			return false;
		}
		return true;
	}

	/**
	 * Returns the {@link DTLSSession} related to the given peer address.
	 * 
	 * @param address the peer address
	 * @return the {@link DTLSSession}, or {@code null}, if session is not available.
	 */
	public final DTLSSession getSessionByAddress(InetSocketAddress address) {
		DTLSContext context = getDtlsContextByAddress(address);
		if (context != null) {
			return context.getSession();
		}
		return null;
	}

	/**
	 * Returns the {@link DTLSContext} related to the given peer address.
	 * 
	 * @param address the peer address
	 * @return the {@link DTLSContext}, or {@code null}, if context is not available.
	 */
	public final DTLSContext getDtlsContextByAddress(InetSocketAddress address) {
		if (address == null) {
			return null;
		}
		Connection connection = connectionStore.get(address);
		if (connection != null) {
			return connection.getEstablishedDtlsContext();
		} else {
			return null;
		}
	}

	@Override
	public void dropReceivedRecord(Record record) {
		DROP_LOGGER.debug("Discarding {} record [epoch {}, rseqn {}] dropped by handshaker for peer [{}]", record.getType(),
				record.getEpoch(), record.getSequenceNumber(), StringUtil.toLog(record.getPeerAddress()));
		if (health != null) {
			health.receivingRecord(true);
		}
	}

	@Override
	public int getMaxDatagramSize(boolean ipv6) {
		int headerSize = ipv6 ? IPV6_HEADER_LENGTH : IPV4_HEADER_LENGTH;
		int mtu = maximumTransmissionUnit != null ? maximumTransmissionUnit : (ipv6 ? ipv6Mtu : ipv4Mtu);
		int size = mtu - headerSize;
		if (size < 64) {
			throw new IllegalStateException(
					String.format("%s, datagram size %d, mtu %d", ipv6 ? "IPV6" : "IPv4", size, mtu));
		}
		return mtu - headerSize;
	}

	@NoPublicAPI
	@Override
	public void sendFlight(List<DatagramPacket> datagrams) throws IOException {
		// send it over the UDP socket
		for (DatagramPacket datagramPacket : datagrams) {
			if (health != null) {
				health.sendingRecord(false);
			}
			sendNextDatagramOverNetwork(datagramPacket);
		}
	}

	protected void sendRecord(Record record) throws IOException {
		if (health != null && record.getType() != ContentType.APPLICATION_DATA) {
			health.sendingRecord(false);
		}
		byte[] recordBytes = record.toByteArray();
		DatagramPacket datagram = new DatagramPacket(recordBytes, recordBytes.length, record.getPeerAddress());
		sendNextDatagramOverNetwork(datagram);
	}

	protected void sendNextDatagramOverNetwork(final DatagramPacket datagramPacket) throws IOException {
		DatagramSocket socket = getSocket();
		if (socket != null && !socket.isClosed()) {
			try {
				socket.send(datagramPacket);
				return;
			} catch (PortUnreachableException e) {
				if (!socket.isClosed()) {
					LOGGER.warn("Could not send record, destination {} unreachable!",
							StringUtil.toLog(datagramPacket.getSocketAddress()));
				}
			} catch (IOException e) {
				if (!socket.isClosed()) {
					LOGGER.warn("Could not send record", e);
					throw e;
				}
			}
		}
		InetSocketAddress address = lastBindAddress;
		if (address == null) {
			address = config.getAddress();
		}
		DROP_LOGGER.debug("Socket [{}] is closed, discarding packet ...", address);
		throw new IOException("Socket closed.");
	}

	/**
	 * Process handshake result.
	 * 
	 * @param handshakeResult asynchronous handshake result
	 * @since 2.5
	 */
	private void processAsynchronousHandshakeResult(final HandshakeResult handshakeResult) {
		final Connection connection = connectionStore.get(handshakeResult.getConnectionId());
		if (connection != null && connection.hasOngoingHandshake()) {
			SerialExecutor serialExecutor = connection.getExecutor();

			try {

				serialExecutor.execute(new Runnable() {

					@Override
					public void run() {
						if (running.get()) {
							Handshaker handshaker = connection.getOngoingHandshake();
							if (handshaker != null) {
								try {
									handshaker.processAsyncHandshakeResult(handshakeResult);
								} catch (HandshakeException e) {
									processExceptionDuringHandshake(null, connection, e);
								} catch (IllegalStateException e) {
									LOGGER.warn("Exception while processing handshake result [{}]", connection, e);
								}
							} else {
								LOGGER.debug("No ongoing handshake for result [{}]", connection);
							}
						} else {
							LOGGER.debug("Execution stopped while processing handshake result [{}]", connection);
						}
					}
				});
			} catch (RejectedExecutionException e) {
				// dont't terminate connection on shutdown!
				LOGGER.debug("Execution rejected while processing handshake result [{}]", connection, e);
			} catch (RuntimeException e) {
				LOGGER.warn("Unexpected error occurred while processing handshake result [{}]", connection, e);
			}
		} else {
			LOGGER.debug("No connection or ongoing handshake for handshake result [{}]", connection);
		}
	}

	/**
	 * Get auto resumption timeout.
	 * 
	 * Check, if {@link DtlsEndpointContext#KEY_RESUMPTION_TIMEOUT} is provided,
	 * or use {@link #autoResumptionTimeoutMillis} as default.
	 * 
	 * @param message message to check for auto resumption timeout.
	 * @return resulting timeout in milliseconds. {@code null} for no auto
	 *         resumption.
	 * @since 3.0 (fixed typo, was "getAutResumptionTimeout" before)
	 */
	private Long getAutoResumptionTimeout(RawData message) {
		Long timeout = autoResumptionTimeoutMillis;
		Number contextTimeout = message.getEndpointContext().getNumber(DtlsEndpointContext.KEY_RESUMPTION_TIMEOUT);
		if (contextTimeout != null) {
			if (contextTimeout.longValue() >= 0) {
				timeout = contextTimeout.longValue();
			} else {
				timeout = null;
			}
		}
		return timeout;
	}

	/**
	 * Gets the maximum amount of unencrypted payload data that can be sent to a given
	 * peer in a single DTLS record.
	 * <p>
	 * The value of this property serves as an upper boundary for the <em>DTLSPlaintext.length</em>
	 * field defined in <a href="http://tools.ietf.org/html/rfc6347#section-4.3.1" target="_blank">DTLS 1.2 spec,
	 * Section 4.3.1</a>. This means that an application can assume that any message containing at
	 * most as many bytes as indicated by this method, will be delivered to the peer in a single
	 * unfragmented datagram.
	 * </p>
	 * <p>
	 * The value returned by this method considers the <em>current write state</em> of the connection
	 * to the peer and any potential ciphertext expansion introduced by this cipher suite used to
	 * secure the connection. However, if no connection exists to the peer, the value returned is
	 * determined as follows:
	 * </p>
	 * <pre>
	 *   maxFragmentLength = network interface's <em>Maximum Transmission Unit</em>
	 *                     - IP header length (20 bytes IPv4, 120 IPv6)
	 *                     - UDP header length (8 bytes)
	 *                     - DTLS record header length (13 bytes)
	 *                     - DTLS message header length (12 bytes)
	 * </pre>
	 * 
	 * @param peer the address of the remote endpoint
	 * 
	 * @return the maximum length in bytes
	 */
	public final int getMaximumFragmentLength(InetSocketAddress peer) {
		DTLSContext dtlsContext = getDtlsContextByAddress(peer);
		if (dtlsContext != null) {
			return dtlsContext.getSession().getMaxFragmentLength();
		} else {
			return getMaxDatagramSize(peer.getAddress() instanceof Inet6Address) - Record.DTLS_HANDSHAKE_HEADER_LENGTH;
		}
	}

	/**
	 * Gets the address this connector is bound to.
	 * 
	 * @return the IP address and port this connector is bound to or configured to
	 *            bind to
	 */
	@Override
	public final InetSocketAddress getAddress() {
		DatagramSocket socket = getSocket();
		int localPort = socket == null ? -1 : socket.getLocalPort();
		if (localPort < 0) {
			return config.getAddress();
		} else {
			return new InetSocketAddress(socket.getLocalAddress(), localPort);
		}
	}

	/**
	 * Checks if this connector is running.
	 * 
	 * @return {@code true} if running.
	 */
	@Override
	public final boolean isRunning() {
		return running.get();
	}

	/**
	 * A worker thread for continuously doing repetitive tasks.
	 */
	protected abstract class Worker extends Thread {
		/**
		 * Instantiates a new worker.
		 *
		 * @param name the name, e.g., of the transport protocol
		 */
		protected Worker(String name) {
			super(NamedThreadFactory.SCANDIUM_THREAD_GROUP, name);
		}

		@Override
		public void run() {
			try {
				LOGGER.info("Starting worker thread [{}]", getName());
				while (running.get()) {
					try {
						doWork();
					} catch (InterruptedIOException e) {
						if (running.get()) {
							LOGGER.info("Worker thread [{}] IO has been interrupted", getName());
						} else {
							LOGGER.debug("Worker thread [{}] IO has been interrupted", getName());
						}
					} catch (InterruptedException e) {
						if (running.get()) {
							LOGGER.info("Worker thread [{}] has been interrupted", getName());
						} else {
							LOGGER.debug("Worker thread [{}] has been interrupted", getName());
						}
					} catch (Exception e) {
						if (running.get()) {
							LOGGER.debug("Exception thrown by worker thread [{}]", getName(), e);
						} else {
							LOGGER.trace("Exception thrown by worker thread [{}]", getName(), e);
						}
					}
				}
			} finally {
				if (running.get()) {
					LOGGER.info("Worker thread [{}] has terminated", getName());
				} else {
					LOGGER.debug("Worker thread [{}] has terminated", getName());
				}
			}
		}

		/**
		 * Does the actual work.
		 * 
		 * Subclasses should do the repetitive work here.
		 * 
		 * @throws Exception if something goes wrong
		 */
		protected abstract void doWork() throws Exception;
	}

	/**
	 * Future implementation for tasks passed in to the serial executors for each
	 * connection.
	 */
	private static class ForEachFuture implements Future<Void> {

		private final Lock lock = new ReentrantLock();
		private final Condition waitDone = lock.newCondition();
		private volatile boolean cancel;
		private volatile boolean done;
		private volatile Exception exception;

		/**
		 * {@inheritDoc}
		 * 
		 * Cancel iteration for each connection.
		 * 
		 * Note: if a connection serial execution busy executing a different
		 * blocking task, cancel will not interrupt that task!
		 */
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			boolean cancelled = false;
			lock.lock();
			try {
				if (!done && !cancel) {
					cancelled = true;
					cancel = true;
				}
			} finally {
				lock.unlock();
			}
			return cancelled;
		}

		@Override
		public boolean isCancelled() {
			return cancel;
		}

		@Override
		public boolean isDone() {
			return done;
		}

		@Override
		public Void get() throws InterruptedException, ExecutionException {
			lock.lock();
			try {
				if (!done) {
					waitDone.await();
				}
				if (exception != null) {
					throw new ExecutionException(exception);
				}
			} finally {
				lock.unlock();
			}
			return null;
		}

		@Override
		public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			lock.lock();
			try {
				if (!done) {
					waitDone.await(timeout, unit);
				}
				if (exception != null) {
					throw new ExecutionException(exception);
				}
			} finally {
				lock.unlock();
			}
			return null;
		}

		/**
		 * Signals, that the task has completed.
		 */
		public void done() {
			lock.lock();
			try {
				done = true;
				waitDone.signalAll();
			} finally {
				lock.unlock();
			}
		}

		public void failed(Exception exception) {
			lock.lock();
			try {
				this.exception = exception;
				done = true;
				waitDone.signalAll();
			} finally {
				lock.unlock();
			}
		}

		public boolean isStopped() {
			return done || cancel;
		}
	}

	@Override
	public void setRawDataReceiver(final RawDataChannel messageHandler) {
		if (isRunning()) {
			throw new IllegalStateException("message handler cannot be set on running connector");
		}
		this.messageHandler = messageHandler;
	}

	@Override
	public void setEndpointContextMatcher(EndpointContextMatcher endpointContextMatcher) {
		this.endpointContextMatcher = endpointContextMatcher;
	}

	private EndpointContextMatcher getEndpointContextMatcher() {
		return endpointContextMatcher;
	}

	/**
	 * Get effective handshake mode.
	 * 
	 * Either the handshake mode provided in the message's endpoint-context, see
	 * {@link DtlsEndpointContext#KEY_HANDSHAKE_MODE}, or, if that is not
	 * available, the default from the configuration
	 * {@link DtlsConnectorConfig#getDefaultHandshakeMode()}.
	 * 
	 * @param message message to be sent
	 * @return effective handshake mode.
	 * @since 2.1
	 */
	private String getEffectiveHandshakeMode(RawData message) {
		String mode = message.getEndpointContext().getString(DtlsEndpointContext.KEY_HANDSHAKE_MODE);
		if (mode == null) {
			mode = defaultHandshakeMode;
		}
		return mode;
	}
	
	/**
	 * Sets a handler to call back if an alert message is received from a peer.
	 * <p>
	 * Setting a handler using this method is useful to be notified when a peer closes
	 * an existing connection, i.e. when the alert message has not been received during
	 * a handshake but after the connection has been established.
	 * <p>
	 * The handler can be set (and changed) at any time, either before the connector has
	 * been started or when the connector is already running.
	 * <p>
	 * Application code interested in being notified when a particular message cannot be sent,
	 * e.g. due to a failing DTLS handshake that has been triggered as part of sending
	 * the message, should instead register a
	 * {@code org.eclipse.californium.core.coap.MessageObserver} on the message and
	 * implement its <em>onSendError</em> method accordingly.
	 * 
	 * @param handler The handler to notify.
	 */
	public final void setAlertHandler(AlertHandler handler) {
		this.alertHandler = handler;
	}

	private void discardRecord(final Record record, final Throwable cause) {
		if (health != null) {
			health.receivingRecord(true);
		}
		byte[] bytes = record.getFragmentBytes();
		if (DROP_LOGGER.isTraceEnabled()) {
			String hexString = StringUtil.byteArray2HexString(bytes, StringUtil.NO_SEPARATOR, 64);
			DROP_LOGGER.trace("Discarding received {} record (epoch {}, payload: {}) from peer [{}]: ",
					record.getType(), record.getEpoch(), hexString, StringUtil.toLog(record.getPeerAddress()), cause);
		} else if (DROP_LOGGER.isDebugEnabled()) {
			String hexString = StringUtil.byteArray2HexString(bytes, StringUtil.NO_SEPARATOR, 16);
			DROP_LOGGER.debug("Discarding received {} record (epoch {}, payload: {}) from peer [{}]: {}",
					record.getType(), record.getEpoch(), hexString, StringUtil.toLog(record.getPeerAddress()),
					cause.getMessage());
		}
	}

	@Override
	public String getProtocol() {
		return "DTLS";
	}

	@Override
	public String toString() {
		return getProtocol() + "-" + StringUtil.toString(getAddress());
	}
	
}
