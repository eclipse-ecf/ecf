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

public class UDSServerStringChannel extends ServerStringChannel {

	public UDSServerStringChannel() throws IOException {
		super();
	}

	public UDSServerStringChannel(Selector selector, int incomingBufferSize, ExecutorService executor) {
		super(selector, incomingBufferSize, executor);
	}

	public UDSServerStringChannel(Selector selector, int incomingBufferSize) {
		super(selector, incomingBufferSize);
	}

	public UDSServerStringChannel(Selector selector) {
		super(selector);
	}

	public void start(UnixDomainSocketAddress address, IOConsumer<SocketChannel> acceptHandler,
			IOConsumer<String> readHandler) throws IOException {
		super.start(StandardProtocolFamily.UNIX, address, acceptHandler, readHandler);
	}

}
