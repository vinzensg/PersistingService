package database.type.string;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.ektorp.support.CouchDbRepositorySupport;

import config.Constants;
import database.type.number.NumberType;

public class StringTypeRepository extends CouchDbRepositorySupport<StringType.Default> {
	
	private String deviceName;
	private CouchDbConnector DBConnector;

	public StringTypeRepository(Class<StringType.Default> type, CouchDbConnector db, String deviceName) {
		super(type, db);
		
		this.deviceName = deviceName;
		this.DBConnector = db;
	}

	
	public List<StringType.Default> queryDevice() {
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/string").viewName("device").key(deviceName);
		return DBConnector.queryView(viewQuery, StringType.Default.class);
	}
	
	public List<StringType.Default> queryDeviceSince(String date) {
		ComplexKey keyStart = ComplexKey.of(deviceName, date);
		DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
        Date dateEnd = new Date();
		ComplexKey keyEnd = ComplexKey.of(deviceName, dateFormat.format(dateEnd));
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/string").viewName("device_date").startKey(keyStart).endKey(keyEnd);
		return DBConnector.queryView(viewQuery, StringType.Default.class);
	}
	
	public List<StringType.Default> queryDeviceRange(String startDate, String endDate) {
		ComplexKey startKey = ComplexKey.of(deviceName, startDate);
		ComplexKey endKey = ComplexKey.of(deviceName, endDate);
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/string").viewName("device_date").startKey(startKey).endKey(endKey);
		return DBConnector.queryView(viewQuery, StringType.Default.class);
	}
	
}
