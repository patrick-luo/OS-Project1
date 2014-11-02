//import java.util.Random;
//
//public class Marshaller {
//
//	static public byte[] packRequestPacket(RequestPacket requestPacket) {
//		return requestPacket.getBytes();
//	}
//
//	static public byte[] packReplyPacket(ReplyPacket replyPacket) {
//		return replyPacket.getBytes();
//	}
//
//	static public RequestPacket unpackRequestPacket(byte[] replyData) {
//		return RequestPacket.getPacketFromBytes(replyData);
//	}
//
//	static public ReplyPacket unpackreplyPacket(byte[] replyData) {
//		return ReplyPacket.getPacketFromBytes(replyData);
//	}
//
//	public static void main(String args[]) {
//		int programNum = 2, versionNum = 3, procNum = 1;
//		int xid = new Random().nextInt();
//		int argNum = 2;
//		char[] argType = new char[] { 0, 1 };
//		int arg1 = 10;
//		int[] arg2 = new int[] { 11, 12 };
//		Object[] argus = new Object[] { arg1, arg2 };
//		char resultType = 1;
//		Object result = (Object) arg2;
//		RequestPacket requestPacket = new RequestPacket(xid, programNum, versionNum,
//				procNum, argNum, argType, argus);
//		byte[] requestData = packRequestPacket(requestPacket);
//		RequestPacket req = unpackRequestPacket(requestData);
//
//		ReplyPacket replyPacket = new ReplyPacket(xid, programNum, versionNum,
//				procNum, resultType, result);
//		byte[] reply = packReplyPacket(replyPacket);
//		replyPacket = unpackreplyPacket(reply);
//
//		System.out.print("hello");
//
//	}
//}
