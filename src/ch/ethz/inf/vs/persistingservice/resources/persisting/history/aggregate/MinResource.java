package ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.persistingservice.database.DatabaseRepository;
import ch.ethz.inf.vs.persistingservice.database.documents.DateMin;
import ch.ethz.inf.vs.persistingservice.database.documents.Default;
import ch.ethz.inf.vs.persistingservice.database.documents.Min;
import ch.ethz.inf.vs.persistingservice.database.documents.Sum;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.time.AbstractQuery;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.time.AbstractTimeResource;

public class MinResource<T extends Comparable> extends LocalResource {
	
	private String type;
	private DatabaseRepository<T> typeRepository;
	private String device;
	private AbstractTimeResource abstractTimeResource;
	
	/** The min. */
	private String min;

	/**
	 * Instantiates a new min resource.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param minResourceType the min resource type
	 */
	public MinResource(String resourceIdentifier, String type, DatabaseRepository<T> typeRepository, String device, AbstractTimeResource abstractTimeResource) {
		super(resourceIdentifier);
		isObservable(true);
		
		this.type = type;
		this.typeRepository = typeRepository;
		this.device = device;
		this.abstractTimeResource = abstractTimeResource;
		
		min = ""+Float.MAX_VALUE;
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.endpoint.LocalResource#performGET(ch.ethz.inf.vs.californium.coap.GETRequest)
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET MIN: get request for device " + device);
		request.prettyPrint();
		
		abstractTimeResource.acceptGetRequest(request, new MinQuery());
	}
	
	private class MinQuery extends AbstractQuery {
		
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {		
			String ret = "";
			switch(timeResID) {
				case NEWEST:
				case ALL:
					List<Min> resSum = typeRepository.queryDeviceMin(type);
					if (!resSum.isEmpty())
						ret = "" + resSum.get(0).getMin();
					break;
				case SINCE:
					List<DateMin> resSinceMin = typeRepository.queryDeviceSinceMin(params[0], type);
					if (!resSinceMin.isEmpty())
						ret = "" + resSinceMin.get(0).getMin();
					break;
				case ONDAY:
				case TIMERANGE:
					List<DateMin> resDateMin = typeRepository.queryDeviceRangeMin(params[0], params[1], type);
					if (!resDateMin.isEmpty())
						ret = "" + resDateMin.get(0).getMin();
					break;
				case LAST:
					List<Default> resLimit = typeRepository.queryDeviceLimit(Integer.parseInt(params[0]), type);
					float min = Float.MAX_VALUE;
					if (!resLimit.isEmpty()) {
						float tmp = 0;
						for (Default nt : resLimit) {
							tmp = nt.getNumberValue();
							if (tmp < min)
								min = tmp;
						}
					}
					ret += ""+min;
					System.out.println("GETRequst MIN: (value: " + ret + ") for device " + device);
					return ret;
				default:
					ret = "UNKNOWN TIMERESID";
			}
			
			System.out.println("GETRequst MIN: (value: " + ret + ") for device " + device);
			return ret;
		}
	}
	
	/**
	 * Notify changed.
	 *
	 * @param value the value
	 */
	public void notifyChanged(String value) {
		if (value.compareTo(min) < 0)
			changed();
	}

}
