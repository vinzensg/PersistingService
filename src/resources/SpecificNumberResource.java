package resources;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import parser.OptionParser;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import database.DatabaseConnection;
import database.type.number.NumberType;
import database.type.number.NumberTypeAvg;
import database.type.number.NumberTypeRepository;
import database.type.number.NumberTypeSum;

public class SpecificNumberResource extends LocalResource {
	
	private String resourceIdentifier;
	private String deviceURI;
	
	private NumberTypeRepository numberTypeRepository ;
	
	private Timer timer;

	public SpecificNumberResource(String resourceIdentifier, String deviceURI) {
		super(resourceIdentifier);
		setTitle("Resource to sign up for observing a String value");
		setResourceType("stringtype");
		
		this.resourceIdentifier = resourceIdentifier;
		this.deviceURI = deviceURI;

		numberTypeRepository = new NumberTypeRepository(NumberType.class, DatabaseConnection.getCouchDbConnector(), deviceURI);
		
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
					
					NumberType numberType = new NumberType();
					numberType.setDevice(deviceURI);
					numberType.setNumberValue(Integer.valueOf(payload));
					DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			        Date date = new Date();
					numberType.setDateTime(dateFormat.format(date));
					
					numberTypeRepository.add(numberType);
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
		
		OptionParser parsedOptions = new OptionParser(request.getOptions(OptionNumberRegistry.URI_QUERY));
		
		if (parsedOptions.notNull() && parsedOptions.containsExactLabels(new String[]{"option"})) {
			String opt = parsedOptions.getValue("option");
			if (opt.equals("sum")) {
				System.out.println("Query Sum");
				List<NumberTypeSum> result = numberTypeRepository.queryDeviceSum();
				if (result.size() > 0) {
					System.out.println("Result: " + result.get(0));
				}
				System.out.println("Size of Result: " + result.size());
				for (NumberTypeSum element : result) {
					response += element.getDevice() + " ";
					response += element.getSum() + "\n";
				}
			} else if (opt.equals("avg")) {
				System.out.println("Query Avg");
				List<NumberTypeAvg> result = numberTypeRepository.queryDeviceAvg();
				if (result.size() > 0) {
					System.out.println("Result: " + result.get(0));
				}
				System.out.println("Size of Result: " + result.size());
				for (NumberTypeAvg element : result) {
					response += element.getDevice() + " ";
					response += element.getAvg() + "\n";
				}
			}
		} else {
			System.out.println("Query Device");
			List<NumberType> result = numberTypeRepository.queryDevice();
			if (result.size() > 0) {
				System.out.println("Result: " + result.get(0));
			}
			System.out.println("Size of Result: " + result.size());
			for (NumberType element : result) {
				response += element.getDevice() + " ";
				response += element.getNumberValue() + " ";
				response += element.getDateTime() + "\n";
			}
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

}
