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

import ch.ethz.inf.vs.californium.coap.DELETERequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.californium.coap.Request;
import ch.ethz.inf.vs.californium.coap.Response;
import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.ResponseHandler;
import ch.ethz.inf.vs.californium.endpoint.LocalResource;
import ch.ethz.inf.vs.persistingservice.config.Constants;
import ch.ethz.inf.vs.persistingservice.database.DatabaseConnection;
import ch.ethz.inf.vs.persistingservice.database.StringType;
import ch.ethz.inf.vs.persistingservice.database.StringTypeRepository;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;

/**
 * The Class SpecificStringResource defines a specific string resource, which
 * requests data from a specified device and stores it in the database.
 * <p>
 * If possible the specific string resource registers as observer on the device
 * resource, otherwise the specific string resource polls data periodically.
 * <p>
 * The data is stored in the following format:<br>
 * device = DEVICE_PATH<br>
 * stringValue = VALUE_OF_STRING<br>
 * dateTime = yyyy/MM/dd-HH/mm/ss
 * <p>
 * The subresources to retrieve data are:<br>
 * -	<b>newest</b>: returns only the value of the newest document stored for the target
 * device<br>
 * -	<b>all</b>: returns the values of all documents stored for the target device<br>
 * -	<b>last</b>: returns the values of the last *limit* documents. <br>
 * -	<b>since</b>: returns the values of all documents stored since *date* for the target
 * device<br>
 * -	<b>onday</b>: returns the values of all documents stored on *date* for the target
 * device<br>
 * -	<b>timerange</b>: returns the values of all documents stored between *startdate* and
 * *enddate* for the target device
 */
public class SpecificStringResource extends LocalResource {
	
	private NewestResource newestResource;	
	private AllResource allResource;
	private LastResource lastResource;
	private SinceResource sinceResource;	
	private OnDayResource ondayResource;
	private TimeRangeResource timerangeResource;
	
	private String resourceIdentifier;
	
	private StringTypeRepository stringTypeRepository;
	
	private String deviceROOT;
	private String deviceURI;
	private String device;

	private Timer timer;

	/**
	 * Instantiates a new specific string resource for the device deviceROOT +
	 * deviceURI and adds the required subresources.
	 * <p>
	 * A private string type repository is created to connect to the database
	 * and store data or query data from it.
	 * <p>
	 * The specific string resource tries to register as observer on the device.
	 * Otherwise it starts a polling task to retrieve the data from the device
	 * periodically.
	 * 
	 * @param resourceIdentifier
	 *            the resource identifier
	 * @param deviceROOT
	 *            the device root
	 * @param deviceURI
	 *            the device uri
	 */
	public SpecificStringResource(String resourceIdentifier, String deviceROOT, String deviceURI) {
		super(resourceIdentifier);
		setTitle("Resource to sign up for observing a String value");
		setResourceType("stringtype");
		this.resourceIdentifier = resourceIdentifier;
		this.deviceROOT = deviceROOT;
		this.deviceURI = deviceURI;
		this.device = deviceROOT + deviceURI;
		
		stringTypeRepository = new StringTypeRepository(StringType.Default.class, DatabaseConnection.getCouchDbConnector(), deviceURI);
		
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
		addSubResource((lastResource = new LastResource("last")));
		addSubResource((sinceResource = new SinceResource("since")));
		addSubResource((ondayResource = new OnDayResource("onday")));
		addSubResource((timerangeResource = new TimeRangeResource("timerange")));
	}
	
	/**
	 * Gets the device specified for this specific string resource.
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
	private void notifyChanged(String value, String date) {
		allResource.notifyChanged();
		newestResource.notifyChanged(value, date);
		lastResource.notifyChanged();
		sinceResource.notifyChanged(date);
		ondayResource.notifyChanged(date);
		timerangeResource.notifyChanged(date);
	}
	
	// Requests ///////////////////////////////////////////////////////////////
	
	/**
	 * perform GET responds with the device specified for this specific string
	 * resource.
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET: get request for " + resourceIdentifier);
		request.prettyPrint();
		
		String ret = "device = " + device;
		
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
	
	/**
	 * perform DELETE deletes this specific string resource and stops the polling task if necessary.
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
	 * If the target device resource is observable this specific string resource
	 * registers as observer and receives push notification when the data has
	 * changed.<br>
	 * Otherwise this specific string resource starts a polling task to
	 * periodically fetch the data from the device.
	 */
	public class CheckObservableHandler implements ResponseHandler {

