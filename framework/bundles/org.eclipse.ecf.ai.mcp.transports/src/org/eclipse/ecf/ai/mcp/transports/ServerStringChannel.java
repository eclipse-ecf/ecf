/****************************************************************************
 * Copyright (c) 2025 Composent, Inc. 
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.ai.mcp.transports;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStringChannel extends AbstractStringChannel {

	private static final Logger logger = LoggerFactory.getLogger(ServerStringChannel.class);

	protected SocketChannel acceptedClient;

	public ServerStringChannel() throws IOException {
		super();
	}

	public ServerStringChannel(Selector selector, int incomingBufferSize, ExecutorService executor) {
		super(selector, incomingBufferSize, executor);
	}

	public ServerStringChannel(Selector selector, int incomingBufferSize) {
		super(selector, incomingBufferSize);
	}

	public ServerStringChannel(Selector selector) {
		super(selector);
	}

	protected void configureServerSocketChannel(java.nio.channels.ServerSocketChannel serverSocketChannel,
			SocketAddress acceptAddress) {
		// Subclasses may override
	}

	public void start(StandardProtocolFamily protocol, SocketAddress address, IOConsumer<SocketChannel> acceptHandler,
			IOConsumer<String> readHandler) throws IOException {
		java.nio.channels.ServerSocketChannel serverChannel = java.nio.channels.ServerSocketChannel.open(protocol);
		serverChannel.configureBlocking(false);
		serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
		configureServerSocketChannel(serverChannel, address);
		serverChannel.bind(address);
		// Start thread/processing of incoming accept, read
		super.start((client) -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Setting client=" + client);
			}
			this.acceptedClient = client;
			if (acceptHandler != null) {
				acceptHandler.apply(this.acceptedClient);
			}
			// No/null connect handler for Acceptors...only accepthandler
		}, null, readHandler);
	}

	@Override
	protected void handleException(SelectionKey key, Throwable e) {
		if (logger.isDebugEnabled()) {
			logger.debug("handleException", e);
		}
		close();
	}

	public void writeMessage(String message) throws IOException {
		SocketChannel c = this.acceptedClient;
		if (c != null) {
			writeMessageToChannel(c, message);
		} else {
			throw new IOException("not connected");
		}
	}

	@Override
	public void close() {
		SocketChannel client = this.acceptedClient;
		if (client != null) {
			hardCloseClient(client, (c) -> {
				if (logger.isDebugEnabled()) {
					logger.debug("Unsetting client=" + c);
				}
				this.acceptedClient = null;
			});
		}
	}

}