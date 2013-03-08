import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Scanner;

import javax.crypto.NoSuchPaddingException;

//author: Ben Long

public class ClientCmd {

	private GroupClientInterface gc;
	private FileClientInterface fc;

	private byte[] myToken;
	private Scanner input;

	public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException {
		new ClientCmd();

	}
	
	// default constructor for command line client application
	ClientCmd() throws NoSuchAlgorithmException, NoSuchPaddingException{
		gc = new GroupClient();
		fc = new FileClient();
		input = new Scanner(System.in);
		
		Run();
	}
	
	//Run service for command line
	private void Run(){
		String command;
		String[] splitCmd;
		while(true){
			command = new String(input.nextLine());
			splitCmd = command.split(" ");
			
			//interpret which command was issued
			if(splitCmd[0].equals("gConnect") && splitCmd.length == 3){
				gConnect(splitCmd);
			}
			else if(splitCmd[0].equals("gDisconnect")){
				gDisconnect();
			}
			else if(splitCmd[0].equals("getToken") && splitCmd.length == 3){
				getToken(splitCmd);
			}
			else if(splitCmd[0].equals("mkUser") && splitCmd.length == 3){
				mkUser(splitCmd);
			}
			else if(splitCmd[0].equals("rmUser") && splitCmd.length == 2){
				rmUser(splitCmd);
			}
			else if(splitCmd[0].equals("mkGroup") && splitCmd.length == 2){
				mkGroup(splitCmd);
			}
			else if(splitCmd[0].equals("rmGroup") && splitCmd.length == 2){
				rmGroup(splitCmd);
			}
			else if(splitCmd[0].equals("lsMem") && splitCmd.length == 2){
				lsMem(splitCmd);
			}
			else if(splitCmd[0].equals("addUser") && splitCmd.length == 3){
				addUser(splitCmd);
			}
			else if(splitCmd[0].equals("delUser") && splitCmd.length == 3){
				delUser(splitCmd);
			}
			else if(splitCmd[0].equals("fConnect") && splitCmd.length == 3){
				fConnect(splitCmd);
			}
			else if(splitCmd[0].equals("fDisconnect")){
				fDisconnect();
			}
			else if(splitCmd[0].equals("lsFiles")){
				lsFiles();
			}
			else if(splitCmd[0].equals("upload") && splitCmd.length == 4){
				upload(splitCmd);
			}
			else if(splitCmd[0].equals("download") && splitCmd.length == 3){
				download(splitCmd);
			}
			else if(splitCmd[0].equals("del") && splitCmd.length == 2){
				del(splitCmd);
			}
			else if(splitCmd[0].equals("exit")){
				Client group = (Client) gc;
				Client file = (Client) fc;
				if (group.isConnected())
					gc.disconnect();
				if (file.isConnected())
					fc.disconnect();
				break;
			}
			else{
				printCmds();
			}
		}
		
		System.exit(0);
	}
	
