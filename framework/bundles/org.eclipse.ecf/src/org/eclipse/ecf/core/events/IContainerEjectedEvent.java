/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.core.events;

import java.io.Serializable;
import org.eclipse.ecf.core.identity.ID;

/**
 * Container ejected event. This event is received when a local container has
 * been ejected from a remote group
 */
public interface IContainerEjectedEvent extends IContainerEvent {
	/**
	 * Get ID of container target (the container we were ejected from)
	 * 
	 * @return ID the ID of the container we were ejected from. Will not be
	 *         null.
	 */
	public ID getTargetID();

	/**
	 * Get reason for ejection
	 * 
	 * @return Serializable reason for ejection. May be null.
	 */
	public Serializable getReason();
}