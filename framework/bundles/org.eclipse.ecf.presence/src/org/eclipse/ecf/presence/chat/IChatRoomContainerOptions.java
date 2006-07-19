/****************************************************************************
* Copyright (c) 2004 Composent, Inc. and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Composent, Inc. - initial API and implementation
*****************************************************************************/
package org.eclipse.ecf.presence.chat;

public interface IChatRoomContainerOptions {
	/** 
	 * Set encoding for chat room manager that supports
	 * IChatRoomOptions
	 * @param encoding
	 * @return true if encoding set properly, false if encoding cannot be set/reset
	 */
	public boolean setEncoding(String encoding);
}
