import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class Mapper {

	private HashMap<String, ServicePool> portMap;
	private DatagramSocket socket;
	private DudePool dudePool;
	private Monitor monitor;

	public Mapper() throws SocketException {
		socket = new DatagramSocket(1111);// mapper's socket
		portMap = new HashMap<String, ServicePool>();
		dudePool = new DudePool(portMap);
		monitor = new Monitor();
	}

	/**
	 * This class deals with heart beat monitoring of all the registered
	 * services. It has two inner classes Chi, Adora which extend Thread. Chi
	 * class is responsible for sending monitor messages while Adora class for
	 * receiving reply.
	 */
	private class Monitor {
		private static final long INTERVAL = 5 * 1000;
		private static final long TIMEOUT = 5 * 1000;
		private Long startTime;
		private Sender chi;
		private Receiver adora;
		private boolean senderHasReachedStartTime;
		private boolean receiverHasReachedStartTime;
		/**
		 * This hashmap is generated from the portMap every time before the
		 * sender sends heart beat message to servers. Each key has this format:
		 * <program>_<version>:<ip>_<port>, while each value is not used.
		 */
		private HashMap<String, String> servers;

		public Monitor() throws SocketException {

			chi = new Sender();
			adora = new Receiver();
		}

		public void launch() {
			startTime = System.currentTimeMillis() + INTERVAL;
			senderHasReachedStartTime = false;
			receiverHasReachedStartTime = false;
			chi.start();
			adora.start();
		}

		private class Sender extends Thread {
			private DatagramSocket socket;

			public Sender() throws SocketException {
				socket = new DatagramSocket();
			}

			// public int getPort()
			// {
			// return socket.getLocalPort();
			// }
			public void run() {
				while (true) {

					while (true) { // waiting for the start time
						synchronized (startTime) {
							if (System.currentTimeMillis() >= startTime) {
								senderHasReachedStartTime = true;
								if (receiverHasReachedStartTime)
									startTime = Long.MAX_VALUE;
								break;
							}
						}
					}

					System.out
							.println("=================[Monitor begins to work now]==============");
					buildHashMap(); // step 1

					try { // step 2: send monitor msg to each server
						send();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					System.out
							.println("Monitor has sent messages to all registered servers!");

					long endTime = System.currentTimeMillis() + TIMEOUT;
					while (true) { // waiting for the end time
						if (System.currentTimeMillis() >= endTime) {
							// System.out.println("Time to end!");
							try {
								stopReceiver();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							break;
						}
					}
					System.out
							.println("Monitor: Sender asks receiver to stop.");
					senderHasReachedStartTime = false;
				}
			}

			private void buildHashMap() {
				servers = new HashMap<String, String>();
				synchronized (portMap) {
					for (String prog_ver : portMap.keySet()) {
						ServicePool pool = portMap.get(prog_ver);
						String[] allServers = pool.getAllServices();
						for (String ip_port : allServers) {
							String eachKey = prog_ver + ":";
							eachKey += ip_port;
							servers.put(eachKey, null);
						}

					}
				}
			}

			private void send() throws IOException {
				// System.out.println("Key size = " + servers.size());
				String[] keySet = new String[servers.size()];
				int i = 0;
				for (String key : servers.keySet()) {
					keySet[i++] = new String(key);
				}
				for (String key : keySet) {
					// System.out.println("Key = " + key);

					DatagramPacket sendPacket;
					byte[] sendData = new byte[1024];

					String ip_port = key.split(":")[1];
					ip_port = ip_port.substring(1);
					InetAddress ip = InetAddress
							.getByName(ip_port.split("_")[0]);
					int port = Integer.parseInt(ip_port.split("_")[1]);

					sendData = ("MapperMonitor:" + adora.getPort()).getBytes(); // This
																				// .getPort()
																				// function
																				// tells
																				// the
																				// server
																				// to
																				// send
																				// reply
																				// to
																				// adora's
																				// socket
																				// port.
					sendPacket = new DatagramPacket(sendData, sendData.length,
							ip, port);
					// System.out.println("Port " + port);
					socket.send(sendPacket);
					// System.out.println("Sender has sent to " + ip_port);
				}
			}

			private void stopReceiver() throws IOException {
				DatagramPacket sendPacket;
				byte[] sendData = new byte[1024];

				InetAddress ip = InetAddress.getByName("localhost");
				int port = adora.getPort();
				sendData = "MonitorStop".getBytes();
				sendPacket = new DatagramPacket(sendData, sendData.length, ip,
						port);
				socket.send(sendPacket);
			}
		}

		private class Receiver extends Thread {
			private DatagramSocket socket;

			public Receiver() throws SocketException {
				socket = new DatagramSocket();
			}

			public int getPort() {
				return socket.getLocalPort();
			}

			public void run() {
				while (true) {

					while (true) {
						synchronized (startTime) {
							if (System.currentTimeMillis() >= startTime) {
								receiverHasReachedStartTime = true;
								if (senderHasReachedStartTime)
									startTime = Long.MAX_VALUE;
								break;
							}
						}
					}
					// System.out.println("Here!");
					while (true) {

						String key = "";
						try {
							key = receive();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (key.startsWith("MonitorStop"))
							break;
						servers.remove(key);
					}
					updatePortMap();
					System.out
							.println("Port Map has been updated! Monitor stops.");
					dudePool.printHashMap();
					synchronized (startTime) {
						startTime = System.currentTimeMillis() + INTERVAL;
					}
					receiverHasReachedStartTime = false;
				}
			}

			private String receive() throws IOException {// Format:
															// "ServerMonitor:<program>_<version>:<registered_port>"
				DatagramPacket receivePacket;
				byte[] receiveData = new byte[1024];
				receivePacket = new DatagramPacket(receiveData,
						receiveData.length);
				socket.receive(receivePacket);
				String content = new String(receivePacket.getData()).trim();
				if (content.startsWith("MonitorStop"))
					return content;
				String[] temp = content.split(":");
				String key = temp[1] + ":";
				key += receivePacket.getAddress().toString() + "_";
				key += temp[2]; // change the port to the registered port
				return key;
			}

			private void updatePortMap() {
				if (servers.size() == 0) {
					System.out.println("No server to be removed.");
					return;
				}
				for (String key : servers.keySet()) {
					String prog_ver = key.split(":")[0];
					String ip_port = key.split(":")[1];
					synchronized (portMap) {
						ServicePool pool = portMap.get(prog_ver);
						pool.deleteService(ip_port);
						System.out.println("ip_port = " + ip_port
								+ " has been removed.");
						if (pool.size() == 0) {
							portMap.remove(prog_ver);
							System.out.println("prog_ver = " + prog_ver
									+ " has been removed.");
						}
					}
				}
			}
		}
	}

	public void Announce() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				"../src/Announcement.java"));
		writer.write("/**\r\n");
		writer.write(" * This class stores public IP address and port number of the MAPPER.\r\n");
		writer.write(" */\r\n");
		writer.write("public class Announcement {\r\n");
		writer.write("\tpublic static final String MAPPER_IP = \"localhost\";\r\n");
		writer.write("\tpublic static final int MAPPER_PORT = 1111;\r\n");
		writer.write("}\r\n");
		writer.close();
	}

	/**
	 * Recieve a packet from Client/Server, and parse the content, ip and port,
	 * and stores the info into a requestInfo struct
	 * 
	 * @return
	 * @throws IOException
	 */
	public RequestInfo receive() throws IOException {
		// receive a request generally.
		DatagramPacket receivePacket;
		RequestInfo requestInfo = new RequestInfo();
		byte[] receiveData = new byte[1024];
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
		socket.receive(receivePacket);
		requestInfo.content = new String(receivePacket.getData()).trim();
		requestInfo.ip = receivePacket.getAddress();
		requestInfo.port = receivePacket.getPort();
		return requestInfo;
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Mapper mapper = new Mapper();
		mapper.monitor.launch();
		// mapper.Announce();
		while (true) {
			// receive a request generally.
			RequestInfo requestInfo = mapper.receive();
			mapper.dudePool.goDude(requestInfo);
		}
	}

}
