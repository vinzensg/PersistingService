package ch.ethz.inf.vs.persistingservice.parser;

import java.util.HashMap;
import java.util.List;

import ch.ethz.inf.vs.californium.coap.Option;

// TODO: Auto-generated Javadoc
/**
 * The Class OptionParser.
 */
public class OptionParser {

	/** The option map. */
	private HashMap<String, String> optionMap = new HashMap<String, String>();
	
	/**
	 * Instantiates a new option parser.
	 *
	 * @param options the options
	 */
	public OptionParser(List<Option> options) {
		for (Option option: options) {
			String[] singleOption = option.getStringValue().split("=");
			optionMap.put(singleOption[0], singleOption[1]);
		}
	}
	
	/**
	 * Not null.
	 *
	 * @return true, if successful
	 */
	public boolean notNull() {
		return !optionMap.isEmpty();
	}
	
	/**
	 * Gets the value.
	 *
	 * @param label the label
	 * @return the value
	 */
	public String getValue(String label) {
		return optionMap.get(label);
	}
	
	/**
	 * Checks for exact labels.
	 *
	 * @param num the num
	 * @return true, if successful
	 */
	public boolean hasExactLabels(int num) {
		return optionMap.size() == num;
	}
	
	/**
	 * Checks for min labels.
	 *
	 * @param num the num
	 * @return true, if successful
	 */
	public boolean hasMinLabels(int num) {
		return optionMap.size() >= num;
	}
	
	/**
	 * Checks for max labels.
	 *
	 * @param num the num
	 * @return true, if successful
	 */
	public boolean hasMaxLabels(int num) {
		return optionMap.size() <= num;
	}
	
	/**
	 * Checks for range labels.
	 *
	 * @param num the num
	 * @return true, if successful
	 */
	public boolean hasRangeLabels(int num) {
		return (optionMap.size() >= num) && (optionMap.size() <= num);
	}
	
	/**
	 * Contains label.
	 *
	 * @param label the label
	 * @return true, if successful
	 */
	public boolean containsLabel(String label) {
		return optionMap.containsKey(label);
	}
	
	/**
	 * Contains labels.
	 *
	 * @param labels the labels
	 * @return true, if successful
	 */
	public boolean containsLabels(String[] labels) {
		for (String label : labels) {
			if (!optionMap.containsKey(label)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Contains exact labels.
	 *
	 * @param labels the labels
	 * @return true, if successful
	 */
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
