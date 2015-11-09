public class Main
{
	public static void main(String[] args)
	{
		System.out.println("Starting up");
		
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
