package ch.ethz.inf.vs.persistingservice.resources.persisting;

import java.util.List;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.HistoryResource;

public class RunningResource extends LocalResource {

	private ObservingResource observingResource;
	private HistoryResource historyResource;
	
	private boolean running;
	private List<Option> options;

	public RunningResource(String resourceIdentifier, boolean running, List<Option> options) {
		super(resourceIdentifier);
		this.running = running;
		this.options = options;
	}
	
	public boolean isRunning() {
		return running;
	}
	
	public void setupReferences(ObservingResource observingResource, HistoryResource historyResource) {
		this.observingResource = observingResource;
		this.historyResource = historyResource;
	}
	
	
	
	public void performGET(GETRequest request) {
		request.respond(CodeRegistry.RESP_CONTENT, Boolean.toString(running));
	}
	
	public void performPUT(PUTRequest request) {
		String payload = request.getPayloadString();
		if (payload.equals("true")) {
			if (!running) {
				running = true;
				while(!observingResource.isSet()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						System.err.println("Exception: " + e.getMessage());
					}
				}
				historyResource.startHistory(observingResource.isObserving(), options);
			}
			request.respond(CodeRegistry.RESP_CHANGED);
		} else if (payload.equals("false")) {
			if (running) {
				running = false;
				while(!observingResource.isSet()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						System.err.println("Exception: " + e.getMessage());
					}
				}
				historyResource.stopHistory(observingResource.isObserving(), options);
			}
			request.respond(CodeRegistry.RESP_CHANGED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST);
		}
	}
}
