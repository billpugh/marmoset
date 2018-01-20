/**
 * Marmoset: a student project snapshot, submission, testing and code review
 * system developed by the Univ. of Maryland, College Park
 * 
 * Developed as part of Jaime Spacco's Ph.D. thesis work, continuing effort led
 * by William Pugh. See http://marmoset.cs.umd.edu/
 * 
 * Copyright 2005 - 2011, Univ. of Maryland
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */

/*
 * Created on Mar 10, 2005
 */
package edu.umd.cs.buildServer.util;

import java.io.IOException;
import java.io.InputStream;

import edu.umd.cs.buildServer.BuildServer;

/**
 * InputStream wrapper that limits the number of bytes that may be read to a
 * fixed amount.
 */
public class LimitedInputStream extends InputStream {
	private InputStream realInputStream;
	private int limit;
	private int drainLimit;
	private int numBytesRead;
	private boolean loggedLimitReached;

	/**
	 * Constructor.
	 * 
	 * @param in
	 *            the InputStream to read from
	 * @param limit
	 *            maximum number of bytes to read
	 */
	public LimitedInputStream(InputStream in, int limit) {
		this.realInputStream = in;
		this.limit = limit;
		this.drainLimit = Integer.MAX_VALUE;
		this.numBytesRead = 0;
	}

	@Override
	public int read() throws IOException {
		byte[] buf = new byte[1];
		for (;;) {
			int n = read(buf, 0, 1);
			if (n < 0)
				return -1;
			if (n == 1)
				return buf[0];
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (limitExceeded())
			return -1;
		len = Math.min(len, getRemainingAllowed());
		int n = realInputStream.read(b, off, len);
		if (n >= 0)
			numBytesRead += n;
		return n;

	}

	/**
	 * Set the maximum number of bytes that will be read or drained from the
	 * underlying input stream. The drain limit must be less than the read
	 * limit.
	 * 
	 * @param drainLimit
	 *            the drain limit
	 */
	public void setDrainLimit(int drainLimit) {
		// FIXME XXX HACK don't use drain limit for now
		// this.drainLimit = drainLimit;
	}

	/**
	 * Drain the underlying input stream. This may be called after the input
	 * limit is exceeded.
	 * 
	 * @throws IOException
	 */
	public void drain() throws IOException {
		byte[] buf = new byte[2048];
		while (numBytesRead < drainLimit) {
			int n = realInputStream.read(buf);
			if (n < 0)
				break;
			numBytesRead += n;
		}
		if (numBytesRead > limit) {
			BuildServer
					.instance()
					.getLog()
					.warn("Drained " + (numBytesRead - limit)
							+ " additional bytes of output");
			if (numBytesRead >= drainLimit) {
				BuildServer
						.instance()
						.getLog()
						.warn("Output stream drain limit (" + drainLimit
								+ ") reached!");
			}
		}
	}

	/**
	 * Return whether or not the input limit was exceeded.
	 * 
	 * @return true if the limit was exceeded, false if not
	 */
	public boolean limitExceeded() {
		boolean exceeded = numBytesRead >= limit;
		if (exceeded && !this.loggedLimitReached) {
			BuildServer.instance().getLog()
					.warn("Input stream limit (" + limit + ") reached");
			loggedLimitReached = true;
		}
		return exceeded;
	}

	/**
	 * Get the number of bytes remaining that may still be read without
	 * exceeding the limit.
	 * 
	 * @return the number of bytes allowed to be read
	 */
	private int getRemainingAllowed() {
		return limit - numBytesRead;
	}
}
