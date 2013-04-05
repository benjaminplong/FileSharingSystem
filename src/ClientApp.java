/**
 * 
 */

/**
 * @author Sean Cardello
 *
 */
import java.awt.*;
import java.awt.event.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.NoSuchPaddingException;
import javax.swing.*;

public class ClientApp extends JFrame implements WindowListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2963114495067720196L;

	private GroupClientInterface gc;
	private FileClientInterface fc;
	private ArrayList<Group> myGroups;

	private byte[] myToken;

	public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		new ClientApp();
	}

	public ClientApp() throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		super("Group-File Client Application");

		gc = new GroupClient();
		fc = new FileClient();
		myGroups = new ArrayList<Group>();

		makeLayout();

		addWindowListener(this);
		
		setBounds(100, 100, 500, 500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true); // display this frame
	}

	private void makeLayout()
	{
		// Group controls
		// Create group client action buttons
		JButton gConnectButton = new JButton("Connect to Server");
		gConnectButton.setName("groupConnect");
		JButton gDisconnectButton = new JButton("Disconnect from Server");
		gDisconnectButton.setName("groupDisconnect");
		JButton getTokenButton = new JButton("Get Token");
		JButton createUserButton = new JButton("Create User");
		JButton deleteUserButton = new JButton("Delete User");
		JButton createGroupButton = new JButton("Create Group");
		JButton deleteGroupButton = new JButton("Delete Group");
		JButton listMembersButton = new JButton("List Members");
		JButton addToGroupButton = new JButton("Add User to Group");
		JButton removeFromGroupButton = new JButton("Remove User From Group");

		// Create listeners
		gConnectButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				connectButtonPressed(e);
			}
		} );
		gDisconnectButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				disconnectButtonPressed(e);
			} 
		} );
		getTokenButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				getTokenButtonPressed();
			} 
		} );
		createUserButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				createUserButtonPressed();
			} 
		} );
		deleteUserButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				deleteUserButtonPressed();
			} 
		} );
		createGroupButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				createGroupButtonPressed();
			} 
		} );
		deleteGroupButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				deleteGroupButtonPressed();
			} 
		} );
		listMembersButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				listMembersButtonPressed();
			} 
		} );
		addToGroupButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				addToGroupButtonPressed();
			} 
		} );
		removeFromGroupButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				removeFromGroupButtonPressed();
			} 
		} );

		// Group buttons together
		JPanel groupPanel = new JPanel();
		groupPanel.setBorder(BorderFactory.createTitledBorder("Group Client"));
		groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.Y_AXIS));

		// Add the buttons to the panel
		groupPanel.add(gConnectButton);
		groupPanel.add(gDisconnectButton);
		groupPanel.add(getTokenButton);
		groupPanel.add(createUserButton);
		groupPanel.add(deleteUserButton);
		groupPanel.add(createGroupButton);
		groupPanel.add(deleteGroupButton);
		groupPanel.add(listMembersButton);
		groupPanel.add(addToGroupButton);
		groupPanel.add(removeFromGroupButton);

		// File controls
		// Create file client action buttons
		JButton fConnectButton = new JButton("Connect to Server");
		fConnectButton.setName("fileConnect");
		JButton fDisconnectButton = new JButton("Disconnect from Server");
		fDisconnectButton.setName("fileDisconnect");
		JButton listFilesButton = new JButton("List Files");
		JButton uploadFileButton = new JButton("Upload File");
		JButton downloadFileButton = new JButton("Download File");
		JButton deleteFileButton = new JButton("Delete File");

		// Create listeners
		fConnectButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				connectButtonPressed(e);
			}
		} );
		fDisconnectButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				disconnectButtonPressed(e);
			} 
		} );
		listFilesButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				listFilesButtonPressed();
			} 
		} );
		uploadFileButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				uploadFileButtonPressed();
			} 
		} );
		downloadFileButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				downloadFileButtonPressed();
			} 
		} );
		deleteFileButton.addActionListener(new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				deleteFileButtonPressed();
			} 
		} );

		JPanel filePanel = new JPanel();
		filePanel.setBorder(BorderFactory.createTitledBorder("File Client"));
		filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));

		filePanel.add(fConnectButton);
		filePanel.add(fDisconnectButton);
		filePanel.add(listFilesButton);
		filePanel.add(uploadFileButton);
		filePanel.add(downloadFileButton);
		filePanel.add(deleteFileButton);

		setLayout(new GridLayout(1, 2, 10, 10));

		add(groupPanel);
		add(filePanel);
	}

	protected void deleteFileButtonPressed()
	{
		String filename = JOptionPane.showInputDialog(this, "Please enter the server file name use wish to delete:", "Input", JOptionPane.QUESTION_MESSAGE);

		if (!filename.isEmpty())
		{
			if (fc.delete(filename, myToken))
				JOptionPane.showMessageDialog(this, "File deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
			else
				JOptionPane.showMessageDialog(this, "File not deleted.", "Warning", JOptionPane.WARNING_MESSAGE);
		}
	}

	protected void downloadFileButtonPressed()
	{
		String sourceFile = JOptionPane.showInputDialog(this, "Please enter the server file name use wish to download:", "Input", JOptionPane.QUESTION_MESSAGE);
		String destFile = JOptionPane.showInputDialog(this, "Please enter the local file name you wish to create:", "Input", JOptionPane.QUESTION_MESSAGE);

		if (!sourceFile.isEmpty() && !destFile.isEmpty())
		{
			if (fc.download(sourceFile, destFile, myToken, myGroups))
				JOptionPane.showMessageDialog(this, "File downloaded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
			else
				JOptionPane.showMessageDialog(this, "File not downloaded.", "Warning", JOptionPane.WARNING_MESSAGE);
		}
	}

	protected void uploadFileButtonPressed()
	{
		String sourceFile = JOptionPane.showInputDialog(this, "Please enter the local file name use wish to upload:", "Input", JOptionPane.QUESTION_MESSAGE);
		String destFile = JOptionPane.showInputDialog(this, "Please enter the server file name you wish to create:", "Input", JOptionPane.QUESTION_MESSAGE);
		String group = JOptionPane.showInputDialog(this, "Please enter the group name to which this file belongs:", "Input", JOptionPane.QUESTION_MESSAGE);

		if (!sourceFile.isEmpty() && !destFile.isEmpty() && !group.isEmpty())
		{
			if (fc.upload(sourceFile, destFile, group, myToken, myGroups))
				JOptionPane.showMessageDialog(this, "File uploaded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
			else
				JOptionPane.showMessageDialog(this, "File not uploaded.", "Warning", JOptionPane.WARNING_MESSAGE);
		}
	}

	protected void listFilesButtonPressed()
	{
		List<String> files = fc.listFiles(myToken);

		if (files != null && !files.isEmpty())
		{
			StringBuilder sb = new StringBuilder("\n");

			for (String f : files)
				sb.append(f + "\n");

			JOptionPane.showMessageDialog(this, "Files to view:" + sb.toString(), "Infomation", JOptionPane.INFORMATION_MESSAGE);
		}
		else
			JOptionPane.showMessageDialog(this, "No files to list.", "Infomation", JOptionPane.INFORMATION_MESSAGE);
	}

	protected void createGroupButtonPressed()
	{
		String groupname = JOptionPane.showInputDialog(this, "Please enter the group name use wish to create:", "Input", JOptionPane.QUESTION_MESSAGE);

		if (!groupname.isEmpty())
		{
			if (gc.createGroup(groupname, myToken))
				JOptionPane.showMessageDialog(this, "Group created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
			else
				JOptionPane.showMessageDialog(this, "Group not created.", "Warning", JOptionPane.WARNING_MESSAGE);
		}
	}

	protected void deleteUserButtonPressed()
	{
		String user = JOptionPane.showInputDialog(this, "Please enter the username use wish to delete:", "Input", JOptionPane.QUESTION_MESSAGE);

		if (!user.isEmpty())
		{
			if (gc.deleteUser(user, myToken))
				JOptionPane.showMessageDialog(this, "User deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
			else
				JOptionPane.showMessageDialog(this, "User not deleted.", "Warning", JOptionPane.WARNING_MESSAGE);
		}
	}

	protected void removeFromGroupButtonPressed()
	{
		String group = JOptionPane.showInputDialog(this, "Please enter the group name use wish to change:", "Input", JOptionPane.QUESTION_MESSAGE);
		String user = JOptionPane.showInputDialog(this, "Please enter the username use wish to remove:", "Input", JOptionPane.QUESTION_MESSAGE);

		if (!user.isEmpty() && !group.isEmpty())
		{
			if (gc.deleteUserFromGroup(user, group, myToken))
				JOptionPane.showMessageDialog(this, "User removed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
			else
				JOptionPane.showMessageDialog(this, "User not removed.", "Warning", JOptionPane.WARNING_MESSAGE);
		}
	}

	protected void addToGroupButtonPressed()
	{
		String group = JOptionPane.showInputDialog(this, "Please enter the group name use wish to change:", "Input", JOptionPane.QUESTION_MESSAGE);
		String user = JOptionPane.showInputDialog(this, "Please enter the username use wish to add:", "Input", JOptionPane.QUESTION_MESSAGE);

		if (!user.isEmpty() && !group.isEmpty())
		{
			if (gc.addUserToGroup(user, group, myToken))
				JOptionPane.showMessageDialog(this, "User added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
			else
				JOptionPane.showMessageDialog(this, "User not added.", "Warning", JOptionPane.WARNING_MESSAGE);
		}
	}

	protected void listMembersButtonPressed()
	{
		String group = JOptionPane.showInputDialog(this, "Please enter the group name use wish to view:", "Input", JOptionPane.QUESTION_MESSAGE);

		if (!group.isEmpty())
		{
			List<String> members = gc.listMembers(group, myToken);

			if (members != null && !members.isEmpty())
			{
				StringBuilder sb = new StringBuilder("\n");

				for (String m : members)
					sb.append(m + "\n");

				JOptionPane.showMessageDialog(this, "Members that belong to " + group + ":" + sb.toString(), "Infomation", JOptionPane.INFORMATION_MESSAGE);
			}
			else
				JOptionPane.showMessageDialog(this, "No members to list.", "Infomation", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	protected void deleteGroupButtonPressed()
	{
		String groupname = JOptionPane.showInputDialog(this, "Please enter the group name use wish to delete:", "Input", JOptionPane.QUESTION_MESSAGE);

		if (!groupname.isEmpty())
		{
			if (gc.deleteGroup(groupname, myToken))
				JOptionPane.showMessageDialog(this, "Group deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
			else
				JOptionPane.showMessageDialog(this, "Group not deleted.", "Warning", JOptionPane.WARNING_MESSAGE);
		}
	}

	protected void createUserButtonPressed()
	{
		String username = JOptionPane.showInputDialog(this, "Please enter the username use wish to create:", "Input", JOptionPane.QUESTION_MESSAGE);
		String password = JOptionPane.showInputDialog(this, "Please enter the user's password wish to create:", "Input", JOptionPane.QUESTION_MESSAGE);

		if (gc.createUser(username, password, myToken))
			JOptionPane.showMessageDialog(this, "User created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
		else
			JOptionPane.showMessageDialog(this, "User not created.", "Warning", JOptionPane.WARNING_MESSAGE);
	}

	protected void getTokenButtonPressed()
	{
		String username = JOptionPane.showInputDialog(this, "Please enter your username:", "Input", JOptionPane.QUESTION_MESSAGE);
		String password = JOptionPane.showInputDialog(this, "Please enter your password:", "Input", JOptionPane.QUESTION_MESSAGE);

		if (!username.isEmpty())
		{
			myToken = gc.getToken(username,password);
			myGroups = gc.getGroups();

			if (myToken == null)
				JOptionPane.showMessageDialog(this, "No token received. Check username.", "Warning", JOptionPane.WARNING_MESSAGE);
			else
				JOptionPane.showMessageDialog(this, "Valid token received.", "Success", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	protected void disconnectButtonPressed(ActionEvent e)
	{
		JButton b = (JButton)e.getSource();
		if (b.getName().equals("groupDisconnect"))
			gc.disconnect();
		else if (b.getName().equals("fileDisconnect"))
			fc.disconnect();

		JOptionPane.showMessageDialog(this, "Disconnected from server.", "Successful", JOptionPane.INFORMATION_MESSAGE);
	}

	protected void connectButtonPressed(ActionEvent e)
	{
		boolean success = false;

		String server = JOptionPane.showInputDialog(this, "Please enter the server name:", "Input", JOptionPane.QUESTION_MESSAGE);

		try {
			int port = Integer.parseInt(JOptionPane.showInputDialog(this, "Please enter the server port number:", "Input", JOptionPane.QUESTION_MESSAGE));

			JButton b = (JButton)e.getSource();
			if (b.getName().equals("groupConnect"))
				success = gc.connect(server, port);
			else if (b.getName().equals("fileConnect"))
				success = fc.connect(server, port);
		}
		catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "You entered an invalid port number!", "Error", JOptionPane.ERROR_MESSAGE);
		}

		if (success)
			JOptionPane.showMessageDialog(this, "Connection successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
		else
			JOptionPane.showMessageDialog(this, "Connection unsuccessful.", "Warning", JOptionPane.WARNING_MESSAGE);
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		Client group = (Client) gc;
		Client file = (Client) fc;
		if (group.isConnected())
			gc.disconnect();
		if (file.isConnected())
			fc.disconnect();
	}
	
	@Override
	public void windowActivated(WindowEvent e) { }

	@Override
	public void windowClosed(WindowEvent e) { }

	@Override
	public void windowDeactivated(WindowEvent e) { }

	@Override
	public void windowDeiconified(WindowEvent e) { }

	@Override
	public void windowIconified(WindowEvent e) { }

	@Override
	public void windowOpened(WindowEvent e) { }
}
