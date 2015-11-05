
public class Utils
{
	public static void dumpBytes(byte val[])
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
}
