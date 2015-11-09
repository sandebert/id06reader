
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

// A - beep-beep-beep
// B - beep
// N - name of device
// V - version of device
// R - reset device
// {STX}response{CR} - response
// {ESC} - card removed

public class ComApplication
{
	byte[] _buffer = new byte[100];
	
	public static final int TIMEOUTSECONDS = 2;
	public static final int BAUD = 9600;
	//public static final int BAUD = 19200;
	
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
	
	public void sendCommand() throws Exception
	{
		//byte command[] = { (byte)0x3A, (byte)0x00, (byte)0x20, (byte)0x00, (byte)0x13 };
		byte command[] = { (byte)0x41 };
		_output.write(command);
		_output.flush();
		
		System.out.println("Command sent");
	}
	
	public void run() throws Exception
	{
		while (true)
		{
			if (_input.available() > 0)
			{
				int len = _input.read(_buffer);
				
				byte[] data = new byte[len];
				System.arraycopy(_buffer, 0, data, 0, len);
				
				System.out.print("Data: ");
				Utils.dumpBytes(data);
				
				sendCommand();
			}
			
			Thread.sleep(1000);
		}
	}
}
