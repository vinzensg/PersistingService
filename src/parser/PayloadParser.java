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
	
	public String getValue(String label) {
		return payloadMap.get(label);
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
	
	
	
	/*
	 * To test class
	 */
	public static void main(String[] args) {
		
		String testString = "resid = thermostat\n" +
							"deviceuri = coap://localhost:5683/running/thermostat";
		
		
		PayloadParser pp = new PayloadParser(testString);
		
		System.out.println("hasExactLabels: " + pp.containsExactLabels(new String[]{"resid", "deviceuri"}));
		System.out.println("Resid: " + pp.getValue("resid"));
		System.out.println("Deviceuri: " + pp.getValue("deviceuri"));
	}
}
