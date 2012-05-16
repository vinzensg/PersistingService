package ch.ethz.inf.vs.persistingservice.resources.persisting.history;

import ch.ethz.inf.vs.persistingservice.database.documents.DefaultStorage;

public abstract class AbstractValueSet {
	
	public abstract void perform(DefaultStorage defaultStorage, String payload);

}
