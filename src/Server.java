import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

public abstract class Server {

	protected int port;
	public String name;

	protected KeyPair RSAkeys;
	protected RSAPublicKeySpec publicKey;

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
			KeyPair kp = keyGen.genKeyPair();
			RSAkeys = kp;
	        KeyFactory fact = KeyFactory.getInstance("RSA");        
	        try {
	        	publicKey = fact.getKeySpec(kp.getPublic(), RSAPublicKeySpec.class);
			} catch (InvalidKeySpecException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace(System.err);
		}
	}

}
