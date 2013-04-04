import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class GroupList {
	public ArrayList<Group> groups;
	private KeyGenerator keyGen = null;

	GroupList(){
		KeyGenerator keyGen = null;
		try {
			keyGen = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//128 bit key length
		keyGen.init(128);
	}
	
	public void addGroup(String name){
		
	}
	
}

class Group{
	public String name;
	public SecretKey key;
	
	Group(String groupName,SecretKey groupKey){
		groupName = name;
		key = groupKey;
	}
}
