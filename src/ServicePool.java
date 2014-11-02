public class ServicePool {
	private MinPQ serviceHeap;

	public int size() {
		return this.serviceHeap.getSize();
	}
	
	public String[] getAllServices(){
		return serviceHeap.getAllServices();
	}

	public ServicePool(String ip_port) {
		serviceHeap = new MinPQ();
		serviceHeap.addNewService(ip_port);
	}

	public boolean addService(String ip_port) {
		return serviceHeap.addNewService(ip_port);
	}

	public boolean deleteService(String ip_port) {
		return serviceHeap.deleteService(ip_port);
	}

	public boolean updateService(String ip_port, int threadNum) {
		return serviceHeap.updateService(ip_port, threadNum);
	}

	public void printServicePool() {
		this.serviceHeap.printHeap();
	}

	/**
	 * load balancing round robin
	 * 
	 * @return #ip#port
	 * @throws Exception
	 */
	public String getNextService() throws Exception {
		if (isEmpty())
			throw new Exception("No service available!");
		return serviceHeap.getNextService();
	}

	private boolean isEmpty() {
		return (serviceHeap.getSize() == 0);
	}
}
