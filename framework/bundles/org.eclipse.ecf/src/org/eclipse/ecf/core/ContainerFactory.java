/*******************************************************************************
 * Copyright (c) 2004, 2007 Composent, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.Status;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.provider.IContainerInstantiator;
import org.eclipse.ecf.core.util.Trace;
import org.eclipse.ecf.internal.core.ECFDebugOptions;
import org.eclipse.ecf.internal.core.ECFPlugin;
import org.eclipse.ecf.internal.core.IDisposable;
import org.eclipse.ecf.internal.core.Messages;

/**
 * Factory for creating {@link IContainer} instances. This class provides ECF
 * clients an entry point to constructing {@link IContainer} instances. <br>
 * <br>
 * Here is an example use of the ContainerFactory to construct an instance of
 * the 'standalone' container (has no connection to other containers): <br>
 * <br>
 * <code>
 * 	    IContainer container = <br>
 * 			ContainerFactory.getDefault().createContainer("ecf.generic.client");
 *      <br><br>
 *      ...further use of container here...
 * </code> For more details on the creation
 * and lifecycle of IContainer instances created via this factory see
 * {@link IContainer}.
 * 
 * @see IContainer
 * @see IContainerFactory
 */
public class ContainerFactory implements IContainerFactory, IContainerManager {

	public static final String BASE_CONTAINER_NAME = Messages.ContainerFactory_Base_Container_Name;

	private static final Map containerdescriptions = new HashMap();

	private static final Map containers = new HashMap();

	private static final List managerListeners = new ArrayList();
	
	private static IContainerFactory instance = null;

	static {
		instance = new ContainerFactory();
	}

	public static IContainerFactory getDefault() {
		return instance;
	}

