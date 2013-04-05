/* This thread does all the work. It communicates with the client through Envelopes.
 * 
 */
import java.lang.Thread;
import java.net.Socket;
import java.io.*;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class GroupThread extends Thread 
{
	private final Socket socket;
	private GroupServer my_gs;
	private SecretKey sessionKey;
	private Random rand;

	public GroupThread(Socket _socket, GroupServer _gs)
	{
		socket = _socket;
		my_gs = _gs;

		rand = new Random(System.currentTimeMillis());
	}

	public void run()
	{
		boolean proceed = true;

		try
		{
			//Announces connection and opens object streams
			System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + "***");
			final ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
			final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
			Envelope response;

			Cipher rsaCipher = Cipher.getInstance("RSA");
			Cipher aesCipher = Cipher.getInstance("AES");
			Mac hmac = Mac.getInstance("HmacSHA1");
			byte[] decrypted;
			byte[] encrypted;

			do
			{
				Envelope message = (Envelope)input.readObject();
				System.out.println("Request received: " + message.getMessage());

				if(message.getMessage().equals("CONNECT"))//Client wants a token
				{
					// alert the user of the file servers public key on connect
					response = new Envelope("PUBLICKEY");
					response.addObject(my_gs.RSAkeys.getPublic());
					output.writeObject(response);
				}
				else if (message.getMessage().equals("SHAREDKEYS"))
				{
					// return the random number to the client
					response = new Envelope("AUTHVALUE");

					// initialize your cipher
					rsaCipher.init(Cipher.DECRYPT_MODE, my_gs.RSAkeys.getPrivate());

					decrypted = rsaCipher.doFinal((byte[]) message.getObjContents().get(1));
					sessionKey = new SecretKeySpec(decrypted, "AES");

					// decrypt the random number from the client
					aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey);
					decrypted = rsaCipher.doFinal((byte[]) message.getObjContents().get(0));
					response.addObject(aesCipher.doFinal(decrypted));

					byte[] value = new byte[4];
					rand.nextBytes(value);

					// send a new random number
					response.addObject(aesCipher.doFinal(value));

					output.writeObject(response);

					// get the hash key value for HMAC
					decrypted = rsaCipher.doFinal((byte[]) message.getObjContents().get(2));
					SecretKey hashKey = new SecretKeySpec(decrypted, "HmacSHA1");
					hmac.init(hashKey);
					
					message = (Envelope)input.readObject();

					if(message.getMessage().equals("AUTHVALUE"))
					{
						aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);
						decrypted = aesCipher.doFinal((byte[])message.getObjContents().get(0));
						// Close the connection if the numbers don't match
						if(!Arrays.equals(decrypted, value))
						{
							socket.close();
							proceed = false;
						}

						response = new Envelope("OK");
						output.writeObject(response);
					}
				}
				else if(message.getMessage().equals("GETPUBKEY"))//Client wants our public key
				{
					//Respond to the client. On error, the client will receive a null token
					response = new Envelope("OK");

					aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey);

					encrypted = aesCipher.doFinal(my_gs.RSAkeys.getPublic().getEncoded());
					response.addObject(encrypted);

					output.writeObject(response);
				}
				else if(message.getMessage().equals("GET"))//Client wants a token
				{
					aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);

					decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(0));
					String username = new String(decrypted); //Get the username
					hmac.update(decrypted);

					decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(1));
					String password = new String(decrypted); //Get the password
					hmac.update(decrypted);

					response = new Envelope("FAIL");

					if (Arrays.equals(message.getChecksum(), hmac.doFinal()))
					{
						UserToken yourToken = createToken(username, password); //Create a token

						//Respond to the client. On error, the client will receive a null token
						if (yourToken != null)
						{
							response = new Envelope("OK");

							rsaCipher.init(Cipher.ENCRYPT_MODE, my_gs.RSAkeys.getPrivate());
							encrypted = rsaCipher.doFinal(yourToken.getBytes());

							aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey);

							hmac.update(encrypted);
							encrypted = aesCipher.doFinal(encrypted);
							response.addObject(encrypted);
							response.setChecksum(hmac.doFinal());
						}
						else
							response = new Envelope("FAIL");
					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("CUSER")) //Client wants to create a user
				{
					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);

								decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(0));
								String username = new String(decrypted); // Extract the username
								hmac.update(decrypted);

								decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(1));
								String password = new String(decrypted); // Extract the password
								hmac.update(decrypted);

								decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(2));
								hmac.update(decrypted);
								rsaCipher.init(Cipher.DECRYPT_MODE, my_gs.RSAkeys.getPublic());
								decrypted = rsaCipher.doFinal(decrypted);

								String tokenParts = new String(decrypted);
								UserToken yourToken = new Token(tokenParts); //Extract the token

								if (Arrays.equals(message.getChecksum(), hmac.doFinal()))
								{
									if(createUser(username, password, yourToken))
										response = new Envelope("OK"); //Success
								}
							}
						}
					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("DUSER")) //Client wants to delete a user
				{

					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);

								decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(0));
								String username = new String(decrypted); // Extract the username
								hmac.update(decrypted);

								decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(1));
								hmac.update(decrypted);
								rsaCipher.init(Cipher.DECRYPT_MODE, my_gs.RSAkeys.getPublic());
								decrypted = rsaCipher.doFinal(decrypted);

								String tokenParts = new String(decrypted);
								UserToken yourToken = new Token(tokenParts); //Extract the token

								if (Arrays.equals(message.getChecksum(), hmac.doFinal()))
								{
									if(deleteUser(username, yourToken))
										response = new Envelope("OK");
								}
							}
						}
					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("CGROUP")) //Client wants to create a group
				{
					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);

								decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(0));
								String groupName = new String(decrypted); // Extract the username
								hmac.update(decrypted);

								decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(1));
								hmac.update(decrypted);
								rsaCipher.init(Cipher.DECRYPT_MODE, my_gs.RSAkeys.getPublic());
								decrypted = rsaCipher.doFinal(decrypted);

								String tokenParts = new String(decrypted);
								UserToken yourToken = new Token(tokenParts); //Extract the token

								if (Arrays.equals(message.getChecksum(), hmac.doFinal()))
								{
									if(createGroup(groupName, yourToken))
										response = new Envelope("OK");
								}
							}
						}
					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("DGROUP")) //Client wants to delete a group
				{
					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);

								decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(0));
								String groupName = new String(decrypted); // Extract the username
								hmac.update(decrypted);

								decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(1));
								hmac.update(decrypted);
								rsaCipher.init(Cipher.DECRYPT_MODE, my_gs.RSAkeys.getPublic());
								decrypted = rsaCipher.doFinal(decrypted);

								String tokenParts = new String(decrypted);
								UserToken yourToken = new Token(tokenParts); //Extract the token

								if (Arrays.equals(message.getChecksum(), hmac.doFinal()))
								{
									if(deleteGroup(groupName, yourToken))
										response = new Envelope("OK");
								}
							}
						}
					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("LMEMBERS")) //Client wants a list of members in a group
				{
					if(message.getObjContents().size() < 2)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);

								decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(0));
								String groupName = new String(decrypted); // Extract the username
								hmac.update(decrypted);

								decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(1));
								hmac.update(decrypted);
								rsaCipher.init(Cipher.DECRYPT_MODE, my_gs.RSAkeys.getPublic());
								decrypted = rsaCipher.doFinal(decrypted);

								String tokenParts = new String(decrypted);
								UserToken yourToken = new Token(tokenParts); //Extract the token

								if (Arrays.equals(message.getChecksum(), hmac.doFinal()))
								{
									List<String> members = listMembers(groupName, yourToken);

									if(members != null)
									{
										response = new Envelope("OK"); //Success

										aesCipher.init(Cipher.ENCRYPT_MODE, sessionKey);

										for (int i = 0; i < members.size(); ++i)
										{
											hmac.update(members.get(i).getBytes());
											encrypted = aesCipher.doFinal(members.get(i).getBytes());
											response.addObject(encrypted);
										}
										response.setChecksum(hmac.doFinal());
									}
								}
							}
						}
					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("AUSERTOGROUP")) //Client wants to add user to a group
				{
					if(message.getObjContents().size() < 3)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								if(message.getObjContents().get(2) != null)
								{
									aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);

									decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(0));
									String username = new String(decrypted); // Extract the username
									hmac.update(decrypted);

									decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(1));
									String groupName = new String(decrypted); // Extract the groupname
									hmac.update(decrypted);

									decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(2));
									hmac.update(decrypted);
									rsaCipher.init(Cipher.DECRYPT_MODE, my_gs.RSAkeys.getPublic());
									decrypted = rsaCipher.doFinal(decrypted);

									String tokenParts = new String(decrypted);
									UserToken yourToken = new Token(tokenParts); //Extract the token

									if (Arrays.equals(message.getChecksum(), hmac.doFinal()))
									{
										if(addUserToGroup(username, groupName, yourToken))
											response = new Envelope("OK");
									}
								}
							}
						}
					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("RUSERFROMGROUP")) //Client wants to remove user from a group
				{
					if(message.getObjContents().size() < 3)
					{
						response = new Envelope("FAIL");
					}
					else
					{
						response = new Envelope("FAIL");

						if(message.getObjContents().get(0) != null)
						{
							if(message.getObjContents().get(1) != null)
							{
								if(message.getObjContents().get(2) != null)
								{
									aesCipher.init(Cipher.DECRYPT_MODE, sessionKey);

									decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(0));
									String username = new String(decrypted); // Extract the username
									hmac.update(decrypted);

									decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(1));
									String groupName = new String(decrypted); // Extract the groupname
									hmac.update(decrypted);

									decrypted = aesCipher.doFinal((byte[]) message.getObjContents().get(2));
									hmac.update(decrypted);
									rsaCipher.init(Cipher.DECRYPT_MODE, my_gs.RSAkeys.getPublic());
									decrypted = rsaCipher.doFinal(decrypted);

									String tokenParts = new String(decrypted);
									UserToken yourToken = new Token(tokenParts); //Extract the token

									if (Arrays.equals(message.getChecksum(), hmac.doFinal()))
									{
										if(deleteUserFromGroup(username, groupName, yourToken))
											response = new Envelope("OK");
									}
								}
							}
						}
					}

					output.writeObject(response);
				}
				else if(message.getMessage().equals("DISCONNECT")) //Client wants to disconnect
				{
					socket.close(); //Close the socket
					proceed = false; //End this communication loop
				}
				else
				{
					response = new Envelope("FAIL"); //Server does not understand client request
					output.writeObject(response);
				}
			} while(proceed);	
		}
		catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

	//Method to create tokens
	private UserToken createToken(String username, String password) 
	{
		//Check that user is who he says he is
		if(my_gs.userList.checkUser(username, password))
		{
			//Issue a new token with server's name, user's name, and user's groups
			UserToken yourToken = new Token(my_gs.name, username, my_gs.userList.getUserGroups(username));
			return yourToken;
		}
		else
		{
			return null;
		}
	}


	//Method to create a user
	private boolean createUser(String username, String password, UserToken yourToken)
	{
		String requester = yourToken.getSubject();

		//Check if requester exists
		if(my_gs.userList.checkUser(requester))
		{
			//Get the user's groups
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			//requester needs to be an administrator
			if(temp.contains("ADMIN"))
			{
				//Does user already exist?
				if(my_gs.userList.checkUser(username))
				{
					return false; //User already exists
				}
				else
				{
					my_gs.userList.addUser(username, password);
					return true;
				}
			}
			else
			{
				return false; //requester not an administrator
			}
		}
		else
		{
			return false; //requester does not exist
		}
	}

	//Method to delete a user
	private boolean deleteUser(String username, UserToken yourToken)
	{
		String requester = yourToken.getSubject();

		//Does requester exist?
		if(my_gs.userList.checkUser(requester))
		{
			ArrayList<String> temp = my_gs.userList.getUserGroups(requester);
			//requester needs to be an administer
			if(temp.contains("ADMIN"))
			{
				//Does user exist?
				if(my_gs.userList.checkUser(username))
				{
					//User needs deleted from the groups they belong
					ArrayList<String> deleteFromGroups = new ArrayList<String>();

					//This will produce a hard copy of the list of groups this user belongs
					for(int index = 0; index < my_gs.userList.getUserGroups(username).size(); index++)
					{
						deleteFromGroups.add(my_gs.userList.getUserGroups(username).get(index));
					}

					//Delete the user from the groups
					//If user is the owner, removeMember will automatically delete group!
					for(int index = 0; index < deleteFromGroups.size(); index++)
					{
						my_gs.userList.removeMember(username, deleteFromGroups.get(index));
					}

					//If groups are owned, they must be deleted
					ArrayList<String> deleteOwnedGroup = new ArrayList<String>();

					//Make a hard copy of the user's ownership list
					for(int index = 0; index < my_gs.userList.getUserOwnership(username).size(); index++)
					{
						deleteOwnedGroup.add(my_gs.userList.getUserOwnership(username).get(index));
					}

					//Delete owned groups
					for(int index = 0; index < deleteOwnedGroup.size(); index++)
					{
						//Use the delete group method. Token must be created for this action
						deleteGroup(deleteOwnedGroup.get(index), new Token(my_gs.name, username, deleteOwnedGroup));
					}

					//Delete the user from the user list
					my_gs.userList.deleteUser(username);

					return true;	
				}
				else
				{
					return false; //User does not exist

				}
			}
			else
			{
				return false; //requester is not an administer
			}
		}
		else
		{
			return false; //requester does not exist
		}
	}

	private boolean deleteGroup(String group, UserToken token)
	{
		String requester = token.getSubject();

		//Does requester exist?
		if(my_gs.userList.checkUser(requester))
		{
			// make sure the requester owns the group
			if(my_gs.userList.getUserOwnership(requester).contains(group))
			{
				my_gs.userList.removeMember(requester, group);

				return true;
			}
		}

		return false;
	}

	private boolean createGroup(String group, UserToken token)
	{
		String requester = token.getSubject();

		//Does requester exist?
		if(my_gs.userList.checkUser(requester))
		{
			// make sure the requester does not already own this group
			if(!my_gs.userList.getUserOwnership(requester).contains(group))
			{
				my_gs.userList.addGroup(requester, group);
				my_gs.userList.addOwnership(requester, group);

				return true;
			}
		}

		return false;
	}

	private List<String> listMembers(String group, UserToken token)
	{
		String requester = token.getSubject();

		//Does requester exist?
		if(my_gs.userList.checkUser(requester))
		{
			// make sure the requester owns the group
			if(my_gs.userList.getUserOwnership(requester).contains(group))
			{
				return my_gs.userList.getMembers(group);
			}
		}

		return null;
	}

	private boolean addUserToGroup(String user, String group, UserToken token)
	{
		String requester = token.getSubject();

		//Does requester exist?
		if(my_gs.userList.checkUser(requester))
		{
			// make sure the requester owns the group
			if(my_gs.userList.getUserOwnership(requester).contains(group))
			{
				// make sure the user exists
				if(my_gs.userList.checkUser(user))
				{
					my_gs.userList.addGroup(user, group);

					return true;
				}
			}
		}

		return false;
	}

	private boolean deleteUserFromGroup(String user, String group, UserToken token)
	{
		String requester = token.getSubject();

		//Does requester exist?
		if(my_gs.userList.checkUser(requester))
		{
			// make sure the requester owns the group
			if(my_gs.userList.getUserOwnership(requester).contains(group))
			{
				// make sure the user exists
				if(my_gs.userList.checkUser(user))
				{
					my_gs.userList.removeMember(user, group);

					return true;
				}
			}
		}
		return false;
	}

}
