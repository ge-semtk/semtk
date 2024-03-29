/**
 ** Copyright 2022 General Electric Company
 **
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 ** 
 **     http://www.apache.org/licenses/LICENSE-2.0
 ** 
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package com.ge.research.semtk.load.utility;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.ge.research.semtk.load.transform.Transform;
import com.ge.research.semtk.utility.LocalLogger;

/**
 * Handle the logic for determining whether to use batch URI lookups
 * @author 200001934
 */
public class UriLookupPerfMonitor {
	private final boolean DISABLE_PRECACHE = false;
	private final boolean LOG_PRECACHE = false;
	
	static final int PREFETCH_MAX_COUNT = 100000; // stop pre-fetching after this many (worried about memory).
	
	public static final int PREFETCH_MAX_EXIST = 200000; // Since pre-fetch queries need sort by each field, performance could
													// be awful
													// So run a count first, and if it is higher than this, abandon all
													// hope of prefetching
													// Divide this number by the number of mappings. This is the value
													// for simple lookup by 1 field.
	private String nodeName = null;
	private long ignoreUntilIndivCount = 0;   // don't consider a batch until this many individual queries run.											
	private long uriLookupCount = 0;
	private long indivAvgQueryTime = 0;
	private long indivQueryCount = 0;
	private long batchAvgQueryTime = 0;
	private long urisCachedSoFar = 0;
	private long urisInTriplestoreEstimate = -1;    // will be incorrect when there are duplicates
	private Boolean anyUrisInTripleStore = null;
	private AtomicBoolean prefetchedAll = new AtomicBoolean(false);
	
	private int queryLimit = 0;
	private int queryOffset = 0;
	
	private AtomicBoolean batchQueryBlocked = new AtomicBoolean(DISABLE_PRECACHE);  // once true, batch success is only way to unblock
	
	public UriLookupPerfMonitor(int mappingCount, String nodeNameForLogging) {
		this.queryLimit = 4900 / mappingCount;
		this.queryOffset = -1;
		this.ignoreUntilIndivCount = 100;
		this.nodeName = nodeNameForLogging;
	}
	
	/**
	 * A Uri lookup attempt is being made (memory or triplestore)
	 */
	public synchronized void recordLookup() {
		this.uriLookupCount += 1;
	}

	public static enum READY { NO, YES, COUNT_NEEDED };
	public void setAnyUrisInTriplestore(boolean v) {
		synchronized(this) {
			if (this.anyUrisInTripleStore == null) {
				this.anyUrisInTripleStore = new Boolean(v);
			} else {
				this.anyUrisInTripleStore = v;
			}
		}
	}
	/**
	 * @return - NO - a query won't be helpful (none in triplestore, or all already prefetched)
	 *           YES - ready to run a query, call recordIndiv() on success
	 *           COUNT_NEEDED - determine whether there is at least one uri in the triplestore and call setAnyUrisInTriplestore()
	 */
	public READY requestNextIndivQuery() throws InternalError {
		if (this.anyUrisInTripleStore == null) {
			throw new InternalError("requestNextIndivQuery() was called before anyUrisInTripleStore was set");
		
		} else if (this.anyUrisInTripleStore == false || this.prefetchedAll.get()) {
			return READY.NO;
		}
		return READY.YES;
		
	}
	
