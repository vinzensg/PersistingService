package ch.ethz.inf.vs.persistingservice.resources;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.MediaTypeRegistry;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.persistingservice.config.Constants;
import ch.ethz.inf.vs.persistingservice.database.DatabaseConnection;
import ch.ethz.inf.vs.persistingservice.database.NumberType;
import ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository;
import ch.ethz.inf.vs.persistingservice.parser.PayloadParser;

public class SpecificNumberResource extends LocalResource {
	
	private AllResource allResource;
	private NewestResource newestResource;
	private SumResource sumResource;
	private AvgResource avgResource;
	private MaxResource maxResource;
	private MinResource minResource;
	private SinceResource sinceResource;
	private OnDayResource ondayResource;
	private TimeRangeResource timerangeResource;
	
	private String resourceIdentifier;
	
	private NumberTypeRepository numberTypeRepository ;
	
	private String deviceROOT;
	private String deviceRES;
	private String device;
	
	private Timer timer;
	
	public SpecificNumberResource(String resourceIdentifier, String deviceROOT, String deviceRES) {
		super(resourceIdentifier);
		setTitle("Resource to sign up for observing a Number value");
		setResourceType("numbertype");
				
		this.resourceIdentifier = resourceIdentifier;
		this.deviceROOT = deviceROOT;
		this.deviceRES = deviceRES;
		this.device = deviceROOT + deviceRES;
		
		numberTypeRepository = new NumberTypeRepository(NumberType.Default.class, DatabaseConnection.getCouchDbConnector(), device);
		
		// check deviceURI for observable & register if possible
		Request request = new GETRequest();
		request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
		request.registerResponseHandler(new CheckObservableHandler());
		request.setURI(device);		
		
		try {
			request.prettyPrint();
			request.execute();
		} catch (IOException e) {
			System.err.println("Exception: " + e.getMessage());
		}		
		
		addSubResource((allResource = new AllResource("all")));
		addSubResource((newestResource = new NewestResource("newest")));
		addSubResource((sumResource = new SumResource("sum")));
		addSubResource((avgResource = new AvgResource("avg")));
		addSubResource((maxResource = new MaxResource("max")));
		addSubResource((minResource = new MinResource("min")));
		addSubResource((sinceResource = new SinceResource("since")));
		addSubResource((ondayResource = new OnDayResource("onday")));
		addSubResource((timerangeResource = new TimeRangeResource("timerange")));
	}
	
	public String getDevice() {
		return this.device;
	}
	
	private void notifyChanged(float value, String date) {
		allResource.notifyChanged();
		newestResource.notifyChanged(value);
		sumResource.notifyChanged(value);
		avgResource.notifyChanged(value);
		maxResource.notifyChanged(value);
		minResource.notifyChanged(value);
		sinceResource.notifyChanged(date);
		ondayResource.notifyChanged(date);
		timerangeResource.notifyChanged(date);
	}

	// Requests ///////////////////////////////////////////////////////////////
	
	public void performGET(GETRequest request) {		
		String ret = "Target Device: " + device;
		
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}

	public void performDELETE(DELETERequest request) {
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
		String responseString = "The Resource " + resourceIdentifier + " has been removed";
		Response response = new Response(CodeRegistry.RESP_CONTENT);
		response.setPayload(responseString);
		response.setContentType(MediaTypeRegistry.TEXT_PLAIN);
		request.respond(response);
		this.remove();
	}
		
	// Handler/ ///////////////////////////////////////////////////////////////
	
	public class CheckObservableHandler implements ResponseHandler {

		@Override
		public void handleResponse(Response response) {
			System.out.println("OBSERVABLE CHECK: checking for observable on device " + device);
			
			String payload = response.getPayloadString();
			System.out.println("PAYLOAD: " + payload);
			// float value = Float.valueOf(payload);
			// newestResource.notifyChanged(value);
						
			if (response.hasOption(OptionNumberRegistry.OBSERVE)) {
				System.out.println("OBSERVING: device " + device + " is being observed.");
				Request request = new GETRequest();
				request.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
				request.registerResponseHandler(new ObservingHandler());
				request.setURI(device);
				
				try {
					request.execute();
				} catch (IOException e) {
					System.err.println("Exception: " + e.getMessage());
				}
			} else {
				System.out.println("POLLING: device " + device + " is being polled.");
				timer = new Timer();
				timer.schedule(new PollingTask(), 0, 5000);
			}
			
			response.getRequest().unregisterResponseHandler(this);
		}
	}
	
	public class ObservingHandler implements ResponseHandler {

