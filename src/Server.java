import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public abstract class Server {

	protected int port;
	public String name;

	protected KeyPair RSAkeys;

	abstract void start();

	public Server(int _SERVER_PORT, String _serverName) {
		port = _SERVER_PORT;
		name = _serverName; 
	}

	public int getPort() {
		return port;
	}

	public String getName() {
		return name;
	}

	protected void createRSAKeyPair() {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			RSAkeys = keyGen.genKeyPair();
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

}
