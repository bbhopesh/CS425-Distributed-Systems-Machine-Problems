package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.apache.commons.lang3.tuple.Pair;

import edu.illinois.uiuc.sp17.cs425.team4.component.Application;
import edu.illinois.uiuc.sp17.cs425.team4.component.GroupManager;
import edu.illinois.uiuc.sp17.cs425.team4.component.Multicast;
import edu.illinois.uiuc.sp17.cs425.team4.component.PointToPointMessenger;
import edu.illinois.uiuc.sp17.cs425.team4.model.Message;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class IsisTotallyOrderedMC implements Multicast, Application {
	
	/** Group Manager. Used for staying up to date about group members.*/
	private final GroupManager groupManager;
	/** Basic (unreliable) multicast on which this multicast is built. */
	private final Multicast reliableMulticast;
	/** Applications registered with this multicast. */
	private Application registeredApplication;
	/** messenger used in the first round of message multicasting in isis algorithm */
	private final PointToPointMessenger p2p;
	/** a priority queue used to store the messages */
	PriorityQueue<Message> queue;
	
	/**Priority comparator anonymous class implementation */
	public static Comparator<Message> idComparator = new Comparator<Message>(){
		
		@Override
		public int compare(Message c1, Message c2) {
            
        }
	};
	
	
	public IsisTotallyOrderedMC(Multicast reliable,PointToPointMessenger p2p ,GroupManager groupManager){
		this.groupManager = groupManager;
		this.reliableMulticast = reliable;
		this.reliableMulticast.registerApplication(this);
		this.p2p = p2p;
		
	}
	

	@Override
	public void multicast(Message m) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean registerApplication(Application application) {
		// TODO Auto-generated method stub
		if (this.registeredApplication != null) {
			throw new ContextedRuntimeException("Application can only be registered once.");
		}
		this.registeredApplication = application;
		return true;
	}


	@Override
	public Message deliver(Pair<Process, Message> incomingMessage) {
		// TODO Auto-generated method stub
		return null;
	}

}
