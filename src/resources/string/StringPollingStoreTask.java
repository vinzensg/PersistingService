package resources.string;

import database.type.string.StringTypeRepository;

public class StringPollingStoreTask extends StringPollingTask {

	public StringPollingStoreTask(String deviceURI, StringTypeRepository stringTypeRepository) {
		super(deviceURI, stringTypeRepository);
	}

	@Override
	public void run() {
		storeData(getRequest(deviceURI));
	}
	
}
