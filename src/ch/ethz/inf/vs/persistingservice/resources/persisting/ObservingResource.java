package ch.ethz.inf.vs.persistingservice.resources.persisting;

import java.io.IOException;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;

public class ObservingResource extends LocalResource {

	private boolean observing;
	private String device;
	private boolean set;
	
	public ObservingResource(String resourceIdentifier, boolean observing, String device, List<Option> options) {
		super(resourceIdentifier);
		this.observing = observing;
		this.device = device;
		this.set = false;
		
		Request request = new GETRequest();
		request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
		if (options != null)
			request.setOptions(OptionNumberRegistry.URI_QUERY, options);
		request.registerResponseHandler(new CheckObservableHandler());
		request.setURI(device);		
		
		try {
			request.prettyPrint();
			request.execute();
		} catch (IOException e) {
			System.err.println("Exception: " + e.getMessage());
		}
	}
	
	public boolean isObserving() {
		return observing;
	}

	public boolean isSet() {
		return set;
	}

	public void performGET(GETRequest request) {
		request.respond(CodeRegistry.RESP_CONTENT, Boolean.toString(observing));
	}
	
	/**
	 * The Class CheckObservableHandler handles the response trying to register
	 * as observer on the device.
	 * <p>
	 * If the target device resource is observable this specific type resource
	 * registers as observer and receives push notification when the data has
	 * changed.<br>
	 * Otherwise this specific type resource starts a polling task to
	 * periodically fetch the data from the device.
	 */
	public class CheckObservableHandler implements ResponseHandler {

		/**
		 * The response comes from the get request to check, if the target
		 * device is observable.
		 * <p>
		 * If the response has the observable option, the resource is observable.<br>
		 * Otherwise this specific type resource will use a polling task to
		 * periodically fetch the data from the device.
		 */
		@Override
		public void handleResponse(Response response) {
			System.out.println("OBSERVABLE CHECK: checking for observable on device " + device);
						
			if (response.hasOption(OptionNumberRegistry.OBSERVE)) {
				observing = true;
			} else {
				observing = false;
			}
			set = true;
			response.getRequest().unregisterResponseHandler(this);
		}
	}
	
}
