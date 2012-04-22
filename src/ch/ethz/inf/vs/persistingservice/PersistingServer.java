package ch.ethz.inf.vs.persistingservice;

import java.net.SocketException;


import ch.ethz.inf.vs.californium.endpoint.LocalEndpoint;
import ch.ethz.inf.vs.persistingservice.database.DatabaseConnection;
import ch.ethz.inf.vs.persistingservice.resources.PersistingResource;

public class PersistingServer extends LocalEndpoint {
	
	public static final int ERR_INIT_FAILED = 1;
	
	public PersistingServer() throws SocketException {
		addResource(new PersistingResource("persistingservice"));
	}
	
	public static void main(String[] args) {
		try {
			PersistingServer DBServer = new PersistingServer();
			System.out.println("Database Server listening on port: " + DBServer.port());
			
			DatabaseConnection DBConnection = new DatabaseConnection();
			DBConnection.startDB();
			System.out.println("Database Connection was established on port: " + DBConnection.PORT);
		} catch (SocketException e) {
			System.err.println("Failed to initialize Database Server");
			System.err.println("CLASS: DatabaseServer");
			System.exit(ERR_INIT_FAILED);
		}
	}
	
}