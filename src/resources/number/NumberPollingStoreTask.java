package resources.number;

import database.type.number.NumberTypeRepository;

public class NumberPollingStoreTask extends NumberPollingTask {
	
	public NumberPollingStoreTask(String deviceURI, NumberTypeRepository numberTypeRepository) {
		super(deviceURI, numberTypeRepository);
	}

	@Override
	public void run() {
		storeData(getRequest(deviceURI));
	}

}
