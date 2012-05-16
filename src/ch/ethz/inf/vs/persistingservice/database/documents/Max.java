package ch.ethz.inf.vs.persistingservice.database.documents;

import org.ektorp.support.CouchDbDocument;

public class Max extends CouchDbDocument {

	/** The device. */
	private String device;
	
	/** The max. */
	private String max;
	
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
	 * @param device the new device
	 */
	public void setDevice(String device) {
		this.device = device;
	}
	
	/**
	 * Gets the max.
	 *
	 * @return the max
	 */
	public String getMax() {
		return max;
	}
	
	public float getNumberMax() {
		return Float.valueOf(this.max);
	}
	
	/**
	 * Sets the max.
	 *
	 * @param max the new max
	 */
	public void setMax(String max) {
		this.max = max;
	}
	
}