	private void del(String[] command){
		String filename = command[1];
		
		if (!filename.isEmpty())
		{
			if (fc.delete(filename, myToken))
				System.out.println("File deleted successfully.");
			else
				System.out.println("File not deleted.");
		}
	}
	private void download(String[] command){
		String sourceFile = command[1];
		String destFile = command[2];

		if (!sourceFile.isEmpty() && !destFile.isEmpty())
		{
			if (fc.download(sourceFile, destFile, myToken))
				System.out.println("File downloaded successfully.");
			else
				System.out.println("File not downloaded.");
		}
	}
	private void upload(String[] command){
		String sourceFile = command[1];
		String destFile = command[2];
		String group = command[3];

		if (!sourceFile.isEmpty() && !destFile.isEmpty() && !group.isEmpty())
		{
			if (fc.upload(sourceFile, destFile, group, myToken))
				System.out.println("File uploaded successfully.");
			else
				System.out.println("File not uploaded.");
		}
	}
	private void lsFiles(){
		List<String> files = fc.listFiles(myToken);

		if (files != null && !files.isEmpty())
		{
			StringBuilder sb = new StringBuilder("\n");

			for (String f : files)
				sb.append(f + "\n");

			System.out.println("Files to view:\n" + sb.toString());
		}
		else
			System.out.println("No files to list.");
	}
	private void fDisconnect(){
		fc.disconnect();
	}
	private void fConnect(String[] command){
		boolean success = false;

		String server = command[1];

		try {
			int port = Integer.parseInt(command[2]);
			success = fc.connect(server, port);
		}
		catch (NumberFormatException ex) {
			System.out.println("Nummber Format Exception");
		}

		if (success)
			System.out.println("Connection Successful");
		else
			System.out.println("Connection Unsuccessful");
	}
	private void delUser(String[] command){
		String group = command[1];
		String user = command[2];

		if (!user.isEmpty() && !group.isEmpty())
		{
			if (gc.deleteUserFromGroup(user, group, myToken))
				System.out.println("User removed successfully.");
			else
				System.out.println("User not removed.");
		}
	}
	private void addUser(String[] command){
		String group = command[1];
		String user = command[2];

		if (!user.isEmpty() && !group.isEmpty())
		{
			if (gc.addUserToGroup(user, group, myToken))
				System.out.println("User added successfully.");
			else
				System.out.println("User not added.");
		}
	}
	private void lsMem(String[] command){
		String group = command[1];

		if (!group.isEmpty())
		{
			List<String> members = gc.listMembers(group, myToken);

			if (members != null && !members.isEmpty())
			{
				StringBuilder sb = new StringBuilder("\n");

				for (String m : members)
					sb.append(m + "\n");

				System.out.println("Members that belong to " + group + ":\n" + sb.toString());
			}
			else
				System.out.println("No members to list.");
		}
	}
	private void rmGroup(String[] command){
		String groupname = command[1];

		if (!groupname.isEmpty())
		{
			if (gc.deleteGroup(groupname, myToken))
				System.out.println("Group deleted successfully.");
			else
				System.out.println("Group not deleted.");
		}
	}
	private void mkGroup(String[] command){
		String groupname = command[1];

		if (!groupname.isEmpty())
		{
			if (gc.createGroup(groupname, myToken))
				System.out.println("Group created successfully.");
			else
				System.out.println("Group not created.");
		}
	}
	private void rmUser(String[] command){
		String user = command[1];

		if (!user.isEmpty())
		{
			if (gc.deleteUser(user, myToken))
				System.out.println("User deleted");
			else
				System.out.println("User not deleted");
		}
	}
	private void mkUser(String[] command){
		String username = command[1];
		String password = command[2];
		if (gc.createUser(username, password, myToken))
			System.out.println("User Created successfully");
		else
			System.out.println("User not Created");
	}
	private void getToken(String[] command){
		String username = command[1];
		String password = command[2];

		if (!username.isEmpty())
		{
			myToken = gc.getToken(username,password);

			if (myToken == null)
				System.out.println("No token received");
			else
				System.out.println("Valid Token Received");
		}
	}
	private void gDisconnect(){
		gc.disconnect();
	}
	private void gConnect(String[] command){
		boolean success = false;

		String server = command[1];

		try {
			int port = Integer.parseInt(command[2]);
			success = gc.connect(server, port);
		}
		catch (NumberFormatException ex) {
			System.out.println("Nummber Format Exception");
		}

		if (success)
			System.out.println("Connection Successful");
		else
			System.out.println("Connection Unsuccessful");
	}
	//print out possible commands so user knows what they can do
	private void printCmds(){
		//group server commands
		System.out.println("Group Commands:");
		System.out.println("\tGroup Connect: gConnect <server name> <port name>");
		System.out.println("\tGroup Disconnect: gDisconnect");
		System.out.println("\tGet User Token: getToken <username> <password>");
		System.out.println("\tCreate User: mkUser <username> <password>");
		System.out.println("\tDelete User: rmUser <username>");
		System.out.println("\tCreate Group: mkGroup <group name>");
		System.out.println("\tDelete Group: rmGroup <group name>");
		System.out.println("\tList Members: lsMem <group name>");
		System.out.println("\tAdd User to Group: addUser <group name> <user name>");
		System.out.println("\tDelete User from Group: delUser <group name> <user name>");
		//file server commands
		System.out.println("File Commands:");
		System.out.println("\tFile Connect: fConnect <server name> <port name>");
		System.out.println("\tFile Disconnect: fDisconnect");
		System.out.println("\tList Files: lsFiles");
		System.out.println("\tUpload File: upload <src file name> <dst file name> <group name>");
		System.out.println("\tDownload File: download <src file name> <dst file name>");
		System.out.println("\tDelete File: del <file name>");
		
		//System commands
		System.out.println("System Commands");
		System.out.println("\tExit: exit");
		
	}

}
