package ch.ethz.inf.vs.persistingservice.database.documents;

import org.ektorp.support.CouchDbDocument;

public class DefaultStorage<T extends Comparable> extends CouchDbDocument {

	/** The device. */
	private String device;
	
	/** The number value. */
	private T value;
	
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
	public T getValue() {
		return value;
	}
	
	/**
	 * Sets the number value.
	 *
	 * @param numberValue the new number value to be stored in the database.
	 */
	public void setValue(T value) {
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
