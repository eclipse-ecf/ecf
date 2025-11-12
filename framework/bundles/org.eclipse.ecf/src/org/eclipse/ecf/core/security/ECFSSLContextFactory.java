/****************************************************************************
 * Copyright (c) 2024 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors: Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.core.security;

import java.security.*;
import java.util.Objects;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ecf.internal.core.identity.Activator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @since 3.12
 */
public class ECFSSLContextFactory implements SSLContextFactory {

	private ServiceTracker<Provider, Provider> providerTracker;
	private final String defaultProtocol;
	private final String defaultProviderName;

	public ECFSSLContextFactory(BundleContext context) throws NoSuchAlgorithmException {
		this(context, null);
	}

	public ECFSSLContextFactory(BundleContext context, String defaultProtocol) throws NoSuchAlgorithmException {
		this(context, defaultProtocol, null);
	}

	public ECFSSLContextFactory(BundleContext context, String defaultProtocol, String defaultProviderName) throws NoSuchAlgorithmException {
		if (context == null)
			throw new NullPointerException("context must not be null"); //$NON-NLS-1$
		if (defaultProviderName == null) {
			defaultProviderName = SSLContext.getDefault().getProvider().getName();
		}
		if (defaultProtocol == null) {
			defaultProtocol = SSLContext.getDefault().getProtocol();
		}
		this.defaultProtocol = defaultProtocol;
		this.defaultProviderName = defaultProviderName;
		this.providerTracker = new ServiceTracker<Provider, Provider>(context, Provider.class, null);
		this.providerTracker.open();
	}

	@Override
	public SSLContext getDefault() throws NoSuchAlgorithmException, NoSuchProviderException {
		return getInstance0(this.defaultProtocol, this.defaultProviderName);
	}

	protected SSLContext getInstance0(String protocol, String providerName) throws NoSuchAlgorithmException, NoSuchProviderException {
		SSLContext defaultContext = SSLContext.getDefault();
		if (protocol == null || Objects.equals(protocol, defaultContext.getProtocol()) && Objects.equals(providerName, defaultContext.getProvider().getName())) {
			return defaultContext;
		}
		Provider provider = findProvider(providerName);
		if (provider == null)
			throw new NoSuchProviderException("No provider registered named '" + providerName + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		return SSLContext.getInstance(protocol, provider);
	}

	@Override
	public SSLContext getInstance(String protocol) throws NoSuchAlgorithmException, NoSuchProviderException {
		return getInstance0(protocol, this.defaultProviderName);
	}

	public synchronized void close() {
		if (this.providerTracker != null) {
			this.providerTracker.close();
			this.providerTracker = null;
		}
	}

	protected Provider findProvider(String providerName) {
		if (providerName == null) {
			return this.providerTracker.getService();
		}
		Optional<Provider> optResult = this.providerTracker.getTracked().values().stream().filter(p ->
		// test that providerName is equal to Provider.getName()
		providerName.equals(p.getName())).findFirst();
		// If there are matching Providers, use first (highest priority from sorted map) and use to create SSLContext.  
		// If none, then throw
		if (optResult.isPresent()) {
			return optResult.get();
		}
		// If providerName is same as current default SSLContext then use it
		try {
			SSLContext defaultContext = SSLContext.getDefault();
			if (providerName.equals(defaultContext.getProvider().getName())) {
				return defaultContext.getProvider();
			}
		} catch (NoSuchAlgorithmException e) {
			Activator.getDefault().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not get SSLContext.getDefault()", e)); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public SSLContext getInstance(String protocol, String providerName) throws NoSuchAlgorithmException, NoSuchProviderException {
		return getInstance0(protocol, providerName);
	}

}
