package ch.ethz.inf.vs.persistingservice.resources.persisting;

import java.io.IOException;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.PUTRequest;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.persistingservice.database.documents.DefaultStorage;
import ch.ethz.inf.vs.persistingservice.resources.PersistingServiceResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.AbstractValueSet;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.HistoryResource;

public class PersistingResource extends LocalResource {
	
	private PersistingServiceResource persistingResource;
	private String topResource;
	
	private TypeResource typeResource;
	private RunningResource runningResource;
	private ObservingResource observingResource;
	private HistoryResource historyResource;
	
	private List<Option> options;

	public PersistingResource(String resourceIdentifier, String type, String deviceROOT, String deviceRES, List<Option> options, PersistingServiceResource persistingResource, String topResource) {
		super(resourceIdentifier);
		this.options = options;
		this.persistingResource = persistingResource;
		this.topResource = topResource;
		
		addSubResource(typeResource = new TypeResource("type", type));
		addSubResource(runningResource = new RunningResource("running", false, options));
		addSubResource(observingResource = new ObservingResource("observing", false, deviceROOT + deviceRES, options));
		
		if (type.equals("number")) {
			class NumberValueSet extends AbstractValueSet {
				public void perform(DefaultStorage defaultStorage, String payload) {
					defaultStorage.setValue(Float.valueOf(payload));
				}
			}
			
			addSubResource(historyResource = new HistoryResource<Float>("history", type, deviceROOT, deviceRES, new NumberValueSet(), true));
		} else if (type.equals("string")) {
			class StringValueSet extends AbstractValueSet {
				public void perform(DefaultStorage defaultStorage, String payload) {
					defaultStorage.setValue(payload);
				}
			}
			
			addSubResource(historyResource = new HistoryResource<String>("histroy", type, deviceROOT, deviceRES, new StringValueSet(), false));
		}
		
		runningResource.setupReferences(observingResource, historyResource);
	}
	
	// Requests //////////////////////////////////////////////
	
	public void performDELETE(DELETERequest request) {
		if (runningResource.isRunning()) {
			historyResource.stopHistory(observingResource.isObservable(), options);
		}
		remove();
		persistingResource.cleanUp(topResource);
		request.respond(CodeRegistry.RESP_DELETED);
	}
}
