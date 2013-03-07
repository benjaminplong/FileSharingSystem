import java.net.Socket;
import java.net.UnknownHostException;
import java.security.PublicKey;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public abstract class Client {

	/* protected keyword is like private but subclasses have access
	 * Socket and input/output streams
	 */
	protected Socket sock;
	protected ObjectOutputStream output;
	protected ObjectInputStream input;
	protected PublicKey serverKey;

	public boolean connect(final String server, final int port) {
		System.out.println("attempting to connect");

		try {						
			sock = new Socket(server, port);
			
			output = new ObjectOutputStream(sock.getOutputStream());
			input = new ObjectInputStream(sock.getInputStream());
			
			Envelope e = (Envelope)input.readObject();
			if(e.getMessage().equals("PUBLICKEY"))
			{
				serverKey = (PublicKey) e.getObjContents().get(0);
			}
			
			return sock.isConnected();
		} catch (UnknownHostException e) {
			System.out.println("host was not found");
		} catch (IOException e) {
			System.out.println("an IO error occured");
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
		return false;
	}

	public boolean isConnected() {
		return (sock != null && sock.isConnected());
	}

	public void disconnect()	 {
		if (isConnected()) {
			try
			{
				Envelope message = new Envelope("DISCONNECT");
				output.writeObject(message);
			}
			catch(Exception e)
			{
				System.err.println("Error: " + e.getMessage());
				e.printStackTrace(System.err);
			}
		}
	}
}
