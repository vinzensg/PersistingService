package ch.ethz.inf.vs.persistingservice.resources.persisting.history.time;

import java.util.List;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.persistingservice.database.DatabaseRepository;
import ch.ethz.inf.vs.persistingservice.database.documents.*;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.AvgResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.MaxResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.MinResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.SumResource;

public class AllResource<T extends Comparable> extends AbstractTimeResource {
	
	private SumResource<T> sumResource;
	private AvgResource<T> avgResource;
	private MaxResource<T> maxResource;
	private MinResource<T> minResource;
	
	private String type;
	private DatabaseRepository<T> typeRepository;	
	private String device;
	private boolean withSubResources;
	
	/**
	 * Instantiates a new all resource and makes it observable. All
	 * subresources are added.
	 * 
	 * @param resourceIdentifier
	 *            the resource identifier
	 */
	public AllResource(String resourceIdentifier, String type, DatabaseRepository<T> typeRepository, String device, boolean withSubResources) {
		super(resourceIdentifier);
		isObservable(true);
		
		this.type = type;
		this.typeRepository = typeRepository;
		this.device = device;
		this.withSubResources = withSubResources;
		
		if (withSubResources) {
			addSubResource((sumResource = new SumResource<T>("sum", type, typeRepository, device, this)));
			addSubResource((avgResource = new AvgResource<T>("avg", type, typeRepository, device, this)));
			addSubResource((maxResource = new MaxResource<T>("max", type, typeRepository, device, this)));
			addSubResource((minResource = new MinResource<T>("min", type, typeRepository, device, this)));
		}
	}
	
	/**
	 * perform GET queries the database for all documents of this device and
	 * responds with their values.
	 */
	public void performGET(GETRequest request) {		
		System.out.println("GET ALL: get request for device " + device);
		request.prettyPrint();

		acceptGetRequest(request, new AllQuery());
	}
	
	public void acceptGetRequest(GETRequest request, AbstractQuery query) {
		List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
		OptionParser parsedOptions = new OptionParser(options);
		
		String ret = "";
		ret += query.perform(parsedOptions, AbstractQuery.ALL);
		
		System.out.println("GETRequst ALL: (value: " + ret.substring(0, Math.min(50, ret.length())) + ") for device " + device);
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
	
	private class AllQuery extends AbstractQuery {
		
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {
			String ret = "";
			List<Default> res = typeRepository.queryDevice(type);
			
			boolean withDate = false;
			if (parsedOptions.containsLabel("withdate"))
				withDate = parsedOptions.getBooleanValue("withdate");
			if (withDate) {
				for (Default nt : res) {
					ret += nt.getNumberValue() + ";" + nt.getDateTime() + "\n";
				}
			} else {
				for (Default nt : res) {
					ret += nt.getNumberValue() + "\n";
				}
			}
			return ret;
		}
	}
	
	/**
	 * Notify changed and pass the notification to the subresources.
	 * 
	 * @param value
	 *            the new value
	 */
	public void notifyChanged(String value) {
		changed();
		
		if (withSubResources) {
			sumResource.notifyChanged(value);
			avgResource.notifyChanged(value);
			maxResource.notifyChanged(value);
			minResource.notifyChanged(value);
		}
	}
}
