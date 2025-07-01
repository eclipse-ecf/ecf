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

import org.eclipse.ecf.ai.mcp.tools.annotation.ToolAnnotations;

/**
 * Describes the ToolAnnotations type in the MCP schema (draft as of 5/18/2025)
 * located <a href=
 * "https://github.com/modelcontextprotocol/modelcontextprotocol/blob/main/schema/draft/schema.json#L2164">here</a>
 */
public record ToolAnnotationsDescription(boolean destructiveHint, boolean idempotentHint, boolean openWorldHint,
		boolean readOnlyHint, String title) implements Serializable {

	public static ToolAnnotationsDescription fromAnnotations(ToolAnnotations annotations) {
		if (annotations != null) {
			return new ToolAnnotationsDescription(annotations.destructiveHint(), annotations.idempotentHint(),
					annotations.openWorldHint(), annotations.readOnlyHint(), annotations.title());
		} else {
			return null;
		}
	}
}
