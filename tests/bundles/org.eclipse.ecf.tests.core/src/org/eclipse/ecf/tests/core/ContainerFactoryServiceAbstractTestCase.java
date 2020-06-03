/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.ecf.tests.core;

import junit.framework.TestCase;

import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.ContainerTypeDescription;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.IContainerFactory;
import org.eclipse.ecf.core.provider.IContainerInstantiator;
import org.eclipse.ecf.internal.tests.core.Activator;

public abstract class ContainerFactoryServiceAbstractTestCase extends TestCase {

	protected static final String DESCRIPTION = "description";

	private IContainerFactory fixture;

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		fixture = Activator.getDefault().getContainerFactory();
		assertNotNull(fixture);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		fixture = null;
	}

	protected IContainerFactory getFixture() {
		return fixture;
	}

	protected ContainerTypeDescription createContainerTypeDescription() {
		return new ContainerTypeDescription(this.getClass().getName(),
				new IContainerInstantiator() {
					public IContainer createInstance(
							ContainerTypeDescription description,
							Object[] parameters)
							throws ContainerCreateException {
						throw new ContainerCreateException();
					}

					public String[] getSupportedAdapterTypes(
							ContainerTypeDescription description) {
						return null;
					}

					public Class[][] getSupportedParameterTypes(
							ContainerTypeDescription description) {
						return null;
					}

					public String[] getSupportedIntents(
							ContainerTypeDescription description) {
						return null;
					}
				}, DESCRIPTION);
	}

}
