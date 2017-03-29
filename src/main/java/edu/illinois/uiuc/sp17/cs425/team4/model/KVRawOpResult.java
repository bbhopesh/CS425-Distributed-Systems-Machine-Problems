package edu.illinois.uiuc.sp17.cs425.team4.model;

import java.util.Map;
import java.util.concurrent.Future;

import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public interface KVRawOpResult<R> {
	
	public boolean succeeded();
	
	public Map<Process, R> completed();
	
	public Map<Process, Throwable> failures();
	
	public Map<Process, Future<R>> inProgress();
}
