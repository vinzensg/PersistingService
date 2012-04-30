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
package ch.ethz.inf.vs.persistingservice;

import java.net.SocketException;

import ch.ethz.inf.vs.californium.endpoint.LocalEndpoint;
import ch.ethz.inf.vs.persistingservice.database.DatabaseConnection;
import ch.ethz.inf.vs.persistingservice.resources.PersistingResource;

/**
 * The Class PersistingServer starts the persisting server and creates and starts a couchdb connection.
 * <p>
 * The persisting server port can be defined in the Califorinium.properties file.
 * The couchdb connection uses couchdb's standard port of 5984.
 */
public class PersistingServer extends LocalEndpoint {
	
	public static final int ERR_INIT_FAILED = 1;
	
	/**
	 * Instantiates a new persisting server and creates the topmost resource: persistingservice
	 *
	 * @throws SocketException the socket exception
	 */
	public PersistingServer() throws SocketException {
		addResource(new PersistingResource("persistingservice"));
	}
	
	/**
	 * The main method creates a new persitingservice object and starts the persisting service server.
	 * It also establishes a connection to the couchdb database and starts the http client to access the database.
	 *
	 * @param args the arguments
	 */
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
