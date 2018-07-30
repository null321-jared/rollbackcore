package net.shadowxcraft.rollbackcore;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.NoSuchElementException;

import org.bukkit.block.data.BlockData;

public class LRUBlockDataCache {
	class Node {
		BlockData data;
		int value;
		boolean hasExtraData;
		Node pre;
		Node next;

		public Node(BlockData key, int value, boolean hasExtraData) {
			this.data = key;
			this.value = value;
			this.hasExtraData = hasExtraData;
		}
	}

	HashMap<BlockData, Node> map = new HashMap<BlockData, Node>();
	ArrayDeque<Integer> unusedValues = new ArrayDeque<Integer>();
	Node head = null;
	Node end = null;

	/**
	 * Creates a new cache that will link the data
	 * to integer values between and including min and max
	 * 
	 * @param minValue The min value. Min 0.
	 * @param maxValue The max value.
	 */
	public LRUBlockDataCache(int minValue, int maxValue) {
		if (minValue < 0) {
			throw new IllegalArgumentException("minValue must be at least 0.");
		}
        for(int i = minValue; i <= maxValue; i++) {
        	unusedValues.add(i);
        }
    }

	private void removeFromList(Node n) {
		if (n.pre != null) {
			n.pre.next = n.next;
		} else {
			head = n.next;
		}

		if (n.next != null) {
			n.next.pre = n.pre;
		} else {
			end = n.pre;
		}

	}

	private void setHead(Node n) {
		n.next = head;
		n.pre = null;

		if (head != null)
			head.pre = n;

		head = n;

		if (end == null)
			end = head;
	}
	
	public Node get(BlockData key) {
		Node node = map.get(key);
		if (node != null) {
			removeFromList(node);
			setHead(node);
			return node;
		}

		return null;
	}
	
	public int remove(BlockData key) {
		Node node = map.remove(key);
		if(node == null) {
			throw new NoSuchElementException();
		} else {
			removeFromList(node);
			unusedValues.add(node.value);
			return node.value;
		}
	}

	public Node add(BlockData key, boolean hasExtraData) {
		Node old = map.get(key);
		if (old != null) {
			return old;
		} else {
			if (unusedValues.isEmpty()) {
				Node node = end;
				map.remove(node.data);
				removeFromList(node);
				node.data = key;
				map.put(key, node);
				setHead(node);
				return node;
			} else {
				Node node = new Node(key, unusedValues.removeFirst(), hasExtraData);
				setHead(node);
				map.put(key, node);
				return node;
			}
		}
	}
}