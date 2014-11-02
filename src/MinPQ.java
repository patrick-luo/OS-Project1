import java.util.Random;

public class MinPQ {
	private static int MAX_SIZE = 100;
	private int size = 0;
	private Node[] items = new Node[MAX_SIZE];
	
	/**
	 * Each node is a service, specified by its ip+port
	 * as well as its current activate worker threads
	 * 
	 * @author zhipeng, qihang, senghua
	 *
	 */
	private class Node{
		private String ip_port;
		private int threadNum;
		
		public Node(String ip_port){
			this.ip_port = ip_port;
			this.threadNum = 0;
		}
		
		public String getIPPort(){
			return ip_port;
		}
	}
	
	public String[] getAllServices(){
		String[] allServices = new String[size];
		for(int i = 0; i < size; i ++){
			allServices[i] = items[i].getIPPort();
		}
		return allServices;
	}
	
	
	/**
	 * Add a new service to this PQ. Specifically,
	 * add this node to the next available leave node,
	 * then maintain the priority property of the PQ.
	 * Return true if success; false otherwise.
	 * 
	 * @param service
	 */
	public boolean addNewService(String ip_port){
		if(find(ip_port) != -1){
			System.out.println("port exists already !");
			return false;
		}
		Node newNode = new Node(ip_port);
		items[size ++] = newNode;
		swim(size - 1);
		return true;
	}
	
	/**
	 * Delete an arbitrary service from this PQ.
	 * Specifically, swap this node and the last leave
	 * node, delete the last node and then maintain the PQ.
	 * It would return true if success; false otherwise.
	 * 
	 * @param service
	 */
	public boolean deleteService(String ip_port){
		int index = find(ip_port);
		if(index == -1)
			return false;
		swap(index, size - 1); // swap the node to be delete with the last node.
		size --; // delete it
		// if the swapped node has the same num of threads
		if(items[index].threadNum == items[size].threadNum)
			return true;
		// if the swapped node has more threads, sink it.
		else if(items[index].threadNum > items[size].threadNum)
			sink(index);
		// if the swapped node has less threads, swim it.
		else
			swim(index);
		return true;
	}
	
	/**
	 * It would return the ip_port of the root, add one more thread to
	 * this root node and maintain the PQ.
	 * @return
	 */
	public String getNextService()
	{
		if(size == 0)
			return null;
		String result = items[0].ip_port;
		items[0].threadNum ++;
		sink(0);
		return result;
	}
	
	/**
	 * Update the service with the latest # of active threads
	 * 
	 * @param ip_port
	 * @param numThreads
	 * @return
	 */
	public boolean updateService(String ip_port, int numThreads)
	{
		int index = find(ip_port);
		if(index == -1)
			return false;
		if(numThreads > items[index].threadNum){
			items[index].threadNum = numThreads;
			sink(index);
		}
		else{
			items[index].threadNum = numThreads;
			swim(index);
		}
		return true;
	}
	
	public int getSize(){
		return size;
	}
	
	public void printHeap(){
		int level = 0;
		int threshold = 0;
		
		for(int i = 0; i < size; i++){
			System.out.print("->[" + items[i].ip_port + " #" + items[i].threadNum + "] ");
			if(i == threshold){
				//System.out.println();
				level ++;
				threshold += (int)Math.pow(2.0, (double)level);
			}
		}
		System.out.println();
	}
	
	/**
	 * Find the index given an arbitrary service.
	 * 
	 * @param service
	 * @return
	 */
	private int find(String ip_port){
		for(int i = 0; i < size; i ++)
			if(items[i].ip_port.equals(ip_port))
				return i;
		return -1;
	}
	
	/**
	 * Swim this node when it has less threads
	 * in order to maintain this PQ.
	 * @param index
	 */
	private void swim(int index){
		if(index == 0)
			return;
		while(index != 0 && 
				items[index].threadNum < items[(index-1)/2].threadNum){
			swap(index, (index-1)/2);
			index = (index-1)/2;
		}
	}
	
	/**
	 * Sink the node when it has more threads 
	 * in order to maintain this PQ
	 * @param index
	 */
	private void sink(int index){
		if(index * 2 + 1 >= size) // current node has no child
			return;
		else if(index * 2 + 1 == size - 1){ // current node has exactly one child
			if(items[index].threadNum > items[index*2+1].threadNum){
				swap(index, index*2+1);
				sink(index*2+1);
			}
		}
		else{// current node has two children
			int left = index * 2 + 1;
			int right = index * 2 + 2;
			int min = items[left].threadNum > items[right].threadNum ?
					right : left;
			if(items[min].threadNum >= items[index].threadNum)
				return;
			else{
				swap(index, min);
				sink(min);
			}
		}
		
	}
	/**
	 * Swap two nodes.
	 * @param i
	 * @param j
	 */
	private void swap(int i, int j){
		Node temp = items[i];
		items[i] = items[j];
		items[j] = temp;
	}
	
	public static void main(String[] args){
		MinPQ inst = new MinPQ();
		inst.addNewService("22");
		for(int i = 22; i < 100; i += 5)
			inst.addNewService(Integer.toString(i));
		inst.printHeap();
		for(int i = 22; i < 100; i += 5)
			inst.updateService(Integer.toString(i), new Random().nextInt(15) + 10);
		inst.printHeap();
		for(int i = 0; i < 10; i ++){
			Node n = inst.items[new Random().nextInt(inst.size)];
			System.out.println("Delete " + n.ip_port);
			inst.deleteService(n.ip_port);
			inst.printHeap();
		}
//		for(int i = 22; i < 100; i += 10)
//			inst.deleteService(Integer.toString(i));
//		inst.printHeap();
		System.out.println(inst.getNextService());
		inst.printHeap();
	}
}
