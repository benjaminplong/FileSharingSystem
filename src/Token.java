
import java.util.List;
import java.util.TreeSet;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 
 */

/**
 * @author Sean Cardello
 *
 */
public class Token implements UserToken, java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6526117584140165192L;
	
	private String _server;
	private String _username;
	private InetAddress _address;
	private TreeSet<String> _groups;


	public Token(String name, String username, List<String> userGroups) {
		_server = name;
		_username = username;
		_address = null;
		_groups = new TreeSet<String>(userGroups);
	}
	public Token(String name, String username, List<String> userGroups, InetAddress address) {
		_server = name;
		_username = username;
		_address = address;
		_groups = new TreeSet<String>(userGroups);
	}
	
	public Token(String parts) {
		String[] fields = parts.split(",");
		_server = fields[0];
		_username = fields[1];
		try {
			_address = InetAddress.getByName(fields[2]);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		_groups = new TreeSet<String>();
		
		for (int i = 3; i <fields.length; ++i)
			_groups.add(fields[i]);
	}

	/* (non-Javadoc)
	 * @see UserToken#getIssuer()
	 */
	public String getIssuer() {
		return _server;
	}

	/* (non-Javadoc)
	 * @see UserToken#getSubject()
	 */
	public String getSubject() {
		return _username;
	}

	/* (non-Javadoc)
	 * @see UserToken#getGroups()
	 */
	@SuppressWarnings("unchecked")
	public TreeSet<String> getGroups() {
		// TODO Auto-generated method stub
		return (TreeSet<String>) _groups.clone();
	}

	@Override
	public byte[] getBytes() {
		StringBuilder sb = new StringBuilder();
		for (String s : getGroups())
			sb.append(s + ",");
		if (sb.length() > 0)
			sb.deleteCharAt(sb.length()-1);
		String s = new String(_server + "," + _username + "," + sb.toString());
		return s.getBytes();
	}
	
	public InetAddress getAddress() {
		return _address;
	}

}
