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
package ch.ethz.inf.vs.persistingservice.resources.persisting.history;

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
import ch.ethz.inf.vs.persistingservice.database.DatabaseRepository;
import ch.ethz.inf.vs.persistingservice.database.documents.*;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.time.*;


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
public class HistoryResource<T extends Comparable> extends LocalResource{
	
	private AllResource allResource;
	private NewestResource newestResource;
	private LastResource lastResource;
	private SinceResource sinceResource;
	private OnDayResource ondayResource;
	private TimeRangeResource timerangeResource;
	
	private String resourceIdentifier;
	
	private DatabaseRepository<T> typeRepository;

	private String type;
	private String deviceROOT;
	private String deviceRES;
	private String device;
	
	private AbstractValueSet abstractSetValue;
	
	private ObservingHandler observingHandler;
	private Request observingRequest;
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
	@SuppressWarnings("unchecked")
	public HistoryResource(String resourceIdentifier, String type, String deviceROOT, String deviceRES, AbstractValueSet abstractSetValue, boolean withSubResources) {
		super(resourceIdentifier);
		setTitle("Resource to sign up for observing a Number value");
		setResourceType("numbertype");
				
		this.resourceIdentifier = resourceIdentifier;
		this.type = type;
		this.deviceROOT = deviceROOT;
		this.deviceRES = deviceRES;
		this.device = deviceROOT + deviceRES;
		
		this.abstractSetValue = abstractSetValue;
		
		typeRepository = new DatabaseRepository<T>((Class<DefaultStorage<T>>) ((Class<? extends DefaultStorage<T>>) DefaultStorage.class), DatabaseConnection.getCouchDbConnector(), device);
		
		addSubResource((allResource = new AllResource<T>("all", type, typeRepository, device, withSubResources)));
		addSubResource((newestResource = new NewestResource("newest", device)));
		addSubResource((lastResource = new LastResource<T>("last", type, typeRepository, device, withSubResources)));
		addSubResource((sinceResource = new SinceResource<T>("since", type, typeRepository, device, withSubResources)));
		addSubResource((ondayResource = new OnDayResource<T>("onday", type, typeRepository, device, withSubResources)));
		addSubResource((timerangeResource = new TimeRangeResource<T>("timerange", type, typeRepository, device, withSubResources)));
		
		List<Default> res = typeRepository.queryDeviceLimit(1, type);
		if (!res.isEmpty())
			newestResource.notifyChanged(res.get(0).getValue(), res.get(0).getDateTime());
	}
	
	/**
	 * Gets the device specified for this specific number resource.
	 *
	 * @return the device
	 */
	public String getDevice() {
		return this.device;
	}
	
	public void startHistory(boolean observing, List<Option> options) {
		if (observing) {
			System.out.println("START OBSERVING: device " + device + " is being observed.");
			observingRequest = new GETRequest();
			observingRequest.setOption(new Option(0, OptionNumberRegistry.OBSERVE));
			if (options != null)
				observingRequest.setOptions(OptionNumberRegistry.URI_QUERY, options);
			observingHandler = new ObservingHandler();
			observingRequest.registerResponseHandler(observingHandler);
			observingRequest.setURI(device);
			
			try {
				observingRequest.execute();
			} catch (IOException e) {
				System.err.println("Exception: " + e.getMessage());
			}
		} else {
			System.out.println("START POLLING: device " + device + " is being polled.");
			timer = new Timer();
			timer.schedule(new PollingTask(options), 0, 5000);
		}
	}
	
	public void stopHistory(boolean observing, List<Option> options) {
		if (observing) {
			System.out.println("STOP OBSERVING: device " + device + " is being observed.");
			observingRequest.unregisterResponseHandler(observingHandler);
			
			observingRequest = new GETRequest();
			if (options != null)
				observingRequest.setOptions(OptionNumberRegistry.URI_QUERY, options);
			observingRequest.registerResponseHandler(observingHandler);
			observingRequest.setURI(device);
			
			try {
				observingRequest.execute();
			} catch (IOException e) {
				System.err.println("Exception: " + e.getMessage());
			}
		} else {
			System.out.println("STOP POLLING: device " + device + " is being polled.");
			if (timer != null)
				timer.cancel();
		}
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
		allResource.notifyChanged(value);
		newestResource.notifyChanged(value, date);
		lastResource.notifyChanged(value);
		sinceResource.notifyChanged(value, date);
		ondayResource.notifyChanged(value, date);
		timerangeResource.notifyChanged(value, date);
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
			System.out.println("OBSERVING: new data (value: " + payload + ") was being pushed from device " + device);
			
			// store in database
			DefaultStorage<T> storageType = new DefaultStorage<T>();
			storageType.setDevice(device);
			abstractSetValue.perform(storageType, payload);
			DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
	        Date date = new Date();
			storageType.setDateTime(dateFormat.format(date));
	
			typeRepository.add(storageType);
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

		private List<Option> options;
		/** The old value. */
		private String oldValue;
		
		/**
		 * Instantiates a new polling task.
		 */
		public PollingTask(List<Option> options) {
			this.oldValue = "";
			this.options = options;
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
			if (options != null)
				getRequest.setOptions(OptionNumberRegistry.URI_QUERY, options);
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
				
				DefaultStorage<T> storageType = new DefaultStorage<T>();
				storageType.setDevice(device);
				abstractSetValue.perform(storageType, payload);
				DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
		        Date date = new Date();
				storageType.setDateTime(dateFormat.format(date));
				
				typeRepository.add(storageType);
				System.out.println("DATABASE: data (value: " + payload + ") was stored for device " + device);
				
				notifyChanged(payload, dateFormat.format(date));
				System.out.println("PUSH NOTIFICATION: notify resources after new data was stored for device " + device);
			}
		}
	}

}