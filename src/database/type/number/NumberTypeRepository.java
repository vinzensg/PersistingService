package database.type.number;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.CouchDbRepositorySupport;

import config.Constants;

public class NumberTypeRepository extends CouchDbRepositorySupport<NumberType.Default> {
	
	private String deviceName;
	private CouchDbConnector DBConnector;

	public NumberTypeRepository(Class<NumberType.Default> type, CouchDbConnector db, String deviceName) {
		super(type, db);
		
		this.deviceName = deviceName;
		this.DBConnector = db;
	}

	
	public List<NumberType.Default> queryDevice() {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/number").viewName("device").key(deviceName);
		return DBConnector.queryView(viewQuery, NumberType.Default.class);
	}
	
	public List<NumberType.Sum> queryDeviceSum() {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/number").viewName("device_sum").key(deviceName);
		return DBConnector.queryView(viewQuery, NumberType.Sum.class);
	}
	
	public List<NumberType.Avg> queryDeviceAvg() {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/number").viewName("device_avg").key(deviceName);
		return DBConnector.queryView(viewQuery, NumberType.Avg.class);
	}
	
	public List<NumberType.Max> queryDeviceMax() {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/number").viewName("device_max").key(deviceName);
		return DBConnector.queryView(viewQuery, NumberType.Max.class);
	}
	
	public List<NumberType.Min> queryDeviceMin() {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/number").viewName("device_min").key(deviceName);
		return DBConnector.queryView(viewQuery, NumberType.Min.class);
	}
	
	/*
	public List<NumberType> queryDeviceLimit(int limit) {
		System.out.println("Limit: " + limit);
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/number").viewName("device_date").key(deviceName).limit(limit).includeDocs(true).descending(true);
		return DBConnector.queryView(viewQuery, NumberType.class);
	}
	*/
	
	public List<NumberType.Default> queryDeviceSince(String date) {
		ComplexKey keyStart = ComplexKey.of(deviceName, date);
		DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
        Date dateEnd = new Date();
		ComplexKey keyEnd = ComplexKey.of(deviceName, dateFormat.format(dateEnd));
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/number").viewName("device_date").startKey(keyStart).endKey(keyEnd);
		return DBConnector.queryView(viewQuery, NumberType.Default.class);
	}
	
	public List<NumberType.Default> queryDeviceRange(String startDate, String endDate) {
		ComplexKey startKey = ComplexKey.of(deviceName, startDate);
		ComplexKey endKey = ComplexKey.of(deviceName, endDate);
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/number").viewName("device_date").startKey(startKey).endKey(endKey);
		return DBConnector.queryView(viewQuery, NumberType.Default.class);
	}

}
