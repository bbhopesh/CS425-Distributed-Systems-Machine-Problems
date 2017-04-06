package edu.illinois.uiuc.sp17.cs425.team4.model.impl;

import java.util.Map;
import java.util.concurrent.Future;

import edu.illinois.uiuc.sp17.cs425.team4.model.KVAsyncOpResult;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

class KVRawOpResultImpl<R> implements KVAsyncOpResult<R> {
	
	private final boolean succeeded;
	private final Map<Process, R> completed;
	private final Map<Process, Throwable> failures;
	private final Map<Process, Future<R>> inProgress;

	
	public KVRawOpResultImpl(boolean succeeded, 
							Map<Process, R> completed, 
							Map<Process, Throwable> failures,
							Map<Process, Future<R>> inProgress) {
		this.succeeded = succeeded;
		this.completed = completed;
		this.failures = failures;
		this.inProgress = inProgress;
	}

	public boolean succeeded(){
		return this.succeeded;
	}
	
	@Override
	public Map<Process, R> completed() {
		return this.completed;
	}

	@Override
	public Map<Process, Throwable> failures() {
		return this.failures;
	}

	@Override
	public Map<Process, Future<R>> inProgress() {
		return this.inProgress;
	}

}
