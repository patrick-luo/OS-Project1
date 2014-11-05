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
	private int clientPort;
	private boolean mapperChanged;

	private ArrayList<DataPacket> receivedPacketsBuffer;
	
	int totalNum;
	int recNum;

	public ClientStub(boolean mapperChanged) throws SocketException, UnknownHostException {
		clientSocket = new DatagramSocket();
		clientPort = clientSocket.getLocalPort();
		this.mapperChanged = mapperChanged;
		if(this.mapperChanged){
			mapperIp = InetAddress.getByName(ShadowMapperAnnouncement.SHADOW_MAPPER_IP);
			mapperPort = ShadowMapperAnnouncement.SHADOW_MAPPER_PORT;
		}
		else{
			mapperIp = InetAddress.getByName(MasterMapperAnnouncement.MASTER_MAPPER_IP);
			mapperPort = MasterMapperAnnouncement.MASTER_MAPPER_PORT;
		}
		
		
		receivedPacketsBuffer = new ArrayList<DataPacket>();
		
		totalNum = -1;
		recNum = 0;
	}
	
	public boolean mapperIsChanged(){
		return mapperChanged;
	}

	public Object doProcedure(String programNum, String versionNum,
			String procedureNum, Object[] arguments) throws IOException,
			InterruptedException, ExecutionException {
		
		InetAddress localIP = InetAddress.getByName("localhost");
        int index = localIP.toString().indexOf('/');
        byte[] dataToMapper = ("Hello:" + programNum + "_" +versionNum + ":"+localIP.toString().substring(index+1)+":" + clientPort)
                        .getBytes(); // port Num doesn't matter
        
        String replyFromMapper = null;
        clientSocket.setSoTimeout(2000);
        
		for(int i = 1; replyFromMapper == null; i ++)
		{
			sendPortRequestToMapper(dataToMapper);
			replyFromMapper = receiveFromMapper();
			if(replyFromMapper != null){
				parseServerIpPort(replyFromMapper);
				if(verifyServer(programNum, versionNum)) // procedure 0)
					break;
				else
				{
					replyFromMapper = null;
					i --;
				}
			}
			if(i == 4)
			{
				System.out.println("Switch to backup mapper...");
				mapperIp = InetAddress.getByName(ShadowMapperAnnouncement.SHADOW_MAPPER_IP);
				mapperPort = ShadowMapperAnnouncement.SHADOW_MAPPER_PORT;
			}
			if(i > 6)
			{
				System.out.println("Both Mapers seem down...");
				System.exit(-1);
			}
		}

		ArrayList<DatagramPacket> listOfUDPPacket = createUdpPacket(programNum,
				versionNum, procedureNum, arguments);

		// System.out.println("+----Client sends request to Server--------+");

		// 出错重传 bug
		// clientSocket.setSoTimeout(5000 * 1000); // set the timeout of
		// receiving
		// to be some Milliseconds.

		// clientSocket sends the list of udp packets to server
		System.out.println("Sending request packets to the server.");
		
		for (int i = 0; i < listOfUDPPacket.size(); i++) {
			clientSocket.send(listOfUDPPacket.get(i));
		//	System.out.print(i + ", ");
			Thread.sleep(2);
		}
		
		System.out.println("Done sending all the request packets.");
		
		long timeout = 200 * 1000;
		clientSocket.setSoTimeout(100);
		
		while(true)
		{
		//	System.out.println("The timeout is " + timeout);
			Object replyFromServer = receiveFromServer((System.currentTimeMillis() + timeout));
		//	System.out.println("The reply is " + replyFromServer);
			if(replyFromServer == null){
				// retransmisstion triggered
				System.out.println("Timeout reached!!! retransmit!");
			//	System.out.println("Sending request packets to the server.");
			//	for (int i = 0; i < listOfUDPPacket.size(); i++) {
				for (int i = 0; i < 1; i++) {
					clientSocket.send(listOfUDPPacket.get(i));
				//	System.out.print(i + ", ");
					Thread.sleep(2);
				}
			//	System.out.println("Done sending all the request packets.");
				timeout += 2 * 1000;
			}
				
			else{
				System.out.println("Done receiving all the reply packets");
				byte[] ackData = ("Ack" + "_" + xid).getBytes();
				sendAckToServer(ackData);
				clientSocket.close();
				return replyFromServer;
			}
		}

	}
	
	

	private boolean verifyServer(String programNum, String versionNum) throws IOException {
		// TODO Auto-generated method stub
		byte[] data = ("Procedure0:" + programNum + "_" + versionNum).getBytes();
		byte[] receiveData = new byte[1024];

		String feedback = "";
        clientSocket.setSoTimeout(3000);
        
		for(int i = 1; feedback.length() == 0; i ++)
		{
			DatagramPacket sendPacket = new DatagramPacket(data, data.length, this.serverIp,
					this.serverPort);
			clientSocket.send(sendPacket);

			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			try{
				clientSocket.receive(receivePacket);
			}
			catch(SocketTimeoutException e)
			{
				System.out.println("Try again to reach the server...");
			}
			
			feedback = new String(receivePacket.getData()).trim();
			
			if(i >= 3)
			{
				System.out.println("Server seems unreachable...");
				return false;
			}
		}
		System.out.println(feedback);
		if(feedback.equals("Verification success!"))
			return true;
		else
			return false;
	}

	private void sendAckToServer(byte[] ackData) throws IOException {
		System.out.println("+==========================================+");
		System.out.println("+----Client sends ACK to server--------+");
		DatagramPacket sendPacket = new DatagramPacket(ackData, ackData.length,
				serverIp, serverPort);
		clientSocket.send(sendPacket);
	}

	// TODO
	private Object receiveFromServer(long timeOfStop) throws IOException {
		System.out.println("+[4]=======================================+");
		System.out.println("+----Client is receiving result from Server-----+");

	//	receivedPacketsBuffer.clear();

		while (!(totalNum > 0 && totalNum == recNum)) {
			if(System.currentTimeMillis() >= timeOfStop){
				System.out.println("Time to stop!");
				return null;
			}
				
			byte[] receiveData = new byte[RequestPacket.NUM_INTEGERS * 4];
			DatagramPacket receivePacket = new DatagramPacket(receiveData,
					receiveData.length);
			try{
				clientSocket.receive(receivePacket);
			}
			catch(SocketTimeoutException e){
			//	System.out.println("Receive the next packet!");
				continue;
			}
			

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
		}
		// 收齐了包裹放在buffer里面， 准备组装成一个replyPacket的类
		ReplyPacket replyPacket = ReplyPacket
				.getPacketFromBytes(receivedPacketsBuffer);

		return replyPacket.result;
	}

	private void putDataPacketIntoBuffer(DataPacket d) {
		// TODO Auto-generated method stub
		int size = receivedPacketsBuffer.size();

		if (size == 0){
			receivedPacketsBuffer.add(d);
			recNum ++;
		}
		else {
			int i;
			for (i = size - 1; i >= 0; i--) {
				if (receivedPacketsBuffer.get(i).sid < d.sid) {
					receivedPacketsBuffer.add(i + 1, d);
					recNum ++;
					break;
				} else if (receivedPacketsBuffer.get(i).sid == d.sid) {
				//	System.err.println("How can this be???");
					break;
				}
			}
			if (i == -1){
				receivedPacketsBuffer.add(0, d);
				recNum ++;
			}
		}

	}

	private String receiveFromMapper() throws IOException {
		byte[] receiveData = new byte[1024];
		DatagramPacket receivePacket = new DatagramPacket(receiveData,
				receiveData.length);
		try{
			clientSocket.receive(receivePacket);
		}
		catch(SocketTimeoutException e){
			System.out.println("Mapper seems down...");
			return null;
		}
		
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
