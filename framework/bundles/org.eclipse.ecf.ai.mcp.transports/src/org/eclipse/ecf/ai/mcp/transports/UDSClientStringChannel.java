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
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

public class UDSClientStringChannel extends ClientStringChannel {

	public UDSClientStringChannel(Selector selector, int incomingBufferSize, ExecutorService executor) {
		super(selector, incomingBufferSize, executor);
	}

	public UDSClientStringChannel() throws IOException {
		super();
	}

	public UDSClientStringChannel(Selector selector, int incomingBufferSize) {
		super(selector, incomingBufferSize);
	}

	public UDSClientStringChannel(Selector selector) {
		super(selector);
	}

	public void connect(UnixDomainSocketAddress address, IOConsumer<SocketChannel> connectHandler,
			IOConsumer<String> readHandler) throws IOException {
		super.connect(StandardProtocolFamily.UNIX, address, connectHandler, readHandler);
	}

}
