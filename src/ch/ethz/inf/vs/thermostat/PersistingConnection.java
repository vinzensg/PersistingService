package ch.ethz.inf.vs.thermostat;

import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;

public class PersistingConnection {

	private final String PERSISTING_SERVICE = "coap://localhost:5684/persistingservice/number/pwrdim";
	
	public PersistingConnection() {
				
	}
	
	public Object[][] getSinceDateValue(String date) {
		String payload = "date = " + date + "\n" +
						 "withdate = true";
	
		String responseString = performRequest("/since", payload);
		if (responseString.isEmpty())
			return new Object[0][0];
		
		String[] values = responseString.split("\n");
		String[] data_x = new String[values.length];
		Float[] data_y = new Float[values.length];
		for (int i=0; i<values.length; i++) {
			data_x[i] = values[i].split(";")[1];
			data_y[i] = Float.parseFloat(values[i].split(";")[0]);
		}
		
		Object[][] ret = {data_x, data_y};
		return ret;
	}
	
	public float getSinceAvg(String date) {
		String payload = "date = " + date;
		
		String responseString = performRequest("/since/avg", payload);
		if (responseString.isEmpty())
			return 0;
		
		float data = Float.valueOf(responseString);
		
		return data;
	}
	
	public float getSinceMax(String date) {
		String payload = "date = " + date;
		
		String responseString = performRequest("/since/max", payload);
		if (responseString.isEmpty())
			return 0;
		
		float data = Float.valueOf(responseString);
		
		return data;
	}
	
	public float getSinceMin(String date) {
		String payload = "date = " + date;
		
		String responseString = performRequest("/since/min", payload);
		if (responseString.isEmpty())
			return 0;
		
		float data = Float.valueOf(responseString);
		
		return data;
	}
	
	private String performRequest(String resource, String payload) {
		Request request = new GETRequest();
		request.setURI(PERSISTING_SERVICE + resource);
		request.setPayload(payload);
		request.enableResponseQueue(true);
		
		try {
			request.execute();
		} catch (IOException e) {
			System.err.println("IOException: " + e.getMessage());
		}
		
		Response response = null;
		
		try {
			response = request.receiveResponse();
			response.prettyPrint();
		} catch (InterruptedException e) {
			System.err.println("InterruptedException: " + e.getMessage());
		}
	
		return response.getPayloadString();
	}
	
	
}
