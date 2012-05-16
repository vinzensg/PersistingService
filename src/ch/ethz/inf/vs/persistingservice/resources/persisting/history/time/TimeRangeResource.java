package ch.ethz.inf.vs.persistingservice.resources.persisting.history.time;

import java.util.List;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.persistingservice.config.Constants;
import ch.ethz.inf.vs.persistingservice.database.DatabaseRepository;
import ch.ethz.inf.vs.persistingservice.database.documents.Default;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.AvgResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.MaxResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.MinResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.SumResource;

public class TimeRangeResource<T extends Comparable> extends AbstractTimeResource {

	private SumResource<T> sumResource;		
	private AvgResource<T> avgResource;		
	private MaxResource<T> maxResource;		
	private MinResource<T> minResource;				

	private String startDate;
	private String endDate;
	
	private String type;
	private DatabaseRepository<T> typeRepository;
	private String device;
	private boolean withSubResources;
	
	/**
	 * Instantiates a new time range resource and makes it observable. All
	 * subresources are added.
	 * 
	 * @param resourceIdentifier
	 *            the resource identifier
	 */
	public TimeRangeResource(String resourceIdentifier, String type, DatabaseRepository<T> typeRepository, String device, boolean withSubResources) {
		super(resourceIdentifier);
		isObservable(true);
		
		this.type = type;
		this.typeRepository = typeRepository;
		this.device = device;
		this.withSubResources = withSubResources;
		
		this.startDate = "EMPTY";
		this.endDate = "EMPTY";
		
		if (withSubResources) {
			addSubResource((sumResource = new SumResource<T>("sum", type, typeRepository, device, this)));
			addSubResource((avgResource = new AvgResource<T>("avg", type, typeRepository, device, this)));
			addSubResource((maxResource = new MaxResource<T>("max", type, typeRepository, device, this)));
			addSubResource((minResource = new MinResource<T>("min", type, typeRepository, device, this)));
		}
	}
	
	/**
	 * perform GET queries the database for all documents of this device
	 * between some time range and responds with their values.
	 * <p>
	 * Payload:<br>
	 * startdate = yyyy/MM/dd-HH:mm:ss<br>
	 * enddate = yyyy/MM/dd-HH:mm:ss
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET TIMERANGE: get request for device " + device);
		request.prettyPrint();

		
	}
	
	public void acceptGetRequest(GETRequest request, AbstractQuery query) {
		List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
		OptionParser parsedOptions = new OptionParser(options);
		
		String ret = "";

		if (parsedOptions.containsLabel("startdate") && parsedOptions.containsLabel("enddate")) {
			this.startDate = parsedOptions.getStringValue("startdate");
			this.endDate = parsedOptions.getStringValue("enddate");
			
			query.perform(parsedOptions, AbstractQuery.TIMERANGE, startDate, endDate);
			
			System.out.println("GETRequst TIMERANGE: (value: " + ret.substring(0, Math.min(50, ret.length())) + ") for device " + device);
			request.respond(CodeRegistry.RESP_CONTENT, ret);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "startdate = " + Constants.DATE_FORMAT + "\n" +
														   "enddate = " + Constants.DATE_FORMAT);
		}
	}
	
	private class TimeRangeQuery extends AbstractQuery {
		
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {
			String ret = "";
			List<Default> resTimeRange = typeRepository.queryDeviceRange(params[0], params[1], type);
			
			boolean withDate = false;
			if (parsedOptions.containsLabel("withdate"))
				withDate = parsedOptions.getBooleanValue("withdate");
			if (withDate) {
				for (Default nt : resTimeRange) {
					ret += nt.getNumberValue() + ";" + nt.getDateTime() + "\n";
				}
			} else {
				for (Default nt : resTimeRange) {
					ret += nt.getNumberValue() + "\n";
				}
			}
			return ret;
		}
	}
	
	/**
	 * Notify changed and pass the notification to the subresources.
	 *
	 * @param date the current date
	 * @param value the newest value
	 */
	public void notifyChanged(String date, String value) {
		if (startDate.compareTo(date) <= 0 && endDate.compareTo(date) >= 0) {
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
