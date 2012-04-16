package database.type.string;

import java.util.List;

import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.CouchDbRepositorySupport;

public class StringTypeRepository extends CouchDbRepositorySupport<StringType> {
	
	private String deviceName;
	private CouchDbConnector DBConnector;

	public StringTypeRepository(Class<StringType> type, CouchDbConnector db, String deviceName) {
		super(type, db);
		
		this.deviceName = deviceName;
		this.DBConnector = db;
	}

	
	public List<StringType> queryDevice() {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/string").viewName("device").key(deviceName);
		return DBConnector.queryView(viewQuery, StringType.class);
	}
	
	public List<StringType> queryDeviceStartDate(String date) {
		ComplexKey ck = ComplexKey.of(deviceName, date);
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/string").viewName("device_datetime").startKey(ck);
		return DBConnector.queryView(viewQuery, StringType.class);
	}
	
}
