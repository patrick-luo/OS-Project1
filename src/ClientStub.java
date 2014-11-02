/*
 * bug: active thread num
 * bug：重发会出现收不到完整的包 java.lang.OutOfMemoryError: GC overhead limit exceeded
 */
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ClientStub {
	private DatagramSocket clientSocket;
	private InetAddress serverIp;
	private int serverPort;
	private InetAddress mapperIp;
	private int mapperPort;
	private int xid;

	private ArrayList<DataPacket> receivedPacketsBuffer;

	public ClientStub() throws SocketException, UnknownHostException {
		clientSocket = new DatagramSocket();
		mapperIp = InetAddress.getByName(Announcement.MAPPER_IP);
		mapperPort = Announcement.MAPPER_PORT;
	}

	public Object doProcedure(String programNum, String versionNum,
			String procedureNum, Object[] arguments) throws IOException,
			InterruptedException, ExecutionException {
		byte[] dataToMapper = ("Hello:" + programNum + "_" + versionNum)
				.getBytes();
		sendPortRequestToMapper(dataToMapper);
		String replyFromMapper = receiveFromMapper();
		parseServerIpPort(replyFromMapper);

		ArrayList<DatagramPacket> listOfUDPPacket = createUdpPacket(programNum,
				versionNum, procedureNum, arguments);

		// System.out.println("+----Client sends request to Server--------+");

		// 出错重传 bug
		// clientSocket.setSoTimeout(5000 * 1000); // set the timeout of
		// receiving
		// to be some Milliseconds.

		// clientSocket sends the list of udp packets to server
		for (int i = 0; i < listOfUDPPacket.size(); i++) {
			clientSocket.send(listOfUDPPacket.get(i));
		//	System.out.print(i + ", ");
			Thread.sleep(2);
		}
		
//		long startTime;
//		long timeout = 5 * 1000;
		
		
		
		

		ExecutorService executor = Executors.newSingleThreadExecutor();
		
		int timeout = 1;
		while (true) {
			Future<Object> future = executor.submit(new Task());
			try {
				
				System.out.println("Started..");
				Object result = future.get(timeout, TimeUnit.SECONDS);
				System.out.println("finished...");
				executor.shutdownNow();
				return result;
			} catch (TimeoutException e) {
				timeout = 60;
				System.out.println("Canceled? " + future.cancel(true));
				System.out.println("Timeout reached!!! retransmit!");
//				for (int i = 0; i < listOfUDPPacket.size(); i++) {
//					clientSocket.send(listOfUDPPacket.get(i));
//				//	System.out.print(i + ", ");
//					Thread.sleep(2);
//				}
			}
		}

	}
	
	

	class Task implements Callable<Object> {
		@Override
		public Object call() throws Exception {
			System.out.println("yu qiao");
			Object replyFromServer = receiveFromServer();
			System.out.println("Done receiving");
			byte[] ackData = ("Ack" + "_" + xid).getBytes();
			sendAckToServer(ackData);
			clientSocket.close();
			return replyFromServer;
		}
	}

	private void sendAckToServer(byte[] ackData) throws IOException {
		System.out.println("+==========================================+");
		System.out.println("+----Client sends request to Mapper--------+");
		DatagramPacket sendPacket = new DatagramPacket(ackData, ackData.length,
				serverIp, serverPort);
		clientSocket.send(sendPacket);
	}

	// TODO
	private Object receiveFromServer() throws IOException {
		System.out.println("+[4]=======================================+");
		System.out.println("+----Client receive result from Server-----+");

		receivedPacketsBuffer = new ArrayList<DataPacket>();
		int totalNum = -1;
		int recNum = 0;
		while (!(totalNum > 0 && totalNum == recNum)) {
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			clientSocket.receive(receivePacket);

			byte[] data = receivePacket.getData();
			int xid = data[3] & 0xFF | (data[2] & 0xFF) << 8
					| (data[1] & 0xFF) << 16 | (data[0] & 0xFF) << 24;
			int sid = data[7] & 0xFF | (data[6] & 0xFF) << 8
					| (data[5] & 0xFF) << 16 | (data[4] & 0xFF) << 24;
			DataPacket dataPacket = new DataPacket(xid, sid, data);
			if (sid == 0) {
				totalNum = data[11] & 0xFF | (data[10] & 0xFF) << 8
						| (data[9] & 0xFF) << 16 | (data[8] & 0xFF) << 24;
			}
			putDataPacketIntoBuffer(dataPacket);
			recNum++;
		}
		// 收齐了包裹放在buffer里面， 准备组装成一个replyPacket的类
		ReplyPacket replyPacket = ReplyPacket
				.getPacketFromBytes(receivedPacketsBuffer);

		return replyPacket.result;
	}

	private void putDataPacketIntoBuffer(DataPacket d) {
		// TODO Auto-generated method stub
		int size = receivedPacketsBuffer.size();

		if (size == 0)
			receivedPacketsBuffer.add(d);
		else {
			int i;
			for (i = size - 1; i >= 0; i--) {
				if (receivedPacketsBuffer.get(i).sid < d.sid) {
					receivedPacketsBuffer.add(i + 1, d);
					break;
				} else if (receivedPacketsBuffer.get(i).sid == d.sid) {
					System.err.println("How can this be???");
					break;
				}
			}
			if (i == -1)
				receivedPacketsBuffer.add(0, d);
		}

	}

	private String receiveFromMapper() throws IOException {
		byte[] receiveData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData,
				receiveData.length);
		clientSocket.receive(receivePacket);
		String reply = new String(receivePacket.getData());
		reply.trim();
		reply = reply.substring(0, reply.indexOf(0));
		return reply;
	}

	private ArrayList<DatagramPacket> createUdpPacket(String programNum,
			String versionNum, String procedureNum, Object[] argument)
			throws IOException, InterruptedException {
		System.out.println("+[3]=======================================+");
		System.out.println("+----create udp packet---------------------+");
		RequestPacket requestPacket = createRequestPacket(programNum,
				versionNum, procedureNum, argument);
		ArrayList<byte[]> dataToServer = requestPacket.getBytes();
		ArrayList<DatagramPacket> udpPackets = new ArrayList<DatagramPacket>();
		for (byte[] data : dataToServer) {
			udpPackets.add(new DatagramPacket(data, data.length, this.serverIp,
					this.serverPort));
		}
		return udpPackets;
	}

	private RequestPacket createRequestPacket(String programNum,
			String versionNum, String procedureNum, Object[] argument) {

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		xid = new Random().nextInt();
		int program = Integer.parseInt(programNum);
		int version = Integer.parseInt(versionNum);
		int procedure = Integer.parseInt(procedureNum);
		int argNum = argument.length;
		char[] argType = new char[argNum];

		switch (procedure) {
		case 1:
			argType[0] = 0;
			break;
		case 2:
			argType[0] = 0;
			break;
		case 3:
			argType[0] = 1;
			break;
		case 4:
			argType[0] = 1;
			break;
		case 5:
			argType[0] = 2;
			argType[1] = 2;
		}

		return new RequestPacket(xid, program, version, procedure, argNum,
				argType, argument);

	}

	private void sendPortRequestToMapper(byte[] sendData) throws IOException {
		System.out.println("+[1]=======================================+");
		System.out.println("+----Client sends request to Mapper--------+");
		DatagramPacket sendPacket = new DatagramPacket(sendData,
				sendData.length, mapperIp, mapperPort);
		clientSocket.send(sendPacket);

	}

	private void parseServerIpPort(String reply) throws UnknownHostException {
		// reply.trim();
		System.out.println("+[2]=======================================+");
		System.out.println("+-----------Mapper replies to client-------+");
		String ipStr = reply.substring(1, reply.indexOf('_'));
		System.out.println("Mapper respondes with available service address ");
		System.out.println("IP: " + ipStr);
		this.serverIp = InetAddress.getByName(ipStr);
		String portStr = reply.substring(reply.indexOf('_') + 1);
		System.out.println("Port: " + portStr);
		this.serverPort = Integer.parseInt(portStr);

	}
}
