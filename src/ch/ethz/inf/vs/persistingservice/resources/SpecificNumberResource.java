/*******************************************************************************
 * Copyright (c) 2012, Institute for Pervasive Computing, ETH Zurich.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * This file is part of the Californium (Cf) CoAP framework.
 ******************************************************************************/
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

/**
 * The Class SpecificNumberResource defines a specific number resource, which
 * requests data from a specified device and stores it in the database.
 * <p>
 * If possible the specific number resource registers as observer on the device
 * resource, otherwise the specific number resource polls data periodically.
 * <p>
 * The data is stored in the following format:<br>
 * device = DEVICE_PATH<br>
 * numberValue = VALUE_OF_NUMBER<br>
 * dateTime = yyyy/MM/dd-HH/mm/ss
 * <p>
 * The subresources to retrieve data are:<br>
 * -	<b>newest</b>: returns only the value of the newest document stored for the target
 * device<br>
 * -	<b>all</b>: returns the values of all documents stored for the target device<br>
 * -	<b>all/sum</b>: returns the sum of all documents stored for the target device<br>
 * -	<b>all/avg</b>: returns the average of all documents stored for the target device<br>
 * -	<b>all/max</b>: returns the maximum of all documents stored for the target device<br>
 * -	<b>all/min</b>: returns the minimum of all documents stored for the target device<br>
 * -	<b>last</b>: returns the values of the last *limit* documents. <br>
 * -	<b>last/sum</b>: returns the sum of the last x documents stored for the target device<br>
 * -	<b>last/avg</b>: returns the average of the last x documents stored for the target device<br>
 * -	<b>last/max</b>: returns the maximum of the last x documents stored for the target device<br>
 * -	<b>last/min</b>: returns the minimum of the last x documents stored for the target device<br>
 * -	<b>since</b>: returns the values of all documents stored since *date* for the target
 * device<br>
 * -	<b>since/sum</b>: returns the sum of all documents stored since *date* for the target device<br>
 * -	<b>since/avg</b>: returns the average of all documents stored since *date* for the target device<br>
 * -	<b>since/max</b>: returns the maximum of all documents stored since *date* for the target device<br>
 * -	<b>since/min</b>: returns the minimum of all documents stored since *date* for the target device<br>
 * -	<b>onday</b>: returns the values of all documents stored on *date* for the target
 * device<br>
 * -	<b>onday/sum</b>: returns the sum of all documents stored on *date* for the target device<br>
 * -	<b>onday/avg</b>: returns the average of all documents stored on *date* for the target device<br>
 * -	<b>onday/max</b>: returns the maximum of all documents stored on *date* for the target device<br>
 * -	<b>onday/min</b>: returns the minimum of all documents stored on *date* for the target device<br>
 * -	<b>timerange</b>: returns the values of all documents stored between *startdate* and
 * *enddate* for the target device<br>
 * -	<b>timerange/sum</b>: returns the sum of all documents stored between *startdate* and *enddate* for the target device<br>
 * -	<b>timerange/avg</b>: returns the average of all documents stored between *startdate* and *enddate* for the target device<br>
 * -	<b>timerange/max</b>: returns the maximum of all documents stored between *startdate* and *enddate* for the target device<br>
 * -	<b>timerange/min</b>: returns the minimum of all documents stored between *startdate* and *enddate* for the target device<br>
 */
public class SpecificNumberResource extends LocalResource {
	
	private AllResource allResource;
	private NewestResource newestResource;
	private LastResource lastResource;
	private SumResourceType sumResourceType;
	private AvgResourceType avgResourceType;
	private MaxResourceType maxResourceType;
	private MinResourceType minResourceType;	
	
	private SinceResource sinceResource;
	private OnDayResource ondayResource;
	private TimeRangeResource timerangeResource;
	
	private String resourceIdentifier;
	
	private NumberTypeRepository numberTypeRepository ;

	private String deviceROOT;
	private String deviceRES;
	private String device;
	
	private Timer timer;
	
	/**
	 * Instantiates a new specific number resource for the device deviceROOT +
	 * deviceURI and adds the required subresources.
	 * <p>
	 * A private string type repository is created to connect to the database
	 * and store data or query data from it.
	 * <p>
	 * The specific number resource tries to register as observer on the device.
	 * Otherwise it starts a polling task to retrieve the data from the device
	 * periodically.
	 * 
	 * @param resourceIdentifier
	 *            the resource identifier
	 * @param deviceROOT
	 *            the device root
	 * @param deviceRES
	 *            the device res
	 */
	public SpecificNumberResource(String resourceIdentifier, String deviceROOT, String deviceRES) {
		super(resourceIdentifier);
		setTitle("Resource to sign up for observing a Number value");
		setResourceType("numbertype");
				
		this.resourceIdentifier = resourceIdentifier;
		this.deviceROOT = deviceROOT;
		this.deviceRES = deviceRES;
		this.device = deviceROOT + deviceRES;
		
		numberTypeRepository = new NumberTypeRepository(NumberType.Default.class, DatabaseConnection.getCouchDbConnector(), device);
		
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
		
		sumResourceType = new SumResourceType();
		avgResourceType = new AvgResourceType();
		maxResourceType = new MaxResourceType();
		minResourceType = new MinResourceType();
		
		addSubResource((allResource = new AllResource("all")));
		addSubResource((newestResource = new NewestResource("newest")));
		addSubResource((lastResource = new LastResource("last")));
		addSubResource((sinceResource = new SinceResource("since")));
		addSubResource((ondayResource = new OnDayResource("onday")));
		addSubResource((timerangeResource = new TimeRangeResource("timerange")));
	}
	
