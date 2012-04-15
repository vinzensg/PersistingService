package database.type;

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
		ViewQuery viewQuery = new ViewQuery().designDocId("_design/string").viewName("device").key("20");
		return DBConnector.queryView(viewQuery, NumberType.class);
	}

}
