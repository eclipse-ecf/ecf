/****************************************************************************
 * Copyright (c) 2014 Composent, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Composent, Inc. - initial API and implementation
 *****************************************************************************/
package org.eclipse.ecf.remoteservice.asyncproxy;

import java.lang.reflect.Method;
import java.util.concurrent.Future;

import org.eclipse.equinox.concurrent.future.IFuture;

public abstract class AbstractAsyncProxyRemoteService {

	@SuppressWarnings("rawtypes")
	protected abstract IFuture callAsync(AbstractAsyncProxyRemoteCall call);
	
	@SuppressWarnings("rawtypes")
	protected abstract Future callFutureAsync(AbstractAsyncProxyRemoteCall call);
	
	protected abstract void callCompletableAsync(AbstractAsyncProxyRemoteCall call, IAsyncProxyCompletable completable);

	protected boolean isReturnAsync(Object proxy, Method method, Object[] args) {
		@SuppressWarnings("rawtypes")
		Class returnType = method.getReturnType();
		return (Future.class.isAssignableFrom(returnType) || IFuture.class.isAssignableFrom(returnType));
	}
	
	protected Object callFuture(AbstractAsyncProxyRemoteCall call, @SuppressWarnings("rawtypes") Class returnType) {
		// Iff it's an IFuture then return
		// IFuture result of callAsync
		if (IFuture.class.isAssignableFrom(returnType))
			return callAsync(call);
		return callFutureAsync(call);
	}

}
