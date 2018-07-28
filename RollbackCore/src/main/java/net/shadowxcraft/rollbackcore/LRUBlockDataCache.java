package net.shadowxcraft.rollbackcore;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.NoSuchElementException;

import org.bukkit.block.data.BlockData;

public class LRUBlockDataCache {
	class Node {
		BlockData key;
		int value;
		Node pre;
		Node next;

		public Node(BlockData key, int value) {
			this.key = key;
			this.value = value;
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
	
	public int get(BlockData key) {
		Node node = map.get(key);
		if (node != null) {
			removeFromList(node);
			setHead(node);
			return node.value;
		}

		return -1;
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

	public int add(BlockData key) {
		Node old = map.get(key);
		if (old != null) {
			return old.value;
		} else {
			if (unusedValues.isEmpty()) {
				Node node = end;
				map.remove(node.key);
				removeFromList(node);
				node.key = key;
				map.put(key, node);
				setHead(node);
				return node.value;
			} else {
				Node node = new Node(key, unusedValues.removeFirst());
				setHead(node);
				map.put(key, node);
				return node.value;
			}
		}
	}
}