	protected ContainerFactory() {
		ECFPlugin.getDefault().addDisposable(new IDisposable() {
			public void dispose() {
				synchronized (containers) {
					for (Iterator i = containers.keySet().iterator(); i
							.hasNext();) {
						IContainer c = (IContainer) containers
								.get(i.next());
						try {
							c.dispose();
						} catch (Throwable e) {
							// Log exception
							ECFPlugin.getDefault().log(
									new Status(Status.ERROR, ECFPlugin
											.getDefault().getBundle()
											.getSymbolicName(),
											Status.ERROR,
											"container dispose error", e)); //$NON-NLS-1$
							Trace.catching(ECFPlugin.PLUGIN_ID,
									ECFDebugOptions.EXCEPTIONS_CATCHING,
									ContainerFactory.class, "doDispose", e); //$NON-NLS-1$
						}
					}
					containers.clear();
				}
				containerdescriptions.clear();
				managerListeners.clear();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerManager#addContainer(org.eclipse.ecf.core.IContainer)
	 */
	public IContainer addContainer(IContainer container) {
		Assert.isNotNull(container);
		ID containerID = container.getID();
		Assert.isNotNull(containerID,
				Messages.ContainerFactory_EXCEPTION_CONTAINER_ID_NOT_NULL);
	    IContainer result = null;
	    synchronized (containers) {
	    	result = (IContainer) containers.put(containerID, container);
	    }
	    if (result == null) fireContainerAdded(container);
	    return result;
	}

	/**
	 * @param result
	 */
	private void fireContainerAdded(IContainer result) {
		List toNotify = null;
		synchronized (managerListeners) {
			toNotify = new ArrayList(managerListeners);
		}
		for(Iterator i=toNotify.iterator(); i.hasNext(); ) {
			IContainerManagerListener cml = (IContainerManagerListener) i.next();
			cml.containerAdded(result);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerManager#removeContainer(org.eclipse.ecf.core.IContainer)
	 */
	public IContainer removeContainer(IContainer container) {
		Assert.isNotNull(container);
		ID containerID = container.getID();
		if (containerID == null)
			return null;
		IContainer result = null;
		synchronized (containers) {
			result = (IContainer) containers.remove(containerID);
		}
		if (result != null) fireContainerRemoved(result);
		return result;
	}

	/**
	 * @param result
	 */
	private void fireContainerRemoved(IContainer result) {
		List toNotify = null;
		synchronized (managerListeners) {
			toNotify = new ArrayList(managerListeners);
		}
		for(Iterator i=toNotify.iterator(); i.hasNext(); ) {
			IContainerManagerListener cml = (IContainerManagerListener) i.next();
			cml.containerRemoved(result);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerFactory#addDescription(org.eclipse.ecf.core.ContainerTypeDescription)
	 */
	public ContainerTypeDescription addDescription(ContainerTypeDescription scd) {
		return addDescription0(scd);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerFactory#getDescriptions()
	 */
	public List getDescriptions() {
		return getDescriptions0();
	}

	protected List getDescriptions0() {
		synchronized (containerdescriptions) {
			return new ArrayList(containerdescriptions.values());			
		}
	}

	protected ContainerTypeDescription addDescription0(
			ContainerTypeDescription n) {
		if (n == null)
			return null;
		synchronized (containerdescriptions) {
			return (ContainerTypeDescription) containerdescriptions.put(
					n.getName(), n);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerFactory#containsDescription(org.eclipse.ecf.core.ContainerTypeDescription)
	 */
	public boolean containsDescription(ContainerTypeDescription scd) {
		return containsDescription0(scd);
	}

	protected boolean containsDescription0(ContainerTypeDescription scd) {
		if (scd == null)
			return false;
		synchronized (containerdescriptions) {
			return containerdescriptions.containsKey(scd.getName());
		}
	}

	protected ContainerTypeDescription getDescription0(
			ContainerTypeDescription scd) {
		if (scd == null)
			return null;
		synchronized (containerdescriptions) {
			return (ContainerTypeDescription) containerdescriptions.get(scd
					.getName());	
		}
	}

	protected ContainerTypeDescription getDescription0(String name) {
		if (name == null)
			return null;
		synchronized (containerdescriptions) {
			return (ContainerTypeDescription) containerdescriptions.get(name);			
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerFactory#getDescriptionByName(java.lang.String)
	 */
	public ContainerTypeDescription getDescriptionByName(String name) {
		return getDescription0(name);
	}

	protected void throwContainerCreateException(String message,
			Throwable cause, String method) throws ContainerCreateException {
		ContainerCreateException except = (cause == null) ? new ContainerCreateException(
				message)
				: new ContainerCreateException(message, cause);
		Trace.throwing(ECFPlugin.PLUGIN_ID,
				ECFDebugOptions.EXCEPTIONS_THROWING, ContainerFactory.class,
				method, except);
		throw except;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerFactory#createContainer()
	 */
	public IContainer createContainer() throws ContainerCreateException {
		return createContainer(BASE_CONTAINER_NAME);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerFactory#createContainer(org.eclipse.ecf.core.ContainerTypeDescription,
	 *      java.lang.Object[])
	 */
	public IContainer createContainer(ContainerTypeDescription description,
			Object[] parameters) throws ContainerCreateException {
		String method = "createContainer"; //$NON-NLS-1$
		Trace.entering(ECFPlugin.PLUGIN_ID, ECFDebugOptions.METHODS_ENTERING,
				ContainerFactory.class, method, new Object[] { description,
						Trace.getArgumentsString(parameters) });
		if (description == null)
			throwContainerCreateException(
					Messages.ContainerFactory_Exception_Create_Container, null,
					method);
		ContainerTypeDescription cd = getDescription0(description);
		if (cd == null)
			throwContainerCreateException("ContainerTypeDescription '" //$NON-NLS-1$
					+ description.getName() + "' not found", null, method); //$NON-NLS-1$
		IContainerInstantiator instantiator = null;
		try {
			instantiator = cd.getInstantiator();
		} catch (Exception e) {
			throwContainerCreateException(
					"createContainer cannot get IContainerInstantiator for description : " //$NON-NLS-1$
							+ description, e, method);
		}
		// Ask instantiator to actually create instance
		IContainer container = instantiator.createInstance(description,
				parameters);
		if (container == null)
			throwContainerCreateException("Instantiator returned null for '" //$NON-NLS-1$
					+ cd.getName() + "'", null, method); //$NON-NLS-1$
		// Add to containers map if container.getID() provides a valid value.
		ID containerID = container.getID();
		if (containerID != null)
			addContainer(container);
		Trace.exiting(ECFPlugin.PLUGIN_ID, ECFDebugOptions.METHODS_EXITING,
				ContainerFactory.class, method, container);
		return container;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerFactory#createContainer(java.lang.String)
	 */
	public IContainer createContainer(String name)
			throws ContainerCreateException {
		return createContainer(getDescriptionByName(name), null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerFactory#createContainer(java.lang.String,
	 *      java.lang.Object[])
	 */
	public IContainer createContainer(String name,
			Object[] parameters) throws ContainerCreateException {
		return createContainer(getDescriptionByName(name),
				parameters);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerFactory#removeDescription(org.eclipse.ecf.core.ContainerTypeDescription)
	 */
	public ContainerTypeDescription removeDescription(
			ContainerTypeDescription scd) {
		return removeDescription0(scd);

	}

	protected ContainerTypeDescription removeDescription0(
			ContainerTypeDescription n) {
		if (n == null)
			return null;
		synchronized (containerdescriptions) {
			return (ContainerTypeDescription) containerdescriptions.remove(n
					.getName());			
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerFactory#getDescriptionsForContainerAdapter(java.lang.Class)
	 */
	public ContainerTypeDescription[] getDescriptionsForContainerAdapter(
			Class containerAdapter) {
		if (containerAdapter == null)
			throw new NullPointerException(
					Messages.ContainerFactory_Exception_Adapter_Not_Null);
		List result = new ArrayList();
		List descriptions = getDescriptions();
		for (Iterator i = descriptions.iterator(); i.hasNext();) {
			ContainerTypeDescription description = (ContainerTypeDescription) i
					.next();
			String[] supportedAdapters = description.getSupportedAdapterTypes();
			if (supportedAdapters != null) {
				for (int j = 0; j < supportedAdapters.length; j++) {
					if (supportedAdapters[j].equals(containerAdapter.getName()))
						result.add(description);
				}
			}
		}
		return (ContainerTypeDescription[]) result
				.toArray(new ContainerTypeDescription[] {});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerManager#getAllContainers()
	 */
	public IContainer[] getAllContainers() {
		List containersList = null;
		synchronized (containers) {
			containersList = new ArrayList(containers.values());
		}
		return (IContainer[]) containersList.toArray(new IContainer[] {});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerManager#getContainer(org.eclipse.ecf.core.identity.ID)
	 */
	public IContainer getContainer(ID containerID) {
		if (containerID == null)
			return null;
		synchronized (containers) {
			for (Iterator i = containers.keySet().iterator(); i.hasNext();) {
				IContainer container = (IContainer) containers.get(containerID);
				if (container != null)
					return container;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.IContainerManager#hasContainer(org.eclipse.ecf.core.identity.ID)
	 */
	public boolean hasContainer(ID containerID) {
		Assert.isNotNull(containerID);
		synchronized (containers) {
			return containers.containsKey(containerID);			
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.core.IContainerManager#addListener(org.eclipse.ecf.core.IContainerManagerListener)
	 */
	public boolean addListener(IContainerManagerListener listener) {
		Assert.isNotNull(listener);
		synchronized (managerListeners) {
			return managerListeners.add(listener);			
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ecf.core.IContainerManager#removeListener(org.eclipse.ecf.core.IContainerManagerListener)
	 */
	public boolean removeListener(IContainerManagerListener listener) {
		Assert.isNotNull(listener);
		synchronized (managerListeners) {
			return managerListeners.remove(listener);
		}
	}
}