	/**
	 * Gets the device specified for this specific number resource.
	 *
	 * @return the device
	 */
	public String getDevice() {
		return this.device;
	}
	
	/**
	 * Notify subresources that new data was received from the device and the
	 * database has changed.
	 * 
	 * @param value
	 *            the new value retrieved from the device
	 * @param date
	 *            the current date
	 */
	private void notifyChanged(float value, String date) {
		allResource.notifyChanged(value);
		newestResource.notifyChanged(value);
		lastResource.notifyChanged(value);
		sinceResource.notifyChanged(date, value);
		ondayResource.notifyChanged(date, value);
		timerangeResource.notifyChanged(date, value);
	}

	// Requests ///////////////////////////////////////////////////////////////
	
	/**
	 * perform GET responds with the device specified for this specific number
	 * resource.
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET: get request for " + resourceIdentifier);
		request.prettyPrint();
		
		String ret = "device = " + device;
		
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}

	/**
	 * perform DELETE deletes this specific number resource and stops the polling task if necessary.
	 */
	public void performDELETE(DELETERequest request) {
		System.out.println("DELETE: delete resource " + resourceIdentifier);
		request.prettyPrint();
		
		if (timer != null) {
			timer.cancel();
			timer.purge();
		}
		String ret = "The Resource " + resourceIdentifier + " has been removed";
		request.respond(CodeRegistry.RESP_DELETED, ret);
		this.remove();
	}
		
	// Handler/ ///////////////////////////////////////////////////////////////
	
	/**
	 * The Class CheckObservableHandler handles the response trying to register
	 * as observer on the device.
	 * <p>
	 * If the target device resource is observable this specific number resource
	 * registers as observer and receives push notification when the data has
	 * changed.<br>
	 * Otherwise this specific number resource starts a polling task to
	 * periodically fetch the data from the device.
	 */
	public class CheckObservableHandler implements ResponseHandler {

		/**
		 * The response comes from the get request to check, if the target
		 * device is observable.
		 * <p>
		 * If the response has the observable option, the resource is observable
		 * and this specific number resource registers as observer and receives
		 * push notification when the data has changed.<br>
		 * Otherwise this specific string resource starts a polling task to
		 * periodically fetch the data from the device.
		 */
		@Override
		public void handleResponse(Response response) {
			System.out.println("OBSERVABLE CHECK: checking for observable on device " + device);

			float value = Float.valueOf(response.getPayloadString());
			newestResource.notifyChanged(value);
			lastResource.notifyChanged(value);
						
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
	
	/**
	 * The Class ObservingHandler handles the response when this specific number
	 * resource registered as observer and stores the data in the database.
	 */
	public class ObservingHandler implements ResponseHandler {

		/**
		 * Handles the response and stored the data in the database. Then
		 * notifies the subresources, that new data was received.
		 */
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
			System.out.println("PUSH NOTIFICATION: notify resources after new data was stored for device " + device);
		}
		
	}
	
	// Polling Task ///////////////////////////////////////////////////////////
	
	/**
	 * The Class PollingTask periodically retrieves data from the target device
	 * and stores it in the database.
	 */
	public class PollingTask extends TimerTask {

		/** The old value. */
		float oldValue;
		
		/**
		 * Instantiates a new polling task.
		 */
		public PollingTask() {
			oldValue = 0;
		}
		
		/**
		 * run performes a get request on the target device and reads the
		 * response value. If the value has changed, it stores it in the
		 * database and notifies the subresources.
		 */
		@Override
		public void run() {
			Request getRequest = new GETRequest();
			getRequest.setURI(device);
			getRequest.enableResponseQueue(true);
			
			try {
				getRequest.execute();
			} catch (IOException e) {
				System.err.println("Exception: " + e.getMessage());
			}

			String payload = null;
			
			try {
				Response response = getRequest.receiveResponse();
				payload = response.getPayloadString();
			} catch (InterruptedException e) {
				System.err.println("Exception: " + e.getMessage());
			}
			System.out.println("POLLING: data (value: " + payload + ") is being polled from device " + device);
			
			float value = Float.valueOf(payload);
			if (oldValue != value) {
				oldValue = value;
				
				NumberType.Default numberType = new NumberType.Default();
				numberType.setDevice(device);
				numberType.setNumberValue(value);
				DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
		        Date date = new Date();
				numberType.setDateTime(dateFormat.format(date));
				
				numberTypeRepository.add(numberType);
				System.out.println("DATABASE: data (value: " + payload + ") was stored for device " + device);
				
				notifyChanged(value, dateFormat.format(date));
				System.out.println("PUSH NOTIFICATION: notify resources after new data was stored for device " + device);
			}
		}
	}
	
	
	// SubResources ///////////////////////////////////////////////////////////
	
	/**
	 * The Class NewestResource implements a subresource to query the newest document
	 * stored in the database for this device.
	 * <p>
	 * NewestResource is observable for its clients.
	 */
	public class NewestResource extends LocalResource {
				
		float value;
		
