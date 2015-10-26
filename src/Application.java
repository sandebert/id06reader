import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Properties;

public class Application
{
	static final int BLOCK_SIZE = 16;
	
	final byte KEY_A[] = { (byte)0xA0, (byte)0xA1, (byte)0xA2, (byte)0xA3, (byte)0xA4, (byte)0xA5 };
	final byte KEY_ID06USER[] = { (byte)0x6E, (byte)0x77, (byte)0x47, (byte)0x39, (byte)0x4E, (byte)0x63 };
	
	private String _url = "";
	private int _readerId = 0;
	
	private TerminalFactory _factory;
	private CardTerminal _terminal;
	
	Application() throws Exception
	{
		InputStream inputStream = null;
		
		try
		{
			Properties prop = new Properties();
			String propFileName = "config.properties";
 
			inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
 
			if (inputStream != null)
			{
				prop.load(inputStream);
			}
			else
			{
				throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
			}
			
			_url = prop.getProperty("url");
			_readerId = Integer.parseInt(prop.getProperty("reader"));
		}
		catch (Exception e)
		{
			System.out.println("Exception: " + e);
		}
		finally
		{
			if (inputStream != null)
			{
				inputStream.close();
			}
		}
		
		_factory = TerminalFactory.getDefault();
		List<CardTerminal> terminals = _factory.terminals().list();
		
		System.out.println("Terminals: " + terminals);
		
		if (terminals.isEmpty())
		{
			throw new Exception("No card terminal found");
		}
		
		if (_readerId < 0 || _readerId >= terminals.size())
		{
			throw new Exception("Invalid terminal index");
		}

		_terminal = terminals.get(_readerId);
	}
	
	public void dumpBytes(byte val[])
	{
		for (int i = 0; i < val.length; ++i)
		{
			if (i != 0)
			{
				System.out.print(", ");
			}

			//System.out.print(val[i]);
			System.out.print(String.format("%02X ", val[i]));
		}
		System.out.print("\n");
	}
	
	public static boolean isSuccess(ResponseAPDU responseAPDU)
	{
		return responseAPDU.getSW1() == 0x90 && responseAPDU.getSW2() == 0x00;
	}
	
	boolean readUID(CardChannel channel) throws Exception
	{
		byte[] uidCommand = { (byte)Apdu.CLS_PTS, (byte)Apdu.INS_GET_DATA, (byte)0x00, (byte)0x00, (byte)0x00 };
		
		ResponseAPDU r = channel.transmit(new CommandAPDU(uidCommand));
		
		if (isSuccess(r))
		{
			System.out.println("UID response: " + r);
			System.out.print("Bytes: ");
			dumpBytes(r.getBytes());
			System.out.print("UID: ");
			dumpBytes(r.getData());
		}
		else
		{
			System.out.println("UID command failed");
			return false;
		}
		
		return true;
	}
	
	boolean loadKey(byte key[], byte memoryKeyId, CardChannel channel) throws Exception
	{
		CommandAPDU loadKeyCommand = new CommandAPDU(Apdu.CLS_PTS, Apdu.INS_EXTERNAL_AUTHENTICATE,
				Acs.P1_LOAD_KEY_INTO_VOLATILE_MEM, memoryKeyId, key);
		
		ResponseAPDU r = channel.transmit(loadKeyCommand);
		
		if (isSuccess(r))
		{
			System.out.println("Key loaded: " + r.toString());
		}
		else
		{
			System.out.println("Load key command failed");
			return false;
		}
		
		return true;
	}
	
	boolean loginToSector(byte blockNumber, byte memoryKeyId, CardChannel channel) throws Exception
	{
		CommandAPDU authenticateCommand = new CommandAPDU(Apdu.CLS_PTS, Apdu.INS_INTERNAL_AUTHENTICATE_ACS, 0, 0, new byte[] { (byte)0x01, (byte)0x00, blockNumber,
				Acs.KEY_A, (byte)memoryKeyId });
		
		ResponseAPDU r = channel.transmit(authenticateCommand);
		
		if (isSuccess(r))
		{
			//System.out.println("Authenticated: " + r.toString());
		}
		else
		{
			System.out.println("Load key command failed");
			return false;
		}
		
		return true;
	}
	
	byte[] readBlock(byte blockNumber, CardChannel channel) throws Exception
	{
		byte[] readBinaryCommand = { (byte)Apdu.CLS_PTS, (byte)Apdu.INS_READ_BINARY, (byte)0x00, (byte)blockNumber, (byte)BLOCK_SIZE };
		ResponseAPDU r = channel.transmit(new CommandAPDU(readBinaryCommand));
		
		if (isSuccess(r))
		{
			System.out.println("Read binary response: " + r.toString());
			//System.out.println("Data: " + r.getData());
			//dumpBytes(r.getBytes());
			
			return r.getData();
		}
		else
		{
			System.out.println("Read binary command failed");
			return null;
		}
	}
	