		@Override
		public void handleResponse(Response response) {
			String payload = response.getPayloadString();
			System.out.println("OBSERVING: new data (value: " + payload + ") is being pushed to device " + device);
			
			// store in database
			NumberType.Default numberType = new NumberType.Default();
			numberType.setDevice(device);
			float value = Float.valueOf(payload);
			numberType.setNumberValue(value);
			DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
	        Date date = new Date();
			numberType.setDateTime(dateFormat.format(date));
			
			numberTypeRepository.add(numberType);
			System.out.println("DATABASE: data (value: " + payload + ") was stored for device " + device);
			
			notifyChanged(value, dateFormat.format(date));
			
			System.out.println("PUSH NOTIFICATION: ...");
		}
		
	}
	
	// Polling Task ///////////////////////////////////////////////////////////
	
	public class PollingTask extends TimerTask {

		@Override
		public void run() {
			// getRequest
			Request getRequest = new GETRequest();
			getRequest.setURI(device);
			getRequest.enableResponseQueue(true);
			
			try {
				getRequest.execute();
			} catch (IOException e) {
				System.err.println("Exception: " + e.getMessage());
			}
			// receive response
			String payload = null;
			
			try {
				Response response = getRequest.receiveResponse();
				payload = response.getPayloadString();
			} catch (InterruptedException e) {
				System.err.println("Exception: " + e.getMessage());
			}
			System.out.println("POLLING: new data (value: " + payload + ") is being pushed to device " + device);
			
			
			// store in database
			NumberType.Default numberType = new NumberType.Default();
			numberType.setDevice(device);
			float value = Float.valueOf(payload);
			numberType.setNumberValue(value);
			DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
	        Date date = new Date();
			numberType.setDateTime(dateFormat.format(date));
			
			numberTypeRepository.add(numberType);
			System.out.println("DATABASE: data (value: " + payload + ") was stored for device " + device);
			
			notifyChanged(value, dateFormat.format(date));
			
			System.out.println("PUSH NOTIFICATION: ...");
		}
	}
	
	
	// SubResources ///////////////////////////////////////////////////////////
	
	public class AllResource extends LocalResource {
		
		public AllResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
		}
		
		public void performGET(GETRequest request) {
			String ret = null;
			List<NumberType.Default> res = numberTypeRepository.queryDevice();
			for (NumberType.Default nt : res) {
				ret += nt.getNumberValue() + "\n";
			}
			
			request.respond(CodeRegistry.RESP_CONTENT, ret);
		}
		
