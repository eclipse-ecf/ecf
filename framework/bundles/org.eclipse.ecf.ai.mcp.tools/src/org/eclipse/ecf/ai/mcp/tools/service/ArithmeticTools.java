/*******************************************************************************
 * Copyright (c) 2025 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.ai.mcp.tools.service;

import java.util.List;

import org.eclipse.ecf.ai.mcp.tools.annotation.Tool;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolAnnotation;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolAnnotations;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolParam;
import org.eclipse.ecf.ai.mcp.tools.annotation.ToolResult;
import org.eclipse.ecf.ai.mcp.tools.util.ToolDescription;

public interface ArithmeticTools extends ToolGroupService {

	@Tool(description = "return the sum of the  arguments")
	@ToolAnnotations({ @ToolAnnotation(destructiveHint = true) })
	@ToolResult(description = "result")
	int add(@ToolParam(description = "first argument") int a, @ToolParam(description = "second argument") int b);

	@Tool(description = "return the product of the arguments")
	int multiply(@ToolParam(description = "first argument") int a, @ToolParam(description = "second argument") int b);

	public static void main(String[] args) throws Exception {
		ArithmeticTools inst = new ArithmeticTools() {

			@Override
			public int add(int a, int b) {
				return 0;
			}

			@Override
			public int multiply(int a, int b) {
				return 0;
			}

		};
		List<ToolDescription> descs = inst.getToolDescriptions(ArithmeticTools.class.getName());
		System.out.println(descs);
	}
}
