package database.type.number;

import java.util.List;

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.CouchDbRepositorySupport;

public class NumberTypeRepository extends CouchDbRepositorySupport<NumberType> {
	
	private String deviceName;
	private CouchDbConnector DBConnector;

	public NumberTypeRepository(Class<NumberType> type, CouchDbConnector db, String deviceName) {
		super(type, db);
		
		this.deviceName = deviceName;
		this.DBConnector = db;
	}

	
	public List<NumberType> queryDevice() {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/number").viewName("device").key(deviceName);
		return DBConnector.queryView(viewQuery, NumberType.class);
	}
	
	public List<NumberTypeSum> queryDeviceSum() {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/number").viewName("device_sum").key(deviceName);
		return DBConnector.queryView(viewQuery, NumberTypeSum.class);
	}
	
	public List<NumberTypeAvg> queryDeviceAvg() {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/number").viewName("device_avg").key(deviceName);
		return DBConnector.queryView(viewQuery, NumberTypeAvg.class);
	}

}