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

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.ecf.ai.mcp.tools.annotation.ToolParam;

public record ToolParamDescription(String description, boolean required) {

	public static List<ToolParamDescription> fromParameters(Parameter[] parameters) {
		return Arrays.asList(parameters).stream().map(p -> {
			ToolParam tp = p.getAnnotation(ToolParam.class);
			return (tp != null) ? new ToolParamDescription(tp.description(), tp.required()) : null;
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}
}
