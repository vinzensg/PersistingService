package ch.ethz.inf.vs.persistingservice.database;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;
import org.ektorp.support.DesignDocument;

public class DatabaseConnection {
	
	final public static int PORT = 5984;
	
	private CouchDbInstance dbInstance;
	private static CouchDbConnector db;
			
	public void startDB() {
		HttpClient httpClient = new StdHttpClient.Builder()
		.host("localhost")
		.port(5984)
		.build();
				
		dbInstance = new StdCouchDbInstance(httpClient);
		db = new StdCouchDbConnector("californiumdb", dbInstance);
	}
	
	public static CouchDbConnector getCouchDbConnector() {
		return db;
	}
	
}
