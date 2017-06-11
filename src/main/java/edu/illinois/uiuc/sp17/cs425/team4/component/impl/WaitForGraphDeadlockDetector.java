package edu.illinois.uiuc.sp17.cs425.team4.component.impl;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.illinois.uiuc.sp17.cs425.team4.component.DeadlockListener;
import edu.illinois.uiuc.sp17.cs425.team4.component.TransactionsDeadlockDetector;
import edu.illinois.uiuc.sp17.cs425.team4.model.Transaction;

/**
 * Detects deadlock by constructing wait for graph between transactions and detecting cycles in the graph.
 * 
 * @author bbassi2
 *
 * @param <K> Key type.
 */
public class WaitForGraphDeadlockDetector<K> implements TransactionsDeadlockDetector<K>, Runnable {
	
	private final Set<Transaction> transactions;
	// Maps what each transaction wants
	// Set is just a placeholder, transaction can only want one thing at a time.
	private final ConcurrentMap<Transaction, Set<Lock<K>>> wants;
	// Maps what read locks are held by which transactions.
	private final ConcurrentMap<K, Set<Transaction>> readLockHolders;
	// Maps what write locks are held by which transactions.
	private final ConcurrentMap<K, Set<Transaction>> writeLockHolders;
	// Maps what read to write upgrade locks are held by which transactions.
	private final ConcurrentMap<K, Set<Transaction>> rToWUpgradeLockHolders;
	private final Thread deadlockDetectorThread;
	private final DeadlockListener deadlockListener;
	
	
	public WaitForGraphDeadlockDetector(DeadlockListener deadlockListener) {
		this.transactions = Collections.newSetFromMap(new ConcurrentHashMap<>());
		this.wants =  new ConcurrentHashMap<>();
		this.readLockHolders = new ConcurrentHashMap<>();
		this.writeLockHolders = new ConcurrentHashMap<>();
		this.rToWUpgradeLockHolders = new ConcurrentHashMap<>();
		
		this.deadlockDetectorThread = new Thread(this);
		
		this.deadlockListener = deadlockListener;
	}
	
	@Override
	public void initialize() {
		this.deadlockDetectorThread.start();
	}
	
