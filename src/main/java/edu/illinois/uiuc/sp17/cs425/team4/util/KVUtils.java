package edu.illinois.uiuc.sp17.cs425.team4.util;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import edu.illinois.uiuc.sp17.cs425.team4.component.KVDataPartitioner;
import edu.illinois.uiuc.sp17.cs425.team4.model.Process;

public class KVUtils {
	private KVUtils() {}
	
	public static <K, D> Map<Process, Map<K, D>> segregateDataProcessWise(
			Map<K, D> data, KVDataPartitioner<K> dataPartitioner) {
		Map<Process, Map<K,D>> segregatedData = new HashMap<>();
		
		for (Entry<K, D> dataEntry: data.entrySet()) {
			// Get all partitions of the key.
			Set<Process> allPartitions = getAllPartitions(dataEntry.getKey(), dataPartitioner);
			// Add this key to each partitions' data.
			for (Process partition: allPartitions) {
				Map<K, D> partitionzData = segregatedData.get(partition);
				if (partitionzData == null) {
					partitionzData = new HashMap<>();
					segregatedData.put(partition, partitionzData);
				}
				partitionzData.put(dataEntry.getKey(), dataEntry.getValue());
			}
		}
		return segregatedData;
	}
	
	public static <K> Set<Process> getAllPartitions(K key, KVDataPartitioner<K> dataPartitioner) {
		Process primaryPartition = dataPartitioner.getPrimaryPartition(key);
		Set<Process> allPartitions = dataPartitioner.getReplicas(primaryPartition);
		allPartitions.add(primaryPartition);
		return allPartitions;
	}
	
	public static Comparator<Long> createDecLongComp() {
		return new LongDescComp();
	}
	private static class LongDescComp implements Comparator<Long>, Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4974823438310012936L;

		@Override
		public int compare(Long o1, Long o2) {
			return -1*o1.compareTo(o2);
		}
	}

}
