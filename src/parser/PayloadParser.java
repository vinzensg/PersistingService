package parser;

import java.util.HashMap;

public class PayloadParser {

	private HashMap<String, String> payloadMap = new HashMap<String, String>();
	
	public PayloadParser(String payload) {
		String tmp = payload.replaceAll(" ", "");
		String[] parameters = tmp.split("\n");
		
		for (int i = 0; i<parameters.length; i++) {
			String[] singleParameter = parameters[i].split("=");
			payloadMap.put(singleParameter[0], singleParameter[1]);
		}
	}
	
	public boolean notNull() {
		return !payloadMap.isEmpty();
	}
	
	public String getStringValue(String label) {
		return payloadMap.get(label);
	}
	
	public int getIntValue(String label) {
		return Integer.valueOf(payloadMap.get(label));
	}
	
	public boolean getBooleanValue(String label) {
		return Boolean.parseBoolean(payloadMap.get(label));
	}
	
	public boolean hasExactLabels(int num) {
		return payloadMap.size() == num;
	}
	
	public boolean hasMinLabels(int num) {
		return payloadMap.size() >= num;
	}
	
	public boolean hasMaxLabels(int num) {
		return payloadMap.size() <= num;
	}
	
	public boolean hasRangeLabels(int num) {
		return (payloadMap.size() >= num) && (payloadMap.size() <= num);
	}
	
	public boolean containsLabel(String label) {
		return payloadMap.containsKey(label);
	}
	
	public boolean containsLabels(String[] labels) {
		for (String label : labels) {
			if (!payloadMap.containsKey(label)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean containsExactLabels(String[] labels) {
		int counter = 0;
		
		for (String label : labels) {
			if (!payloadMap.containsKey(label)) {
				return false;
			}
			counter++;
		}
		
		if (payloadMap.size() == counter) {
			return true;
		}
		return false;
	}
	
	public boolean isBoolean(String label) {
		if (payloadMap.get(label).equals("true") || payloadMap.get(label).equals("false")){
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isInteger(String label) {
		char[] num = payloadMap.get(label).toCharArray();
		for (int i=0; i<num.length; i++) {
			if (!Character.isDigit(num[i])) {
				return false;
			}
		}
		return true;
	}

}
