/****************************************************************************
 * Copyright (c) 2025 Composent, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
  * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.ai.mcp.tools.util;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.eclipse.ecf.ai.mcp.tools.annotation.ToolResult;

public record ToolResultDescription(String description, Class<?> returnType) implements Serializable {

	public static ToolResultDescription fromMethod(Method method) {
		ToolResult tr = method.getAnnotation(ToolResult.class);
		return tr != null ? new ToolResultDescription(tr.description(), method.getReturnType())
				: new ToolResultDescription("", method.getReturnType());
	}
}