		/**
		 * The response comes from the get request to check, if the target
		 * device is observable.
		 * <p>
		 * If the response has the observable option, the resource is observable
		 * and this specific string resource registers as observer and receives
		 * push notification when the data has changed.<br>
		 * Otherwise this specific string resource starts a polling task to
		 * periodically fetch the data from the device.
		 */
		@Override
		public void handleResponse(Response response) {
			System.out.println("OBSERVABLE CHECK: checking for observable on device " + device);
			
			String value = response.getPayloadString();
			DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
			Date date = new Date();
			newestResource.notifyChanged(value, dateFormat.format(date));
							
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
	 * The Class ObservingHandler handles the response when this specific string
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
				
			StringType.Default stringType = new StringType.Default();
			stringType.setDevice(device);
			stringType.setStringValue(payload);
			DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
	        Date date = new Date();
			stringType.setDateTime(dateFormat.format(date));
				
			stringTypeRepository.add(stringType);
			System.out.println("DATABASE: data (value: " + payload + ") was stored for device " + device);
			
			notifyChanged(payload, dateFormat.format(date));
			System.out.println("PUSH NOTIFICATION: notify resources after new data was stored for device " + device);
		}
		
	}
	
	// Polling Task ///////////////////////////////////////////////////////////
	
	/**
	 * The Class PollingTask periodically retrieves data from the target device
	 * and stores it in the database.
	 */
	public class PollingTask extends TimerTask {
		
		String oldValue;

