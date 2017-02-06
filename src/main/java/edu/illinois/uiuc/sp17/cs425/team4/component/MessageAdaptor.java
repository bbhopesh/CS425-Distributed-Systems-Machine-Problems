package edu.illinois.uiuc.sp17.cs425.team4.component;

import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;


public interface MessageAdaptor {
	
	public Pair<Process,Message> read(Object conn);
	public void write(Object conn, Pair<Process,Message> sourceAndMessage);
}
