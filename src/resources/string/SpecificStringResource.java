package resources.string;

import java.util.List;
import java.util.Timer;

import config.Constants;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import database.DatabaseConnection;
import database.type.string.StringType;
import database.type.string.StringTypeRepository;

public class SpecificStringResource extends LocalResource {
	
	private String resourceIdentifier;
	
	private StringTypeRepository stringTypeRepository;
	
	private Timer timer;

	public SpecificStringResource(String resourceIdentifier, String deviceURI, boolean push, String pushtarget, int datainterval) {
		super(resourceIdentifier);
		setTitle("Resource to sign up for observing a String value");
		setResourceType("stringtype");
		this.resourceIdentifier = resourceIdentifier;
		stringTypeRepository = new StringTypeRepository(StringType.class, DatabaseConnection.getCouchDbConnector(), deviceURI);
		
		switch(evalMode(push, datainterval)) {
			case Constants.PUSH_PUSH:
				System.out.println("PUSH_PUSH");
				// register for push on device
				// store in history and push to pushtarget
				break;
			case Constants.POLLING_PUSH:
				System.out.println("POLLING_PUSH");
				timer = new Timer();
				timer.schedule(new StringPollingPushTask(deviceURI, stringTypeRepository, pushtarget), 0, datainterval);
				// push to pushtarget 
				break;
			case Constants.PUSH_STORE:
				System.out.println("PUSH_STORE");
				// register for push on device
				// store in history ONLY
				break;
			case Constants.POLLING_STORE:
				System.out.println("POLLING_STORE");
				timer = new Timer();
				timer.schedule(new StringPollingStoreTask(deviceURI, stringTypeRepository), 0, datainterval);
				break;
			default:
			
		}
	}
	
	public void performGET(GETRequest request) {
		String response = "GET request received.\n";
		List<StringType> result = stringTypeRepository.queryDevice();
		System.out.println("Result: " + result.get(0));
		System.out.println("Size of Result: " + result.size());
		for (StringType element : result) {
			response += element.getDevice() + " ";
			response += element.getStringValue() + "\n";
			response += element.getDateTime() + "\n";
		}
		request.respond(CodeRegistry.RESP_CONTENT, response);
	}
	
	public void performDELETE(DELETERequest request) {
		timer.cancel();
		timer.purge();
		String responseString = "The Resource " + resourceIdentifier + " has been removed";
		Response response = new Response(CodeRegistry.RESP_CONTENT);
		response.setPayload(responseString);
		response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
		request.respond(response);
		this.remove();
	}
	
	private int evalMode(boolean push, int datainterval) {
		if (push && datainterval <= 0) {
			// check push mechanism at device
			boolean devicepush = false;
			if (devicepush) {
				return Constants.PUSH_PUSH;
			} else {
				return Constants.POLLING_PUSH;
			}
		} else if (push && datainterval > 0) {
			return Constants.POLLING_PUSH;
		} else if (!push && datainterval <= 0) {
			// check push mechanism on device
			boolean devicepush = false;
			if (devicepush) {
				return Constants.PUSH_STORE;
			} else {
				return Constants.POLLING_STORE;
			}
		} else if (!push && datainterval > 0) {
			return Constants.POLLING_STORE;
		}
		return Constants.POLLING_STORE;
	}
}
