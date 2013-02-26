import java.util.ArrayList;
import java.util.List;

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
	private ArrayList<String> _groups; 

	@SuppressWarnings("unchecked")
	public Token(String name, String username, ArrayList<String> userGroups) {
		_server = name;
		_username = username;
		_groups = (ArrayList<String>) userGroups.clone();
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

}
