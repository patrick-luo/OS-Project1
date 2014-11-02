import java.util.concurrent.ConcurrentHashMap;

public class Cache<K, V> {

	private ConcurrentHashMap<K, V> cache;

	public Cache() {
		this.cache = new ConcurrentHashMap<K, V>();
	}

	public void put(K key, V value) {
		this.cache.put(key, value);
	}

	public boolean isHit(K key) {
		return this.cache.containsKey(key);
	}

	public V get(K key) {
		return this.cache.get(key);
	}

	public void remove(K key) {
		this.cache.remove(key);
	}

	public void printCache() {
		System.out.println("+++++++++++");
		for (K k : this.cache.keySet()) {
			System.out.println(k);
		}
		System.out.println("+++++++++++");
	}

}
