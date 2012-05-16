package ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.persistingservice.database.DatabaseRepository;
import ch.ethz.inf.vs.persistingservice.database.documents.DateSum;
import ch.ethz.inf.vs.persistingservice.database.documents.Default;
import ch.ethz.inf.vs.persistingservice.database.documents.Sum;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.time.AbstractQuery;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.time.AbstractTimeResource;

public class SumResource<T extends Comparable> extends LocalResource {
	
	private String type;
	private DatabaseRepository<T> typeRepository;
	private String device;
	private AbstractTimeResource abstractTimeResource;
	
	/**
	 * Instantiates a new sum resource.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param sumResourceType the sum resource type
	 */
	public SumResource(String resourceIdentifier, String type, DatabaseRepository<T> typeRepository, String device, AbstractTimeResource abstractTimeResource) {
		super(resourceIdentifier);
		isObservable(true);
		
		this.type = type;
		this.typeRepository = typeRepository;
		this.device = device;
		this.abstractTimeResource = abstractTimeResource;
	}
	
	/* (non-Javadoc)
	 * @see ch.ethz.inf.vs.californium.endpoint.LocalResource#performGET(ch.ethz.inf.vs.californium.coap.GETRequest)
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET SUM: get request for device " + device);
		request.prettyPrint();
		
		abstractTimeResource.acceptGetRequest(request, new SumQuery());
	}
	
	private class SumQuery extends AbstractQuery {

		@Override
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {
			String ret = "";
			switch(timeResID) {
				case NEWEST:
				case ALL:
					List<Sum> resSum = typeRepository.queryDeviceSum(type);
					if (!resSum.isEmpty())
						ret = "" + resSum.get(0).getSum();
					break;
				case SINCE:
					List<DateSum> resSinceSum = typeRepository.queryDeviceSinceSum(params[0], type);
					if (!resSinceSum.isEmpty())
						ret = "" + resSinceSum.get(0).getSum();
					break;
				case ONDAY:
				case TIMERANGE:
					List<DateSum> resDateSum = typeRepository.queryDeviceRangeSum(params[0], params[1], type);
					if (!resDateSum.isEmpty())
						ret = "" + resDateSum.get(0).getSum();
					break;
				case LAST:
					List<Default> resLimit = typeRepository.queryDeviceLimit(Integer.parseInt(params[0]), type);
					float sum = 0;
					if (!resLimit.isEmpty()) {
						for (Default nt : resLimit) {
							sum += nt.getNumberValue();
						}
					}
					ret += ""+sum;
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
		if (!value.equals("0"))
			changed();
	}

}
