import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ReplyPacket {
	int xid;
	int programNum;
	int versionNum;
	int procNum;

	char resultType;// 0 = int; 1 = int[]; 2 = String
	Object result;

	private ArrayList<byte[]> listOfPackets;
	private ByteBuffer buffer;
	int currentSID;

	public ReplyPacket(int xid, int programNum, int versionNum, int procNum,
			char resultType, Object result) {
		this.xid = xid;
		this.programNum = programNum;
		this.versionNum = versionNum;
		this.procNum = procNum;
		this.resultType = resultType;
		this.result = result;

		listOfPackets = new ArrayList<byte[]>();
		buffer = ByteBuffer.allocate(4 * RequestPacket.NUM_INTEGERS);
		currentSID = 1;
	}

	public ReplyPacket() { // This special constructor is for WorkerPool's
							// DuplicateCache
		// TODO Auto-generated constructor stub
		result = null;
	}

	public ArrayList<byte[]> getBytes() {
		buffer = ByteBuffer.allocate(4 * RequestPacket.NUM_INTEGERS);
		buffer.putInt(xid);
		buffer.putInt(currentSID);

		/**
		 * This for loop and following if branch deal with adding data to the
		 * list of packets.
		 */

		putIntInBuffer((int) resultType);
		switch (resultType) {
		case 0:// int
			putIntInBuffer((Integer) result);
			break;
		case 1:// int[]
			int[] A = (int[]) result;
			int size = A.length;
			putIntInBuffer(size);
			for (int j = 0; j < size; j++)
				putIntInBuffer(A[j]);
			break;
		case 2:// int[][]
			int[][] C = (int[][]) result;
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

		if (buffer.position() != 8) { // this means the last packet has not been
										// added to the listOfPackets yet. It
										// only
										// contains the header, 8 bytes
			listOfPackets.add(buffer.array());
			buffer.clear();
		}
		/**
		 * This part deals with adding the header to the list of packets. It has
		 * the SID to be 0.
		 */
		buffer = ByteBuffer.allocate(RequestPacket.NUM_INTEGERS * 4);
		buffer.putInt(xid);
		buffer.putInt(0); // SID
		buffer.putInt(listOfPackets.size() + 1); // num of packets, plus itself
		buffer.putInt(programNum);
		buffer.putInt(versionNum);
		buffer.putInt(procNum);

		listOfPackets.add(buffer.array());
		return listOfPackets;
	}

	private void putIntInBuffer(int value) {
		buffer.putInt(value);
		if (buffer.position() == buffer.capacity()) {
			listOfPackets.add(buffer.array());
			buffer.clear();
			buffer = ByteBuffer.allocate(RequestPacket.NUM_INTEGERS * 4);
			buffer.putInt(xid);
			buffer.putInt(++currentSID);
		}
	}

	// public byte[] getBytes1() {
	// ByteBuffer bytebuffer = ByteBuffer.allocate(1024);
	// bytebuffer.putInt(xid);
	// bytebuffer.putInt(programNum);
	// bytebuffer.putInt(versionNum);
	// bytebuffer.putInt(procNum);
	// bytebuffer.putChar(resultType);
	// switch (resultType) {
	// case 0:// int
	// bytebuffer.putInt((Integer) result);
	// break;
	// case 1:// int[]
	// int[] A = (int[]) result;
	// bytebuffer.putInt(A.length);
	// for (int j = 0; j < A.length; j++)
	// bytebuffer.putInt(A[j]);
	// break;
	// case 2:// int[][]
	// int[][] C = (int[][]) result;
	// int m = C.length,
	// n = C[0].length;
	// bytebuffer.putInt(m);
	// bytebuffer.putInt(n);
	// for (int i = 0; i < m; i++)
	// for (int j = 0; j < n; j++)
	// bytebuffer.putInt(C[i][j]);
	// break;
	// default:
	// System.out.println("unknown type");
	// }
	//
	// return bytebuffer.array();
	// }

	static public ReplyPacket getPacketFromBytes(ArrayList<DataPacket> list) {
//		System.out.println("+[4]=======================================+");
//		System.out.println("+----Server demarshalls client's request---+");
		ByteBuffer bytebuffer = ByteBuffer.allocate(RequestPacket.NUM_INTEGERS * list.size()
				* 4);

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

		/**
		 * Then we deal with decoding all the data packets.
		 */
		bytebuffer.clear();
		for (int i = 1; i < list.size(); i++) {
			byte[] data = list.get(i).data;
			bytebuffer.put(data, 8, data.length - 8);
		}
		bytebuffer.rewind();
		char resultType;
		Object result = null;
		resultType = (char) bytebuffer.getInt();
		switch (resultType) {
		case 0:// int
			result= bytebuffer.getInt();
			break;
		case 1:// int[]
			int size = bytebuffer.getInt();
			int[] A = new int[size];
			for (int j = 0; j < size; j++)
				A[j] = bytebuffer.getInt();
			result = A;
			break;
		case 2:// int[][]
			int m = bytebuffer.getInt();
			int n = bytebuffer.getInt();
			int[][] C = new int[m][n];
			for (int k = 0; k < m; k++)
				for (int j = 0; j < n; j++)
					C[k][j] = bytebuffer.getInt();
			result = C;
			break;

		default:
			System.out.println("unknown type");
		}
		System.out.println("xid#: " + xid);
		System.out.println("prog#: " + programNum);
		System.out.println("versionNum#: " + versionNum);
		System.out.println("procNum#: " + procNum);

		return new ReplyPacket(xid, programNum, versionNum, procNum, resultType, result);
	}

}