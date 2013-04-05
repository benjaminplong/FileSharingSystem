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
			e.printStackTrace();
		}
		keyGen.init(128);
		groups = new ArrayList<Group>();
	}
	
	public void addGroup(String name){
		Group newGroup = new Group(name,keyGen.generateKey());
		groups.add(newGroup);
	}
	
	public void addGroupKey(String groupName){
		for(Group group : groups){
			if(group.name.equals(groupName)){
				group.addKey(keyGen.generateKey());
			}
		}
	}
	
	public SecretKey getGroupKey(String groupName,int index){
		for(Group group : groups){
			if(group.name.equals(groupName)){
				return group.getKey(index);
			}
		}
		return null;
	}
	
	public int getNumGroupKeys(String groupName){
		for(Group group : groups){
			if(group.name.equals(groupName)){
				return group.keys.size();
			}
		}
		return 0;
	}
	
	public void removeGroup(String groupName){
		for(Group group : groups){
			if(group.name.equals(groupName)){
				groups.remove(group);
			}
		}
	}
	
	public Group getGroup(String groupName){
		for(Group group : groups){
			if(group.name.equals(groupName)){
				return group;
			}
		}
		return null;
	}
}