		/**
		 * Instantiates a new polling task.
		 */
		public PollingTask() {
			oldValue = "";
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

			if (!oldValue.equals(payload)) {
				oldValue = payload;
				
				StringType.Default stringType = new StringType.Default();
				stringType.setDevice(device);
				stringType.setStringValue(payload);
				DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
				Date date = new Date();
				stringType.setDateTime(dateFormat.format(date));
	
				stringTypeRepository.add(stringType);
				System.out.println("DATABASE: data (value: " + payload + ") was stored for device " + device);
	
				notifyChanged(payload, dateFormat.format(date));
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

		String value;
		String date;

		/**
		 * Instantiates a new newest resource and makes it observable.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public NewestResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			
			value = "";
			date = "";
		}

		/**
		 * perform GET responds with the newest value stored in the database.
		 */
		public void performGET(GETRequest request) {
			System.out.println("GET STRING NEWEST: get request for device " + device);
			request.prettyPrint();
			
			List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
			OptionParser parsedOptions = new OptionParser(options);
			
			String ret = "";
			
			boolean withDate = false;
			if (parsedOptions.containsLabel("withdate"))
				withDate = parsedOptions.getBooleanValue("withdate");
			if (withDate) {
				ret += value + ";" + date;
			} else {
				ret += value;
			}
			request.respond(CodeRegistry.RESP_CONTENT, ret);

			System.out.println("GETRequst NEWEST: (value: " + ret + ") for device " + device);
		}

		/**
		 * Notify changed.
		 * 
		 * @param value
		 *            the newest value
		 */
		public void notifyChanged(String value, String date) {
			if (!this.value.equals(value)) {
				this.value = value;
				this.date = date;
				changed();
			}
		}
	}
	
	/**
	 * The Class AllResource implements a subresource to query all documents
	 * stored in the database for this device.
	 * <p>
	 * AllResource is observable for its clients.
	 */
	public class AllResource extends LocalResource {

		/**
		 * Instantiates a new all resource and makes it observable.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public AllResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
		}

		/**
		 * perform GET queries the database for all documents of this device and
		 * responds with their values.
		 */
		public void performGET(GETRequest request) {
			System.out.println("GET STRING ALL: get request for device " + device);
			request.prettyPrint();
			
			List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
			OptionParser parsedOptions = new OptionParser(options);
		
			String ret = "";
			
			List<StringType.Default> res = stringTypeRepository.queryDevice();
			
			boolean withDate = false;
			if (parsedOptions.containsLabel("withdate"))
				withDate = parsedOptions.getBooleanValue("withdate");
			if (withDate) {
				for (StringType.Default nt : res) {
					ret += nt.getStringValue() + ";" + nt.getDateTime() + "\n";
				}
			} else {
				for (StringType.Default nt : res) {
					ret += nt.getStringValue() + "\n";
				}
			}

			request.respond(CodeRegistry.RESP_CONTENT, ret);
		}

		/**
		 * Notify changed.
		 */
		public void notifyChanged() {
			changed();
		}
	}
	
	/**
	 * The Class LastResource implements a subresource to query the last documents
	 * stored in the database for this device.
	 * <p>
	 * LastResource is observable for its clients.
	 */
	public class LastResource extends LocalResource {
				
		/**
		 * Instantiates a new last resource and makes it observable.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public LastResource(String resourceIdentifier) {
			super(resourceIdentifier);
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
	
			List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
			OptionParser parsedOptions = new OptionParser(options);
			
			String ret = "";
	
			if (parsedOptions.containsLabel("limit")) {
				int limit = parsedOptions.getIntValue("limit");
				if (limit <= Constants.MAX_LIMIT || limit > 0) {
					List<StringType.Default> resLimit = stringTypeRepository.queryDeviceLimit(limit);
					
					boolean withDate = false;
					if (parsedOptions.containsLabel("withdate"))
						withDate = parsedOptions.getBooleanValue("withdate");
					if (withDate) {
						for (StringType.Default nt : resLimit) {
							ret += nt.getStringValue() + ";" + nt.getDateTime() + "\n";
						}
					} else {
						for (StringType.Default nt : resLimit) {
							ret += nt.getStringValue() + "\n";
						}
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
		 * Notify changed.
		 */
		public void notifyChanged() {
			changed();
		}
	}
	
	/**
	 * The Class SinceResource implements a subresource to query all documents
	 * stored in the database since some date for this device.
	 * <p>
	 * SinceResource is observable for its clients.
	 */
	public class SinceResource extends LocalResource {
		
		String date;
		
		/**
		 * Instantiates a new since resource and makes it observable.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public SinceResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			
			date = "";
		}
		
		/**
		 * perform GET queries the database for all documents since some date of
		 * this device and responds with their values.
		 */
		public void performGET(GETRequest request) {
			System.out.println("GET STRING SINCE: get request for device " + device);
			request.prettyPrint();
			
			List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
			OptionParser parsedOptions = new OptionParser(options);
			
			String ret = "";

			if (parsedOptions.containsLabel("date")) {
				String date = parsedOptions.getStringValue("date");
				List<StringType.Default> resSince = stringTypeRepository.queryDeviceSince(date);
				
				boolean withDate = false;
				if (parsedOptions.containsLabel("withdate"))
					withDate = parsedOptions.getBooleanValue("withdate");
				if (withDate) {
					for (StringType.Default nt : resSince) {
						ret += nt.getStringValue() + ";" + nt.getDateTime() + "\n";
					}
				} else {
					for (StringType.Default nt : resSince) {
						ret += nt.getStringValue() + "\n";
					}
				}
				
				System.out.println("GETRequst SINCE: (value: " + ret.substring(0, Math.max(50, ret.length())) + ") for device " + device);
				request.respond(CodeRegistry.RESP_CONTENT, ret);
			} else {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
															   "date = " + Constants.DATE_FORMAT);
			}	
		}
		
		/**
		 * Notify changed.
		 * 
		 * @param date
		 *            the current date
		 */
		public void notifyChanged(String date) {
			if (this.date.compareTo(date) < 0)
				changed();
		}
	}
	
