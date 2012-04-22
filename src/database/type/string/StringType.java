package database.type.string;

import java.util.Date;

import org.ektorp.support.CouchDbDocument;

public class StringType extends CouchDbDocument {
	
	public static class Default extends CouchDbDocument {
		private String device;
		private String stringValue;
		private String dateTime;
		
		public void setDevice(String device) {
			this.device = device;
		}
		
		public String getDevice() {
			System.out.println("Device: " + device);
			return this.device;
		}
		
		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}
		
		public String getStringValue() {
			return this.stringValue;
		}
		
		public void setDateTime(String dateTime) {
			this.dateTime = dateTime;
		}
	
		public String getDateTime() {
			return this.dateTime;
		}
	}
}
