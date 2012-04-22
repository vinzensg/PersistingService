package database.type.number;

import org.ektorp.support.CouchDbDocument;

public class NumberType {
	
	public static class Default extends CouchDbDocument {
	
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
	
	public static class Sum extends CouchDbDocument {
		
		private String device;
		private int sum;
		
		public String getDevice() {
			return device;
		}
		public void setDevice(String device) {
			this.device = device;
		}
		public int getSum() {
			return sum;
		}
		public void setSum(int sum) {
			this.sum = sum;
		}

	}
	
	public static class Avg extends CouchDbDocument {
		
		private String device;
		private int count;
		private float avg;
		
		public String getDevice() {
			return device;
		}
		public void setDevice(String device) {
			this.device = device;
		}
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		public float getAvg() {
			return avg;
		}
		public void setAvg(float avg) {
			this.avg = avg;
		}
		
	}
	
	public static class Max extends CouchDbDocument {

		private String device;
		private int max;
		
		public String getDevice() {
			return device;
		}
		public void setDevice(String device) {
			this.device = device;
		}
		public int getMax() {
			return max;
		}
		public void setMax(int max) {
			this.max = max;
		}
		
	}
	
	public static class Min extends CouchDbDocument {

		private String device;
		private int min;
		
		public String getDevice() {
			return device;
		}
		public void setDevice(String device) {
			this.device = device;
		}
		public int getMin() {
			return min;
		}
		public void setMin(int min) {
			this.min = min;
		}

	}

}