	/**
	 * Preset next batch query limit and offset
	 * @return - NO - can't run a query right now
	 *           YES - ready to run a batch query, call recordBatchSuccess() on success
	 *           COUNT_NEEDED - call setUrisInTriplestore() and try again, counting up to at least PREFETCH_MAX_EXIST + 1
	 */
	synchronized public READY requestNextBatchQuery() {
		if (this.batchQueryBlocked.get()) {
			// another thread is already doing it OR it has been permanently ended for this load
			return READY.NO;
			
		} else if (this.indivQueryCount < this.ignoreUntilIndivCount) {
			// in break-in or breathing-room spot
			return READY.NO;
			
		} else if (this.urisInTriplestoreEstimate == -1 ) {
			// ready to count total in triplestore before continuing
			this.batchQueryBlocked.set(true);
			return READY.COUNT_NEEDED;
			
		} else {
		
			// estimate times to lookup individually vs batch
			// lots of assumptions
			// seem 2x to high but the two estimates cross at approximately the correct place
			long estimatedLookupsRemaining = Math.max(5000, this.uriLookupCount) ;  // generous swag at how many lookups remain 
			long urisToCache = Math.min(this.urisInTriplestoreEstimate, PREFETCH_MAX_COUNT) - this.urisCachedSoFar;
			long batchQueryTime = this.batchAvgQueryTime * urisToCache;
			long estWithBatch     =  batchQueryTime + this.indivAvgQueryTime * estimatedLookupsRemaining * (1 - (this.urisCachedSoFar + urisToCache) / this.urisInTriplestoreEstimate); 
			long estWithoutBatch  =  0              + this.indivAvgQueryTime * estimatedLookupsRemaining * (1 - (this.urisCachedSoFar + 0          ) / this.urisInTriplestoreEstimate);
			
			
			
			// if no batch lookups have been tried yet or it looks faster
			if (this.batchAvgQueryTime == 0 || estWithBatch < estWithoutBatch) {
				if (LOG_PRECACHE) LocalLogger.logToStdOut(String.format("precache %s EVAL YES indiv=%,d batch=%,d", this.nodeName, estWithoutBatch, estWithBatch));
				
				// set the new offset
				if (this.queryOffset < 0)
					this.queryOffset = 0;
				else
					this.queryOffset += this.queryLimit;
				
				// permanently stop if we've cached enough already
				if (queryOffset > PREFETCH_MAX_COUNT) {
					this.batchQueryBlocked.set(true);
					return READY.NO;
				} else {
				
					this.batchQueryBlocked.set(true);
					this.ignoreUntilIndivCount = (long) (this.indivQueryCount * 1.05);
					return READY.YES;
				}
				
			} else {
				// batch pre-caching doesn't seem efficient
				
				if (LOG_PRECACHE) LocalLogger.logToStdOut(String.format("precache %s EVAL NO  indiv=%,d batch=%,d", this.nodeName, estWithoutBatch, estWithBatch));
				
				// don't compute efficiency again in a while
				this.ignoreUntilIndivCount = (long) (this.indivQueryCount * 1.5);
				return READY.NO;
			}
		}
		
	}
	
	public int getQueryLimit() { return this.queryLimit; }
	public int getQueryOffset() { return this.queryOffset; }
	public boolean getPrefetchedAll() { return this.prefetchedAll.get(); }
	
	public void setUrisInTriplestore(long val) { 
		this.urisInTriplestoreEstimate = val; 
		if (val > PREFETCH_MAX_EXIST) {
			// too many uris in triplestore:  never try a batch lookup
			this.batchQueryBlocked.set(true);
		} else {
			this.batchQueryBlocked.set(false);
		}
	}
	
	/**
	 * Once set, only a successful batch query can unblock
	 */
	public void setBatchQueryBlocked() { 
		this.batchQueryBlocked.set(true); 
	}
	
	/**
	 * Log fact that a URI was just looked up, and how long it took
	 * @param nanosec
	 */
	public void recordIndivQuery(long nanosec) {
		final double PAST_WEIGHT = 0.98;
		final double CURR_WEIGHT = 1 - PAST_WEIGHT;
		
		// keep vars in sync with each other
		synchronized(this) {
			this.indivQueryCount += 1;
			if (this.indivAvgQueryTime <= 0) {
				this.indivAvgQueryTime = nanosec;
			} else {
				// keep 50 rolling average
				this.indivAvgQueryTime = 	(long) ((double)this.indivAvgQueryTime * PAST_WEIGHT + (double) nanosec * CURR_WEIGHT);
			}
		}
		if (LOG_PRECACHE && this.indivQueryCount % 50 == 0) LocalLogger.logToStdOut(String.format("precache %s INDIV: looked up 1 total=%d, avg nanosec=%,d", this.nodeName, this.indivQueryCount, this.indivAvgQueryTime));
	}
	
	public void recordBatchQuery(long nanosec, long returnCount) {
		final double PAST_WEIGHT = 0.75;
		final double CURR_WEIGHT = 1 - PAST_WEIGHT;
		
		// keep vars in sync with each other
		synchronized(this) {
			if (returnCount < this.queryLimit) {
				this.prefetchedAll.set(true);
				this.batchQueryBlocked.set(true);
				if (LOG_PRECACHE) LocalLogger.logToStdOut(String.format("precache %s BATCH DONE: looked up %d, avg nanosec=%,d", this.nodeName, returnCount, this.batchAvgQueryTime));
	
			} else {
				this.urisCachedSoFar += returnCount;
				long avg = nanosec / this.queryLimit;
				if (this.batchAvgQueryTime <= 0) {
					this.batchAvgQueryTime = avg;
				} else {
					// keep 4-set rolling average
					this.batchAvgQueryTime = 	(long) ((double)this.batchAvgQueryTime * PAST_WEIGHT + (double) avg * CURR_WEIGHT);
				}
				
				this.batchQueryBlocked.set(false);
				if (LOG_PRECACHE) LocalLogger.logToStdOut(String.format("precache %s BATCH: looked up %d, avg nanosec=%,d", this.nodeName, returnCount, this.batchAvgQueryTime));
			}
		}
	} 
	
}
