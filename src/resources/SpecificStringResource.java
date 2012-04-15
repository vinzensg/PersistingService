package resources;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.POSTRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import database.DatabaseConnection;
import database.type.StringType;
import database.type.StringTypeRepository;

public class SpecificStringResource extends LocalResource {
	
	private String resourceIdentifier;
	private String deviceURI;
	
	private StringTypeRepository stringTypeRepository;
	
	private Timer timer;

	public SpecificStringResource(String resourceIdentifier, String deviceURI) {
		super(resourceIdentifier);
		setTitle("Resource to sign up for observing a String value");
		setResourceType("stringtype");
		
		this.resourceIdentifier = resourceIdentifier;
		this.deviceURI = deviceURI;

		stringTypeRepository = new StringTypeRepository(StringType.class, DatabaseConnection.getCouchDbConnector(), deviceURI);
		
		timer = new Timer();
		timer.schedule(new TimeTask(), 0, 10000);
	}

	private class TimeTask extends TimerTask {
		
		@Override
		public void run() {
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

			try {
				Response response = request.receiveResponse();

				if (response != null) {
					String payload = response.getPayloadString();
					
					// String payload = request.getPayloadString();
					System.out.println("URI: " + deviceURI);
					System.out.println("Payload: " + payload);
					
					StringType stringType = new StringType();
					stringType.setDevice(deviceURI);
					stringType.setStringValue(payload);
					DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			        Date date = new Date();
					stringType.setDateTime(dateFormat.format(date));
					
					stringTypeRepository.add(stringType);
				} else {
					System.out.println("Get request on device did not work.");
					System.out.println("CLASS: SpecificStringResource");
				}
			
			} catch (InterruptedException e) {
				System.err.println("Receiving of response interrupted: " + e.getMessage());
				System.err.println("CLASS: SpecificStringResource");
				System.exit(-1);
			}
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
	
	public void performPOST(POSTRequest request) {
		
		request.respond(CodeRegistry.RESP_CONTENT, "POST request received.");
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
}
