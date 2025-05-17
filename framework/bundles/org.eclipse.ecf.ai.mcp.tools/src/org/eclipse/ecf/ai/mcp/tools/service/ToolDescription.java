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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.ecf.ai.mcp.tools.annotation.Tool;

public record ToolDescription(String name, String description, List<ToolParamDescription> toolParamDescriptions) {

	public static List<ToolDescription> fromClass(Class<?> clazz) {
		return Arrays.asList(clazz.getMethods()).stream().map(m -> {
			Tool ma = m.getAnnotation(Tool.class);
			return (ma != null)
					? new ToolDescription(m.getName(), ma.description(),
							ToolParamDescription.fromParameters(m.getParameters()))
					: null;
		}).filter(Objects::nonNull).collect(Collectors.toList());

	}

	public static List<ToolDescription> fromService(Object svc, String serviceClass) {
		Optional<Class<?>> optClass = Arrays.asList(svc.getClass().getInterfaces()).stream().filter(c -> {
			return c.getName().equals(serviceClass);
		}).findFirst();
		return optClass.isPresent() ? ToolDescription.fromClass(optClass.get()) : Collections.emptyList();
	}
}
