package ch.ethz.inf.vs.persistingservice.database.documents;

import org.ektorp.support.CouchDbDocument;

public class Min extends CouchDbDocument {
	
	/** The device. */
	private String device;
	
	/** The min. */
	private String min;
	
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
	 * Gets the min.
	 *
	 * @return the min
	 */
	public String getMin() {
		return min;
	}
	
	/**
	 * Sets the min.
	 *
	 * @param min the new min
	 */
	public void setMin(String min) {
		this.min = min;
	}
	
	public float getNumberMin() {
		return Float.valueOf(this.min);
	}

}
