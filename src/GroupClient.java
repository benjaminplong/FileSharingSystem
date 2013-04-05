/* Implements the GroupClient Interface */

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.crypto.spec.SecretKeySpec;

public class GroupClient extends Client implements GroupClientInterface {

	ArrayList<Group> clientGroups;

	GroupClient(){
		super();
		clientGroups = new ArrayList<Group>();
	}

	
	public ArrayList<Group> getGroups(){
		return clientGroups;
	}
	public byte[] getToken(String username,String password)
	{
		try
		{
			byte[] token;
			Envelope message = null, response = null;

			//Tell the server to return a token.
			message = new Envelope("GET");
			message.addObject(encryptAES(username.getBytes())); //Add user name string
			message.addObject(encryptAES(password.getBytes()));
			output.writeObject(message);

			//Get the response from the server
			response = (Envelope)input.readObject();

			//Successful response
			if(response.getMessage().equals("OK"))
			{
				//If there is a token in the Envelope, return it 
				ArrayList<Object> temp = null;
				temp = response.getObjContents();

				if(temp.size() != 0)
				{
					int count = 0;
					token = decryptAES((byte[])temp.get(count));
					count++;
					if(temp.size() > 1){
						int numGroups = ByteBuffer.wrap((decryptAES((byte[])temp.get(count)))).getInt();
						count++;
						if(numGroups > 0){
							for(int i = 0; i < numGroups; i++,count++){
								Group group = new Group(decryptAES((byte[])temp.get(count)).toString());
								count++;
								int numKeys = ByteBuffer.wrap((decryptAES((byte[])temp.get(count)))).getInt();
								for(int j = 0; j < numKeys; j++, count++){
									byte[] key = decryptAES((byte[])temp.get(count));
									group.addKey(new SecretKeySpec(key, 0, key.length, "AES"));
								}
								clientGroups.add(group);
							}
						}
					}
					
					
					return token;
				}
			}

			return null;
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}

	}

	public boolean createUser(String username, String password, byte[] token)
	{
		try
		{
			Envelope message = null, response = null;
			//Tell the server to create a user
			message = new Envelope("CUSER");
			message.addObject(encryptAES(username.getBytes())); //Add user name string
			message.addObject(encryptAES(password.getBytes()));
			message.addObject(encryptAES(token));
			output.writeObject(message);

			response = (Envelope)input.readObject();

			//If server indicates success, return true
			if(response.getMessage().equals("OK"))
			{
				return true;
			}

			return false;
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean deleteUser(String username, byte[] token)
	{
		try
		{
			Envelope message = null, response = null;

			//Tell the server to delete a user
			message = new Envelope("DUSER");
			message.addObject(encryptAES(username.getBytes())); //Add user name
			message.addObject(encryptAES(token));  //Add requester's token
			output.writeObject(message);

			response = (Envelope)input.readObject();

			//If server indicates success, return true
			if(response.getMessage().equals("OK"))
			{
				return true;
			}

			return false;
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean createGroup(String groupname, byte[] token)
	{
		try
		{
			Envelope message = null, response = null;
			//Tell the server to create a group
			message = new Envelope("CGROUP");
			message.addObject(encryptAES(groupname.getBytes())); //Add the group name string
			message.addObject(encryptAES(token)); //Add the requester's token
			output.writeObject(message); 

			response = (Envelope)input.readObject();

			//If server indicates success, return true
			if(response.getMessage().equals("OK"))
			{
				return true;
			}

			return false;
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean deleteGroup(String groupname, byte[] token)
	{
		try
		{
			Envelope message = null, response = null;
			//Tell the server to delete a group
			message = new Envelope("DGROUP");
			message.addObject(encryptAES(groupname.getBytes())); //Add group name string
			message.addObject(encryptAES(token)); //Add requester's token
			output.writeObject(message); 

			response = (Envelope)input.readObject();
			//If server indicates success, return true
			if(response.getMessage().equals("OK"))
			{
				return true;
			}

			return false;
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public List<String> listMembers(String group, byte[] token)
	{
		try
		{
			Envelope message = null, response = null;
			//Tell the server to return the member list
			message = new Envelope("LMEMBERS");
			message.addObject(encryptAES(group.getBytes())); //Add group name string
			message.addObject(encryptAES(token)); //Add requester's token
			output.writeObject(message); 

			response = (Envelope)input.readObject();

			ArrayList<String> members = new ArrayList<String>();
			
			//If server indicates success, return the member list
			if(response.getMessage().equals("OK"))
			{
				for (Object o : response.getObjContents())
					members.add(new String(decryptAES((byte[])o)));
				
				return members;
			}

			return null;

		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return null;
		}
	}

	public boolean addUserToGroup(String username, String groupname, byte[] token)
	{
		try
		{
			Envelope message = null, response = null;
			//Tell the server to add a user to the group
			message = new Envelope("AUSERTOGROUP");
			message.addObject(encryptAES(username.getBytes())); //Add user name string
			message.addObject(encryptAES(groupname.getBytes())); //Add group name string
			message.addObject(encryptAES(token)); //Add requester's token
			output.writeObject(message); 

			response = (Envelope)input.readObject();
			//If server indicates success, return true
			if(response.getMessage().equals("OK"))
			{
				return true;
			}

			return false;
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

	public boolean deleteUserFromGroup(String username, String groupname, byte[] token)
	{
		try
		{
			Envelope message = null, response = null;
			//Tell the server to remove a user from the group
			message = new Envelope("RUSERFROMGROUP");
			message.addObject(encryptAES(username.getBytes())); //Add user name string
			message.addObject(encryptAES(groupname.getBytes())); //Add group name string
			message.addObject(encryptAES(token)); //Add requester's token
			output.writeObject(message);

			response = (Envelope)input.readObject();
			//If server indicates success, return true
			if(response.getMessage().equals("OK"))
			{
				return true;
			}

			return false;
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
			return false;
		}
	}

}
