/****************************************************************************
 * Copyright (c) 2004 Composent, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *****************************************************************************/

package org.eclipse.ecf.remoteservice.util;

import org.eclipse.ecf.remoteservice.IRemoteServiceReference;

/**
 * The <code>IRemoteServiceTrackerCustomizer</code> interface allows a
 * <code>RemoteServiceTracker</code> object to customize the service objects that
 * are tracked. The <code>IRemoteServiceTrackerCustomizer</code> object is called
 * when a service is being added to the <code>RemoteServiceTracker</code> object.
 * The <code>IRemoteServiceTrackerCustomizer</code> can then return an object for the
 * tracked service. The <code>IRemoteServiceTrackerCustomizer</code> object is also
 * called when a tracked service is modified or has been removed from the
 * <code>RemoteServiceTracker</code> object.
 * 
 * <p>
 * The methods in this interface may be called as the result of a
 * <code>IRemoteServiceEvent</code> being received by a <code>RemoteServiceTracker</code>
 * object. Since <code>IRemoteServiceEvent</code> s are synchronously delivered by
 * the Framework, it is highly recommended that implementations of these methods
 * do not register (<code>IRemoteServiceContainerAdapter.registerService</code>), modify (
 * <code>IRemoteServiceRegistration.setProperties</code>) or unregister (
 * <code>IRemoteServiceRegistration.unregister</code>) a service while being
 * synchronized on any object.
 * 
 * <p>
 * The <code>RemoteServiceTracker</code> class is thread-safe. It does not call a
 * <code>IRemoteServiceTrackerCustomizer</code> object while holding any locks.
 * <code>IRemoteServiceTrackerCustomizer</code> implementations must also be
 * thread-safe.
 * 
 * @ThreadSafe
 */
public interface IRemoteServiceTrackerCustomizer {
	/**
	 * A service is being added to the <code>RemoteServiceTracker</code> object.
	 * 
	 * <p>
	 * This method is called before a service which matched the search
	 * parameters of the <code>RemoteServiceTracker</code> object is added to it.
	 * This method should return the service object to be tracked for this
	 * <code>IRemoteServiceReference</code> object. The returned service object is
	 * stored in the <code>RemoteServiceTracker</code> object and is available from
	 * the <code>getRemoteService</code> and <code>getRemoteServices</code> methods.
	 * 
	 * @param reference Reference to service being added to the
	 *        <code>RemoteServiceTracker</code> object.
	 * @return The service object to be tracked for the
	 *         <code>IRemoteServiceReference</code> object or <code>null</code> if
	 *         the <code>IRemoteServiceReference</code> object should not be tracked.
	 */
	public Object addingService(IRemoteServiceReference reference);

	/**
	 * A service tracked by the <code>RemoteServiceTracker</code> object has been
	 * modified.
	 * 
	 * <p>
	 * This method is called when a service being tracked by the
	 * <code>RemoteServiceTracker</code> object has had it properties modified.
	 * 
	 * @param reference IRemoteServiceReference to service that has been modified.
	 * @param service The service object for the modified service.
	 */
	public void modifiedService(IRemoteServiceReference reference, Object service);

	/**
	 * A service tracked by the <code>RemoteServiceTracker</code> object has been
	 * removed.
	 * 
	 * <p>
	 * This method is called after a service is no longer being tracked by the
	 * <code>RemoteServiceTracker</code> object.
	 * 
	 * @param reference IRemoteServiceReference to service that has been removed.
	 * @param service The service object for the removed service.
	 */
	public void removedService(IRemoteServiceReference reference, Object service);
}