		public void notifyChanged() {
			changed();
		}
	}
	
	public class NewestResource extends LocalResource {
		
		float value;
		
		public NewestResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
		}
		
		public void performGET(GETRequest request) {
			String ret = "" + value;
			request.respond(CodeRegistry.RESP_CONTENT, ret);
			
			System.out.println("GETRequst NEWEST: (value: " + ret + ") for device " + device);
		}
		
		public void notifyChanged(float value) {
			if (this.value != value) {
				this.value = value;
				changed();
			}
		}
	}
	
	public class SumResource extends LocalResource {
		
		public SumResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
		}
		
		public void performGET(GETRequest request) {
			String ret = null;
			List<NumberType.Sum> resSum = numberTypeRepository.queryDeviceSum();
			if (!resSum.isEmpty()) {
				ret = "" + resSum.get(0).getSum();
				System.out.println("GETRequst SUM: (value: " + ret + ") for device " + device);
			}
				
			request.respond(CodeRegistry.RESP_CONTENT, ret);
		}
		
		public void notifyChanged(float value) {
			if (value != 0)
				changed();
		}
	}
	
	public class AvgResource extends LocalResource {
		
		float avg;

		public AvgResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			avg = 0;
		}
		
		public void performGET(GETRequest request) {
			String ret = null;
			List<NumberType.Avg> resAvg = numberTypeRepository.queryDeviceAvg();
			if (!resAvg.isEmpty()) {
				ret = "" + resAvg.get(0).getAvg();
				avg = Float.valueOf(ret);
				System.out.println("GETRequst AVG: (value: " + ret + ") for device " + device);
			}
			
			request.respond(CodeRegistry.RESP_CONTENT, ret);
		}
		
		public void notifyChanged(float value) {
			if (avg != value)
				changed();
		}
	}
	
	public class MaxResource extends LocalResource {
		
		float max;

		public MaxResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			max = 0;
		}
		
		public void performGET(GETRequest request) {
			String ret = null;
			List<NumberType.Max> resSum = numberTypeRepository.queryDeviceMax();
			if (!resSum.isEmpty()) {
				ret = "" + resSum.get(0).getMax();
				System.out.println("RET: " + ret);
				max = Float.valueOf(ret);
				Integer.valueOf(ret);
				System.out.println("MAX: " + max);
				System.out.println("GETRequst MAX: (value: " + ret + ") for device " + device);
			}
			
			request.respond(CodeRegistry.RESP_CONTENT, ret);
		}
		
		public void notifyChanged(float value) {
			if (value > max)
				changed();
		}
	}
	
	public class MinResource extends LocalResource {
		
		float min;

		public MinResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			min = 0;
		}
		
		public void performGET(GETRequest request) {
			String ret = null;
			List<NumberType.Min> resSum = numberTypeRepository.queryDeviceMin();
			if (!resSum.isEmpty()) {
				ret = "" + resSum.get(0).getMin();
				min = Float.valueOf(ret);
				System.out.println("GETRequst MIN: (value: " + ret + ") for device " + device);
			}
			
			request.respond(CodeRegistry.RESP_CONTENT, ret);
		}
		
		public void notifyChanged(float value) {
			if (value < min)
				changed();
		}
	}

	public class SinceResource extends LocalResource {
		
		String date;
		
		public SinceResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			date = "";
		}
		
		public void performGET(GETRequest request) {
			String ret = null;
			String payload = request.getPayloadString();
			PayloadParser parsedPayload = new PayloadParser(payload);
			if (parsedPayload.containsLabel("date")) {
				String date = parsedPayload.getStringValue("date");
				List<NumberType.Default> resSince = numberTypeRepository.queryDeviceSince(date);
				for (NumberType.Default nt : resSince) {
					ret += nt.getNumberValue() + "\n";
				}
				
				System.out.println("GETRequst SINCE: (value: " + ret.substring(0, Math.max(50, ret.length())) + ") for device " + device);
				request.respond(CodeRegistry.RESP_CONTENT, ret);
			} else {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Support:\n" +
															   "date = " + Constants.DATE_FORMAT);
			}	
		}
		
		public void notifyChanged(String date) {
			if (this.date.compareTo(date) < 0)
				changed();
		}
	}
	
	public class OnDayResource extends LocalResource {
		
		String date;
		
		public OnDayResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			date = "";
		}
		
		public void performGET(GETRequest request) {
			String ret = null;
			String payload = request.getPayloadString();
			PayloadParser parsedPayload = new PayloadParser(payload);
			if (parsedPayload.containsLabel("date")) {
				this.date = parsedPayload.getStringValue("date");
				String startOnDay = this.date + "-00:00:00";
				String endOnDay = this.date + "-23:59:59";
				List<NumberType.Default> resOnDay = numberTypeRepository.queryDeviceRange(startOnDay, endOnDay);
				for (NumberType.Default nt : resOnDay) {
					ret += nt.getNumberValue() + "\n";
				}
				
				System.out.println("GETRequst ONDAY: (value: " + ret.substring(0, Math.max(50, ret.length())) + ") for device " + device);
				request.respond(CodeRegistry.RESP_CONTENT, ret);
			} else {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Support:\n" +
															   "date = " + Constants.DATE_FORMAT_DAY);
			}
		}
		
		public void notifyChanged(String date) {
			if (date.equals(this.date))
				changed();
		}
		
	}
	
	public class TimeRangeResource extends LocalResource {
		
		String startDate;
		String endDate;
		
		public TimeRangeResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			startDate = "";
			endDate = "";
		}
		
		public void performGET(GETRequest request) {
			String ret = null;
			String payload = request.getPayloadString();
			PayloadParser parsedPayload = new PayloadParser(payload);
			if (parsedPayload.containsLabel("startdate") && parsedPayload.containsLabel("enddate")) {
				this.startDate = parsedPayload.getStringValue("startdate");
				this.endDate = parsedPayload.getStringValue("enddate");
				List<NumberType.Default> resTimeRange = numberTypeRepository.queryDeviceRange(this.startDate, this.endDate);
				for (NumberType.Default nt : resTimeRange) {
					ret += nt.getNumberValue() + "\n";
				}
				
				System.out.println("GETRequst TIMERANGE: (value: " + ret.substring(0, Math.max(50, ret.length())) + ") for device " + device);
				request.respond(CodeRegistry.RESP_CONTENT, ret);
			} else {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Support:\n" +
															   "startdate = " + Constants.DATE_FORMAT + "\n" +
															   "enddate = " + Constants.DATE_FORMAT);
			}
		}
		
		public void notifyChanged(String date) {
			if (startDate.compareTo(date) <= 0 && endDate.compareTo(date) >= 0)
				changed();
		}
	}
}
