package ch.ethz.inf.vs.persistingservice.database.documents;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.ektorp.support.CouchDbDocument;

public class Default extends CouchDbDocument {

	/** The device. */
	private String device;
	
	/** The number value. */
	private String value;
	
	/** The date time. */
	private String dateTime;
	
	/**
	 * Gets the device.
	 *
	 * @return the device
	 */
	public String getDevice() {
		return device;
	}
	
	/**
	 * Sets the device.
	 *
	 * @param device the new device to be stored in the database.
	 */
	public void setDevice(String device) {
		this.device = device;
	}
	
	/**
	 * Gets the number value.
	 *
	 * @return the number value
	 */
	public String getValue() {
		return value;
	}
	
	public float getNumberValue() {
		return Float.valueOf(this.value);
	}
	
	/**
	 * Sets the number value.
	 *
	 * @param numberValue the new number value to be stored in the database.
	 */
	public void setValue(String value) {
		this.value = value;
	}
	
	/**
	 * Gets the date time.
	 *
	 * @return the date time
	 */
	public String getDateTime() {
		return dateTime;
	}
	
	/**
	 * Sets the date time.
	 *
	 * @param dateTime the new date time to be stored in the database.
	 */
	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}
	
}
