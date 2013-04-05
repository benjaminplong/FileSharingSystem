import java.util.ArrayList;


public class Envelope implements java.io.Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7726335089122193103L;
	private String msg;
	private int number;
	private ArrayList<Object> objContents = new ArrayList<Object>();
	private byte[] checksum;
	
	public Envelope(String text)
	{
		msg = text;
	}
	
	public Envelope(String text, int index)
	{
		msg = text;
		number = index;
	}
	
	public String getMessage()
	{
		return msg;
	}
	
	public int getNumber() {
		return number;
	}

	public ArrayList<Object> getObjContents()
	{
		return objContents;
	}
	
	public void addObject(Object object)
	{
		objContents.add(object);
	}

	public byte[] getChecksum() {
		return checksum;
	}

	public void setChecksum(byte[] checksum) {
		this.checksum = checksum.clone();
	}

}
