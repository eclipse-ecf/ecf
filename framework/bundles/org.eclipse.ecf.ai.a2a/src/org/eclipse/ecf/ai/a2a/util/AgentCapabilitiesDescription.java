/****************************************************************************
 * Copyright (c) 2025 Composent, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
  * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.ai.a2a.util;

import java.io.Serializable;

public record AgentCapabilitiesDescription(
		boolean pushNotifications, 
		boolean stateTransitionHistory, 
		boolean streaming) implements Serializable {
}
