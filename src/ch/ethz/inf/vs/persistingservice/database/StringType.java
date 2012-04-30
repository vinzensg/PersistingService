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

import org.ektorp.support.CouchDbDocument;

/**
 * The Class StringType defines the document formats for the string value documents stored in the couchdb database.
 */
public class StringType extends CouchDbDocument {
	
	/**
	 * The Class Default defines the default format for a string value document stored in the couchdb database.
	 */
	public static class Default extends CouchDbDocument {
		
		/** The device. */
		private String device;
		
		/** The string value. */
		private String stringValue;
		
		/** The date time. */
		private String dateTime;
		
		/**
		 * Sets the device.
		 *
		 * @param device the new device to be stored in the database.
		 */
		public void setDevice(String device) {
			this.device = device;
		}
		
		/**
		 * Gets the device.
		 *
		 * @return the device
		 */
		public String getDevice() {
			System.out.println("Device: " + device);
			return this.device;
		}
		
		/**
		 * Sets the string value.
		 *
		 * @param stringValue the new string value to be stored in the database.
		 */
		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}
		
		/**
		 * Gets the string value.
		 *
		 * @return the string value
		 */
		public String getStringValue() {
			return this.stringValue;
		}
		
		/**
		 * Sets the date time.
		 *
		 * @param dateTime the new date time to be stored in the database.
		 */
		public void setDateTime(String dateTime) {
			this.dateTime = dateTime;
		}
	
		/**
		 * Gets the date time.
		 *
		 * @return the date time
		 */
		public String getDateTime() {
			return this.dateTime;
		}
	}
}
