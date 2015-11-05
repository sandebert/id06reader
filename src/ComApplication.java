
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Enumeration;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class ComApplication
{
	public static final int TIMEOUTSECONDS = 2;
	public static final int BAUD = 9600;
	
	CommPortIdentifier _portId = null;
	SerialPort _port = null;
	
	protected InputStream _input;
	protected OutputStream _output;
	
	public ComApplication() throws Exception
	{
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
		
		while (portList.hasMoreElements())
		{
			CommPortIdentifier portId = portList.nextElement();
			
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL)
			{
				System.out.println(portId.getName());
				
				if (portId.getName().equals("COM4"))
				{
					_portId = portId;
				}
			}
		}
		
		if (_portId == null)
		{
			throw new Exception("No port found");
		}
		
		_port = (SerialPort)_portId.open("id06reader", TIMEOUTSECONDS * 1000);
		
		System.out.println("Port open");
		
		_port.setSerialPortParams(BAUD, SerialPort.DATABITS_8,
		        SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		
		_input = _port.getInputStream();
		_output = _port.getOutputStream();

		System.out.println("Params set");
	}
	
	public void run() throws Exception
	{
		while (true)
		{
			if (_input.available() > 0)
			{
				byte[] buffer = new byte[100];
				
				int len = _input.read(buffer);
				
				System.out.print("Data: ");
				Utils.dumpBytes(buffer);
			}
			
			Thread.sleep(1000);
		}
	}
}
