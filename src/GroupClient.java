/* Implements the GroupClient Interface */

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GroupClient extends Client implements GroupClientInterface {


	GroupClient(){
		super();
	}

	@Override
	public byte[] getPublicKey() {
		//Tell the server to return its public key
		Envelope message = new Envelope("GETPUBKEY");
		try {
			output.writeObject(message);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		//Get the response from the server
		Envelope response;
		try {
			response = (Envelope)input.readObject();
			
			//Successful response
			if(response.getMessage().equals("OK"))
				return decryptAES((byte[])response.getObjContents().get(0));
			
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
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

				if(temp.size() == 1)
				{
					token = decryptAES(temp.get(0).toString().getBytes());
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

	@SuppressWarnings("unchecked")
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

			//If server indicates success, return the member list
			if(response.getMessage().equals("OK"))
			{ 
				return (List<String>)response.getObjContents().get(0); //This cast creates compiler warnings. Sorry.
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
