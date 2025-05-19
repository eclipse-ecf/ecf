/****************************************************************************
 * Copyright (c) 2025 Composent, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
  * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.ai.mcp.tools.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.ecf.ai.mcp.tools.annotation.Tool;

/**
 * Describes the ToolAnnotation type in the MCP schema (draft as of 5/18/2025)
 * located <a href=
 * "https://github.com/modelcontextprotocol/modelcontextprotocol/blob/main/schema/draft/schema.json#L2164">here</a>
 */
public record ToolAnnotationDescription(boolean destructiveHint, boolean idempotentHint, boolean openWorldHint,
		boolean readOnlyHint, String title) {

	public static List<ToolAnnotationDescription> fromAnnotations(Tool.ToolAnnotation[] annotations) {
		return (annotations != null) ? Arrays.asList(annotations).stream().map(a -> {
			return new ToolAnnotationDescription(a.destructiveHint(), a.idempotentHint(), a.openWorldHint(),
					a.readOnlyHint(), a.title());
		}).collect(Collectors.toList()) : Collections.emptyList();

	}
}