		/**
		 * Instantiates a new newest resource and makes it observable.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public NewestResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			
			value = 0;
		}
		
		/**
		 * perform GET responds with the newest value stored in the database.
		 */
		public void performGET(GETRequest request) {
			System.out.println("GET INTEGER NEWEST: get request for device " + device);
			request.prettyPrint();
			
			String ret = "" + value;
			request.respond(CodeRegistry.RESP_CONTENT, ret);
			
			System.out.println("GETRequst NEWEST: (value: " + ret + ") for device " + device);
		}
		
		/**
		 * Notify changed.
		 * 
		 * @param value
		 *            the newest value
		 */
		public void notifyChanged(float value) {
			if (this.value != value) {
				this.value = value;
				changed();
			}
		}
	}
	
	/**
	 * The Class AllResource implements a subresource to query all documents
	 * stored in the database for this device.
	 * <p>
	 * AllResource is observable for its clients.
	 * <p>
	 * It has the subresources:<br>
	 * sum: returns the sum of all documents stored for the target device<br>
	 * avg: returns the average of all documents stored for the target device<br>
	 * max: returns the maximum of all documents stored for the target device<br>
	 * min: returns the minimum of all documents stored for the target device<br>
	 * all subresources are observable. 
	 */
	public class AllResource extends LocalResource {
				
		private SumResource sumResource;
		private AvgResource avgResource;
		private MaxResource maxResource;
		private MinResource minResource;
		
		/**
		 * Instantiates a new all resource and makes it observable. All
		 * subresources are added.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public AllResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
						
			addSubResource((sumResource = new SumResource("sum", sumResourceType.new Default())));
			addSubResource((avgResource = new AvgResource("avg", avgResourceType.new Default())));
			addSubResource((maxResource = new MaxResource("max", maxResourceType.new Default())));
			addSubResource((minResource = new MinResource("min", minResourceType.new Default())));
		}
		
		/**
		 * perform GET queries the database for all documents of this device and
		 * responds with their values.
		 */
		public void performGET(GETRequest request) {
			System.out.println("GET INTEGER ALL: get request for device " + device);
			request.prettyPrint();
			
			String ret = "";
			List<NumberType.Default> res = numberTypeRepository.queryDevice();
			for (NumberType.Default nt : res) {
				ret += nt.getNumberValue() + "\n";
			}
			
			request.respond(CodeRegistry.RESP_CONTENT, ret);
		}
		
		/**
		 * Notify changed and pass the notification to the subresources.
		 * 
		 * @param value
		 *            the new value
		 */
		public void notifyChanged(float value) {
			changed();
			
			sumResource.notifyChanged(value);
			avgResource.notifyChanged(value);
			maxResource.notifyChanged(value);
			minResource.notifyChanged(value);
		}
	}
	
	/**
	 * The Class LastResource implements a subresource to query the last documents
	 * stored in the database for this device.
	 * <p>
	 * LastResource is observable for its clients.
	 * <p>
	 * It has the subresources:<br>
	 * sum: returns the sum of the last x documents stored for the target device<br>
	 * avg: returns the average of the last x documents stored for the target device<br>
	 * max: returns the maximum of the last x documents stored for the target device<br>
	 * min: returns the minimum of the last x documents stored for the target device<br>
	 * all subresources are observable.
	 */
	public class LastResource extends LocalResource {
				
		private SumResource sumResource;
		private AvgResource avgResource;		
		private MaxResource maxResource;
		private MinResource minResource;
		
		/**
		 * Instantiates a new last resource and makes it observable. All
		 * subresources are added.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public LastResource(String resourceIdentifier) {
			super(resourceIdentifier);
			
			addSubResource((sumResource = new SumResource("sum", sumResourceType.new Last())));
			addSubResource((avgResource = new AvgResource("avg", avgResourceType.new Last())));
			addSubResource((maxResource = new MaxResource("max", maxResourceType.new Last())));
			addSubResource((minResource = new MinResource("min", minResourceType.new Last())));
		}
		
		/**
		 * perform GET queries the database for the last documents of this device and
		 * responds with their values.
		 * <p>
		 * Payload:<br>
		 * limit = <1 - 1000>
		 */
		public void performGET(GETRequest request) {
			System.out.println("GET INTEGER LAST: get request for device " + device);
			request.prettyPrint();
			
			String ret = "";
			String payload = request.getPayloadString();
			PayloadParser parsedPayload = new PayloadParser(payload);
			if (parsedPayload.containsLabel("limit")) {
				int limit = parsedPayload.getIntValue("limit");
				if (limit <= Constants.MAX_LIMIT || limit > 0) {
					List<NumberType.Default> resLimit = numberTypeRepository.queryDeviceLimit(limit);
					for (NumberType.Default nt : resLimit) {
						ret += nt.getNumberValue() + "\n";
					}
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
															       "last = <1 - " + Constants.MAX_LIMIT + ">");
				}
			} else {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
															   "last = <1 - " + Constants.MAX_LIMIT + ">");
			}
		}
		
