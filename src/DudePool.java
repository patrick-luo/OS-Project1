import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DudePool {

	private static final int POOL_SIZE = 10;
	private HashMap<String, ServicePool> portMap = null;
	private ExecutorService fixedPool = null;
	private boolean isMaster;

	public DudePool(HashMap<String, ServicePool> portMap, boolean isMaster) {
		fixedPool = Executors.newFixedThreadPool(POOL_SIZE);
		this.portMap = portMap;
		this.isMaster = isMaster;
	}

	public void goDude(RequestInfo requestInfo) {
		fixedPool.submit(new Dude(requestInfo));
	}

	public void printHashMap() {
		synchronized (portMap) {
			if (portMap.size() == 0) {
				System.out.println("The port map contains nothing.");
				return;
			}
			System.out.println("The port map is as follows:");
			for (String key : this.portMap.keySet()) {
				ServicePool servicePool = portMap.get(key);
				// System.out.println("key = " + key + " value: ");
				System.out.print("[#" + key + "]->");

				servicePool.printServicePool();
			}
		}
	}

	private class Dude extends Thread {
		private RequestInfo requestInfo = null;

		public Dude(RequestInfo requestInfo) {
			this.requestInfo = requestInfo;
		}

		public void run() {
			boolean isForwarded = false;
			String reply = "";
			if (requestInfo.content.startsWith("Hello")) {

				if (requestInfo.content.contains("forward"))
				{
					isForwarded = true;
				}
				reply = this.answerClient(requestInfo);

				if (isMaster) {
					System.out.println("Synchronize to shadow Mapper.");
					DatagramPacket synchronizePacket;
					requestInfo.content += ":" + "forward";
					byte[] sendData = requestInfo.content.getBytes();

					InetAddress shadowMapperIp;
					try {
						shadowMapperIp = InetAddress
								.getByName(ShadowMapperAnnouncement.SHADOW_MAPPER_IP);
						synchronizePacket = new DatagramPacket(sendData,
								sendData.length, shadowMapperIp,
								ShadowMapperAnnouncement.SHADOW_MAPPER_PORT);
						DatagramSocket socket;
						socket = new DatagramSocket();
						socket.send(synchronizePacket);
						socket.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			} else if (requestInfo.content.startsWith("Server")) {
				if (requestInfo.content.startsWith("ServerUpdate"))
				{
					if(requestInfo.content.contains("forward"))
						isForwarded = true;
					reply = updateServerInfo(requestInfo);
				}
				else
				{
					if(requestInfo.content.contains("forward"))
						isForwarded = true;
					reply = this.registerServer(requestInfo);
				}
				if (isMaster) {
					System.out.println("Synchronize to shadow Mapper.");
					DatagramPacket synchronizePacket;
					requestInfo.content += ":" + "forward";
					byte[] sendData = requestInfo.content.getBytes();

					InetAddress shadowMapperIp;
					try {
						shadowMapperIp = InetAddress
								.getByName(ShadowMapperAnnouncement.SHADOW_MAPPER_IP);
						synchronizePacket = new DatagramPacket(sendData,
								sendData.length, shadowMapperIp,
								ShadowMapperAnnouncement.SHADOW_MAPPER_PORT);
						DatagramSocket socket;
						socket = new DatagramSocket();
						socket.send(synchronizePacket);
						socket.close();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
					
			} else
				reply = "Please specify if you request as a client or register as a server.";
			try {
				if(isMaster ||!isForwarded)
					this.send(requestInfo, reply);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void send(RequestInfo requestInfo, String reply)
				throws IOException {
			// send back the reply
			DatagramPacket sendPacket;
			byte[] sendData = new byte[1024];
			sendData = reply.getBytes();
			sendPacket = new DatagramPacket(sendData, sendData.length,
					requestInfo.ip, requestInfo.port);
			DatagramSocket socket = new DatagramSocket();
			socket.send(sendPacket);
			socket.close();
		}

		/**
		 * receive query from a client. Valid query should have this format:
		 * "Hello:<Program#>_<Version#>"
		 */
		public String answerClient(RequestInfo requestInfo) {
			String reply = "";
			try {
				String key = requestInfo.content.split(":")[1];
				System.out
						.println("+[1]=======================================+");
				System.out
						.println("+=====Mapper receives request from client==+");
				System.out
						.println("asking for service: <Program#>_<Version#> = "
								+ key);
				synchronized (portMap) {
					if (portMap.containsKey(key)) {
						try {
							reply = portMap.get(key).getNextService();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					else
						reply = "Can't find (Program#_Version#) = " + key;
				}
			} catch (Exception e) {
				reply = "Please offer a valid query format: \"Hello:<Program#>_<Version#>\"";
			}
			printHashMap();
			System.out.println("+[2]=======================================+");
			System.out.println("+=Mapper returns service address to client=+");
			System.out.println("reply msg /<IP>_<port>: " + reply);

			return reply;
		}

		/**
		 * receive register info from a server. Valid info should have this
		 * format: "Server:<Program#>_<Version#>:<IPaddress>:<port>"
		 */
		public String registerServer(RequestInfo requestInfo) {
			String reply;
			try {
				String key = requestInfo.content.split(":")[1];
				System.out
						.println("+[1]=======================================+");
				System.out
						.println("+=====Mapper registers for server==========+");
				System.out
						.println("resigtering for service: <Program#>_<Version#> = "
								+ key);
				synchronized (portMap) {
					if (portMap.containsKey(key)) {
						// TODO add new port to the list
						ServicePool servicePool = portMap.get(key);
						servicePool.addService(requestInfo.toString());
					} else {
						// TODO UDP port != server number
						ServicePool newServicePool = new ServicePool(
								requestInfo.toString());
						portMap.put(key, newServicePool);
					}
				}
				reply = "MapperReply: Register Success!";
			} catch (Exception e) {
				reply = "MapperReply: Please offer a valid query format: \"Server:<Program#>_<Version#>\"";
			}
			// DEBUG
			printHashMap();
			return reply;
		}

		public String updateServerInfo(RequestInfo requestInfo) {// Format:
		// "ServerUpdate:<program>_<version>:<IPaddress>:<communication port>:<registered_port>:<num_threads>"
			String reply = "";
			try {
				String[] temp = requestInfo.content.split(":");
				String key = temp[1];
				
				int threadNum = Integer
						.parseInt(requestInfo.content.split(":")[4]);
				System.out
						.println("+[1]=======================================+");
				System.out
						.println("+=Mapper recieves update server workload===+");
				System.out
						.println("<program>_<version>:<registered_port>:<num_threads> "
								+ key
								+ " "
								+ requestInfo.port
								+ " "
								+ threadNum);
				synchronized (portMap) {
					if (portMap.containsKey(key)) {
						// TODO add new port to the list
						ServicePool servicePool = portMap.get(key);
						String ip_port = requestInfo.ip.toString() + "_";
						ip_port += temp[4];
						if (servicePool.updateService(ip_port,
								threadNum))
							reply = "MapperReply: Update Success!";
						else
							reply = "MapperReply: You service is obselete. Please re-register your service";
					} else {
						reply = "MapperReply: You service is obselete. Please re-register your service";
					}
				}
			} catch (Exception e) {
				reply = "MapperReply: Please offer a valid update format: \"ServerUpdate:<Program#>_<Version#>:<ThreadNum>\"";
			}
			printHashMap();
			return reply;
		}

	}
}
