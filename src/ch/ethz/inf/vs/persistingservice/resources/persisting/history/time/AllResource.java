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
package ch.ethz.inf.vs.persistingservice.resources.persisting.history.time;

import java.util.List;

import ch.ethz.inf.vs.californium.coap.CodeRegistry;
import ch.ethz.inf.vs.californium.coap.GETRequest;
import ch.ethz.inf.vs.californium.coap.Option;
import ch.ethz.inf.vs.californium.coap.OptionNumberRegistry;
import ch.ethz.inf.vs.persistingservice.database.DatabaseRepository;
import ch.ethz.inf.vs.persistingservice.database.documents.*;
import ch.ethz.inf.vs.persistingservice.parser.OptionParser;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.AvgResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.MaxResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.MinResource;
import ch.ethz.inf.vs.persistingservice.resources.persisting.history.aggregate.SumResource;

/**
 * The Class AllResource is observable and can be used to retrieve all documents for some source device.
 * <p>
 * Depending on the data type, additional subresources are added to retrieve aggregated values from the database.
 * 
 * Resource Tree:
 *	/all
 *	|...(/avg)
 *	|...(/max)
 *	|...(/min)
 *	|...(/sum)
 *
 * @param <T> the generic type
 */
public class AllResource<T extends Comparable> extends AbstractTimeResource {
	
	/** The sum resource. */
	private SumResource<T> sumResource;
	
	/** The avg resource. */
	private AvgResource<T> avgResource;
	
	/** The max resource. */
	private MaxResource<T> maxResource;
	
	/** The min resource. */
	private MinResource<T> minResource;
	
	/** The type. */
	private String type;
	
	/** The type repository. */
	private DatabaseRepository<T> typeRepository;	
	
	/** The device. */
	private String device;
	
	/** The with sub resources. */
	private boolean withSubResources;
	
	/**
	 * Instantiates a new all resource and makes it observable. Subresources are added if necessary.
	 *
	 * @param resourceIdentifier the resource identifier
	 * @param type the type
	 * @param databaseRepository the database repository is used to retrieve data from the database.
	 * @param device the device
	 * @param withSubResources the with sub resources adds the aggregation sub resources if true.
	 */
	public AllResource(String resourceIdentifier, String type, DatabaseRepository<T> databaseRepository, String device, boolean withSubResources) {
		super(resourceIdentifier);
		isObservable(true);
		
		this.type = type;
		this.typeRepository = databaseRepository;
		this.device = device;
		this.withSubResources = withSubResources;
		
		if (withSubResources) {
			add((sumResource = new SumResource<T>("sum", type, databaseRepository, device, this)));
			add((avgResource = new AvgResource<T>("avg", type, databaseRepository, device, this)));
			add((maxResource = new MaxResource<T>("max", type, databaseRepository, device, this)));
			add((minResource = new MinResource<T>("min", type, databaseRepository, device, this)));
		}
	}
	
	/**
	 * perform GET queries the database for all documents of this device and
	 * responds with their values.
	 *
	 * @param request the request
	 */
	public void performGET(GETRequest request) {		
		System.out.println("GET ALL: get request for device " + device);
		request.prettyPrint();

		acceptGetRequest(request, new AllQuery());
	}
	
	/**
	 * Accept get request reacts to the get request.
	 * 
	 * @param request
	 * 			the request is the get request received.
	 * @param query
	 * 			the query is a container for a method, which defines the mechanism to retrieve data from the database. 
	 */
	public void acceptGetRequest(GETRequest request, AbstractQuery query) {
		List<Option> options = request.getOptions(OptionNumberRegistry.URI_QUERY);
		OptionParser parsedOptions = new OptionParser(options);
		
		String ret = "";
		ret += query.perform(parsedOptions, AbstractQuery.ALL);
		
		System.out.println("GETRequst ALL: (value: " + ret.substring(0, Math.min(50, ret.length())) + ") for device " + device);
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
	
	/**
	 * The Class AllQuery accesses the database and returns the retrieved data.
	 */
	private class AllQuery extends AbstractQuery {
		
		/**
		 * perform retrieves the data from the database depending on the parent resource and returns it.
		 * <p>
		 * The values can also be retrieved with the date they where stored.
		 */
		public String perform(OptionParser parsedOptions, int timeResID, String...params) {
			String ret = "";
			List<Default> res = typeRepository.queryDevice(type);
			
			boolean withDate = false;
			if (parsedOptions.containsLabel("withdate"))
				withDate = parsedOptions.getBooleanValue("withdate");
			if (withDate) {
				for (Default nt : res) {
					ret += nt.getNumberValue() + ";" + nt.getDateTime() + "\n";
				}
			} else {
				for (Default nt : res) {
					ret += nt.getNumberValue() + "\n";
				}
			}
			return ret;
		}
	}
	
	/**
	 * Notify changed and pass the notification to the subresources.
	 * 
	 * @param value
	 *            the new value
	 */
	public void notifyChanged(String value) {
		changed();
				
		if (withSubResources) {
			sumResource.notifyChanged(value);
			avgResource.notifyChanged(value);
			maxResource.notifyChanged(value);
			minResource.notifyChanged(value);
		}
	}
}
