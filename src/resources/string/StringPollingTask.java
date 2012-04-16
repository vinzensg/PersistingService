package resources.string;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import resources.AbstractPollingTask;

import config.Constants;
import database.type.string.StringType;
import database.type.string.StringTypeRepository;

public abstract class StringPollingTask extends AbstractPollingTask{

	protected StringTypeRepository stringTypeRepository;
	
	public StringPollingTask(String deviceURI, StringTypeRepository stringTypeRepository) {
		super(deviceURI);
		this.stringTypeRepository = stringTypeRepository;
	}

	@Override
	protected void storePayload(String payload) {
		StringType stringType = new StringType();
		stringType.setDevice(deviceURI);
		stringType.setStringValue(payload);
		DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
        Date date = new Date();
		stringType.setDateTime(dateFormat.format(date));
		stringTypeRepository.add(stringType);
	}

	@Override
	public abstract void run();
	
}
