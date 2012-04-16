package resources.number;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import config.Constants;

import parser.OptionParser;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import database.DatabaseConnection;
import database.type.number.NumberType;
import database.type.number.NumberTypeAvg;
import database.type.number.NumberTypeRepository;
import database.type.number.NumberTypeSum;

public class SpecificNumberResource extends LocalResource {
	
	private String resourceIdentifier;
	
	private NumberTypeRepository numberTypeRepository ;
	
	private Timer timer;

	public SpecificNumberResource(String resourceIdentifier, String deviceURI, boolean push, String pushtarget, int datainterval) {
		super(resourceIdentifier);
		setTitle("Resource to sign up for observing a Number value");
		setResourceType("numbertype");
		this.resourceIdentifier = resourceIdentifier;
		numberTypeRepository = new NumberTypeRepository(NumberType.class, DatabaseConnection.getCouchDbConnector(), deviceURI);
		
		switch(evalMode(push, datainterval)) {
			case Constants.PUSH_PUSH:
				System.out.println("PUSH_PUSH");
				// register for push on device
				// store in history and push to pushtarget
				break;
			case Constants.POLLING_PUSH:
				System.out.println("POLLING_PUSH");
				timer = new Timer();
				timer.schedule(new NumberPollingPushTask(deviceURI, numberTypeRepository, pushtarget), 0, datainterval);
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
				timer.schedule(new NumberPollingStoreTask(deviceURI, numberTypeRepository), 0, datainterval);
				break;
			default:
				
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
