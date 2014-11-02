import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.corba.se.impl.ior.ByteBuffer;

public class WorkerPool {

	private static final int POOL_SIZE = 500;
	private ExecutorService fixedPool = null;
	private Integer activeThreadNum;
	public Cache<Integer, ReplyPacket> duplicateCache;
	private HashMap<Integer, ArrayList<DataPacket>> requestPacketsBuffer; // This
																			// buffer
																			// is
																			// to
																			// temporarily
																			// store
																			// the
																			// packets
																			// of
																			// a
																			// transaction.
	private HashMap<Integer, Vector<Integer>> packetsNumMap; // each vector is
																// an integer
																// pair, where
																// the first
																// means total
																// number of
																// packets and
																// the second
																// means the
																// present
																// number of
																// packets
																// received.

	private int programNum;
	private int versionNum;
	private int registeredPort;// registered port for the entire server, not for
								// a single worker
	Integer cnt = 0;

	public WorkerPool(String programNum, String versionNum, int port) {
		fixedPool = Executors.newFixedThreadPool(POOL_SIZE);
		activeThreadNum = 0;
		this.programNum = Integer.parseInt(programNum);
		this.versionNum = Integer.parseInt(versionNum);
		this.registeredPort = port;
		this.duplicateCache = new Cache<Integer, ReplyPacket>();
		this.requestPacketsBuffer = new HashMap<Integer, ArrayList<DataPacket>>();
		this.packetsNumMap = new HashMap<Integer, Vector<Integer>>();
	}

	public void goWorker(DatagramPacket receivedPacket) throws SocketException {
		fixedPool.submit(new Worker(receivedPacket));
	}

	private class Worker extends Thread {
		InetAddress IPAddress; // The ip address of the coming packet
		int senderPort; // The port of the coming packet
		String request;
		RequestPacket requestPacket; // decoded from the received packet
		DatagramPacket receivedPacket; // raw received packet

		public Worker(DatagramPacket receivedPacket) throws SocketException {
			IPAddress = receivedPacket.getAddress();
			senderPort = receivedPacket.getPort();
			// request is used to identify if the destination of the request
			// is to mapper or to server
			request = new String(receivedPacket.getData()).trim();
			this.receivedPacket = receivedPacket;

		}

