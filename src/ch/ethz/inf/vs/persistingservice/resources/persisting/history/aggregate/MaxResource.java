package ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.persistingservice.database.DatabaseRepository;
import ch.ethz.inf.vs.persistingservice.database.documents.DateMax;
import ch.ethz.inf.vs.persistingservice.database.documents.Default;
import ch.ethz.inf.vs.persistingservice.database.documents.Max;
import ch.ethz.inf.vs.persistingservice.database.documents.Sum;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.time.AbstractQuery;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.time.AbstractTimeResource;

public class MaxResource<T extends Comparable> extends LocalResource {
	
	private String type;
	private DatabaseRepository<T> typeRepository;
	private String device;
	private AbstractTimeResource abstractTimeResource;
	
	/** The max. */
	private String max;

	/**
	 * Instantiates a new max resource.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param maxResourceType the max resource type
	 */
	public MaxResource(String resourceIdentifier, String type, DatabaseRepository<T> typeRepository, String device, AbstractTimeResource abstractTimeResource) {
		super(resourceIdentifier);
		isObservable(true);
		
		this.type = type;
		this.typeRepository = typeRepository;
		this.device = device;
		this.abstractTimeResource = abstractTimeResource;
		
		max = ""+Float.MIN_VALUE;
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.endpoint.LocalResource#performGET(ch.ethz.inf.vs.californium.coap.GETRequest)
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET MAX: get request for device " + device);
		request.prettyPrint();

		abstractTimeResource.acceptGetRequest(request, new MaxQuery());
	}
	
	private class MaxQuery extends AbstractQuery {
		
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {
			String ret = "";
			switch(timeResID) {
				case NEWEST:
				case ALL:
					List<Max> resMax = typeRepository.queryDeviceMax(type);
					if (!resMax.isEmpty())
						ret = "" + resMax.get(0).getMax();
					break;
				case SINCE:
					List<DateMax> resSinceMax = typeRepository.queryDeviceSinceMax(params[0], type);
					if (!resSinceMax.isEmpty())
						ret = "" + resSinceMax.get(0).getMax();
					break;
				case ONDAY:
				case TIMERANGE:
					List<DateMax> resDateMax = typeRepository.queryDeviceRangeMax(params[0], params[1], type);
					if (!resDateMax.isEmpty())
						ret = "" + resDateMax.get(0).getMax();
					break;
				case LAST:
					List<Default> resLimit = typeRepository.queryDeviceLimit(Integer.parseInt(params[0]), type);
					float max = Float.MIN_VALUE;
					if (!resLimit.isEmpty()) {
						float tmp = 0;
						for (Default nt : resLimit) {
							tmp = nt.getNumberValue();
							if (tmp < max)
								max = tmp;
						}
					}
					ret += ""+max;
					break;
				default:
					ret += "UNKOWN TIMERESID";
			}

			System.out.println("GETRequst Max: (value: " + ret + ") for device " + device);
			return ret;
		}
	}
	
	/**
	 * Notify changed.
	 *
	 * @param value the value
	 */
	public void notifyChanged(String value) {
		if (value.compareTo(max) > 0)
			changed();
	}

}
