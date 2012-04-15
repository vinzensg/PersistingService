package database.type.number;

import org.ektorp.support.CouchDbDocument;

public class NumberTypeAvg extends CouchDbDocument {
	
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
