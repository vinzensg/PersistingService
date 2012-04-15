package database.type.number;

import org.ektorp.support.CouchDbDocument;

public class NumberTypeSum extends CouchDbDocument {
	
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
