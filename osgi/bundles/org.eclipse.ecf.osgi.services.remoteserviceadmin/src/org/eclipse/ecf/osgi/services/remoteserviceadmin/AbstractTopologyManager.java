/****************************************************************************
 * Copyright (c) 2010-2011 Composent, Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *   Composent, Inc. - initial API and implementation
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.ecf.osgi.services.remoteserviceadmin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ecf.discovery.IServiceInfo;
import org.eclipse.ecf.internal.osgi.services.remoteserviceadmin.Activator;
import org.eclipse.ecf.internal.osgi.services.remoteserviceadmin.DebugOptions;
import org.eclipse.ecf.internal.osgi.services.remoteserviceadmin.LogUtility;
import org.eclipse.ecf.internal.osgi.services.remoteserviceadmin.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.remoteserviceadmin.EndpointEventListener;
import org.osgi.service.remoteserviceadmin.EndpointListener;
import org.osgi.service.remoteserviceadmin.ImportRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Abstract superclass for topology managers. This abstract superclass provides
 * basic functionality for topology managers to reuse. New topology managers can
 * extend this class to get or customize desired functionality. Alternatively,
 * they can use this class as a guide to implementing desired topology manager
 * behavior. For description of the role of topology managers see the
 * <a href="http://www.osgi.org/download/r4v42/r4.enterprise.pdf">OSGI 4.2
 * Remote Service Admin specification (chap 122)</a>.
 * 
 */
public abstract class AbstractTopologyManager {

	public static final String SERVICE_EXPORTED_INTERFACES_WILDCARD = "*"; //$NON-NLS-1$

	private BundleContext context;

	private ServiceTracker<IServiceInfoFactory, IServiceInfoFactory> serviceInfoFactoryTracker;

	private ServiceTracker remoteServiceAdminTracker;
	private Object remoteServiceAdminTrackerLock = new Object();

	class EndpointRegistrationHolder {
		private final org.osgi.service.remoteserviceadmin.EndpointDescription ed;
		private final ServiceRegistration<IServiceInfo> reg;
		
		EndpointRegistrationHolder(org.osgi.service.remoteserviceadmin.EndpointDescription ed, ServiceRegistration<IServiceInfo> reg) {
			this.ed = ed;
			this.reg = reg;
		}
		
