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
package ch.ethz.inf.vs.persistingservice.database;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.CouchDbRepositorySupport;

import ch.ethz.inf.vs.persistingservice.config.Constants;


/**
 * The Class StringTypeRepository contains the methods to connect to the couchdb
 * database and store and retrieve data from it.
 * <p>
 * It contains the standard methods of a CouchDbRepository (i.e. add, contains,
 * remove and update) and defines more methods to retrieve data from the
 * database.
 */
public class StringTypeRepository extends CouchDbRepositorySupport<StringType.Default> {

	/** The target device name. */
	private String deviceName;

	private CouchDbConnector DBConnector;

	/**
	 * Instantiates a new string type repository inheriting the standard methods
	 * add, contains, remove and update.
	 * 
	 * @param type
	 *            the type defines the default type for the standard methods.
	 * @param db
	 *            the db is used to connect to the couchdb database and perform
	 *            the queries.
	 * @param deviceName
	 *            the device name is stored to retrieve the documents containing
	 *            data for the specified device.
	 */
	public StringTypeRepository(Class<StringType.Default> type,
			CouchDbConnector db, String deviceName) {
		super(type, db);
		this.deviceName = deviceName;
		this.DBConnector = db;
	}

	/**
	 * Query device performs the standard query to get all documents for the
	 * device.
	 * 
	 * @return the list a list containing all the documents for the specified
	 *         device is returned with the format of {@link StringType.Default}.
	 */
	public List<StringType.Default> queryDevice() {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/string")
				.viewName("device").key(deviceName);
		return DBConnector.queryView(viewQuery, StringType.Default.class);
	}

	/**
	 * Query device since performs a query, which returns all documents created
	 * after some specified date for the device.
	 * 
	 * @param date
	 *            the date
	 * @return the list a list containing all the documents for the specified
	 *         device is returned with the format of {@link StringType.Default}.
	 */
	public List<StringType.Default> queryDeviceSince(String date) {
		ComplexKey keyStart = ComplexKey.of(deviceName, date);
		DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
		Date dateEnd = new Date();
		ComplexKey keyEnd = ComplexKey.of(deviceName,
				dateFormat.format(dateEnd));
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/string")
				.viewName("device_date").startKey(keyStart).endKey(keyEnd);
		return DBConnector.queryView(viewQuery, StringType.Default.class);
	}

	/**
	 * Query device range performs a query, which returns all documents created
	 * within some specified time range for the device.
	 * 
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @return the list a list containing all the documents for the specified
	 *         device is returned with the format of {@link StringType.Default}.
	 */
	public List<StringType.Default> queryDeviceRange(String startDate,
			String endDate) {
		ComplexKey startKey = ComplexKey.of(deviceName, startDate);
		ComplexKey endKey = ComplexKey.of(deviceName, endDate);
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/string")
				.viewName("device_date").startKey(startKey).endKey(endKey);
		return DBConnector.queryView(viewQuery, StringType.Default.class);
	}
	
	public List<StringType.Default> queryDeviceLimit(int limit) {
		DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
		Date date = new Date();
		ComplexKey keyEnd = ComplexKey.of(deviceName, dateFormat.format(date));
		ComplexKey keyStart = ComplexKey.of(deviceName, "2012/03/01-00:00:00");
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/string").viewName("device_date").startKey(keyEnd).endKey(keyStart).limit(limit).descending(true);
		return DBConnector.queryView(viewQuery, StringType.Default.class);
	}
	
}
