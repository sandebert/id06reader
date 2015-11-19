public class Main
{
	static String VERSION = "1.2";
	
	public static void main(String[] args)
	{
		System.out.println("ID06 reader v" + VERSION);
		
		Application app = null;
		//ComApplication app = null;
		
		try
		{
			app = new Application();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		if (app != null)
		{
			try
			{
				app.run();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
