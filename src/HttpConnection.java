import java.io.BufferedReader;
//import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpConnection
{
	public static final String USER_AGENT = "Mozilla/5.0";
	
	// HTTP POST request
	public static void sendPost(String url, String uid, String firstName, String lastName, String countryCode,
			String companyNumber, String nationality, String personalNumber, String companyName,
			long lfSerial, String validity, String speedDial, String companyUrl, String training, String relativePhone) throws Exception
	{
		Map<String,Object> params = new LinkedHashMap<>();
		params.put("uid", uid);
        params.put("first_name", firstName);
        params.put("last_name", lastName);
        params.put("country_code", countryCode);
        params.put("company_number", companyNumber);
        params.put("nationality", nationality);
        
        params.put("personal_number", personalNumber);
        params.put("company_name", companyName);
        params.put("lf_serial", lfSerial);
        
        params.put("validity", validity);
        params.put("speed_dial", speedDial);
        params.put("company_url", companyUrl);
        params.put("training", training);
        params.put("relative_phone", relativePhone);
        
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String,Object> param : params.entrySet())
        {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
        }
        //byte[] postDataBytes = postData.toString().getBytes("UTF-8");
        
        URL obj = new URL(url + "?" + postData.toString());
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		//add request header
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		
		// Send post request
		/*con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.write(postDataBytes);
		wr.flush();
		wr.close();*/
		
		//String postString = new String(postDataBytes);
		int responseCode = con.getResponseCode();
		System.out.println("Sending request to URL: " + obj.toString());
		//System.out.println("Post parameters: " + postString);
		System.out.println("Response Code: " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null)
		{
			response.append(inputLine);
		}
		in.close();
		
		//print result
		System.out.println(response.toString());
	}
}
