import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.swing.text.html.StyleSheet.ListPainter;

public class RequestPacket {
	// globals
	public static final int NUM_INTEGERS = 2560; // defines the safe size of udp
													// byte[], in int size, 4
													// Bytes

	private ArrayList<byte[]> listOfPackets;
	private ByteBuffer buffer; // this buffer works for each data packet first,
								// then for the header at last
	int currentSID;

	public int xid;
	public int programNum;
	public int versionNum;
	public int procNum;
	public int argNum;
	public char[] argType;
	public Object[] args;


	public RequestPacket(int xid, int programNum, int versionNum, int procNum,
			int argNum, char[] argType, Object[] args) {
		this.xid = xid;
		this.programNum = programNum;
		this.versionNum = versionNum;
		this.procNum = procNum;
		this.argNum = argNum;
		this.argType = argType;
		this.args = args;

		listOfPackets = new ArrayList<byte[]>();
	//	buffer = ByteBuffer.allocate(4 * NUM_INTEGERS);
		currentSID = 1;

	}

	/**
	 * It returns an array of byte[] because the data to be sent may be split
	 * into several udp packets.
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public ArrayList<byte[]> getBytes() {
		buffer = ByteBuffer.allocate(4 * NUM_INTEGERS);
		buffer.putInt(xid);
		buffer.putInt(currentSID);

		/**
		 * This for loop and following if branch deal with adding
		 * data to the list of packets.
		 */
		for (int i = 0; i < argNum; i++) {
			putIntInBuffer((int) argType[i]);
			switch (argType[i]) {
			case 0:// int
				putIntInBuffer((Integer) args[i]);
				break;
			case 1:// int[]
				int[] A = (int[]) args[i];
				int size = A.length;
				putIntInBuffer(size);
				for (int j = 0; j < size; j++)
					putIntInBuffer(A[j]);
				break;
			case 2:// int[][]
				int[][] C = (int[][]) args[i];
				int m = C.length,
				n = C[0].length;
				putIntInBuffer(m);
				putIntInBuffer(n);
				for (int k = 0; k < m; k++)
					for (int j = 0; j < n; j++)
						putIntInBuffer(C[k][j]);
				break;
			default:
				System.out.println("unknown type");
			}
		}
		if (buffer.position() != 8) { // this means the last packet has not been
									// added to the listOfPackets yet. It only
									// contains the header, 8 bytes
			listOfPackets.add(buffer.array());
			buffer.clear();
		}
		/**
		 * This part deals with adding the header to the list of packets.
		 * It has the SID to be 0.
		 */
		buffer = ByteBuffer.allocate(NUM_INTEGERS * 4);
		buffer.putInt(xid);
		buffer.putInt(0); // SID
		buffer.putInt(listOfPackets.size() + 1); // plus itself
		buffer.putInt(programNum);
		buffer.putInt(versionNum);
		buffer.putInt(procNum);
		buffer.putInt(argNum);
		
		listOfPackets.add(buffer.array());
		return listOfPackets;
	}

	private void putIntInBuffer(int value) {
		buffer.putInt(value);
		if (buffer.position() == buffer.capacity()) {
			listOfPackets.add(buffer.array());
			buffer.clear();
			buffer = ByteBuffer.allocate(NUM_INTEGERS * 4);
			buffer.putInt(xid);
			buffer.putInt(++currentSID);
		}
	}

	static public RequestPacket getPacketFromBytes(ArrayList<DataPacket> list) {
		System.out.println("+[4]=======================================+");
		System.out.println("+----Server demarshalls client's request---+");
		ByteBuffer bytebuffer = ByteBuffer.allocate(NUM_INTEGERS * list.size() * 4);
		
		/**
		 * This part deals with decoding the header packet.
		 */
		bytebuffer.put(list.get(0).data);
		bytebuffer.rewind();
		int xid = bytebuffer.getInt();
		bytebuffer.getInt(); // skip sid
		bytebuffer.getInt(); // skip total number of packets
		int programNum = bytebuffer.getInt();
		int versionNum = bytebuffer.getInt();
		int procNum = bytebuffer.getInt();
		int argNum = bytebuffer.getInt();
		
		/**
		 * Then we deal with decoding all the data packets.
		 */
		bytebuffer.clear();
		for(int i = 1; i < list.size(); i ++){
			byte[] data = list.get(i).data;
			bytebuffer.put(data, 8, data.length - 8);
		}
		bytebuffer.rewind();
		char[] argType = new char[argNum];
		Object[] args = new Object[argNum];
		for (int i = 0; i < argNum; i++) {
			argType[i] = (char) bytebuffer.getInt();
			switch (argType[i]) {
			case 0:// int
				args[i] = bytebuffer.getInt();
				break;
			case 1:// int[]
				int size = bytebuffer.getInt();
				int[] A = new int[size];
				for (int j = 0; j < size; j++)
					A[j] = bytebuffer.getInt();
				args[i] = A;
				break;
			case 2:// int[][]
				int m = bytebuffer.getInt();
				int n = bytebuffer.getInt();
				int[][] C = new int[m][n];
				for (int k = 0; k < m; k++)
					for (int j = 0; j < n; j++)
						C[k][j] = bytebuffer.getInt();
				args[i] = C;
				break;

			default:
				System.out.println("unknown type");
			}
		}
		System.out.println("xid#: " + xid);
		System.out.println("prog#: " + programNum);
		System.out.println("versionNum#: " + versionNum);
		System.out.println("procNum#: " + procNum);
		System.out.println("argNum#: " + argNum);

		return new RequestPacket(xid, programNum, versionNum, procNum, argNum,
				argType, args);
	}

//	public static void main(String[] args) {
//		RequestPacket inst = new RequestPacket(0, 0, 0, 0, 0, null, null);
//		System.out.println(inst.buffer.position());
//		inst.buffer.putInt(0);
//		System.out.println(inst.buffer.position() == inst.buffer.capacity());
//		inst.buffer.putInt(0);
//	}
}