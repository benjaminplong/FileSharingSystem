
import java.util.List;
import java.util.TreeSet;

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
	private TreeSet<String> _groups; 

	public Token(String name, String username, List<String> userGroups) {
		_server = name;
		_username = username;
		_groups = new TreeSet<String>(userGroups);
	}
	
	public Token(String parts) {
		String[] fields = parts.split(",");
		_server = fields[0];
		_username = fields[1];
		_groups = new TreeSet<String>();
		
		for (int i = 2; i <fields.length; ++i)
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
	public List<String> getGroups() {
		// TODO Auto-generated method stub
		return (List<String>) _groups.clone();
	}

	@Override
	public byte[] getBytes() {
		StringBuilder sb = new StringBuilder();
		for (String s : getGroups())
			sb.append(s + ",");
		sb.deleteCharAt(sb.length()-1);
		String s = new String(_server + "," + _username + "," + sb.toString());
		return s.getBytes();
	}

}
