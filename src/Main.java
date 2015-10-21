public class Main
{
	public static void main(String[] args)
	{
		System.out.println("Starting up");
		
		String url = "";
		int readerId = 0;
		
		if (args.length >= 1)
		{
			url = args[0];
		}
		if (args.length >= 2)
		{
			readerId = Integer.parseInt(args[1]);
		}
		
		Application app = null;
		
		try
		{
			app = new Application(url, readerId);
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
