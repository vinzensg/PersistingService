package ch.ethz.inf.vs.persistingservice.resources.persisting.history.time;

import java.util.List;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;

public class NewestResource extends AbstractTimeResource {

	private String value;
	private String date;
	private String device;
	
	/**
	 * Instantiates a new newest resource and makes it observable.
	 * 
	 * @param resourceIdentifier
	 *            the resource identifier
	 */
	public NewestResource(String resourceIdentifier, String device) {
		super(resourceIdentifier);
		isObservable(true);
		
		this.device = device;
		
		value = "EMPTY";
		date = "EMPTY";
	}
	
	/**
	 * perform GET responds with the newest value stored in the database.
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET NEWEST: get request for device " + device);
		request.prettyPrint();
		
		acceptGetRequest(request, new NewestQuery());
	}
	
	public void acceptGetRequest(GETRequest request, AbstractQuery query) {
		List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
		OptionParser parsedOptions = new OptionParser(options);
		
		String ret = "";
		ret += query.perform(parsedOptions, AbstractQuery.NEWEST);
		
		request.respond(CodeRegistry.RESP_CONTENT, ret);
		
		System.out.println("GETRequst NEWEST: (value: " + ret + ") for device " + device);
	}
	
	private class NewestQuery extends AbstractQuery {
		
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {
			String ret = "";
			
			boolean withDate = false;
			if (parsedOptions.containsLabel("withdate"))
				withDate = parsedOptions.getBooleanValue("withdate");
			if (withDate) {
				ret += value + ";" + date;
			} else {
				ret += value;
			}
			return ret;
		}
	}
	
	/**
	 * Notify changed.
	 * 
	 * @param value
	 *            the newest value
	 */
	public void notifyChanged(String value, String date) {
		if (!this.value.equals(value)) {
			this.value = value;
			this.date = date;
			changed();
		}
	}
	
}
