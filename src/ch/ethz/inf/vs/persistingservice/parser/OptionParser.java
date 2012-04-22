package ch.ethz.inf.vs.persistingservice.parser;

import java.util.HashMap;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.Option;

public class OptionParser {

	private HashMap<String, String> optionMap = new HashMap<String, String>();
	
	public OptionParser(List<Option> options) {
		for (Option option: options) {
			String[] singleOption = option.getStringValue().split("=");
			optionMap.put(singleOption[0], singleOption[1]);
		}
	}
	
	public boolean notNull() {
		return !optionMap.isEmpty();
	}
	
	public String getValue(String label) {
		return optionMap.get(label);
	}
	
	public boolean hasExactLabels(int num) {
		return optionMap.size() == num;
	}
	
	public boolean hasMinLabels(int num) {
		return optionMap.size() >= num;
	}
	
	public boolean hasMaxLabels(int num) {
		return optionMap.size() <= num;
	}
	
	public boolean hasRangeLabels(int num) {
		return (optionMap.size() >= num) && (optionMap.size() <= num);
	}
	
	public boolean containsLabel(String label) {
		return optionMap.containsKey(label);
	}
	
	public boolean containsLabels(String[] labels) {
		for (String label : labels) {
			if (!optionMap.containsKey(label)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean containsExactLabels(String[] labels) {
		int counter = 0;
		
		for (String label : labels) {
			if (!optionMap.containsKey(label)) {
				return false;
			}
			counter++;
		}
		
		if (optionMap.size() == counter) {
			return true;
		}
		return false;
	}

}
