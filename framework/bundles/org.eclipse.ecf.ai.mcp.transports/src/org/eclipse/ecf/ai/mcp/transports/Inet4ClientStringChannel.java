/****************************************************************************
 * Copyright (c) 2025 Composent, Inc. and others.
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
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

public class Inet4ClientStringChannel extends ClientStringChannel {

	public Inet4ClientStringChannel() throws IOException {
		super();
	}

	public Inet4ClientStringChannel(Selector selector, int incomingBufferSize, ExecutorService executor) {
		super(selector, incomingBufferSize, executor);
	}

	public Inet4ClientStringChannel(Selector selector, int incomingBufferSize) {
		super(selector, incomingBufferSize);
	}

	public Inet4ClientStringChannel(Selector selector) {
		super(selector);
	}

	public void connectBlocking(Inet4Address address, int port, IOConsumer<SocketChannel> connectHandler,
			IOConsumer<String> readHandler) throws IOException {
		super.connect(StandardProtocolFamily.INET, new InetSocketAddress(address, port), connectHandler, readHandler);
	}

}
