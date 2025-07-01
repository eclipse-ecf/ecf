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
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.ecf.ai.mcp.tools.annotation.ToolParam;

public record ToolParamDescription(String name, String description, boolean required) implements Serializable {

	public static List<ToolParamDescription> fromParameters(Parameter[] parameters) {
		return parameters != null ? Arrays.asList(parameters).stream().map(p -> {
			ToolParam tp = p.getAnnotation(ToolParam.class);
			if (tp != null) {
				String name = tp.name();
				if ("".equals(name)) {
					name = p.getName();
				}
				return new ToolParamDescription(name, tp.description(), tp.required());
			}
			return null;
		}).filter(Objects::nonNull).collect(Collectors.toList()) : Collections.emptyList();
	}

}
