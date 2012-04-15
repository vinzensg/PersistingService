package database.type.number;

import org.ektorp.support.CouchDbDocument;

public class NumberType extends CouchDbDocument {
	
	private String device;
	private int numberValue;
	private String dateTime;
	
	public String getDevice() {
		return device;
	}
	public void setDevice(String device) {
		this.device = device;
	}
	public int getNumberValue() {
		return numberValue;
	}
	public void setNumberValue(int numberValue) {
		this.numberValue = numberValue;
	}
	public String getDateTime() {
		return dateTime;
	}
	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}

}
