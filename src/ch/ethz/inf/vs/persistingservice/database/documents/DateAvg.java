package ch.ethz.inf.vs.persistingservice.database.documents;

import org.ektorp.support.CouchDbDocument;

public class DateAvg extends CouchDbDocument {

	/** The device. */
	private String[] device;
	
	/** The count. */
	private int count;
	
	/** The avg. */
	private String avg;

	/**
	 * Gets the device.
	 *
	 * @return the device
	 */
	public String[] getDevice() {
		return device;
	}

	/**
	 * Sets the device.
	 *
	 * @param device the new device
	 */
	public void setDevice(String[] device) {
		this.device = device;
	}

	/**
	 * Gets the count.
	 *
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Sets the count.
	 *
	 * @param count the new count
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * Gets the avg.
	 *
	 * @return the avg
	 */
	public String getAvg() {
		return avg;
	}
	
	public float getNumberAvg() {
		return Float.valueOf(this.avg);
	}

	/**
	 * Sets the avg.
	 *
	 * @param avg the new avg
	 */
	public void setAvg(String avg) {
		this.avg = avg;
	}
	
}
