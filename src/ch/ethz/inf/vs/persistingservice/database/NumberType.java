package ch.ethz.inf.vs.persistingservice.database;

import org.ektorp.support.CouchDbDocument;

public class NumberType {
	
	public static class Default extends CouchDbDocument {
	
		private String device;
		private float numberValue;
		private String dateTime;
		
		public String getDevice() {
			return device;
		}
		public void setDevice(String device) {
			this.device = device;
		}
		public float getNumberValue() {
			return numberValue;
		}
		public void setNumberValue(float numberValue) {
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
		private float sum;
		
		public String getDevice() {
			return device;
		}
		public void setDevice(String device) {
			this.device = device;
		}
		public float getSum() {
			return sum;
		}
		public void setSum(float sum) {
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
		private float max;
		
		public String getDevice() {
			return device;
		}
		public void setDevice(String device) {
			this.device = device;
		}
		public float getMax() {
			return max;
		}
		public void setMax(float max) {
			this.max = max;
		}
		
	}
	
	public static class Min extends CouchDbDocument {

		private String device;
		private float min;
		
		public String getDevice() {
			return device;
		}
		public void setDevice(String device) {
			this.device = device;
		}
		public float getMin() {
			return min;
		}
		public void setMin(float min) {
			this.min = min;
		}

	}

}
