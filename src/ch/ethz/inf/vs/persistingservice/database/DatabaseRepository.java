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
import ch.ethz.inf.vs.persistingservice.database.documents.*;

/**
 * The Class NumberTypeRepository contains the methods to connect to the couchdb
 * database and store and retrieve data from it.
 * <p>
 * It contains the standard methods of a CouchDbRepository (i.e. add, contains,
 * remove and update) and defines more methods to retrieve data from the
 * database.
 */
public class DatabaseRepository<T extends Comparable> extends CouchDbRepositorySupport<DefaultStorage<T>> {
	
	/** The target device name. */
	private String deviceName;
	
	private CouchDbConnector DBConnector;

	/**
	 * Instantiates a new number type repository inheriting the standard methods
	 * add, contains, remove and update.
	 * 
	 * @param class1
	 *            the type defines the default type for the standard methods.
	 * @param db
	 *            the db is used to connect to the couchdb database and perform
	 *            the queries.
	 * @param deviceName
	 *            the device name is stored to retrieve the documents containing
	 *            data for the specified device.
	 */
	public DatabaseRepository(Class<DefaultStorage<T>> class1, CouchDbConnector db, String deviceName) {
		super(class1, db);
		
		this.deviceName = deviceName;
		this.DBConnector = db;
	}

	
	/**
	 * Query device performs the standard query to get all documents for the
	 * device.
	 * 
	 * @return the list contains all the documents for the specified
	 *         device is returned with the format of {@link NumberType.Default}.
	 */
	public List<Default> queryDevice(String type) {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device").key(deviceName);
		return DBConnector.queryView(viewQuery, Default.class);
	}
	
	/**
	 * Query device sum performs a query to get the sum of all documents for the
	 * device.
	 * 
	 * @return the list contains only one document for the specified
	 *         device, which contains the sum and is returned with the format of {@link NumberType.Sum}.
	 */
	public List<Sum> queryDeviceSum(String type) {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_sum").key(deviceName);
		return DBConnector.queryView(viewQuery, Sum.class);
	}

	/**
	 * Query device avg performs a query to get the avg of all documents for the
	 * device.
	 * 
	 * @return the list contains only one document for the specified
	 *         device, which contains the avg and is returned with the format of {@link NumberType.Avg}.
	 */
	public List<Avg> queryDeviceAvg(String type) {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_avg").key(deviceName);
		return DBConnector.queryView(viewQuery, Avg.class);
	}

	/**
	 * Query device max performs a query to get the max of all documents for the
	 * device.
	 * 
	 * @return the list contains only one document for the specified
	 *         device, which contains the max and is returned with the format of {@link NumberType.Max}.
	 */
	public List<Max> queryDeviceMax(String type) {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_max").key(deviceName);
		return DBConnector.queryView(viewQuery, Max.class);
	}

	/**
	 * Query device min performs a query to get the min of all documents for the
	 * device.
	 * 
	 * @return the list contains only one document for the specified
	 *         device, which contains the min and is returned with the format of {@link NumberType.Min}.
	 */
	public List<Min> queryDeviceMin(String type) {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_min").key(deviceName);
		return DBConnector.queryView(viewQuery, Min.class);
	}
	
	
	/**
	 * Query device limit performs a query, which returns a defined number of
	 * document for the device.
	 * 
	 * @param limit
	 *            the limit defines the number of documents to be returned.
	 * @return the list contains all the documents for the specified device,
	 *         which is returned with the format of {@link NumberType.Default}.
	 */
	public List<Default> queryDeviceLimit(int limit, String type) {
		DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
		Date date = new Date();
		ComplexKey keyEnd = ComplexKey.of(deviceName, dateFormat.format(date));
		ComplexKey keyStart = ComplexKey.of(deviceName, "2012/03/01-00:00:00");
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_date").startKey(keyEnd).endKey(keyStart).limit(limit).descending(true);
		return DBConnector.queryView(viewQuery, Default.class);
	}
	
	/**
	 * Query device since performs a query, which returns all documents created
	 * after some specified date for the device.
	 * 
	 * @param date
	 *            the date
	 * @return the list contains all the documents for the specified
	 *         device, which is returned with the format of {@link NumberType.Default}.
	 */
	public List<Default> queryDeviceSince(String date, String type) {
		ComplexKey keyStart = ComplexKey.of(deviceName, date);
		DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
        Date dateEnd = new Date();
		ComplexKey keyEnd = ComplexKey.of(deviceName, dateFormat.format(dateEnd));
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_date").startKey(keyStart).endKey(keyEnd);
		return DBConnector.queryView(viewQuery, Default.class);
	}
	
	/**
	 * Query device since sum performs a query to get the sum of all documents
	 * created after some specified date for the device.
	 * 
	 * @param date
	 *            the date
	 * @return the list contains only one document for the specified device,
	 *         which contains the sum and is returned in the format
	 *         {@link NumberType.DateSum}.
	 */
	public List<DateSum> queryDeviceSinceSum(String date, String type) {
		ComplexKey keyStart = ComplexKey.of(deviceName, date);
		DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
		Date dateEnd = new Date();
		ComplexKey keyEnd = ComplexKey.of(deviceName, dateFormat.format(dateEnd));
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_date_sum").groupLevel(1).startKey(keyStart).endKey(keyEnd);
		return DBConnector.queryView(viewQuery, DateSum.class);
	}
	
