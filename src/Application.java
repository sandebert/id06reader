import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import java.util.List;

public class Application
{
	private String _url;
	
	private TerminalFactory _factory;
	private CardTerminal _terminal;
	
	Application(String url, int terminalId) throws Exception
	{
		_url = url;
		
		_factory = TerminalFactory.getDefault();
		List<CardTerminal> terminals = _factory.terminals().list();
		
		System.out.println("Terminals: " + terminals);
		
		if (terminals.isEmpty())
		{
			throw new Exception("No card terminal found");
		}
		
		if (terminalId < 0 || terminalId >= terminals.size())
		{
			throw new Exception("Invalid terminal index");
		}

		_terminal = terminals.get(terminalId);
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
	
	public String parseUID(byte val[])
	{
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < val.length; i++)
		{
            byte b = val[i];
            
            if (i <= val.length - 3)
            {
            	// append uid
            	sb.append(String.format("%02X", b));
            }
		}
		
		return sb.toString();
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
					card = _terminal.connect("*"); // T={protocol}
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
						ResponseAPDU r;
						
						byte[] uidCommand = { (byte)Apdu.CLS_PTS, (byte)Apdu.INS_GET_DATA, (byte)0x00, (byte)0x00, (byte)0x00 };
						
						r = channel.transmit(new CommandAPDU(uidCommand));
						System.out.println("UID response: " + r);
						System.out.println("UID: " + parseUID(r.getBytes()));
						
						
						byte uidBytes[] = card.getATR().getBytes();
						
						dumpBytes(uidBytes);
						System.out.println("UID: " + parseUID(uidBytes));
						
						byte[] historyCommand = { (byte)Apdu.CLS_PTS, (byte)Apdu.INS_GET_DATA, (byte)0x01, (byte)0x00, (byte)0x00 };
						
						r = channel.transmit(new CommandAPDU(historyCommand));
						System.out.println("Hystorical bytes response: " + r.toString());

						dumpBytes(r.getBytes());
						
						byte histricalBytes[] = card.getATR().getHistoricalBytes();
						
						dumpBytes(histricalBytes);
						
						
						byte IDENTITY_FILE_AID[] = {   
							   (byte) 0x3F,// MASTER FILE, Head directory MF "3f00"  
							   (byte) 0x00,   
							   (byte) 0xDF,// Dedicated File, subdirectory identity DF(ID) "DF01"  
							   (byte) 0x01,   
							   (byte) 0x40,// Elementary File, the identity file itself EF(ID#RN) "4031"  
							   (byte) 0x31 };
						
						CommandAPDU selectFileApdu = new CommandAPDU(   
								0x00, //CLA   
								0xA4, //INS  
								0x08, //P1  
								0x0C, //P2  
								IDENTITY_FILE_AID);  
						
						r = channel.transmit(selectFileApdu);
						System.out.println("response: " + r.toString());

						dumpBytes(r.getBytes());
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
