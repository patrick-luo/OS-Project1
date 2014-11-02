import java.net.InetAddress;

/**
 * 
 * request information from clients or servers.
 * 
 */
public class RequestInfo {
	public  String content;
	public  InetAddress ip;
	public int port;

	public String toString() {
		return ip.toString() + "_" + port;

	}
}