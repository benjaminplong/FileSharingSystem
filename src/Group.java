import java.util.ArrayList;

import javax.crypto.SecretKey;

public class Group implements java.io.Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2486978295354019804L;
	public String name;
	public ArrayList<SecretKey> keys;

	Group(String groupName){
		name = groupName;
		keys = new ArrayList<SecretKey>();
	}
	
	Group(String groupName,SecretKey groupKey){
		name = groupName;
		keys = new ArrayList<SecretKey>();
		keys.add(groupKey);
	}
	public void addKey(SecretKey key){
		keys.add(key);
	}
	
	public SecretKey getKey(int index){
		return keys.get(index);
	}
}