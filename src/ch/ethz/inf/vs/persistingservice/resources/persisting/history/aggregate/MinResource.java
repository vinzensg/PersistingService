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

/**
 * The Class MinResource is a resource to retrieve the min of a collection of documents belonging to a source device.
 *
 * @param <T> the generic type of the data
 */
public class MinResource<T extends Comparable> extends LocalResource {
	
	/** The type. */
	private String type;
	
	/** The type repository. */
	private DatabaseRepository<T> databaseRepository;
	
	/** The device. */
	private String device;
	
	/** The abstract time resource. */
	private AbstractTimeResource abstractTimeResource;
	
	/** The min. */
	private String min;


	/**
	 * Instantiates a new min resource and makes it observable.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param type the type
	 * @param databaseRepository the type repository
	 * @param device the device
	 * @param abstractTimeResource the abstract time resource is the parent resource of this resource
	 */
	public MinResource(String resourceIdentifier, String type, DatabaseRepository<T> databaseRepository, String device, AbstractTimeResource abstractTimeResource) {
		super(resourceIdentifier);
		isObservable(true);
		
		this.type = type;
		this.databaseRepository = databaseRepository;
		this.device = device;
		this.abstractTimeResource = abstractTimeResource;
		
		min = ""+Float.MAX_VALUE;
	}
	
	/**
	 * performGET responds with the min.
	 *
	 * @param request the request
	 */
	public void performGET(GETRequest request) {
		System.out.println("GET MIN: get request for device " + device);
		request.prettyPrint();
		
		abstractTimeResource.acceptGetRequest(request, new MinQuery());
	}
	
	/**
	 * The Class MinQuery accesses the database and returns the retrieved data.
	 * <p>
	 * The data retrieval depends on the type of parent resource.
	 */
	private class MinQuery extends AbstractQuery {
		
		/**
		 * perform retrieves the data from the database depending on the parent resource and returns it.
		 *
		 * @param parsedOptions the parsed options
		 * @param timeResID the time res id is used to decide, which min has to be retrieved from the database.
		 * @param params the params
		 * @return the string is the data retrieved from the database
		 */
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {		
			String ret = "";
			switch(timeResID) {
				case NEWEST:
				case ALL:
					List<Min> resSum = databaseRepository.queryDeviceMin(type);
					if (!resSum.isEmpty())
						ret = "" + resSum.get(0).getMin();
					break;
				case SINCE:
					List<DateMin> resSinceMin = databaseRepository.queryDeviceSinceMin(params[0], type);
					if (!resSinceMin.isEmpty())
						ret = "" + resSinceMin.get(0).getMin();
					break;
				case ONDAY:
				case TIMERANGE:
					List<DateMin> resDateMin = databaseRepository.queryDeviceRangeMin(params[0], params[1], type);
					if (!resDateMin.isEmpty())
						ret = "" + resDateMin.get(0).getMin();
					break;
				case LAST:
					List<Default> resLimit = databaseRepository.queryDeviceLimit(Integer.parseInt(params[0]), type);
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
