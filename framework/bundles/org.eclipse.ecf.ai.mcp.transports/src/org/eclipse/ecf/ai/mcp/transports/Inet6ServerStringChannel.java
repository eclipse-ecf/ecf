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
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

public class Inet6ServerStringChannel extends ServerStringChannel {

	public Inet6ServerStringChannel() throws IOException {
		super();
	}

	public Inet6ServerStringChannel(Selector selector, int incomingBufferSize, ExecutorService executor) {
		super(selector, incomingBufferSize, executor);
	}

	public Inet6ServerStringChannel(Selector selector, int incomingBufferSize) {
		super(selector, incomingBufferSize);
	}

	public Inet6ServerStringChannel(Selector selector) {
		super(selector);
	}

	public void start(Inet6Address address, int port, IOConsumer<SocketChannel> acceptHandler,
			IOConsumer<String> readHandler) throws IOException {
		super.start(StandardProtocolFamily.INET6, new InetSocketAddress(address, port), acceptHandler, readHandler);
	}

}
