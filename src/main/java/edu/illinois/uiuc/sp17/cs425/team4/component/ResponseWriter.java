package edu.illinois.uiuc.sp17.cs425.team4.component;

import org.apache.commons.lang3.exception.ContextedRuntimeException;

import edu.illinois.uiuc.sp17.cs425.team4.model.Message;

public interface ResponseWriter {
	/**
	 * Write response. This method should not be called after calling close().
	 * @param response
	 * @throws ContextedRuntimeException
	 */
	public void writeResponse(Message response) throws ContextedRuntimeException;
	
	public void close();
}
