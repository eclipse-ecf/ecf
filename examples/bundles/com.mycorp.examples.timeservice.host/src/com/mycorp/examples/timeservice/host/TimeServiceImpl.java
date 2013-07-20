package com.mycorp.examples.timeservice.host;

import com.mycorp.examples.timeservice.ITimeService;

public class TimeServiceImpl implements ITimeService {

	/**
	 * Implementation of my time service. 
	 */
	public Long getCurrentTime() {
		// Print out to host std out that a call to this service was received.
		System.out.println("TimeServiceImpl.  Received call to getCurrentTime()");
		// Eventually, this should (e.g.) contact NIST time server and return more
		// accurate time.  For the time being, we will return the System time for
		// this host.
		return new Long(System.currentTimeMillis());
	}

}
