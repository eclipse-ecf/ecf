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
package org.eclipse.ecf.core.sharedobject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.ecf.core.events.IContainerConnectedEvent;
import org.eclipse.ecf.core.events.IContainerDisconnectedEvent;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.sharedobject.events.ISharedObjectActivatedEvent;
import org.eclipse.ecf.core.sharedobject.events.ISharedObjectCommitEvent;
import org.eclipse.ecf.core.sharedobject.events.ISharedObjectCreateResponseEvent;
import org.eclipse.ecf.core.sharedobject.events.ISharedObjectMessageEvent;
import org.eclipse.ecf.core.sharedobject.events.SharedObjectCommitEvent;
import org.eclipse.ecf.core.util.Event;
import org.eclipse.ecf.core.util.IEventProcessor;
import org.eclipse.ecf.core.util.Trace;
import org.eclipse.ecf.internal.core.sharedobject.Activator;
import org.eclipse.ecf.internal.core.sharedobject.SharedObjectDebugOptions;

/**
 * Implementation of two-phase commit for transactional replication of shared
 * objects.
 * 
 * @see ISharedObjectTransactionConfig
 * @see ISharedObjectTransactionParticipantsFilter
 * 
 */
public class TwoPhaseCommitEventProcessor implements IEventProcessor,
		ISharedObjectContainerTransaction {
	BaseSharedObject sharedObject = null;

	byte transactionState = ISharedObjectContainerTransaction.ACTIVE;

	Object lock = new Object();

	List participants = new Vector();

	Map failed = new HashMap();

	int timeout = ISharedObjectTransactionConfig.DEFAULT_TIMEOUT;

	int minFailedToAbort = 0;

	long identifier = 0;

	ISharedObjectTransactionParticipantsFilter participantsFilter = null;

	public TwoPhaseCommitEventProcessor(BaseSharedObject bse,
			ISharedObjectTransactionConfig config) {
		this.sharedObject = bse;
		if (config == null) {
			config = new TransactionSharedObjectConfiguration();
		}
		this.timeout = config.getTimeout();
		this.participantsFilter = config.getParticipantsFilter();
	}

	protected void trace(String msg) {
		Trace.trace(Activator.getDefault(), msg);
	}

	protected void traceStack(String msg, Throwable t) {
		Trace.catching(Activator.getDefault(),
				SharedObjectDebugOptions.EXCEPTIONS_CATCHING,
				TwoPhaseCommitEventProcessor.class, "traceStack", t);
		;
	}

	protected int getTimeout() {
		return timeout;
	}

	protected int getMinFailedToAbort() {
		return minFailedToAbort;
	}

	protected boolean isPrimary() {
		return getSharedObject().isPrimary();
	}

	protected BaseSharedObject getSharedObject() {
		return sharedObject;
	}

	protected ID getHomeID() {
		return getSharedObject().getHomeContainerID();
	}

	protected void addParticipants(ID[] ids) {
		if (ids != null) {
			for (int i = 0; i < ids.length; i++) {
				trace("addParticipant(" + ids[i] + ")");
				if (!getHomeID().equals(ids[i]))
					participants.add(ids[i]);
			}
		}
	}

	protected void removeParticipant(ID id) {
		if (id != null) {
			trace("removeParticipant(" + id + ")");
			participants.remove(id);
		}
	}

	protected void addFailed(ID remote, Throwable failure) {
		if (remote != null && failure != null) {
			trace("addFailed(" + remote + "," + failure + ")");
			failed.put(remote, failure);
		}
	}

	protected ISharedObjectContext getContext() {
		return getSharedObject().getContext();
	}

	/*
	 * Implementation of IEventProcessor. These methods are entry point methods
	 * for BaseSharedObject method dispatch to call
	 */
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ecf.core.util.IEventProcessor#processEvent(org.eclipse.ecf.core.util.Event)
	 */
	public boolean processEvent(Event event) {
		if (event instanceof ISharedObjectActivatedEvent) {
			handleActivated((ISharedObjectActivatedEvent) event);
		} else if (event instanceof IContainerConnectedEvent) {
			handleJoined((IContainerConnectedEvent) event);
		} else if (event instanceof ISharedObjectCreateResponseEvent) {
			handleCreateResponse((ISharedObjectCreateResponseEvent) event);
		} else if (event instanceof IContainerDisconnectedEvent) {
			handleDeparted((IContainerDisconnectedEvent) event);
		} else if (event instanceof ISharedObjectMessageEvent) {
			ISharedObjectMessageEvent some = (ISharedObjectMessageEvent) event;
			Object data = some.getData();
			if (data instanceof ISharedObjectCommitEvent)
				localCommitted();
		}
		// Let other event processors have a shot at this event
		return false;
	}

	protected void handleActivated(ISharedObjectActivatedEvent event) {
		trace("handleActivated(" + event + ")");
		// No other state changes while this is going on
		synchronized (lock) {
			if (isPrimary()) {
				// Primary
				handlePrimaryActivated(event);
			} else {
				handleReplicaActivated(event);
			}
			// Notify any threads waiting on state change
			lock.notifyAll();
		}
	}

	protected void replicateTo(ID[] remotes) {
		getSharedObject().replicateToRemoteContainers(remotes);
	}

	protected void handlePrimaryActivated(ISharedObjectActivatedEvent event) {
		trace("handlePrimaryActivated(" + event + ")");
		// First get current group membership
		if (getContext().getConnectedID() != null) {
			ID[] groupMembers = getContext().getGroupMemberIDs();
			// Now get participants
			ID[] transactionParticipants = null;
			// If there is a participants filter specified then use it and ask
			// it to return an ID [] of participants (given
			// the current group membership
			if (participantsFilter != null) {
				transactionParticipants = participantsFilter
						.filterParticipants(groupMembers);
			}
			// replicate
			if (transactionParticipants == null) {
				// This means that all current group members should be included
				// as participants
				replicateTo(null);
				transactionParticipants = groupMembers;
			} else {
				// This means the participants filter provided us with an ID []
				// and so we replicate only to that ID []
				replicateTo(transactionParticipants);
			}

			// Add participants to the collection
			addParticipants(transactionParticipants);
			// Now set transaction state to VOTING
			setTransactionState(ISharedObjectContainerTransaction.VOTING);
		} else {
			setTransactionState(ISharedObjectContainerTransaction.COMMITTED);
		}
	}

	private long getNextIdentifier() {
		return identifier++;
	}

	protected void handleReplicaActivated(ISharedObjectActivatedEvent event) {
		trace("handleReplicaActivated(" + event + ")");
		try {
			// Try to respond with create success message back to host
			getContext().sendCreateResponse(getHomeID(), null,
					getNextIdentifier());
			// If above succeeds, we're now in prepared state
			setTransactionState(ISharedObjectContainerTransaction.PREPARED);
		} catch (Exception except) {
			// If throws exception, we're doomed
			traceStack("handleReplicaActivated(" + event + ")", except);
			setTransactionState(ISharedObjectContainerTransaction.ABORTED);
		}
	}

	protected void handleJoined(IContainerConnectedEvent event) {
		trace("handleJoined(" + event + ")");
		// If we are primary then this event matters to us
		if (isPrimary()) {
			// If transactionstate is VOTING then we replicate ourselves to
			// participants
			if (getTransactionState() == ISharedObjectContainerTransaction.VOTING) {
				synchronized (lock) {
					// First send replicate message *no matter what state we are
					// in*
					ID[] newMember = new ID[] { event.getTargetID() };
					replicateTo(newMember);
					addParticipants(newMember);
				}
			}
		}
	}

	protected void handleCreateResponse(ISharedObjectCreateResponseEvent event) {
		trace("handleCreateResponse(" + event + ")");
		if (isPrimary()) {
			synchronized (lock) {
				Throwable except = event.getException();
				ID remoteID = event.getRemoteContainerID();
				long ident = event.getSequence();
				if (getTransactionState() == ISharedObjectContainerTransaction.VOTING) {
					if (except == null) {
						removeParticipant(remoteID);
					} else {
						addFailed(remoteID, except);
					}
				} else {
					handleVotingCompletedCreateResponse(remoteID, except, ident);
				}
				lock.notifyAll();
			}
		} else {
			// we don't care as we are note transaction monitor
		}
	}

	protected void handleDeparted(IContainerDisconnectedEvent event) {
		trace("handleDeparted(" + event + ")");
		if (isPrimary()) {
			ID remoteID = event.getTargetID();
			synchronized (lock) {
				if (getTransactionState() == ISharedObjectContainerTransaction.VOTING) {
					addFailed(remoteID, new Exception("Container " + remoteID
							+ " left"));
				}
				lock.notifyAll();
			}
		} else {
			// we don't care as we are not transaction monitor
		}
	}

	protected void handleVotingCompletedCreateResponse(ID fromID, Throwable e,
			long identifier) {
		trace("handleVotingCompletedCreateResponse(" + fromID + "," + e + ","
				+ identifier + ")");
		// If remote creation was successful, simply send commit message back.
		if (e == null) {
			try {
				getSharedObject().getContext().sendMessage(fromID,
						new SharedObjectCommitEvent(getSharedObject().getID()));
			} catch (Exception e2) {
				traceStack("Exception in sendCommit to " + fromID, e2);
			}
		} else {
			// Too late to vote no
			handlePostCommitFailure(fromID, e, identifier);
		}
	}

	protected void handlePostCommitFailure(ID fromID, Throwable e,
			long identifier) {
		// Do nothing but report
		trace("handlePostCommitFailure(" + fromID + "," + e + "," + identifier
				+ ")");
	}

	protected void sendCommit() throws SharedObjectAddAbortException {
		try {
			getContext().sendMessage(null,
					new SharedObjectCommitEvent(getSharedObject().getID()));
		} catch (Exception e2) {
			doTMAbort(new SharedObjectAddAbortException(
					"Exception sending commit message", e2, getTimeout()));
		}
	}

	public byte getTransactionState() {
		synchronized (lock) {
			return transactionState;
		}
	}

	protected void setTransactionState(byte state) {
		synchronized (lock) {
			transactionState = state;
		}
	}

	public void waitToCommit() throws SharedObjectAddAbortException {
		if (getTransactionState() == ISharedObjectContainerTransaction.COMMITTED)
			return;
		synchronized (lock) {
			long end = System.currentTimeMillis() + getTimeout();
			try {
				while (!isVotingCompleted()) {
					long wait = end - System.currentTimeMillis();
					trace("waitForFinish waiting " + wait + "ms on "
							+ getSharedObject().getID());
					if (wait <= 0L)
						throw new SharedObjectAddAbortException(
								"Timeout adding " + getSharedObject().getID()
										+ " to " + getHomeID(),
								(Throwable) null, getTimeout());
					// Wait right here
					lock.wait(wait);
				}
			} catch (Exception except) {
				// Aborted for some reason. Clean up and throw
				doTMAbort(except);
			}
			// Success. Send commit to remotes and clean up before returning.
			doTMCommit();
		}
	}

	protected void doTMAbort(Throwable except)
			throws SharedObjectAddAbortException {
		trace("doTMAbort:" + except);
		// Set our own state variable to ABORTED
		setTransactionState(ISharedObjectContainerTransaction.ABORTED);
		// Send destroy message here so all remotes get destroyed, and we remove
		// ourselves from local space as well.
		getSharedObject().destroySelf();
		// throw so caller gets exception and can deal with it
		if (except instanceof SharedObjectAddAbortException)
			throw (SharedObjectAddAbortException) except;
		else
			throw new SharedObjectAddAbortException("Aborted", except,
					getTimeout());
	}

	protected void doTMCommit() throws SharedObjectAddAbortException {
		trace("doTMCommit");
		// Make sure we are connected. If so then send commit message
		if (getSharedObject().getGroupID() != null) {
			sendCommit();
		}
		// Call local committed message
		localCommitted();
	}

	protected void localCommitted() {
		trace("localCommitted()");
		// Set state variable to committed.
		setTransactionState(ISharedObjectContainerTransaction.COMMITTED);
		getSharedObject().creationCompleted();
	}

	protected boolean isVotingCompleted() throws SharedObjectAddAbortException {
		// The test here is is we've received any indication of failed
		// participants in the transaction. If so, we throw.
		if (getTransactionState() == ISharedObjectContainerTransaction.COMMITTED)
			return true;
		if (failed.size() > getMinFailedToAbort()) {
			// Abort!
			trace("isVotingCompleted:aborting:failed>" + getMinFailedToAbort()
					+ ":failed=" + failed);
			throw new SharedObjectAddAbortException("Abort received",
					participants, failed, getTimeout());
			// If no problems, and the number of participants to here from is 0,
			// then we're done
		} else if (getTransactionState() == ISharedObjectContainerTransaction.VOTING
				&& participants.size() == 0) {
			// Success!
			trace("isVotingCompleted() returning true");
			return true;
		}
		// Else continue waiting
		trace("isVotingCompleted:false");
		return false;
	}
}
