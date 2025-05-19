/****************************************************************************
 * Copyright (c) 2025 Composent, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
  * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.ai.mcp.tools.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tool {

	String name() default "";

	String description() default "";

	/**
	 * Supports the addition of ToolAnnotations to Tool spec in the MCP schema
	 * (draft as of 5/18/2025) located <a href=
	 * "https://github.com/modelcontextprotocol/modelcontextprotocol/blob/main/schema/draft/schema.json#L2164">here</a>
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface ToolAnnotation {
		boolean destructiveHint() default false;

		boolean idempotentHint() default false;

		boolean openWorldHint() default false;

		boolean readOnlyHint() default false;

		String title() default "";
	}

	ToolAnnotation[] annotations();
}