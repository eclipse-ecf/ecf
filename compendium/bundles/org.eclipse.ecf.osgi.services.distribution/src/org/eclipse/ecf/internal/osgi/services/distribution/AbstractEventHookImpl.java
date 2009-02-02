/*******************************************************************************
 * Copyright (c) 2009 EclipseSource and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.internal.osgi.services.distribution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ecf.core.util.Trace;
import org.eclipse.ecf.osgi.services.distribution.ServiceConstants;
import org.eclipse.ecf.remoteservice.IRemoteServiceRegistration;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventHook;

public abstract class AbstractEventHookImpl implements EventHook {

	private final static String[] EMPTY_STRING_ARRAY = new String[0];

	private final DistributionProviderImpl distributionProvider;

	private final Map remoteServiceReferences = new HashMap();

	public AbstractEventHookImpl(DistributionProviderImpl distributionProvider) {
		this.distributionProvider = distributionProvider;
	}

	public void event(ServiceEvent event, Collection contexts) {
		switch (event.getType()) {
		case ServiceEvent.MODIFIED:
			handleModifiedServiceEvent(event.getServiceReference(), contexts);
			break;
		case ServiceEvent.MODIFIED_ENDMATCH:
			break;
		case ServiceEvent.REGISTERED:
			handleRegisteredServiceEvent(event.getServiceReference(), contexts);
			break;
		case ServiceEvent.UNREGISTERING:
			handleUnregisteringServiceEvent(event.getServiceReference(),
					contexts);
			break;
		default:
			break;
		}
	}

	protected void handleRegisteredServiceEvent(
			ServiceReference serviceReference, Collection contexts) {
		// This checks to see if the serviceReference has any remote interfaces
		// declared via
		// osgi.remote.interfaces
		Object osgiRemotes = serviceReference
				.getProperty(ServiceConstants.OSGI_REMOTE_INTERFACES);
		// If osgi.remote.interfaces required property is non-null then we
		// handle further, if null then ignore
		if (osgiRemotes != null) {
			// The osgiRemotes should be of type String [] according to
			// RFC119...if it's not String [] we ignore
			String[] remoteInterfacesArr = (String[]) ((osgiRemotes instanceof String[]) ? osgiRemotes
					: null);
			if (remoteInterfacesArr == null) {
				trace("handleRegisteredServiceEvent",
						"remoteInterfaces not of String [] type as required by RFC 119");
				return;
			}
			trace("handleRegisteredServiceEvent", "serviceReference="
					+ serviceReference + " has remoteInterfaces="
					+ Arrays.asList(remoteInterfacesArr));
			// We compare the osgi.remote.interfaces with those exposed by the
			// service reference and
			// make sure that expose some common interfaces
			String[] remoteInterfaces = (remoteInterfacesArr != null) ? getInterfacesForServiceReference(
					remoteInterfacesArr, serviceReference)
					: null;
			if (remoteInterfaces == null) {
				trace("handleRegisteredServiceEvent",
						"No exposed remoteInterfaces found for serviceReference="
								+ serviceReference);
				return;
			}
			// Now get optional service property osgi.remote.configuration.type
			Object osgiRemoteConfigurationType = serviceReference
					.getProperty(ServiceConstants.OSGI_REMOTE_CONFIGURATION_TYPE);
			// The osgiRemoteConfigurationType is optional and can be null. If
			// non-null, it should be of type String [] according to RFC119...if
			// it's non-null and not String [] we ignore
			String[] remoteConfigurationType = null;
			if (osgiRemoteConfigurationType != null) {
				if (!(osgiRemoteConfigurationType instanceof String[])) {
					trace("handleRegisteredServiceEvent",
							"osgi.remote.configuration.type is not String[] as required by RFC 119");
					return;
				}
				remoteConfigurationType = (String[]) osgiRemoteConfigurationType;
			}
			// Now call registerRemoteService
			registerRemoteService(serviceReference, remoteInterfaces,
					remoteConfigurationType);
		}
	}

	protected abstract void registerRemoteService(
			ServiceReference serviceReference, String[] remoteInterfaces,
			String[] remoteConfigurationType);

	protected void fireRemoteServiceRegistered(
			ServiceReference serviceReference,
			IRemoteServiceRegistration remoteServiceRegistration) {
		remoteServiceReferences
				.put(serviceReference, remoteServiceRegistration);
		distributionProvider.addExposedService(serviceReference);
	}

	private String[] getInterfacesForServiceReference(
			String[] remoteInterfaces, ServiceReference serviceReference) {
		if (remoteInterfaces == null || remoteInterfaces.length == 0)
			return EMPTY_STRING_ARRAY;
		List results = new ArrayList();
		List interfaces = Arrays.asList((String[]) serviceReference
				.getProperty(Constants.OBJECTCLASS));
		for (int i = 0; i < remoteInterfaces.length; i++) {
			String intf = remoteInterfaces[i];
			if (ServiceConstants.OSGI_REMOTE_INTERFACES_WILDCARD.equals(intf))
				return (String[]) interfaces.toArray(new String[] {});
			if (intf != null && interfaces.contains(intf))
				results.add(intf);
		}
		return (String[]) results.toArray(new String[] {});
	}

	protected void trace(String methodName, String message) {
		Trace.trace(Activator.PLUGIN_ID, DebugOptions.EVENTHOOKDEBUG, this
				.getClass(), methodName, message);
	}

	protected void handleUnregisteringServiceEvent(
			ServiceReference serviceReference, Collection contexts) {
		IRemoteServiceRegistration remoteRegistration = removeRemoteRegistration(serviceReference);
		if (remoteRegistration != null) {
			trace("handleUnregisteringServiceEvent",
					"found serviceRegistration=" + remoteRegistration);
			distributionProvider.removeRemoteService(serviceReference);
			remoteRegistration.unregister();
		}
	}

	private IRemoteServiceRegistration removeRemoteRegistration(
			ServiceReference serviceReference) {
		return (IRemoteServiceRegistration) remoteServiceReferences
				.remove(serviceReference);
	}

	protected void handleModifiedServiceEvent(
			ServiceReference serviceReference, Collection contexts) {
		// TODO
	}

	protected Object getService(ServiceReference sr) {
		return Activator.getDefault().getContext().getService(sr);
	}

}
