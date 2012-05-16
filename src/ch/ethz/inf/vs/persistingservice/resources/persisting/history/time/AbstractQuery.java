package ch.ethz.inf.vs.persistingservice.resources.persisting.history.time;

import ch.ethz.inf.vs.persistingservice.parser.OptionParser;

public abstract class AbstractQuery {
	
	public final static int NEWEST = 0;
	public final static int ALL = 1;
	public final static int SINCE = 2;
	public final static int ONDAY = 3;
	public final static int TIMERANGE = 4;
	public final static int LAST = 5;
	
	public abstract String perform(OptionParser parsedOptions, int timeResID, String...params);

}
