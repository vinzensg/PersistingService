package ch.ethz.inf.vs.persistingservice.database;

import org.ektorp.support.CouchDbDocument;

/**
 * The Class NumberType defines the document formats for the number value documents stored in the couchdb database.
 */
public class NumberType {
	
	/**
	 * The Class Default defines the default format for a number value document
	 * stored in the couchdb database.
	 * <p>
	 * It is both used to store and retrieve data from the database.
	 */
	public static class Default extends CouchDbDocument {
	
		/** The device. */
		private String device;
		
		/** The number value. */
		private float numberValue;
		
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
		public float getNumberValue() {
			return numberValue;
		}
		
		/**
		 * Sets the number value.
		 *
		 * @param numberValue the new number value to be stored in the database.
		 */
		public void setNumberValue(float numberValue) {
			this.numberValue = numberValue;
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
	
	/**
	 * The Class Sum defines the sum format of the documents in couchdb.
	 * <p>
	 * It is used to retrieve the a sum value from the database.
	 */
	public static class Sum extends CouchDbDocument {
		
		/** The device. */
		private String device;
		
		/** The sum. */
		private float sum;
		
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
		 * Gets the sum.
		 *
		 * @return the sum
		 */
		public float getSum() {
			return sum;
		}
		
		/**
		 * Sets the sum.
		 *
		 * @param sum the new sum
		 */
		public void setSum(float sum) {
			this.sum = sum;
		}

	}
	
	/**
	 * The Class DateSum defines the datesum format of the documents in couchdb.
	 * <p>
	 * It is used to retrieve the a sum value combined with a defined date from
	 * the database.
	 */
	public static class DateSum extends CouchDbDocument {
		
		
		/** The device stores both the device and the date in an array */
		private String[] device;
		
		/** The sum. */
		private float sum;

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
		public float getSum() {
			return sum;
		}

		/**
		 * Sets the sum.
		 *
		 * @param sum the new sum
		 */
		public void setSum(float sum) {
			this.sum = sum;
		}
		
	}
	
	/**
	 * The Class Avg defines the avg format of the documents in couchdb.
	 * <p>
	 * It is used to retrieve the a avg value from the database.
	 */
	public static class Avg extends CouchDbDocument {
		
		/** The device. */
		private String device;
		
		/** The count. */
		private int count;
		
		/** The avg. */
		private float avg;
		
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
		public float getAvg() {
			return avg;
		}
		
		/**
		 * Sets the avg.
		 *
		 * @param avg the new avg
		 */
		public void setAvg(float avg) {
			this.avg = avg;
		}
		
	}
	
	/**
	 * The Class DateAvg defines the dateavg format of the documents in couchdb.
	 * <p>
	 * It is used to retrieve the a avg value combined with a defined date from
	 * the database.
	 */
	public static class DateAvg extends CouchDbDocument {
		
		/** The device. */
		private String[] device;
		
		/** The count. */
		private int count;
		
		/** The avg. */
		private float avg;

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
		public float getAvg() {
			return avg;
		}

		/**
		 * Sets the avg.
		 *
		 * @param avg the new avg
		 */
		public void setAvg(float avg) {
			this.avg = avg;
		}
		
	}
	
	/**
	 * The Class Max defines the sum format of the documents in couchdb.
	 * <p>
	 * It is used to retrieve the a max value from the database.
	 */
	public static class Max extends CouchDbDocument {

		/** The device. */
		private String device;
		
		/** The max. */
		private float max;
		
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
		public float getMax() {
			return max;
		}
		
		/**
		 * Sets the max.
		 *
		 * @param max the new max
		 */
		public void setMax(float max) {
			this.max = max;
		}
		
	}
	
	/**
	 * The Class DateMax defines the datemax format of the documents in couchdb.
	 * <p>
	 * It is used to retrieve the a max value combined with a defined date from
	 * the database.
	 */
	public static class DateMax extends CouchDbDocument {
		
		/** The device. */
		private String[] device;
		
		/** The max. */
		private float max;

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
		 * Gets the max.
		 *
		 * @return the max
		 */
		public float getMax() {
			return max;
		}

		/**
		 * Sets the max.
		 *
		 * @param max the new max
		 */
		public void setMax(float max) {
			this.max = max;
		}
		
	}
	
	/**
	 * The Class Min defines the sum format of the documents in couchdb.
	 * <p>
	 * It is used to retrieve the a min value from the database.
	 */
	public static class Min extends CouchDbDocument {

		/** The device. */
		private String device;
		
		/** The min. */
		private float min;
		
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
		public float getMin() {
			return min;
		}
		
		/**
		 * Sets the min.
		 *
		 * @param min the new min
		 */
		public void setMin(float min) {
			this.min = min;
		}

	}
	
	/**
	 * The Class DateMin defines the datemin format of the documents in couchdb.
	 * <p>
	 * It is used to retrieve the a min value combined with a defined date from
	 * the database.
	 */
	public static class DateMin extends CouchDbDocument {
		
		/** The device. */
		private String[] device;
		
		/** The min. */
		private float min;

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
		 * Gets the min.
		 *
		 * @return the min
		 */
		public float getMin() {
			return min;
		}

		/**
		 * Sets the min.
		 *
		 * @param min the new min
		 */
		public void setMin(float min) {
			this.min = min;
		}
		
	}

}
