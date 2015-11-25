import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

public class Application
{
	static final int BLOCK_SIZE = 16;
	
	final byte KEY_A[] = { (byte)0xA0, (byte)0xA1, (byte)0xA2, (byte)0xA3, (byte)0xA4, (byte)0xA5 };
	final byte KEY_ID06USER[] = { (byte)0x6E, (byte)0x77, (byte)0x47, (byte)0x39, (byte)0x4E, (byte)0x63 };
	
	private String _url = "";
	private int _readerId = 0;
	
	private CardTerminal _terminal;
	
	class StringValue
	{
		String value;
		int dataLength;
	}
	
	Application() throws Exception
	{
		InputStream inputStream = null;
		
		try
		{
			Properties prop = new Properties();
			String propFileName = "config.properties";
 
			inputStream = new FileInputStream(propFileName);

			prop.load(inputStream);
			
			_url = prop.getProperty("url");
			_readerId = Integer.parseInt(prop.getProperty("reader"));
			
			System.out.println("URL: " + _url);
			System.out.println("Reader ID: " + String.valueOf(_readerId));
		}
		catch (Exception e)
		{
			throw e;
		}
		finally
		{
			if (inputStream != null)
			{
				inputStream.close();
			}
		}
	}
	
	public static boolean isSuccess(ResponseAPDU responseAPDU)
	{
		return responseAPDU.getSW1() == 0x90 && responseAPDU.getSW2() == 0x00;
	}
	
