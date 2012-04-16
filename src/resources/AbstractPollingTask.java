package resources;

import java.io.IOException;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;

public abstract class AbstractPollingTask extends TimerTask {
	
	protected String deviceURI;
	
	public AbstractPollingTask(String deviceURI) {
		this.deviceURI = deviceURI;
	}
	
	protected Request getRequest(String deviceURI) {
		Request request = new GETRequest();
		request.setURI(deviceURI);
		request.enableResponseQueue(true);

		try {
			request.execute();
		} catch (IOException e) {
			System.err.println("Failed to execute request: " + e.getMessage());
			System.err.println("CLASS: SpecificStringResource");
			System.exit(-1);
		}
		
		return request;
	}
	
	protected String storeData(Request request) {
		String payload = null;
		try {
			Response response = request.receiveResponse();
			if (response != null) {
				payload = response.getPayloadString();
				System.out.println("URI: " + deviceURI);
				System.out.println("PAYLOAD: " + payload);	
				storePayload(payload);
			} else {
				System.out.println("Get request on device did not work.");
			}
		} catch (InterruptedException e) {
			System.err.println("Receiving of response interrupted: " + e.getMessage());
			System.exit(-1);
		}
		return payload;
	}
	
	protected abstract void storePayload(String payload);
	
	public abstract void run();

}
