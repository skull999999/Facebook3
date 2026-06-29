package thefacebook.datastructures;

import java.util.ArrayList;
import java.util.List;

public class MyHashTable<K, V> {
    private static class Entry<K, V> {
        K key;
        V value;
        Entry<K, V> next;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    private Entry<K, V>[] buckets;
    private int size;

    @SuppressWarnings("unchecked")
    public MyHashTable() {
        buckets = (Entry<K, V>[]) new Entry[31];
    }

    public void put(K key, V value) {
        if (size > buckets.length * 0.75) {
            resize();
        }
        int index = index(key);
        Entry<K, V> current = buckets[index];
        while (current != null) {
            if (current.key.equals(key)) {
                current.value = value;
                return;
            }
            current = current.next;
        }
        Entry<K, V> entry = new Entry<>(key, value);
        entry.next = buckets[index];
        buckets[index] = entry;
        size++;
    }

    public V get(K key) {
        Entry<K, V> current = buckets[index(key)];
        while (current != null) {
            if (current.key.equals(key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    public boolean containsKey(K key) {
        return get(key) != null;
    }

    public void remove(K key) {
        int index = index(key);
        Entry<K, V> current = buckets[index];
        Entry<K, V> previous = null;
        while (current != null) {
            if (current.key.equals(key)) {
                if (previous == null) {
                    buckets[index] = current.next;
                } else {
                    previous.next = current.next;
                }
                size--;
                return;
            }
            previous = current;
            current = current.next;
        }
    }

    public List<V> values() {
        ArrayList<V> values = new ArrayList<>();
        for (Entry<K, V> bucket : buckets) {
            Entry<K, V> current = bucket;
            while (current != null) {
                values.add(current.value);
                current = current.next;
            }
        }
        return values;
    }

    private int index(K key) {
        return Math.abs(key.hashCode()) % buckets.length;
    }

    private void resize() {
        List<V> oldValues = values();
        List<K> oldKeys = keys();
        @SuppressWarnings("unchecked")
        Entry<K, V>[] newBuckets = (Entry<K, V>[]) new Entry[buckets.length * 2 + 1];
        buckets = newBuckets;
        size = 0;
        for (int i = 0; i < oldKeys.size(); i++) {
            put(oldKeys.get(i), oldValues.get(i));
        }
    }

    public List<K> keys() {
        ArrayList<K> keys = new ArrayList<>();
        for (Entry<K, V> bucket : buckets) {
            Entry<K, V> current = bucket;
            while (current != null) {
                keys.add(current.key);
                current = current.next;
            }
        }
        return keys;
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        buckets = (Entry<K, V>[]) new Entry[31];
        size = 0;
    }
}