	String readUID(CardChannel channel) throws Exception
	{
		byte[] uidCommand = { (byte)Apdu.CLS_PTS, (byte)Apdu.INS_GET_DATA, (byte)0x00, (byte)0x00, (byte)0x00 };
		
		ResponseAPDU r = channel.transmit(new CommandAPDU(uidCommand));
		
		if (isSuccess(r))
		{
			System.out.println("UID response: " + r);
			System.out.print("Bytes: ");
			Utils.dumpBytes(r.getBytes());
			System.out.print("UID: ");
			Utils.dumpBytes(r.getData());
			
			String result = "";
			byte[] data = r.getData();
			
			for (int i = 0; i < data.length; ++i)
			{
				result += String.format("%02X", data[i]);
			}
			
			return result;
		}
		else
		{
			System.out.println("UID command failed");
			return null;
		}
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
	
	byte[] readSection(byte sector, byte memoryKeyId, CardChannel channel) throws Exception
	{
		MemoryLayout memoryLayout = MemoryLayout.CLASSIC_1K;
		
		ByteBuffer buffer = ByteBuffer.allocate(4 * 16);
		
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
	
	StringValue readString(int start, int length, byte[] data) throws Exception
	{
		StringValue result = new StringValue();
		
		int realLength = length;
		
		if (realLength == 0)
		{
			realLength = data.length - start;
			
			for (int i = start; i < data.length - start; ++i)
			{
				if (data[i] == (byte)0x00)
				{
					realLength = i - start - 1;
					break;
				}
			}
		}
		
		if (realLength > 0)
		{
			byte[] buffer = new byte[realLength];
			
			for (int i = start; i < start + realLength; ++i)
			{				
				buffer[i - start] = data[i];
				
				//System.out.println("" + i + " - " + data[i]);
			}
			
			System.out.print("String ");
			Utils.dumpBytes(buffer);
			
			result.value = new String(buffer, Charset.forName("ISO-8859-15"));
			result.dataLength = realLength;
		}
		else
		{
			result.value = "";
			result.dataLength = 0;
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
	
	public void connect() throws Exception
	{
		System.out.println("Connecting to terminal...");
		
		while (_terminal == null)
		{
			_terminal = getTerminal();
			
			if (_terminal != null)
			{
				System.out.println("Connected to " + _terminal.getName());
			}
			
			Thread.sleep(1000);
		}
	}
	
	private CardTerminal getTerminal()
	{
		try
		{
			TerminalFactory factory = TerminalFactory.getDefault();
			CardTerminals terminals = factory.terminals();
			//System.out.println("Got terminals");
			
			List<CardTerminal> terminalList = terminals.list();
			//System.out.println("Got terminal list");
			
			if (!terminalList.isEmpty())
			{
				//System.out.println("Terminals: " + terminals);
				
				if (_readerId < 0 || _readerId >= terminalList.size())
				{
					throw new Exception("Invalid terminal index");
				}

				return terminalList.get(_readerId);
			}
			else
			{
				System.out.println("No terminals connected");
			}
		}
		catch (Exception e)
		{
			//e.printStackTrace();
		}
		
		return null;
	}
	
	public void run() throws Exception
	{
		connect();
		
		while (true)
		{
			try
			{
				if (_terminal == null)
				{
					//connect();
					return;
				}
				else
				{
					while (!_terminal.waitForCardPresent(1000))
					{
						if (_terminal != getTerminal())
						{
							_terminal = null;
							break;
						}
					}
					
					if (_terminal != null)
					{
						Card card = _terminal.connect("T=1");
						
						if (card != null)
						{
							System.out.println("Card connected " + card);
							
							CardChannel channel = card.getBasicChannel();
							
							// Load key
							byte keyAMemoryId = (byte)0x00;
							loadKey(KEY_A, keyAMemoryId, channel);
							
							byte keyId06MemoryId = (byte)0x01;
							loadKey(KEY_ID06USER, keyId06MemoryId, channel);
							
							String uid = readUID(channel);
							
							byte[] section1 = readSection((byte)0x01, keyAMemoryId, channel);
							System.out.print("Section 1: ");
							Utils.dumpBytes(section1);
							
							int index = 1; // length
							StringValue lastNameValue = readString(index, 0, section1);
							String lastName = lastNameValue.value;
							index += lastNameValue.dataLength + 2; // 0 and length

							StringValue firstNameValue = readString(index, 0, section1);
							String firstName = firstNameValue.value;
							
							byte[] section2 = readSection((byte)0x02, keyId06MemoryId, channel);
							System.out.print("Section 2: ");
							Utils.dumpBytes(section2);
							
							String countryCode = readString(16, 2, section2).value;
							
							String companyNumber = readString(18, 0, section2).value;
							
							String nationality = readString(32, 2, section2).value;
							
							String personalNumber = readString(34, 0, section2).value;
							
							byte[] section3 = readSection((byte)0x03, keyId06MemoryId, channel);
							System.out.print("Section 3: ");
							Utils.dumpBytes(section3);
							
							String companyName = readString(0, 0, section3).value;
							
							byte[] section4 = readSection((byte)0x04, keyId06MemoryId, channel);
							System.out.print("Section 4: ");
							Utils.dumpBytes(section4);
							
							long lfSerial = readLong(0, section4);
							
							String validity = readString(8, 8, section4).value;
							String speedDial = readString(16, 0, section4).value;
							String companyUrl = readString(32, 0, section4).value;
							
							byte[] section5 = readSection((byte)0x05, keyId06MemoryId, channel);
							System.out.print("Section 5: ");
							Utils.dumpBytes(section5);
							String training = readString(0, 0, section5).value;
							String relativePhone = readString(16, 0, section5).value;
							
							byte[] section6 = readSection((byte)0x06, keyId06MemoryId, channel);
							System.out.print("Section 6: ");
							Utils.dumpBytes(section6);
							//String relativePhone = readString(16, 0, section5);
							
							System.out.println("UID: " + uid);
							System.out.println("First name: " + firstName);
							System.out.println("Last name: " + lastName);
							
							System.out.println("Country code: " + countryCode);
							System.out.println("Company number: " + companyNumber);
							System.out.println("Nationality: " + nationality);
							System.out.println("Personal number: " + personalNumber);
							
							System.out.println("Company name: " + companyName);
							
							System.out.println("LF serial: " + String.valueOf(lfSerial));
							System.out.println("Validity: " + validity);
							System.out.println("Speed dial: " + speedDial);
							System.out.println("Company url: " + companyUrl);
							
							System.out.println("Training: " + training);
							System.out.println("Relative phone: " + relativePhone);
							
							HttpConnection.sendData(_url, uid, firstName, lastName, countryCode,
									companyNumber, nationality, personalNumber, companyName,
									lfSerial, validity, speedDial, companyUrl, training, relativePhone);
						
							card.disconnect(false);
							
							while (_terminal != null && !_terminal.waitForCardAbsent(1000))
							{
								if (_terminal != getTerminal())
								{
									_terminal = null;
									break;
								}
							}
							
							System.out.println("Card disconnected");
						}
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				
				if (_terminal != null)
				{
					_terminal = null;
				}
			}
		}
	}
}