	/**
	 * Query device since avg performs a query to get the avg of all documents
	 * created after some specified date for the device.
	 * 
	 * @param date
	 *            the date
	 * @return the list contains only one document for the specified device,
	 *         which contains the avg and is returned in the format
	 *         {@link NumberType.DateAvg}.
	 */
	public List<DateAvg> queryDeviceSinceAvg(String date, String type) {
		ComplexKey keyStart = ComplexKey.of(deviceName, date);
		DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
		Date dateEnd = new Date();
		ComplexKey keyEnd = ComplexKey.of(deviceName, dateFormat.format(dateEnd));
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_date_avg").groupLevel(1).startKey(keyStart).endKey(keyEnd);
		return DBConnector.queryView(viewQuery, DateAvg.class);
	}
	
	/**
	 * Query device since max performs a query to get the max of all documents
	 * created after some specified date for the device.
	 * 
	 * @param date
	 *            the date
	 * @return the list contains only one document for the specified device,
	 *         which contains the max and is returned in the format
	 *         {@link NumberType.DateMax}.
	 */
	public List<DateMax> queryDeviceSinceMax(String date, String type) {
		ComplexKey keyStart = ComplexKey.of(deviceName, date);
		DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
		Date dateEnd = new Date();
		ComplexKey keyEnd = ComplexKey.of(deviceName, dateFormat.format(dateEnd));
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_date_max").groupLevel(1).startKey(keyStart).endKey(keyEnd);
		return DBConnector.queryView(viewQuery, DateMax.class);
	}
	
	/**
	 * Query device since min performs a query to get the min of all documents
	 * created after some specified date for the device.
	 * 
	 * @param date
	 *            the date
	 * @return the list contains only one document for the specified device,
	 *         which contains the min and is returned in the format
	 *         {@link NumberType.DateMin}.
	 */
	public List<DateMin> queryDeviceSinceMin(String date, String type) {
		ComplexKey keyStart = ComplexKey.of(deviceName, date);
		DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
		Date dateEnd = new Date();
		ComplexKey keyEnd = ComplexKey.of(deviceName, dateFormat.format(dateEnd));
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_date_min").groupLevel(1).startKey(keyStart).endKey(keyEnd);
		return (List<DateMin>) DBConnector.queryView(viewQuery, DateMin.class);
	}
	
	/**
	 * Query device range performs a query, which returns all documents created
	 * within some specified time range for the device.
	 * 
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @return the list contains all the documents for the specified
	 *         device, which is returned with the format of {@link NumberType.Default}.
	 */
	public List<Default> queryDeviceRange(String startDate, String endDate, String type) {
		ComplexKey startKey = ComplexKey.of(deviceName, startDate);
		ComplexKey endKey = ComplexKey.of(deviceName, endDate);
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_date").startKey(startKey).endKey(endKey);
		return DBConnector.queryView(viewQuery, Default.class);
	}
	
	/**
	 * Query device range sum performs a query to get the sum of all documents
	 * created within some specified time range for the device.
	 * 
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @return the list contains only one document, which contains the sum and
	 *         is returned in the format of {@link NumberType.DateSum}.
	 */
	public List<DateSum> queryDeviceRangeSum(String startDate, String endDate, String type) {
		ComplexKey startKey = ComplexKey.of(deviceName, startDate);
		ComplexKey endKey = ComplexKey.of(deviceName, endDate);
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_date_sum").groupLevel(1).startKey(startKey).endKey(endKey);
		return (List<DateSum>) DBConnector.queryView(viewQuery, DateSum.class);
	}

	/**
	 * Query device range avg performs a query to get the avg of all documents
	 * created within some specified time range for the device.
	 * 
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @return the list contains only one document, which contains the avg and
	 *         is returned in the format of {@link NumberType.DateAvg}.
	 */
	public List<DateAvg> queryDeviceRangeAvg(String startDate, String endDate, String type) {
		ComplexKey startKey = ComplexKey.of(deviceName, startDate);
		ComplexKey endKey = ComplexKey.of(deviceName, endDate);
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_date_avg").groupLevel(1).startKey(startKey).endKey(endKey);
		return (List<DateAvg>) DBConnector.queryView(viewQuery, DateAvg.class);
	}
	
	/**
	 * Query device range max performs a query to get the max of all documents
	 * created within some specified time range for the device.
	 * 
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @return the list contains only one document, which contains the max and
	 *         is returned in the format of {@link NumberType.DateMax}.
	 */
	public List<DateMax> queryDeviceRangeMax(String startDate, String endDate, String type) {
		ComplexKey startKey = ComplexKey.of(deviceName, startDate);
		ComplexKey endKey = ComplexKey.of(deviceName, endDate);
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_date_max").groupLevel(1).startKey(startKey).endKey(endKey);
		return (List<DateMax>) DBConnector.queryView(viewQuery, DateMax.class);
	}
	
	/**
	 * Query device range min performs a query to get the min of all documents
	 * created within some specified time range for the device.
	 * 
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @return the list contains only one document, which contains the min and
	 *         is returned in the format of {@link NumberType.DateMin}.
	 */
	public List<DateMin> queryDeviceRangeMin(String startDate, String endDate, String type) {
		ComplexKey startKey = ComplexKey.of(deviceName, startDate);
		ComplexKey endKey = ComplexKey.of(deviceName, endDate);
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/" + type).viewName("device_date_min").groupLevel(1).startKey(startKey).endKey(endKey);
		return (List<DateMin>) DBConnector.queryView(viewQuery, DateMin.class);
	}
}