	byte[] readSection(byte sector, byte[] key, CardChannel channel) throws Exception
	{
		MemoryLayout memoryLayout = MemoryLayout.CLASSIC_1K;
		
		ByteBuffer buffer = ByteBuffer.allocate(4 * 16);
		
		// Load key
		byte memoryKeyId = (byte)0x00;
		
		loadKey(key, memoryKeyId, channel);
		
		for (int i = 0; i <= memoryLayout.getTrailerBlockNumberForSector(sector); ++i)
		{
			byte blockNumber = (byte)memoryLayout.getBlockNumber(sector, i);
			loginToSector(blockNumber, memoryKeyId, channel);
			
			byte[] bytes = readBlock(blockNumber, channel);
			
			if (bytes != null)
			{
				buffer.put(bytes);
			}
			else
			{
				return null;
			}
		}
		
		return buffer.array();
	}
	
	String readString(int start, int length, byte[] data)
	{
		String result = "";
		
		int limit = (length == 0) ? data.length : start + length;
		
		for (int i = start; i < limit; ++i)
		{
			if (length == 0 && data[i] == 0x00)
			{
				break;
			}
			
			result += (char)data[i];
		}
		
		return result;
	}
	
	long readLong(int start, byte[] data)
	{
		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.position(start);
		buffer.order(ByteOrder.BIG_ENDIAN);
		
		return buffer.getLong();
	}
	
	public void run() throws Exception
	{
		while (true)
		{
			if (_terminal.waitForCardPresent(0))
			{
				Card card = null;
				
				try
				{
					card = _terminal.connect("T=1");
				}
				catch (Exception e)
				{
					e.printStackTrace();
					
					if (_terminal.waitForCardAbsent(0))
					{
						System.out.println("Card disconnected");
					}
				}
				
				if (card != null)
				{
					System.out.println("Card connected " + card);
					
					try
					{
						CardChannel channel = card.getBasicChannel();
						
						readUID(channel);
						
						byte[] section1 = readSection((byte)0x01, KEY_A, channel);
						System.out.print("Section 1: ");
						dumpBytes(section1);
						
						int index = 1; // length
						String firstName = readString(index, 0, section1);
						index += firstName.length() + 2; // 0 and length
						String lastName = readString(index, 0, section1);
						
						byte[] section2 = readSection((byte)0x02, KEY_ID06USER, channel);
						System.out.print("Section 2: ");
						dumpBytes(section2);
						
						String countryCode = readString(16, 2, section2);
						
						String companyNumber = readString(18, 0, section2);
						
						String nationality = readString(32, 2, section2);;
						
						String personalNumber = readString(34, 0, section2);
						
						byte[] section3 = readSection((byte)0x03, KEY_ID06USER, channel);
						System.out.print("Section 3: ");
						dumpBytes(section3);
						
						String companyName = readString(0, 0, section3);
						
						byte[] section4 = readSection((byte)0x04, KEY_ID06USER, channel);
						System.out.print("Section 4: ");
						dumpBytes(section4);
						
						long lfSerial = readLong(0, section4);
						
						String validity = readString(8, 8, section4);
						String speedDial = readString(16, 0, section4);
						String url = readString(32, 0, section4);
						
						byte[] section5 = readSection((byte)0x05, KEY_ID06USER, channel);
						System.out.print("Section 5: ");
						dumpBytes(section5);
						String training = readString(0, 0, section5);
						String relativePhone = readString(16, 0, section5);
						
						byte[] section6 = readSection((byte)0x06, KEY_ID06USER, channel);
						System.out.print("Section 6: ");
						dumpBytes(section6);
						//String relativePhone = readString(16, 0, section5);
						
						
						System.out.println(firstName);
						System.out.println(lastName);
						
						System.out.println(countryCode);
						System.out.println(companyNumber);
						System.out.println(nationality);
						System.out.println(personalNumber);
						
						System.out.println(companyName);
						
						System.out.println(lfSerial);
						System.out.println(validity);
						System.out.println(speedDial);
						System.out.println(url);
						
						System.out.println(training);
						System.out.println(relativePhone);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					finally
					{
						card.disconnect(false);
						
						if (_terminal.waitForCardAbsent(0))
						{
							System.out.println("Card disconnected");
						}
					}
				}
			}
		}
	}
}
