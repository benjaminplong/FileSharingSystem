/* This list represents the users on the server */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class UserList implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7600343803563417992L;
	private Hashtable<String, User> list = new Hashtable<String, User>();

	public synchronized void addUser(String username, String password)
	{
		User newUser = new User(username, password);
		list.put(username, newUser);
	}

	public synchronized void deleteUser(String username)
	{
		list.remove(username);
	}

	public synchronized boolean checkUser(String username)
	{
		if(list.containsKey(username))
			return true;
		
		return false;
	}
	
	public synchronized boolean checkUser(String username, String password)
	{
		if(list.containsKey(username))
		{
			if(list.get(username).correctPassword(password))
				return true;
		}
		
		return false;
	}

	public synchronized ArrayList<String> getUserGroups(String username)
	{
		return list.get(username).getGroups();
	}

	public synchronized ArrayList<String> getUserOwnership(String username)
	{
		return list.get(username).getOwnership();
	}

	public synchronized void addGroup(String user, String groupname)
	{
		list.get(user).addGroup(groupname);
	}

	public synchronized void removeGroup(String user, String groupname)
	{
		list.get(user).removeGroup(groupname);
	}

	public synchronized void addOwnership(String user, String groupname)
	{
		list.get(user).addOwnership(groupname);
	}

	public synchronized void removeOwnership(String user, String groupname)
	{
		list.get(user).removeOwnership(groupname);
	}

	public synchronized void removeMember(String username, String groupname)
	{
		User u = list.get(username);
		if (u != null)
		{
			u.removeGroup(groupname);

			if (u.getOwnership().remove(groupname))
			{
				u.removeOwnership(groupname);

				// remove all users from the group
				for ( User user : list.values())
				{
					user.removeGroup(groupname);
				}
			}
		}
	}

	public synchronized List<String> getMembers(String group)
	{
		ArrayList<String> users = new ArrayList<String>();

		// remove all users from the group
		for ( User user : list.values())
		{
			if (user.getGroups().contains(group))
				users.add(user.name);
		}

		return users;
	}

	class User implements java.io.Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6699986336399821598L;
		private String name;
		private byte[] salt;
		private byte[] passHash;
		private ArrayList<String> groups;
		private ArrayList<String> ownership;

		public User(String name, String password)
		{
			this.name = name;
			SecureRandom random = new SecureRandom(SecureRandom.getSeed(8));

			salt = new byte[8];
			random.nextBytes(salt);

			byte[] passbytes = password.getBytes();

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );

			try {
				outputStream.write(salt);
				outputStream.write(passbytes);

				MessageDigest md = MessageDigest.getInstance("SHA-1");
				passHash = md.digest(outputStream.toByteArray());

			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			groups = new ArrayList<String>();
			ownership = new ArrayList<String>();
		}

		public boolean correctPassword(String password) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );

			byte[] passbytes = password.getBytes();

			try {
				outputStream.write(salt);
				outputStream.write(passbytes);

				MessageDigest md = MessageDigest.getInstance("SHA-1");
				if (Arrays.equals(passHash, md.digest(outputStream.toByteArray())))
					return true;

			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return false;
		}

		public ArrayList<String> getGroups()
		{
			return groups;
		}

		public ArrayList<String> getOwnership()
		{
			return ownership;
		}

		public void addGroup(String group)
		{
			groups.add(group);
		}

		public void removeGroup(String group)
		{
			if(!groups.isEmpty())
			{
				if(groups.contains(group))
				{
					groups.remove(groups.indexOf(group));
				}
			}
		}

		public void addOwnership(String group)
		{
			ownership.add(group);
		}

		public void removeOwnership(String group)
		{
			if(!ownership.isEmpty())
			{
				if(ownership.contains(group))
				{
					ownership.remove(ownership.indexOf(group));
				}
			}
		}

	}
}	
