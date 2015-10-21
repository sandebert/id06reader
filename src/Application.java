import java.util.List;

import javax.smartcardio.*;

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
	
	public void run() throws Exception
	{
		while (true)
		{
			if (_terminal.waitForCardPresent(0))
			{
				Card card = null;
				
				try
				{
					card = _terminal.connect("*");
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				if (card != null)
				{
					System.out.println("Card connected " + card);
					
					try
					{
						byte[] getDataCommand = {(byte)0xff, (byte)0xca, (byte)0x00, (byte)0x00, (byte)0x00};
						
						CardChannel channel = card.getBasicChannel();
						ResponseAPDU r = channel.transmit(new CommandAPDU(getDataCommand));
						System.out.println("response: " + r.getBytes());
						
						byte response[] = r.getBytes();
						
						for (int i = 0; i < response.length; ++i)
						{
							System.out.println(response[i]);
						}
						
						byte[] command = {(byte)0x00, (byte)0xca, (byte)0x00, (byte)0x40, (byte)0xff};
						ResponseAPDU answer = channel.transmit(new CommandAPDU(command));
						System.out.println("answer: " + answer.toString());
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
