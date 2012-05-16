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

public class SinceResource<T extends Comparable> extends AbstractTimeResource {
	
	
	private SumResource<T> sumResource;
	private AvgResource<T> avgResource;
	private MaxResource<T> maxResource;
	private MinResource<T> minResource;
	
	private String date;
	
	private String type;
	private DatabaseRepository<T> typeRepository;
	private String device;
	private boolean withSubResources;
	
	
	/**
	 * Instantiates a new since resource and makes it observable. All
	 * subresources are added.
	 * 
	 * @param resourceIdentifier
	 *            the resource identifier
	 */
	public SinceResource(String resourceIdentifier, String type, DatabaseRepository<T> typeRepository, String device, boolean withSubResources) {
		super(resourceIdentifier);
		isObservable(true);

		this.type = type;
		this.typeRepository = typeRepository;
		this.device = device;
		this.withSubResources = withSubResources;
					
		this.date = "EMPTY";
		
		if (withSubResources) {
			addSubResource((sumResource = new SumResource<T>("sum", type, typeRepository, device, this)));
			addSubResource((avgResource = new AvgResource<T>("avg", type, typeRepository, device, this)));
			addSubResource((maxResource = new MaxResource<T>("max", type, typeRepository, device, this)));
			addSubResource((minResource = new MinResource<T>("min", type, typeRepository, device, this)));
		}
	}
	
	/**
	 * perform GET queries the database for all documents since some date of
	 * this device and responds with their values.
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET SINCE: get request for device " + device);
		request.prettyPrint();
		
		acceptGetRequest(request, new SinceQuery());
	}
	
	public void acceptGetRequest(GETRequest request, AbstractQuery query) {
		List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
		OptionParser parsedOptions = new OptionParser(options);
		
		String ret = "";

		if (parsedOptions.containsLabel("date")) {
			String date = parsedOptions.getStringValue("date");
			
			ret += query.perform(parsedOptions, AbstractQuery.SINCE, date);
			
			request.respond(CodeRegistry.RESP_CONTENT, ret);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "date = " + Constants.DATE_FORMAT);
		}	
	}
	
	class SinceQuery extends AbstractQuery {

		@Override
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {
			String ret = "";
			List<Default> res = typeRepository.queryDeviceSince(params[0], type);
			
			boolean withDate = false;
			if (parsedOptions.containsLabel("withdate"))
				withDate = parsedOptions.getBooleanValue("withdate");
			if (withDate) {
				for (Default nt : res) {
					ret += nt.getValue() + ";" + nt.getDateTime() + "\n";
				}
			} else {
				for (Default nt : res) {
					ret += nt.getValue() + "\n";
				}
			}
			
			System.out.println("GETRequst SINCE: (value: " + ret.substring(0, Math.min(50, ret.length())) + ") for device " + device);
			return ret;
		}
		
	}
	
	/**
	 * Notify changed and pass the notification to the subresources.
	 * 
	 * @param date
	 *            the current date
	 * @param value
	 *            the newest value
	 */
	public void notifyChanged(String value, String date) {
		if (this.date.compareTo(date) < 0) {
			changed();
			
			if (withSubResources) {
				sumResource.notifyChanged(value);
				avgResource.notifyChanged(value);
				maxResource.notifyChanged(value);
				minResource.notifyChanged(value);
			}
		}
	}
	
}