		public void run() {

			if (request.startsWith("Mapper")) {
				if (request.startsWith("MapperMonitor")) {
					// respond with heartbeats
					try {
						heartBeatAnswer();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else if (request.startsWith("Ack")) {
				// System.out.println("received ACK!!!!!! " );
				String xidStr = request.split("_")[1];
				int xid = Integer.parseInt(xidStr);
				// 重发ACK， 做sync
				synchronized (duplicateCache) {
					if (duplicateCache.isHit(xid)) {
						// System.out.println("remove cache key " + xid);

						duplicateCache.remove(xid);
						duplicateCache.printCache();

					} else {
						System.out.println("Weird ACK, not hit!!!");
					}
				}

			} else // this message should come from the client to invoke
					// function
			{
				// activeThreadNum++;
				// System.out
				// .println("+[3]=======================================+");
				// System.out
				// .println("+----worker recieved client's packet--------+");
				// synchronized (cnt) {
				// System.err.print(++cnt + ", ");
				// }
				// return;
				ReplyPacket replyPacket = null;
				byte[] data = receivedPacket.getData();
				int xid = data[3] & 0xFF | (data[2] & 0xFF) << 8
						| (data[1] & 0xFF) << 16 | (data[0] & 0xFF) << 24;
				int sid = data[7] & 0xFF | (data[6] & 0xFF) << 8
						| (data[5] & 0xFF) << 16 | (data[4] & 0xFF) << 24;
				DataPacket dataPacket = new DataPacket(xid, sid, data);

				if (duplicateCache.isHit(xid)) {
					// System.out.println("cache hit!");

					if (duplicateCache.get(xid).result == null) // "null" means
																// is being
																// processed
						return;
					else
						replyPacket = duplicateCache.get(xid);
				} else {
					putDataPacketInHashTable(dataPacket);
				//	System.err
				//			.println(requestPacketsBuffer.get(xid).toString());
					if (requestPacketReady(xid)) { // all received
						synchronized (activeThreadNum) {
							activeThreadNum++;
						}
						requestPacket = recoverRequestPacket(xid);
						ReplyPacket running = new ReplyPacket();
						duplicateCache.put(xid, running); // "running": being
															// processed
						try {
							replyPacket = this.createReplyPacket();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						duplicateCache.put(xid, replyPacket);
					} else
						return;
				}

				try {
					System.out
							.println("+[5]=======================================+");
					System.out
							.println("+----worker sends result back----------------+");
					this.send(replyPacket);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				synchronized (activeThreadNum) {
					activeThreadNum--;
				}
				try {
					System.out
							.println("+[6]=======================================+");
					System.out
							.println("+--worker updates server workload to mapper--+");
					this.updateToMapper();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		private RequestPacket recoverRequestPacket(int xid) {
			// TODO Auto-generated method stub
			RequestPacket r;
			synchronized (requestPacketsBuffer) {
				r = RequestPacket.getPacketFromBytes(requestPacketsBuffer
						.get(xid));
				requestPacketsBuffer.remove(xid);
			}
			synchronized (packetsNumMap) {
				packetsNumMap.remove(xid);
			}

			return r;
		}

		private boolean requestPacketReady(int xid) {
			// TODO Auto-generated method stub
			synchronized (packetsNumMap) {
				Vector<Integer> pair = packetsNumMap.get(xid);
				// System.out.println(pair.toString());
				int total = pair.get(0);
				int cur = pair.get(1);
				if (total > 0 && total == cur)
					return true;
			}
			return false;
		}

		private void putDataPacketInHashTable(DataPacket d) {
			// TODO Auto-generated method stub

			synchronized (requestPacketsBuffer) {
				if (requestPacketsBuffer.containsKey(d.xid)) {
					ArrayList<DataPacket> list = requestPacketsBuffer
							.get(d.xid);
					if (d.sid == 0) {// header packet
						if (list.get(0).sid != 0) {
							list.add(0, d);
							// update orderOfPackets
							int num = d.data[11] & 0xFF
									| (d.data[10] & 0xFF) << 8
									| (d.data[9] & 0xFF) << 16
									| (d.data[8] & 0xFF) << 24;
							synchronized (packetsNumMap) {
								Vector<Integer> pair = packetsNumMap.get(d.xid);
								pair.set(0, num);
								pair.set(1, pair.get(1) + 1);
							}
						}
					} else {
						int i;
						boolean isDuplicatePacket = false;
						int size = list.size();
						if (size == 0)
							list.add(d);
						else {
							for (i = size - 1; i >= 0; i--) {
								if (d.sid > list.get(i).sid) {
									list.add(i + 1, d);
									break;
								} else if (d.sid == list.get(i).sid) // duplicate
																		// packet,
																		// resulting
																		// from
																		// retransmission
								{
									isDuplicatePacket = true;
									break;
								} else
									continue;
							}
							if (i == -1) // should be added to the very
								list.add(0, d);
						}
						if (!isDuplicatePacket) {
							synchronized (packetsNumMap) {
								Vector<Integer> pair = packetsNumMap.get(d.xid);
								pair.set(1, pair.get(1) + 1);
							}
						}
					}
				} else {
					ArrayList<DataPacket> newList = new ArrayList<DataPacket>();
					newList.add(d);
					requestPacketsBuffer.put(d.xid, newList);
					Vector<Integer> pair = new Vector<Integer>();
					if (d.sid == 0) { // header packet
						int num = d.data[11] & 0xFF | (d.data[10] & 0xFF) << 8
								| (d.data[9] & 0xFF) << 16
								| (d.data[8] & 0xFF) << 24;
						pair.add(num);
						pair.add(1);
					} else {
						pair.add(-1); // -1 means have not got the total number
										// yet
						pair.add(1);
					}
					synchronized (packetsNumMap) {
						packetsNumMap.put(d.xid, pair);
					}
				}
			}

		}

		/**
		 * parse the request and invoke the corresponding procedure client's
		 * request format <proc_num>:<argument>
		 * 
		 * @throws InterruptedException
		 */
		// TODO server unpackaging
		private ReplyPacket createReplyPacket() throws InterruptedException {
			int procNum = requestPacket.procNum;
			int xid = requestPacket.xid;
			Object result;
			char resultType;
			/*
			 * String[] temp = request.split(":"); int procedureNumer =
			 * Integer.parseInt(temp[0]); int argument =
			 * Integer.parseInt(temp[1]);
			 */
			switch (procNum) {
			case 1:
				int argument = (Integer) requestPacket.args[0];
				result = Procedures.square(argument);
				resultType = 0;
				break;
			case 2:
				argument = (Integer) requestPacket.args[0];
				result = Procedures.cube(argument);
				resultType = 0;
				break;
			case 3:
				int[] args = (int[]) requestPacket.args[0];
				result = Procedures.max(args);
				resultType = 0;
				break;
			case 4:
				args = (int[]) requestPacket.args[0];
				Procedures.sort(args);
				result = args;
				resultType = 1;
				break;
			case 5:
				int[][] A = (int[][]) requestPacket.args[0];
				int[][] B = (int[][]) requestPacket.args[1];
				result = (int[][]) Procedures.multiply(A, B);
				resultType = 2;
				break;

			default:
				result = new String("Invalid procedure num!\n");
				resultType = 3;// result Type 3 means String
				break;
			}
			ReplyPacket replyPacket = new ReplyPacket(xid, programNum,
					versionNum, procNum, resultType, result);
			return replyPacket;
		}

		/**
		 * send the reply to the client
		 * 
		 * @throws IOException
		 * @throws InterruptedException
		 */
		private void send(ReplyPacket replyPacket) throws IOException,
				InterruptedException {
			// send back the reply
			// System.out.println("reply from server: " + reply);
			ArrayList<byte[]> data = replyPacket.getBytes();
			// Get the IP and port of the client
			DatagramSocket socket = new DatagramSocket();
			for (byte[] sendData : data) {
				Thread.sleep(2);
				DatagramPacket sendPacket = new DatagramPacket(sendData,
						sendData.length, IPAddress, senderPort);
				socket.send(sendPacket);
			}
			socket.close();
		}

		/**
		 * Update the number of active threads to Mapper.
		 * 
		 * @throws IOException
		 */
		private void updateToMapper() throws IOException {
			byte[] sendData = new byte[1024];
			String updateInfo = "ServerUpdate:" + programNum + "_" + versionNum
					+ ":" + registeredPort + ":";
			synchronized (activeThreadNum) {
				updateInfo += activeThreadNum.toString();
			}
			sendData = updateInfo.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length,
					InetAddress.getByName(Announcement.MAPPER_IP),
					Announcement.MAPPER_PORT);
			DatagramSocket socket = new DatagramSocket();
			socket.send(sendPacket);
			socket.close();
		}

		/**
		 * This is to answer the heart beat request from Mapper.
		 * 
		 * @throws IOException
		 */
		private void heartBeatAnswer() throws IOException {
			System.out
					.println("===========[Reply Heart Beat Request]===========");
			duplicateCache.printCache();
			byte[] sendData = new byte[1024];
			String updateInfo = "ServerMonitor:" + programNum + "_" + versionNum
					+ ":" + registeredPort;
			int port = Integer.parseInt(new String(receivedPacket.getData())
					.trim().split(":")[1]);
			sendData = updateInfo.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length, receivedPacket.getAddress(), port);
			DatagramSocket socket = new DatagramSocket();
			socket.send(sendPacket);
			socket.close();
		}
	}
}
