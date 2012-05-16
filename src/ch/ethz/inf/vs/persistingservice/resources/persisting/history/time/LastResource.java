package ch.ethz.inf.vs.persistingservice.resources.persisting.history.time;

import java.util.List;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.persistingservice.config.Constants;
import ch.ethz.inf.vs.persistingservice.database.DatabaseRepository;
import ch.ethz.inf.vs.persistingservice.database.documents.Default;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.AvgResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.MaxResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.MinResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.SumResource;

public class LastResource<T extends Comparable> extends AbstractTimeResource {

	private SumResource<T> sumResource;
	private AvgResource<T> avgResource;		
	private MaxResource<T> maxResource;
	private MinResource<T> minResource;
	
	private String type;
	private DatabaseRepository<T> typeRepository;
	private String device;
	private boolean withSubResources;
	
	/**
	 * Instantiates a new last resource and makes it observable. All
	 * subresources are added.
	 * 
	 * @param resourceIdentifier
	 *            the resource identifier
	 */
	public LastResource(String resourceIdentifier, String type, DatabaseRepository<T> typeRepository, String device, boolean withSubResources) {
		super(resourceIdentifier);
		
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
	 * perform GET queries the database for the last documents of this device and
	 * responds with their values.
	 * <p>
	 * Payload:<br>
	 * limit = <1 - 1000>
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET LAST: get request for device " + device);
		request.prettyPrint();

		
	}
	
	public void acceptGetRequest(GETRequest request, AbstractQuery query) {
		List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
		OptionParser parsedOptions = new OptionParser(options);
		
		String ret = "";

		if (parsedOptions.containsLabel("limit")) {
			int limit = parsedOptions.getIntValue("limit");
			if (limit <= Constants.MAX_LIMIT || limit > 0) {

				ret += query.perform(parsedOptions, AbstractQuery.LAST, ""+limit);
				
				System.out.println("GETRequst LAST: (value: " + ret.substring(0, Math.min(50, ret.length())) + ") for device " + device);
				request.respond(CodeRegistry.RESP_CONTENT, ret);
			} else {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														       "limit = <1 - " + Constants.MAX_LIMIT + ">");
			}
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "limit = <1 - " + Constants.MAX_LIMIT + ">");
		}
	}
	
	private class LastQuery extends AbstractQuery {

		public String perform(OptionParser parsedOptions, int timeResID, String... params) {
			String ret = "";
			List<Default> resLimit = typeRepository.queryDeviceLimit(Integer.valueOf(params[0]), type);
			
			boolean withDate = false;
			if (parsedOptions.containsLabel("withdate"))
				withDate = parsedOptions.getBooleanValue("withdate");
			if (withDate) {
				for (Default nt : resLimit) {
					ret += nt.getNumberValue() + ";" + nt.getDateTime() + "\n";
				}
			} else {
				for (Default nt : resLimit) {
					ret += nt.getNumberValue() + "\n";
				}
			}
			return ret;
		}
		
	}
	
	/**
	 * Notify changed and ass the notification to the subresources.
	 * 
	 * @param value
	 *            the newest value
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
