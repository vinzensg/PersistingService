package ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElementRef.DEFAULT;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.persistingservice.database.DatabaseRepository;
import ch.ethz.inf.vs.persistingservice.database.documents.Avg;
import ch.ethz.inf.vs.persistingservice.database.documents.DateAvg;
import ch.ethz.inf.vs.persistingservice.database.documents.Default;
import ch.ethz.inf.vs.persistingservice.database.documents.Sum;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.time.AbstractQuery;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.time.AbstractTimeResource;

public class AvgResource<T extends Comparable> extends LocalResource {
	
	private String type;
	private DatabaseRepository<T> typeRepository;
	private String device;
	private AbstractTimeResource abstractTimeResource;
	
	/** The avg. */
	private String avg;

	/**
	 * Instantiates a new avg resource.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param avgResourceType the avg resource type
	 */
	public AvgResource(String resourceIdentifier, String type, DatabaseRepository<T> typeRepository, String device, AbstractTimeResource abstractTimeResource) {
		super(resourceIdentifier);
		isObservable(true);
		
		this.type = type;
		this.typeRepository = typeRepository;
		this.device = device;
		this.abstractTimeResource = abstractTimeResource;
		
		avg = "0";
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.endpoint.LocalResource#performGET(ch.ethz.inf.vs.californium.coap.GETRequest)
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET INTEGER ALL AVG: get request for device " + device);
		request.prettyPrint();

		abstractTimeResource.acceptGetRequest(request, new AvgQuery());
	}
	
	private class AvgQuery extends AbstractQuery {
		
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {
			String ret = "";
			List<Avg> res = null;
			switch(timeResID) {
				case NEWEST:
				case ALL:
					List<Avg> resAvg = typeRepository.queryDeviceAvg(type);
					if (!resAvg.isEmpty())
						ret = "" + resAvg.get(0).getAvg();
					break;
				case SINCE:
					List<DateAvg> resSinceAvg = typeRepository.queryDeviceSinceAvg(params[0], type);
					if (!resSinceAvg.isEmpty())
						ret = "" + resSinceAvg.get(0).getAvg();
					break;
				case ONDAY:
				case TIMERANGE:
					List<DateAvg> resDateAvg = typeRepository.queryDeviceRangeAvg(params[0], params[1], type);
					if (!resDateAvg.isEmpty())
						ret = "" + resDateAvg.get(0).getAvg();
					break;
				case LAST:
					List<Default> resLimit = typeRepository.queryDeviceLimit(Integer.parseInt(params[0]), type);
					float sum = 0;
					if (!resLimit.isEmpty()) {
						for (Default nt : resLimit) {
							sum += nt.getNumberValue();
						}
					}
					ret += ""+(sum/resLimit.size());
					break;
				default:
					ret = "UNKNOWN TIMERESID";
			}
			
			System.out.println("GETRequst SUM: (value: " + ret + ") for device " + device);
			
			return ret;
		}
	}
	
	/**
	 * Notify changed.
	 *
	 * @param value the value
	 */
	public void notifyChanged(String value) {
		if (!value.equals(avg))
			changed();
	}
	
}