		public void unregister() {
			try {
				this.reg.unregister();
			} catch (Exception e) {
				logError("unregister","Exception in unregistering service info registration",e); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
	
	private final Map<String, List<EndpointRegistrationHolder>> registrations = new HashMap<String, List<EndpointRegistrationHolder>>();
	private final Lock registrationLock;

	private boolean requireServiceExportedConfigs = new Boolean(System.getProperty(
			"org.eclipse.ecf.osgi.services.remoteserviceadmin.AbstractTopologyManager.requireServiceExportedConfigs", //$NON-NLS-1$
			"false")).booleanValue(); //$NON-NLS-1$

	public AbstractTopologyManager(BundleContext context) {
		serviceInfoFactoryTracker = new ServiceTracker(context, createISIFFilter(context), null);
		serviceInfoFactoryTracker.open();
		this.context = context;
		// Use a FAIR lock here to guarantee that an endpoint removed operation
		// for EP x never executes before its corresponding endpoint added op
		// for EP x.
		// This might happen for an unfair lock (e.g. synchronized) because it
		// doesn't maintain ordering of the waiting threads.
		this.registrationLock = new ReentrantLock(true);
	}

	protected BundleContext getContext() {
		return context;
	}

	protected String getFrameworkUUID() {
		Activator a = Activator.getDefault();
		if (a == null)
			return null;
		return a.getFrameworkUUID();
	}

	public void close() {
		registrationLock.lock();
		try {
			for(Iterator<List<EndpointRegistrationHolder>> i = registrations.values().iterator(); i.hasNext(); ) {
				for(Iterator<EndpointRegistrationHolder> j = i.next().iterator(); i.hasNext(); ) {
					// call unregister
					j.next().unregister();
					j.remove();
				}
				i.remove();
			}
		} finally {
			registrationLock.unlock();
		}
		synchronized (remoteServiceAdminTrackerLock) {
			if (remoteServiceAdminTracker != null) {
				remoteServiceAdminTracker.close();
				remoteServiceAdminTracker = null;
			}
		}
		context = null;
	}

	protected void logWarning(String methodName, String message) {
		LogUtility.logWarning(methodName, DebugOptions.TOPOLOGY_MANAGER, this.getClass(), message);
	}

	protected Filter createRSAFilter() {
		String filterString = "(&(" //$NON-NLS-1$
				+ org.osgi.framework.Constants.OBJECTCLASS + "=" //$NON-NLS-1$
				+ org.osgi.service.remoteserviceadmin.RemoteServiceAdmin.class.getName() + ")(" //$NON-NLS-1$
				+ org.eclipse.ecf.osgi.services.remoteserviceadmin.RemoteServiceAdmin.SERVICE_PROP + "=*))"; //$NON-NLS-1$
		try {
			return getContext().createFilter(filterString);
		} catch (InvalidSyntaxException doesNotHappen) {
			// Should never happen
			doesNotHappen.printStackTrace();
			return null;
		}
	}

	/**
	 * @param ctx the bundle context
	 * @return Filter the created filter
	 * @since 4.0
	 */
	protected Filter createISIFFilter(BundleContext ctx) {
		String filterString = "(" //$NON-NLS-1$
				+ org.osgi.framework.Constants.OBJECTCLASS + "=" //$NON-NLS-1$
				+ IServiceInfoFactory.class.getName() + ")"; //$NON-NLS-1$
		try {
			return ctx.createFilter(filterString);
		} catch (InvalidSyntaxException doesNotHappen) {
			// Should never happen
			doesNotHappen.printStackTrace();
			return null;
		}
	}

	protected org.osgi.service.remoteserviceadmin.RemoteServiceAdmin getRemoteServiceAdmin() {
		synchronized (remoteServiceAdminTrackerLock) {
			if (remoteServiceAdminTracker == null) {
				remoteServiceAdminTracker = new ServiceTracker(Activator.getContext(), createRSAFilter(), null);
				remoteServiceAdminTracker.open();
			}
		}
		return (org.osgi.service.remoteserviceadmin.RemoteServiceAdmin) remoteServiceAdminTracker.getService();
	}

	private void addRegistration(org.osgi.service.remoteserviceadmin.EndpointDescription ed,
			ServiceRegistration<IServiceInfo> reg) {
		List<EndpointRegistrationHolder> ehs = this.registrations.get(ed.getId());
		if (ehs == null) {
			ehs = new ArrayList<EndpointRegistrationHolder>();
		}
		ehs.add(new EndpointRegistrationHolder(ed,reg));
		this.registrations.put(ed.getId(),ehs);
	}

	private Map<String,Object> removeOSGiProperties(Map<String,Object> m) {
		HashMap<String,Object> result = new HashMap<String,Object>(m);
		for(Iterator<String> i = result.keySet().iterator(); i.hasNext(); ) {
			String key = i.next();
			if (PropertiesUtil.isOSGiProperty(key) || PropertiesUtil.isECFProperty(key)) {
				i.remove();
			}
		}
		return result;
	}
	/**
	 * @since 4.9
	 */
	protected boolean hasDescription(org.osgi.service.remoteserviceadmin.EndpointDescription ed) {
		Map<String, Object> edProps = removeOSGiProperties(ed.getProperties());
		List<EndpointRegistrationHolder> ehs = this.registrations.get(ed.getId());
		if (ehs != null) {
			for(int i = ehs.size()-1; i >= 0; i--) {
				Map<String,Object> ehsProps = removeOSGiProperties(ehs.get(i).ed.getProperties());
				if (edProps.equals(ehsProps)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param endpointDescription endpoint description
	 * @since 4.1
	 */
	protected void advertiseModifyEndpointDescription(
			org.osgi.service.remoteserviceadmin.EndpointDescription endpointDescription) {
		this.registrationLock.lock();
		try {
			final IServiceInfoFactory service = serviceInfoFactoryTracker.getService();
			if (service != null) {
				if (!hasDescription(endpointDescription)) {
					final IServiceInfo serviceInfo = service.createServiceInfo(null, endpointDescription);
					if (serviceInfo != null) {
						trace("advertiseModifyEndpointDescription", //$NON-NLS-1$
								"advertising modify endpointDescription=" + endpointDescription + //$NON-NLS-1$
										" and IServiceInfo " + serviceInfo); //$NON-NLS-1$

						final ServiceRegistration<IServiceInfo> registerService = this.context
								.registerService(IServiceInfo.class, serviceInfo, null);

						addRegistration(endpointDescription, registerService);
					} else {
						logError("advertiseModifyEndpointDescription", //$NON-NLS-1$
								"IServiceInfoFactory failed to convert EndpointDescription " + endpointDescription); //$NON-NLS-1$ 1
					}
				}
			} else {
				logError("advertiseModifyEndpointDescription", //$NON-NLS-1$
						"no IServiceInfoFactory service found"); //$NON-NLS-1$
			}
		} finally {
			this.registrationLock.unlock();
		}
	}

	/**
	 * @param endpointDescription endpoint description
	 * @since 3.0
	 */
	protected void advertiseEndpointDescription(
			org.osgi.service.remoteserviceadmin.EndpointDescription endpointDescription) {
		this.registrationLock.lock();
		try {
			if (this.registrations.containsKey(endpointDescription.getId())) {
				return;
			}
			final IServiceInfoFactory service = serviceInfoFactoryTracker.getService();
			if (service != null) {
				final IServiceInfo serviceInfo = service.createServiceInfo(null, endpointDescription);
				if (serviceInfo != null) {
					trace("advertiseEndpointDescription", //$NON-NLS-1$
							"advertising endpointDescription=" + endpointDescription + //$NON-NLS-1$
									" and IServiceInfo " + serviceInfo); //$NON-NLS-1$

					final ServiceRegistration<IServiceInfo> registerService = this.context
							.registerService(IServiceInfo.class, serviceInfo, null);
					
					addRegistration(endpointDescription, registerService);

				} else {
					logError("advertiseEndpointDescription", //$NON-NLS-1$
							"IServiceInfoFactory failed to convert EndpointDescription " + endpointDescription); //$NON-NLS-1$ 1
				}
			} else {
				logError("advertiseEndpointDescription", //$NON-NLS-1$
						"no IServiceInfoFactory service found"); //$NON-NLS-1$
			}
		} finally {
			this.registrationLock.unlock();
		}
	}

	/**
	 * @param endpointDescription endpoint description
	 * @since 3.0
	 */
	protected void unadvertiseEndpointDescription(
			org.osgi.service.remoteserviceadmin.EndpointDescription endpointDescription) {
		this.registrationLock.lock();
		try {
			final List<EndpointRegistrationHolder> ehs = this.registrations
					.remove(endpointDescription.getId());
			if (ehs != null) {
				ehs.forEach(eh -> eh.unregister());
			}
		} finally {
			this.registrationLock.unlock();
		}
	}

	protected void logError(String methodName, String message, Throwable exception) {
		LogUtility.logError(methodName, DebugOptions.TOPOLOGY_MANAGER, this.getClass(), message, exception);
	}

	protected void logError(String methodName, String message, IStatus result) {
		LogUtility.logError(methodName, DebugOptions.TOPOLOGY_MANAGER, this.getClass(), result);
	}

	protected void trace(String methodName, String message) {
		LogUtility.trace(methodName, DebugOptions.TOPOLOGY_MANAGER, this.getClass(), message);
	}

	protected void logError(String methodName, String message) {
		LogUtility.logError(methodName, DebugOptions.TOPOLOGY_MANAGER, this.getClass(), message);
	}

	/**
	 * @param endpointDescription endpoint description
	 * @since 3.0
	 */
	protected void handleECFEndpointAdded(EndpointDescription endpointDescription) {
		trace("handleECFEndpointAdded", "endpointDescription=" //$NON-NLS-1$ //$NON-NLS-2$
				+ endpointDescription);
		// Import service
		RemoteServiceAdmin rsa = (RemoteServiceAdmin) getRemoteServiceAdmin();
		if (rsa != null)
			rsa.importService(endpointDescription);
	}

	/**
	 * @param endpointDescription endpoint description
	 * @since 3.0
	 */
	protected void handleECFEndpointRemoved(
			org.osgi.service.remoteserviceadmin.EndpointDescription endpointDescription) {
		trace("handleECFEndpointRemoved", "endpointDescription=" //$NON-NLS-1$ //$NON-NLS-2$
				+ endpointDescription);
		RemoteServiceAdmin rsa = (RemoteServiceAdmin) getRemoteServiceAdmin();
		if (rsa != null) {
			List<RemoteServiceAdmin.ImportRegistration> importedRegistrations = rsa.getImportedRegistrations();
			org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription ed = (org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription) endpointDescription;
			for (RemoteServiceAdmin.ImportRegistration importedRegistration : importedRegistrations) {
				if (importedRegistration.match(ed)) {
					trace("handleEndpointRemoved", "closing importedRegistration=" //$NON-NLS-1$ //$NON-NLS-2$
							+ importedRegistration);
					importedRegistration.close();
				}
			}
		}
	}

	/**
	 * @param endpoint endpoint description
	 * @since 4.1
	 */
	protected void handleECFEndpointModified(EndpointDescription endpoint) {
		trace("handleECFEndpointModified", "endpointDescription=" //$NON-NLS-1$ //$NON-NLS-2$
				+ endpoint);
		RemoteServiceAdmin rsa = (RemoteServiceAdmin) getRemoteServiceAdmin();
		if (rsa != null) {
			List<RemoteServiceAdmin.ImportRegistration> importedRegistrations = rsa.getImportedRegistrations();
			org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription ed = (org.eclipse.ecf.osgi.services.remoteserviceadmin.EndpointDescription) endpoint;
			for (RemoteServiceAdmin.ImportRegistration importedRegistration : importedRegistrations) {
				if (importedRegistration.match(ed)) {
					trace("handleECFEndpointModified", "updating importedRegistration=" //$NON-NLS-1$ //$NON-NLS-2$
							+ importedRegistration);
					importedRegistration.update(endpoint);
				}
			}
		}
	}

	/**
	 * @param listener            listener
	 * @param endpointDescription endpoint description
	 * @since 4.9
	 */
	protected void handleNonECFEndpointAdded(EndpointListener listener,
			org.osgi.service.remoteserviceadmin.EndpointDescription endpointDescription) {
		trace("handleNonECFEndpointAdded", "ed=" + endpointDescription); //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * @param listener            listener
	 * @param endpointDescription endpoint description
	 * @since 4.9
	 */
	protected void handleNonECFEndpointRemoved(EndpointListener listener,
			org.osgi.service.remoteserviceadmin.EndpointDescription endpointDescription) {
		trace("handleNonECFEndpointRemoved", "ed=" + endpointDescription); //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * @param basicTopologyManagerImpl basic topology manager
	 * @param endpointDescription      endpointDescription
	 * @since 4.9
	 */
	protected void handleNonECFEndpointModified(EndpointEventListener basicTopologyManagerImpl,
			org.osgi.service.remoteserviceadmin.EndpointDescription endpointDescription) {
		trace("handleNonECFEndpointModified", "ed=" + endpointDescription); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @param endpointDescription endpoint description
	 * @param matchedFilter       matched filter
	 * @since 3.0
	 */
	protected void handleNonECFEndpointRemoved(
			org.osgi.service.remoteserviceadmin.EndpointDescription endpointDescription, String matchedFilter) {
		advertiseEndpointDescription(endpointDescription);
	}

	/**
	 * @param result              result
	 * @param endpointDescription endpoint description
	 * @param advertise           advertise
	 * @since 3.0
	 */
	protected void handleAdvertisingResult(IStatus result,
			org.osgi.service.remoteserviceadmin.EndpointDescription endpointDescription, boolean advertise) {
		if (!result.isOK())
			logError("handleAdvertisingResult", //$NON-NLS-1$
					(advertise ? "Advertise" : "Unadvertise") + " of endpointDescription=" + endpointDescription //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							+ " FAILED", //$NON-NLS-1$
					result);
	}

	protected void handleInvalidImportRegistration(ImportRegistration importRegistration, Throwable t) {
		logError("handleInvalidImportRegistration", "importRegistration=" //$NON-NLS-1$ //$NON-NLS-2$
				+ importRegistration, t);
	}

	/**
	 * @param event     the service event
	 * @param listeners map of listeners
	 * @since 3.0
	 */
	protected void handleEvent(ServiceEvent event, Map listeners) {
		switch (event.getType()) {
		case ServiceEvent.MODIFIED:
			handleServiceModifying(event.getServiceReference());
			break;
		case ServiceEvent.REGISTERED:
			handleServiceRegistering(event.getServiceReference());
			break;
		case ServiceEvent.UNREGISTERING:
			handleServiceUnregistering(event.getServiceReference());
			break;
		default:
			break;
		}
	}

	protected void handleServiceRegistering(ServiceReference serviceReference) {
		// Using OSGI 5 Chap 13 Remote Services spec, get the specified remote
		// interfaces for the given service reference
		String[] exportedInterfaces = PropertiesUtil.getExportedInterfaces(serviceReference);
		// If no remote interfaces set, then we don't do anything with it
		if (exportedInterfaces == null)
			return;

		// Get serviceExportedConfigs property
		String[] serviceExportedConfigs = PropertiesUtil.getStringArrayFromPropertyValue(serviceReference
				.getProperty(org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_CONFIGS));
		// If requireServiceExportedConfigs is set to true (default is false) then if
		// serviceExportedConfigs
		// is null/not set, then we don't do anything with this service registration
		if (requireServiceExportedConfigs
				&& (serviceExportedConfigs == null || Arrays.asList(serviceExportedConfigs).size() == 0))
			return;
		// If we get this far, then we are going to export it
		// prepare export properties
		Map<String, Object> exportProperties = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
		exportProperties.put(org.osgi.service.remoteserviceadmin.RemoteConstants.SERVICE_EXPORTED_INTERFACES,
				exportedInterfaces);
		trace("handleServiceRegistering", "serviceReference=" //$NON-NLS-1$ //$NON-NLS-2$
				+ serviceReference + " exportProperties=" + exportProperties); //$NON-NLS-1$
		org.osgi.service.remoteserviceadmin.RemoteServiceAdmin rsa = getRemoteServiceAdmin();
		// Do the export with RSA
		if (rsa != null)
			rsa.exportService(serviceReference, exportProperties);
	}

	protected void handleServiceModifying(ServiceReference serviceReference) {
		RemoteServiceAdmin rsa = (RemoteServiceAdmin) getRemoteServiceAdmin();
		if (rsa != null) {
			List<RemoteServiceAdmin.ExportRegistration> exportedRegistrations = rsa.getExportedRegistrations();
			for (RemoteServiceAdmin.ExportRegistration exportedRegistration : exportedRegistrations) {
				if (exportedRegistration.match(serviceReference)) {
					trace("handleServiceModifying", "modifying exportRegistration for serviceReference=" //$NON-NLS-1$ //$NON-NLS-2$
							+ serviceReference);
					EndpointDescription updatedED = (EndpointDescription) exportedRegistration.update(null);
					if (updatedED == null)
						logWarning("handleServiceModifying", "ExportRegistration.update failed with exception=" //$NON-NLS-1$//$NON-NLS-2$
								+ exportedRegistration.getException());
				}
			}
		}
	}

	protected void handleServiceUnregistering(ServiceReference serviceReference) {
		RemoteServiceAdmin rsa = (RemoteServiceAdmin) getRemoteServiceAdmin();
		if (rsa != null) {
			List<RemoteServiceAdmin.ExportRegistration> exportedRegistrations = ((RemoteServiceAdmin) getRemoteServiceAdmin())
					.getExportedRegistrations();
			for (RemoteServiceAdmin.ExportRegistration exportedRegistration : exportedRegistrations) {
				if (exportedRegistration.match(serviceReference)) {
					trace("handleServiceUnregistering", "closing exportRegistration for serviceReference=" //$NON-NLS-1$ //$NON-NLS-2$
							+ serviceReference);
					exportedRegistration.close();
				}
			}
		}
	}
}
