package ch.ethz.inf.vs.persistingservice.database.documents;

import org.ektorp.support.CouchDbDocument;

public class DateSum extends CouchDbDocument {

	/** The device stores both the device and the date in an array */
	private String[] device;
	
	/** The sum. */
	private String sum;

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
	 * Gets the sum.
	 *
	 * @return the sum
	 */
	public String getSum() {
		return sum;
	}
	
	public float getNumberSum() {
		return Float.valueOf(this.sum);
	}

	/**
	 * Sets the sum.
	 *
	 * @param sum the new sum
	 */
	public void setSum(String sum) {
		this.sum = sum;
	}

}
