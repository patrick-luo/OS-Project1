import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TestServer {
	// TODO programNumber & versionNumber data struct modify, <prog, vers> list
	private String programNumber;
	private String versionNumber;
	private int serverPort; 

	private DatagramSocket serverSocket;// used to receive packets from mapper
										// or client
	private WorkerPool workerPool;

	public TestServer(String programNumber, String versionNumber, String portNum)
			throws SocketException { // TODO hard coded
										// programNumber = "123";
										// versionNumber = "1";
		this.programNumber = programNumber;
		this.versionNumber = versionNumber;
		try {
			serverPort = Integer.parseInt(portNum);
		} catch (Exception e) {
			System.out.println("invalid port number: must be number");
			System.exit(-1);
		}
		serverSocket = new DatagramSocket(serverPort);
		workerPool = new WorkerPool(programNumber, versionNumber, serverPort);
	}

	/**
	 * save the request from the user to this.request
	 */
	public DatagramPacket receive() throws IOException {
		// receive a request generally.
		byte[] receiveData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData,
				receiveData.length);
		serverSocket.receive(receivePacket);
	//	String request = new String(receivePacket.getData()).trim();
	//	System.out.println("RECEIVED:\n" + request);
		return receivePacket;
	}

	public void register() throws IOException {
		System.out.println("+[1]=======================================+");
		System.out.println("+=====Server registers it self to Mapper===+");

		InetAddress IPAddress = InetAddress.getByName(Announcement.MAPPER_IP);
		byte[] sendData = new byte[1024];
		byte[] receiveData = new byte[1024];
		String register = "ServerRegister:" + programNumber + "_"
				+ versionNumber;
		System.out.println("programNum: " + programNumber);
		System.out.println("versionNumber: " + versionNumber);
		System.out.println("portNum: " + serverPort);
		sendData = register.getBytes();
		// binding: 1111 is the port for mapper
		DatagramPacket sendPacket = new DatagramPacket(sendData,
				sendData.length, IPAddress, Announcement.MAPPER_PORT);
		serverSocket.send(sendPacket);

		// wait for the response from the mapper
		DatagramPacket receivePacket = new DatagramPacket(receiveData,
				receiveData.length);
		serverSocket.receive(receivePacket);
		String feedback = new String(receivePacket.getData()).trim();
		System.out.println("+[2]=======================================+");
		System.out.println("+=====Mapper replies to server=============+");
		System.out.println(feedback);
		// serverSocket.close();
	}

	public static void main(String[] args) throws IOException {
		int cnt = 0;
		// TODO Auto-generated method stub
		if (args.length != 3) {
			System.out
					.println("Usage: java TestServer <progNum> <VersionNum> <PortNum>");
			System.exit(-1);
		}

		TestServer server = new TestServer(args[0], args[1], args[2]);
		server.register();

		while (true) {
			// receive a request generally.
			DatagramPacket receivePacket = server.receive();
		//	System.err.print(++cnt + ", ");
			server.workerPool.goWorker(receivePacket);
		}

	}

}
