package resources.number;

import java.io.IOException;

import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import database.type.number.NumberTypeRepository;

public class NumberPollingPushTask extends NumberPollingTask {
	
	private String pushtarget;

	public NumberPollingPushTask(String deviceURI, NumberTypeRepository numberTypeRepository, String pushtarget) {
		super(deviceURI, numberTypeRepository);
		this.pushtarget = pushtarget;
		System.out.println("PUSHTARGET: " + pushtarget);
	}

	@Override
	public void run() {
		Request request = getRequest(deviceURI);
		String payload = storeData(request);
		pushData(request, payload);
	}
	
	private void pushData(Request request, String payload) {		
		POSTRequest pushrequest = new POSTRequest();
		pushrequest.setURI(pushtarget);
		System.out.println("PAYLOAD: " + payload);
		pushrequest.setPayload(payload);
		try {
			pushrequest.execute();
		} catch (IOException e) {
			System.err.println("Failed to execute push: " + e.getMessage());
			System.exit(-1);
		}
	}

}