	@Override
	public void run() {
		while (true) {
			
			DirectedGraph<Transaction, DefaultEdge> waitForGraph = buildWaitForGraph();
			CycleDetector<Transaction, DefaultEdge> cycleDetector = new CycleDetector<>(waitForGraph);
			Set<Transaction> cycleEdges = cycleDetector.findCycles();
			if (cycleEdges != null && !cycleEdges.isEmpty()) {
				// Randomly abort a transaction.
				this.deadlockListener.abortTransaction(cycleEdges.iterator().next());
			}
			
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
	
	private DirectedGraph<Transaction, DefaultEdge> buildWaitForGraph() {
		// All our maps are thread-safe, even if updates to multiple maps are not atomic, it's okay
		// because once a deadlock occurs, it will stay as it, so in subsequent transaction, we will find it.
		// Also we always remove information before adding, so multiple updates across maps shouldn't cause false deadlocks.
		// e.g. If we get a call that some transaction got a lock, we first remove it from map of waiting transactions and then add it to map of holding transactions.
		DirectedGraph<Transaction, DefaultEdge> waitForGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
		
		for (Transaction waitingTransaction: this.transactions) {
			// For a transaction, find the locks it is waiting on
			// and then for each of those locks, find which transactions are holding them.
			Set<Lock<K>> waitingOn = this.wants.get(waitingTransaction);
			if (waitingOn != null && !waitingOn.isEmpty()) {
				for (Lock<K> waitingOnLock: waitingOn) {
					if (waitingOnLock.lockType == LockType.READ) {
						// If transaction is waiting on read lock, then add an edge to all transactions who are holding write lock.
						addEdges(waitingTransaction, this.writeLockHolders.get(waitingOnLock.key), waitForGraph);
					}
					
					if (waitingOnLock.lockType == LockType.WRITE) {
						// If transaction is waiting on write lock, then add an edge to all transactions who are holding write lock or read lock
						addEdges(waitingTransaction, this.writeLockHolders.get(waitingOnLock.key), waitForGraph);
						addEdges(waitingTransaction, this.readLockHolders.get(waitingOnLock.key), waitForGraph);
					}
					
					if (waitingOnLock.lockType == LockType.READ_TO_WRITE_UPGRADE) {
						addEdges(waitingTransaction, this.rToWUpgradeLockHolders.get(waitingOnLock.key), waitForGraph);
					}
				}
			}
		}
		return waitForGraph;
	}
	
	private void addEdges(Transaction from, Set<Transaction> to, DirectedGraph<Transaction, DefaultEdge> graph) {
		if (to != null && !to.isEmpty()) {
			for (Transaction toT: to) {
				addEdge(from, toT, graph);
			}
		}
	}
	
	private void addEdge(Transaction from, Transaction to, DirectedGraph<Transaction, DefaultEdge> graph) {
		graph.addVertex(from);
		graph.addVertex(to);
		graph.addEdge(from, to);
	}
	
	@Override
	public void wantWriteLock(Transaction transaction, K key) {
		this.transactions.add(transaction);
		
		addToWantedLocks(transaction, key, LockType.WRITE);
	}

	@Override
	public void gotWriteLock(Transaction transaction, K key) {
		this.transactions.add(transaction);
		
		removeFromWantedLocks(transaction, key, LockType.WRITE);
		addToHoldedLocks(transaction, key, this.writeLockHolders);
		// Actually only one transaction can have write lock, so set should always have one element.
	}

	@Override
	public void releaseWriteLock(Transaction transaction, K key) {
		this.transactions.add(transaction);
		
		removeFromHoldedLocks(transaction, key, this.writeLockHolders);
	}

	@Override
	public void wantReadLock(Transaction transaction, K key) {
		this.transactions.add(transaction);
		
		addToWantedLocks(transaction, key, LockType.READ);
	}

	@Override
	public void gotReadLock(Transaction transaction, K key) {
		this.transactions.add(transaction);
		
		removeFromWantedLocks(transaction, key, LockType.READ);
		addToHoldedLocks(transaction, key, this.readLockHolders);
	}

	@Override
	public void releaseReadLock(Transaction transaction, K key) {
		this.transactions.add(transaction);
		
		removeFromHoldedLocks(transaction, key, this.readLockHolders);
	}

	@Override
	public void wantReadToWriteUpgradeLock(Transaction transaction, K key) {
		this.transactions.add(transaction);

		addToWantedLocks(transaction, key, LockType.READ_TO_WRITE_UPGRADE);
	}

	@Override
	public void gotReadToWriteUpgradeLock(Transaction transaction, K key) {
		this.transactions.add(transaction);
		
		removeFromWantedLocks(transaction, key, LockType.READ_TO_WRITE_UPGRADE);
		addToHoldedLocks(transaction, key, this.rToWUpgradeLockHolders);
		// Actually only one transaction can have upgrade lock, so set should always have one element.
	}

	@Override
	public void releaseReadToWriteUpgradeLock(Transaction transaction, K key) {
		this.transactions.add(transaction);
		
		removeFromHoldedLocks(transaction, key, this.rToWUpgradeLockHolders);
	}
	
	private void addToHoldedLocks(Transaction transaction, K key, ConcurrentMap<K, Set<Transaction>> holders) {
		holders.putIfAbsent(key, Collections.newSetFromMap(new ConcurrentHashMap<>()));
		Set<Transaction> transactionsHoldingLock = holders.get(key);
		transactionsHoldingLock.add(transaction);
	}
	
	private void addToWantedLocks(Transaction transaction, K key, LockType lockType) {
		this.wants.putIfAbsent(transaction, Collections.newSetFromMap(new ConcurrentHashMap<>()));
		Set<Lock<K>> wantedLocks = this.wants.get(transaction);
		wantedLocks.add(new Lock<K>(lockType, key));
	}
	
	private void removeFromHoldedLocks(Transaction transaction, K key, ConcurrentMap<K, Set<Transaction>> holders) {
		Set<Transaction> transactions = holders.get(key);
		if (transactions != null) {
			transactions.remove(transaction);
		}
	}
	
	private void removeFromWantedLocks(Transaction transaction, K key, LockType lockType) {
		this.wants.remove(transaction);
	}
	
	@Override
	public void clear(Transaction transaction) {
		this.transactions.remove(transaction);
		this.wants.remove(transaction);
		removeTransactionFromHolded(transaction, this.readLockHolders);
		removeTransactionFromHolded(transaction, this.writeLockHolders);
		removeTransactionFromHolded(transaction, this.rToWUpgradeLockHolders);
	}
	
	private void removeTransactionFromHolded(Transaction transaction, ConcurrentMap<K, Set<Transaction>> holder) {
		for (Entry<K, Set<Transaction>> e: holder.entrySet()) {
			e.getValue().remove(transaction);
		}
	}
	
	
	@Override
	public void close() {
		this.deadlockDetectorThread.interrupt();
	}
	
	
	private enum LockType {
		READ,
		WRITE,
		READ_TO_WRITE_UPGRADE
	}
	
	private static class Lock<K> {
		
		
		private LockType lockType;
		
		private K key;
		
		public Lock(LockType lockType, K key) {
			this.lockType = lockType;
			this.key = key;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			result = prime * result + ((lockType == null) ? 0 : lockType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("rawtypes")
			Lock other = (Lock) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			if (lockType != other.lockType)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Lock [lockType=" + lockType + ", key=" + key + "]";
		}
	}
}