	/**
	 * The Class OnDayResource implements a subresource to query all documents
	 * stored in the database on some day for this device.
	 * <p>
	 * AllResource is observable for its clients.
	 */
	public class OnDayResource extends LocalResource {
		
		String date;
		
		/**
		 * Instantiates a new on day resource and makes it observable.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public OnDayResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			
			date = "";
		}
		
		/**
		 * perform GET queries the database for all documents of this device
		 * stored on some day and responds with their values.
		 * <p>
		 * Payload:<br>
		 * date = yyyy/MM/dd-HH:mm:ss
		 */
		public void performGET(GETRequest request) {
			System.out.println("GET STRING ONDAY: get request for device " + device);
			request.prettyPrint();
			
			List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
			OptionParser parsedOptions = new OptionParser(options);
			
			String ret = "";

			if (parsedOptions.containsLabel("date")) {
				this.date = parsedOptions.getStringValue("date");
				String startOnDay = this.date + "-00:00:00";
				String endOnDay = this.date + "-23:59:59";
				List<StringType.Default> resOnDay = stringTypeRepository.queryDeviceRange(startOnDay, endOnDay);
				
				boolean withDate = false;
				if (parsedOptions.containsLabel("withdate"))
					withDate = parsedOptions.getBooleanValue("withdate");
				if (withDate) {
					for (StringType.Default nt : resOnDay) {
						ret += nt.getStringValue() + ";" + nt.getDateTime() + "\n";
					}
				} else {
					for (StringType.Default nt : resOnDay) {
						ret += nt.getStringValue() + "\n";
					}
				}
				System.out.println("GETRequst ONDAY: (value: " + ret.substring(0, Math.max(50, ret.length())) + ") for device " + device);
				request.respond(CodeRegistry.RESP_CONTENT, ret);
			} else {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
															   "date = " + Constants.DATE_FORMAT_DAY);
			}
		}
		
		/**
		 * Notify changed.
		 * 
		 * @param date
		 *            the current date
		 */
		public void notifyChanged(String date) {
			if (date.equals(this.date))
				changed();
		}
		
	}
	
	/**
	 * The Class TimeRangeResource implements a subresource to query all documents
	 * stored in the database between some time range for this device.
	 * <p>
	 * TimeRangeResource is observable for its clients.
	 */
	public class TimeRangeResource extends LocalResource {
		
		String startDate;
		String endDate;
		
		/**
		 * Instantiates a new time range resource and makes it observable.
		 * 
		 * @param resourceIdentifier
		 *            the resource identifier
		 */
		public TimeRangeResource(String resourceIdentifier) {
			super(resourceIdentifier);
			isObservable(true);
			
			startDate = "";
			endDate = "";
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
			System.out.println("GET STRING TIMERANGE: get request for device " + device);
			request.prettyPrint();
			
			List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
			OptionParser parsedOptions = new OptionParser(options);
			
			String ret = "";

			if (parsedOptions.containsLabel("startdate") && parsedOptions.containsLabel("enddate")) {
				this.startDate = parsedOptions.getStringValue("startdate");
				this.endDate = parsedOptions.getStringValue("enddate");
				List<StringType.Default> resTimeRange = stringTypeRepository.queryDeviceRange(this.startDate, this.endDate);
				
				boolean withDate = false;
				if (parsedOptions.containsLabel("withdate"))
					withDate = parsedOptions.getBooleanValue("withdate");
				if (withDate) {
					for (StringType.Default nt : resTimeRange) {
						ret += nt.getStringValue() + ";" + nt.getDateTime() + "\n";
					}
				} else {
					for (StringType.Default nt : resTimeRange) {
						ret += nt.getStringValue() + "\n";
					}
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
		 * Notify changed.
		 * 
		 * @param date
		 *            the current date
		 */
		public void notifyChanged(String date) {
			if (startDate.compareTo(date) <= 0 && endDate.compareTo(date) >= 0)
				changed();
		}
	}
	
}