		/**
		 * Notify changed and ass the notification to the subresources.
		 * 
		 * @param value
		 *            the newest value
		 */
		public void notifyChanged(float value) {
			changed();
			sumResource.notifyChanged(value);
			avgResource.notifyChanged(value);
			maxResource.notifyChanged(value);
			minResource.notifyChanged(value);
		}
	}

	/**
	 * The Class SinceResource implements a subresource to query all documents
	 * stored in the database since some date for this device.
	 * <p>
	 * SinceResource is observable for its clients.
	 * <p>
	 * sum: returns the sum of all documents stored since *date* for the target device<br>
	 * avg: returns the average of all documents stored since *date* for the target device<br>
	 * max: returns the maximum of all documents stored since *date* for the target device<br>
	 * min: returns the minimum of all documents stored since *date* for the target device<br>
	 * all subresources are observable.
	 */
	public class SinceResource extends LocalResource {
				
		private SumResource sumResource;
		private AvgResource avgResource;
		private MaxResource maxResource;
		private MinResource minResource;
		
		private String date;
		
		/**
		 * Instantiates a new since resource and makes it observable. All
		 * subresources are added.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public SinceResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			date = "";
						
			addSubResource((sumResource = new SumResource("sum", sumResourceType.new Since())));
			addSubResource((avgResource = new AvgResource("avg", avgResourceType.new Since())));
			addSubResource((maxResource = new MaxResource("max", maxResourceType.new Since())));
			addSubResource((minResource = new MinResource("min", minResourceType.new Since())));
		}
		
		/**
		 * perform GET queries the database for all documents since some date of
		 * this device and responds with their values.
		 */
		public void performGET(GETRequest request) {
			System.out.println("GET INTEGER NEWEST: get request for device " + device);
			request.prettyPrint();
			
			String ret = "";
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
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
															   "date = " + Constants.DATE_FORMAT);
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
		public void notifyChanged(String date, float value) {
			if (this.date.compareTo(date) < 0) {
				changed();
				sumResource.notifyChanged(value);
				avgResource.notifyChanged(value);
				maxResource.notifyChanged(value);
				minResource.notifyChanged(value);
			}
		}
	}
	
	/**
	 * The Class OnDayResource implements a subresource to query all documents
	 * stored in the database on some day for this device.
	 * <p>
	 * AllResource is observable for its clients.
	 * <p>
	 * It has the subresources:<br>
	 * sum: returns the sum of all documents stored on *date* for the target device<br>
	 * avg: returns the average of all documents stored on *date* for the target device<br>
	 * max: returns the maximum of all documents stored on *date* for the target device<br>
	 * min: returns the minimum of all documents stored on *date* for the target device<br>
	 * All subresources are observable.
	 */
	public class OnDayResource extends LocalResource {
				
		private SumResource sumResource;		
		private AvgResource avgResource;		
		private MaxResource maxResource;
		private MinResource minResource;
		
		private String date;
		
		/**
		 * Instantiates a new on day resource and makes it observable. All
		 * subresources are added.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public OnDayResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			date = "";
						
			addSubResource((sumResource = new SumResource("sum", sumResourceType.new OnDay())));
			addSubResource((avgResource = new AvgResource("avg", avgResourceType.new OnDay())));
			addSubResource((maxResource = new MaxResource("max", maxResourceType.new OnDay())));
			addSubResource((minResource = new MinResource("min", minResourceType.new OnDay())));
		}
		
		/**
		 * perform GET queries the database for all documents of this device
		 * stored on some day and responds with their values.
		 * <p>
		 * Payload:<br>
		 * date = yyyy/MM/dd-HH:mm:ss
		 */
		public void performGET(GETRequest request) {
			System.out.println("GET INTEGER ONDAY: get request for device " + device);
			request.prettyPrint();
			
			String ret = "";
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
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
															   "date = " + Constants.DATE_FORMAT_DAY);
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
		public void notifyChanged(String date, float value) {
			if (date.equals(this.date)) {
				changed();
				sumResource.notifyChanged(value);
				avgResource.notifyChanged(value);
				maxResource.notifyChanged(value);
				minResource.notifyChanged(value);
			}
		}
		
	}
	
	/**
	 * The Class TimeRangeResource implements a subresource to query all documents
	 * stored in the database between some time range for this device.
	 * <p>
	 * TimeRangeResource is observable for its clients.
	 * <p>
	 * It has the subresources:<br>
	 * sum: returns the sum of all documents stored between *startdate* and *enddate* for the target device<br>
	 * avg: returns the average of all documents stored between *startdate* and *enddate* for the target device<br>
	 * max: returns the maximum of all documents stored between *startdate* and *enddate* for the target device<br>
	 * min: returns the minimum of all documents stored between *startdate* and *enddate* for the target device<br>
	 * All subresources are observable.
	 */
	public class TimeRangeResource extends LocalResource {
				
		private SumResource sumResource;		
		private AvgResource avgResource;		
		private MaxResource maxResource;		
		private MinResource minResource;				

		private String startDate;
		private String endDate;
		
		/**
		 * Instantiates a new time range resource and makes it observable. All
		 * subresources are added.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public TimeRangeResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
						
			startDate = "";
			endDate = "";
						
			addSubResource((sumResource = new SumResource("sum", sumResourceType.new TimeRange())));
			addSubResource((avgResource = new AvgResource("avg", avgResourceType.new TimeRange())));
			addSubResource((maxResource = new MaxResource("max", maxResourceType.new TimeRange())));
			addSubResource((minResource = new MinResource("min", minResourceType.new TimeRange())));
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
			System.out.println("GET INTEGER TIMERANGE: get request for device " + device);
			request.prettyPrint();
			
			String ret = "";
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
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
															   "startdate = " + Constants.DATE_FORMAT + "\n" +
															   "enddate = " + Constants.DATE_FORMAT);
			}
		}
		
		/**
		 * Notify changed and pass the notification to the subresources.
		 *
		 * @param date the current date
		 * @param value the newest value
		 */
		public void notifyChanged(String date, float value) {
			if (startDate.compareTo(date) <= 0 && endDate.compareTo(date) >= 0) {
				changed();
				sumResource.notifyChanged(value);
				avgResource.notifyChanged(value);
				maxResource.notifyChanged(value);
				minResource.notifyChanged(value);
			}
		}
	}
	
	/**
	 * The Class SumResource
	 */
	public class SumResource extends LocalResource {
		
		/** The sum resource type. */
		SumResourceType.SumResourceInterface sumResourceType;
		
		/**
		 * Instantiates a new sum resource.
		 *
		 * @param resourceIdentifier the resource identifier
		 * @param sumResourceType the sum resource type
		 */
		public SumResource(String resourceIdentifier, SumResourceType.SumResourceInterface sumResourceType) {
			super(resourceIdentifier);
			isObservable(true);
			
			this.sumResourceType = sumResourceType;
		}
		
		/* (non-Javadoc)
		 * @see ch.ethz.inf.vs.californium.endpoint.LocalResource#performGET(ch.ethz.inf.vs.californium.coap.GETRequest)
		 */
		public void performGET(GETRequest request) {
			sumResourceType.perform(request, numberTypeRepository, device);
		}
		
		/**
		 * Notify changed.
		 *
		 * @param value the value
		 */
		public void notifyChanged(float value) {
			if (value != 0)
				changed();
		}
		
	}
	
	/**
	 * The Class SumResourceType.
	 */
	public static class SumResourceType {
		
		/**
		 * The Interface SumResourceInterface.
		 */
		public interface SumResourceInterface {
			
			/**
			 * Perform.
			 *
			 * @param request the request
			 * @param numberTypeRepository the number type repository
			 * @param device the device
			 */
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device);
		}
		
		/**
		 * The Class Default.
		 */
		public class Default implements SumResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.SumResourceType.SumResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER ALL SUM: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				List<NumberType.Sum> res = numberTypeRepository.queryDeviceSum();
				if (!res.isEmpty()) {
					ret = "" + res.get(0).getSum();
					System.out.println("GETRequst SUM: (value: " + ret + ") for device " + device);
				}
					
				request.respond(CodeRegistry.RESP_CONTENT, ret);
			}
			
		}
		
		/**
		 * The Class Since.
		 */
		public class Since implements SumResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.SumResourceType.SumResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER SINCE SUM: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("date")) {
					String date = parsedPayload.getStringValue("date");
					List<NumberType.DateSum> resSince = numberTypeRepository.queryDeviceSinceSum(date);
					if (!resSince.isEmpty())
						ret += resSince.get(0).getSum();
					System.out.println("GETRequst SINCE: (value: " + ret + ") for device " + device);
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "date = " + Constants.DATE_FORMAT);
				}
			}
		}
		
		/**
		 * The Class OnDay.
		 */
		public class OnDay implements SumResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.SumResourceType.SumResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER ONDAY SUM: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("date")) {
					String date = parsedPayload.getStringValue("date");
					String startOnDay = date + "-00:00:00";
					String endOnDay = date + "-23:59:59";
					List<NumberType.DateSum> resOnDay = numberTypeRepository.queryDeviceRangeSum(startOnDay, endOnDay);
					if (!resOnDay.isEmpty())
						ret += resOnDay.get(0).getSum();
					System.out.println("GETRequst ONDAY: (value: " + ret + ") for device " + device);
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "date = " + Constants.DATE_FORMAT_DAY);
				}
			}
		}
		
		/**
		 * The Class TimeRange.
		 */
		public class TimeRange implements SumResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.SumResourceType.SumResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER TIMERANGE SUM: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("startdate") && parsedPayload.containsLabel("enddate")) {
					String startDate = parsedPayload.getStringValue("startdate");
					String endDate = parsedPayload.getStringValue("enddate");
					List<NumberType.DateSum> resTimeRange = numberTypeRepository.queryDeviceRangeSum(startDate, endDate);
					if (!resTimeRange.isEmpty())
						ret += resTimeRange.get(0).getSum();
					System.out.println("GETRequst TIMERANGE: (value: " + ret + ") for device " + device);
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "startdate = " + Constants.DATE_FORMAT + "\n" +
																   "enddate = " + Constants.DATE_FORMAT);
				}
			}
		}
		
		/**
		 * The Class Last.
		 */
		public class Last implements SumResourceInterface {
			
			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.SumResourceType.SumResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER LAST SUM: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("limit")) {
					int limit = parsedPayload.getIntValue("limit");
					if (limit <= Constants.MAX_LIMIT || limit > 0) {
						List<NumberType.Default> resLimit = numberTypeRepository.queryDeviceLimit(limit);
						if (!resLimit.isEmpty())
							ret += calcSum(resLimit);
						request.respond(CodeRegistry.RESP_CONTENT, ret);
					} else {
						request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																       "last = <1 - " + Constants.MAX_LIMIT + ">");
					}
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "last = <1 - " + Constants.MAX_LIMIT + ">");
				}
			}
			
			/**
			 * Calc sum.
			 *
			 * @param list the list
			 * @return the float
			 */
			private float calcSum(List<NumberType.Default> list) {
				float sum = 0;
				for (NumberType.Default nt : list) {
					sum += nt.getNumberValue();
				}
				return sum;
			}
		}
	}
	
	/**
	 * The Class AvgResource.
	 */
	public class AvgResource extends LocalResource {
		
		/** The avg resource type. */
		private AvgResourceType.AvgResourceInterface avgResourceType;
		
		/** The avg. */
		private float avg;

		/**
		 * Instantiates a new avg resource.
		 *
		 * @param resourceIdentifier the resource identifier
		 * @param avgResourceType the avg resource type
		 */
		public AvgResource(String resourceIdentifier, AvgResourceType.AvgResourceInterface avgResourceType) {
			super(resourceIdentifier);
			isObservable(true);
			
			this.avgResourceType = avgResourceType;
			
			avg = 0;
		}
		
		/* (non-Javadoc)
		 * @see ch.ethz.inf.vs.californium.endpoint.LocalResource#performGET(ch.ethz.inf.vs.californium.coap.GETRequest)
		 */
		public void performGET(GETRequest request) {
			avgResourceType.perform(request, numberTypeRepository, device);
		}
		
		/**
		 * Notify changed.
		 *
		 * @param value the value
		 */
		public void notifyChanged(float value) {
			if (avg != value)
				changed();
		}
	}
	
	/**
	 * The Class AvgResourceType.
	 */
	public static class AvgResourceType {
		
		/**
		 * The Interface AvgResourceInterface.
		 */
		public interface AvgResourceInterface {
			
			/**
			 * Perform.
			 *
			 * @param request the request
			 * @param numberTypeRepository the number type repository
			 * @param device the device
			 */
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device);
		}
		
		/**
		 * The Class Default.
		 */
		public class Default implements AvgResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.AvgResourceType.AvgResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER ALL AVG: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				List<NumberType.Avg> res = numberTypeRepository.queryDeviceAvg();
				if (!res.isEmpty()) {
					ret = "" + res.get(0).getAvg();
					System.out.println("GETRequst SUM: (value: " + ret + ") for device " + device);
				}
					
				request.respond(CodeRegistry.RESP_CONTENT, ret);
			}
			
		}
		
		/**
		 * The Class Since.
		 */
		public class Since implements AvgResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.AvgResourceType.AvgResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER SINCE AVG: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("date")) {
					String date = parsedPayload.getStringValue("date");
					List<NumberType.DateAvg> resSince = numberTypeRepository.queryDeviceSinceAvg(date);
					if (!resSince.isEmpty())
						ret += resSince.get(0).getAvg();
					System.out.println("GETRequst SINCE: (value: " + ret + ") for device " + device);
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "date = " + Constants.DATE_FORMAT);
				}
			}
		}
		
		/**
		 * The Class OnDay.
		 */
		public class OnDay implements AvgResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.AvgResourceType.AvgResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER ONDAY AVG: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("date")) {
					String date = parsedPayload.getStringValue("date");
					String startOnDay = date + "-00:00:00";
					String endOnDay = date + "-23:59:59";
					List<NumberType.DateAvg> resOnDay = numberTypeRepository.queryDeviceRangeAvg(startOnDay, endOnDay);
					if (!resOnDay.isEmpty())
						ret += resOnDay.get(0).getAvg();
					System.out.println("GETRequst ONDAY: (value: " + ret + ") for device " + device);
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "date = " + Constants.DATE_FORMAT_DAY);
				}
			}
		}
		
		/**
		 * The Class TimeRange.
		 */
		public class TimeRange implements AvgResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.AvgResourceType.AvgResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER TIMERANGE AVG: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("startdate") && parsedPayload.containsLabel("enddate")) {
					String startDate = parsedPayload.getStringValue("startdate");
					String endDate = parsedPayload.getStringValue("enddate");
					List<NumberType.DateAvg> resTimeRange = numberTypeRepository.queryDeviceRangeAvg(startDate, endDate);
					if (!resTimeRange.isEmpty())
						ret += resTimeRange.get(0).getAvg();
					System.out.println("GETRequst TIMERANGE: (value: " + ret + ") for device " + device);
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "startdate = " + Constants.DATE_FORMAT + "\n" +
																   "enddate = " + Constants.DATE_FORMAT);
				}
			}
		}
		
		/**
		 * The Class Last.
		 */
		public class Last implements AvgResourceInterface {
			
			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.AvgResourceType.AvgResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER LAST AVG: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("limit")) {
					int limit = parsedPayload.getIntValue("limit");
					if (limit <= Constants.MAX_LIMIT || limit > 0) {
						List<NumberType.Default> resLimit = numberTypeRepository.queryDeviceLimit(limit);
						if (!resLimit.isEmpty())
							ret += calcAvg(resLimit);
						request.respond(CodeRegistry.RESP_CONTENT, ret);
					} else {
						request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																       "last = <1 - " + Constants.MAX_LIMIT + ">");
					}
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "last = <1 - " + Constants.MAX_LIMIT + ">");
				}
			}
			
			/**
			 * Calc avg.
			 *
			 * @param list the list
			 * @return the float
			 */
			private float calcAvg(List<NumberType.Default> list) {
				float count = 0;
				float sum = 0;
				for (NumberType.Default nt : list) {
					sum += nt.getNumberValue();
					count++;
				}
				return (sum/count);
			}
		}
	}
	
	/**
	 * The Class MaxResource.
	 */
	public class MaxResource extends LocalResource {
		
		/** The max resource type. */
		private MaxResourceType.MaxResourceInterface maxResourceType;
		
		/** The max. */
		private float max;

		/**
		 * Instantiates a new max resource.
		 *
		 * @param resourceIdentifier the resource identifier
		 * @param maxResourceType the max resource type
		 */
		public MaxResource(String resourceIdentifier, MaxResourceType.MaxResourceInterface maxResourceType) {
			super(resourceIdentifier);
			isObservable(true);
			
			this.maxResourceType = maxResourceType;
			
			max = 0;
		}
		
		/* (non-Javadoc)
		 * @see ch.ethz.inf.vs.californium.endpoint.LocalResource#performGET(ch.ethz.inf.vs.californium.coap.GETRequest)
		 */
		public void performGET(GETRequest request) {
			maxResourceType.perform(request, numberTypeRepository, device);
		}
		
		/**
		 * Notify changed.
		 *
		 * @param value the value
		 */
		public void notifyChanged(float value) {
			if (value > max)
				changed();
		}
	}
	
	/**
	 * The Class MaxResourceType.
	 */
	public static class MaxResourceType {
		
		/**
		 * The Interface MaxResourceInterface.
		 */
		public interface MaxResourceInterface {
			
			/**
			 * Perform.
			 *
			 * @param request the request
			 * @param numberTypeRepository the number type repository
			 * @param device the device
			 */
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device);
		}
		
		/**
		 * The Class Default.
		 */
		public class Default implements MaxResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.MaxResourceType.MaxResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER ALL MAX: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				List<NumberType.Max> res = numberTypeRepository.queryDeviceMax();
				if (!res.isEmpty()) {
					ret = "" + res.get(0).getMax();
					System.out.println("GETRequst SUM: (value: " + ret + ") for device " + device);
				}
					
				request.respond(CodeRegistry.RESP_CONTENT, ret);
			}
			
		}
		
		/**
		 * The Class Since.
		 */
		public class Since implements MaxResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.MaxResourceType.MaxResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER SINCE MAX: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("date")) {
					String date = parsedPayload.getStringValue("date");
					List<NumberType.DateMax> resSince = numberTypeRepository.queryDeviceSinceMax(date);
					if (!resSince.isEmpty())
						ret += resSince.get(0).getMax();
					System.out.println("GETRequst SINCE: (value: " + ret + ") for device " + device);
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "date = " + Constants.DATE_FORMAT);
				}
			}
		}
		
		/**
		 * The Class OnDay.
		 */
		public class OnDay implements MaxResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.MaxResourceType.MaxResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER ONDAY MAX: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("date")) {
					String date = parsedPayload.getStringValue("date");
					String startOnDay = date + "-00:00:00";
					String endOnDay = date + "-23:59:59";
					List<NumberType.DateMax> resOnDay = numberTypeRepository.queryDeviceRangeMax(startOnDay, endOnDay);
					if (!resOnDay.isEmpty())
						ret += resOnDay.get(0).getMax();
					System.out.println("GETRequst ONDAY: (value: " + ret + ") for device " + device);
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "date = " + Constants.DATE_FORMAT_DAY);
				}
			}
		}
		
		/**
		 * The Class TimeRange.
		 */
		public class TimeRange implements MaxResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.MaxResourceType.MaxResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER TIMERANGE MAX: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("startdate") && parsedPayload.containsLabel("enddate")) {
					String startDate = parsedPayload.getStringValue("startdate");
					String endDate = parsedPayload.getStringValue("enddate");
					List<NumberType.DateMax> resTimeRange = numberTypeRepository.queryDeviceRangeMax(startDate, endDate);
					if (!resTimeRange.isEmpty())
						ret += resTimeRange.get(0).getMax();
					System.out.println("GETRequst TIMERANGE: (value: " + ret + ") for device " + device);
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "startdate = " + Constants.DATE_FORMAT + "\n" +
																   "enddate = " + Constants.DATE_FORMAT);
				}
			}
		}
		
		/**
		 * The Class Last.
		 */
		public class Last implements MaxResourceInterface {
			
			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.MaxResourceType.MaxResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER LAST MAX: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("limit")) {
					int limit = parsedPayload.getIntValue("limit");
					if (limit <= Constants.MAX_LIMIT || limit > 0) {
						List<NumberType.Default> resLimit = numberTypeRepository.queryDeviceLimit(limit);
						if (!resLimit.isEmpty())
							ret += calcMax(resLimit);
						request.respond(CodeRegistry.RESP_CONTENT, ret);
					} else {
						request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																       "last = <1 - " + Constants.MAX_LIMIT + ">");
					}
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "last = <1 - " + Constants.MAX_LIMIT + ">");
				}
			}
			
			/**
			 * Calc max.
			 *
			 * @param list the list
			 * @return the float
			 */
			private float calcMax(List<NumberType.Default> list) {
				float max = Float.MIN_VALUE;
				for (NumberType.Default nt : list) {
					if (max < nt.getNumberValue())
						max = nt.getNumberValue();
				}
				return max;
			}
		}
	}
	
	/**
	 * The Class MinResource.
	 */
	public class MinResource extends LocalResource {
		
		/** The min resource type. */
		private MinResourceType.MinResourceInterface minResourceType;
		
		/** The min. */
		private float min;

		/**
		 * Instantiates a new min resource.
		 *
		 * @param resourceIdentifier the resource identifier
		 * @param minResourceType the min resource type
		 */
		public MinResource(String resourceIdentifier, MinResourceType.MinResourceInterface minResourceType) {
			super(resourceIdentifier);
			isObservable(true);
			
			this.minResourceType = minResourceType;
			
			min = 0;
		}
		
		/* (non-Javadoc)
		 * @see ch.ethz.inf.vs.californium.endpoint.LocalResource#performGET(ch.ethz.inf.vs.californium.coap.GETRequest)
		 */
		public void performGET(GETRequest request) {
			minResourceType.perform(request, numberTypeRepository, device);
		}
		
		/**
		 * Notify changed.
		 *
		 * @param value the value
		 */
		public void notifyChanged(float value) {
			if (value < min)
				changed();
		}
	}
	
	/**
	 * The Class MinResourceType.
	 */
	public static class MinResourceType {
		
		/**
		 * The Interface MinResourceInterface.
		 */
		public interface MinResourceInterface {
			
			/**
			 * Perform.
			 *
			 * @param request the request
			 * @param numberTypeRepository the number type repository
			 * @param device the device
			 */
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device);
		}
		
		/**
		 * The Class Default.
		 */
		public class Default implements MinResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.MinResourceType.MinResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER ALL MIN: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				List<NumberType.Min> res = numberTypeRepository.queryDeviceMin();
				if (!res.isEmpty()) {
					ret = "" + res.get(0).getMin();
					System.out.println("GETRequst SUM: (value: " + ret + ") for device " + device);
				}
					
				request.respond(CodeRegistry.RESP_CONTENT, ret);
			}
			
		}
		
		/**
		 * The Class Since.
		 */
		public class Since implements MinResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.MinResourceType.MinResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER SINCE MIN: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("date")) {
					String date = parsedPayload.getStringValue("date");
					List<NumberType.DateMin> resSince = numberTypeRepository.queryDeviceSinceMin(date);
					if (!resSince.isEmpty())
						ret += resSince.get(0).getMin();
					System.out.println("GETRequst SINCE: (value: " + ret + ") for device " + device);
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "date = " + Constants.DATE_FORMAT);
				}
			}
		}
		
		/**
		 * The Class OnDay.
		 */
		public class OnDay implements MinResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.MinResourceType.MinResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER SINCE MIN: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("date")) {
					String date = parsedPayload.getStringValue("date");
					String startOnDay = date + "-00:00:00";
					String endOnDay = date + "-23:59:59";
					List<NumberType.DateMin> resOnDay = numberTypeRepository.queryDeviceRangeMin(startOnDay, endOnDay);
					if (!resOnDay.isEmpty())
						ret += resOnDay.get(0).getMin();
					System.out.println("GETRequst ONDAY: (value: " + ret + ") for device " + device);
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "date = " + Constants.DATE_FORMAT_DAY);
				}
			}
		}
		
		/**
		 * The Class TimeRange.
		 */
		public class TimeRange implements MinResourceInterface {

			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.MinResourceType.MinResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER TIMERANGE MIN: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("startdate") && parsedPayload.containsLabel("enddate")) {
					String startDate = parsedPayload.getStringValue("startdate");
					String endDate = parsedPayload.getStringValue("enddate");
					List<NumberType.DateMin> resTimeRange = numberTypeRepository.queryDeviceRangeMin(startDate, endDate);
					if (!resTimeRange.isEmpty())
						ret += resTimeRange.get(0).getMin();
					System.out.println("GETRequst TIMERANGE: (value: " + ret + ") for device " + device);
					request.respond(CodeRegistry.RESP_CONTENT, ret);
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "startdate = " + Constants.DATE_FORMAT + "\n" +
																   "enddate = " + Constants.DATE_FORMAT);
				}
			}
		}
		
		/**
		 * The Class Last.
		 */
		public class Last implements MinResourceInterface {
						
			/* (non-Javadoc)
			 * @see ch.ethz.inf.vs.persistingservice.resources.SpecificNumberResource.MinResourceType.MinResourceInterface#perform(ch.ethz.inf.vs.californium.coap.GETRequest, ch.ethz.inf.vs.persistingservice.database.NumberTypeRepository, java.lang.String)
			 */
			@Override
			public void perform(GETRequest request, NumberTypeRepository numberTypeRepository, String device) {
				System.out.println("GET INTEGER LAST MIN: get request for device " + device);
				request.prettyPrint();
				
				String ret = "";
				String payload = request.getPayloadString();
				PayloadParser parsedPayload = new PayloadParser(payload);
				if (parsedPayload.containsLabel("limit")) {
					int limit = parsedPayload.getIntValue("limit");
					if (limit <= Constants.MAX_LIMIT || limit > 0) {
						List<NumberType.Default> resLimit = numberTypeRepository.queryDeviceLimit(limit);
						if (!resLimit.isEmpty())
							ret += calcMin(resLimit);
						request.respond(CodeRegistry.RESP_CONTENT, ret);
					} else {
						request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																       "last = <1 - " + Constants.MAX_LIMIT + ">");
					}
				} else {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
																   "last = <1 - " + Constants.MAX_LIMIT + ">");
				}
			}
			
			/**
			 * Calc min.
			 *
			 * @param list the list
			 * @return the float
			 */
			private float calcMin(List<NumberType.Default> list) {
				float min = Float.MAX_VALUE;
				for (NumberType.Default nt : list) {
					if (min > nt.getNumberValue())
						min = nt.getNumberValue();
				}
				return min;
			}
		}
		
		
	}
}
