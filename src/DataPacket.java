/**
 * 
 * This class to encapsulate each data udp packet.
 *
 */
public class DataPacket {
	public int xid;
	public int sid;
	public byte[] data;
	
	public DataPacket(int xid, int sid, byte[] data){
		this.xid = xid;
		this.sid = sid;
		this.data = data;
	}
	public String toString(){
		return this.sid + "";
	}

}
