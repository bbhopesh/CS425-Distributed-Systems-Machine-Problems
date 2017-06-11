package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.util.Map;
import java.util.concurrent.Future;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

/**
 * Result object containing result of {@link KVRawDataManager}
 * 
 * @author bbassi2
 *
 * @param <R> Result type.
 */
public interface KVAsyncOpResult<R> {
	
	/**
	 * Did the overall operation succeeded?
	 * @return the result of the operation.
	 */
	public boolean succeeded();
	
	/**
	 * @return Already completed(both succeeded and failed) executions and results of these executions. 
	 */
	public Map<Process, R> completed();
	
	/**
	 * @return Already failed executions and errors of these executions.
	 */
	public Map<Process, Throwable> failures();
	
	/**
	 * @return In progress transactions and result bearing futures.
	 */
	public Map<Process, Future<R>> inProgress();
}
