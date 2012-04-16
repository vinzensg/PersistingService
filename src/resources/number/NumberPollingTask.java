package resources.number;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import resources.AbstractPollingTask;

import config.Constants;
import database.type.number.NumberType;
import database.type.number.NumberTypeRepository;

public abstract class NumberPollingTask extends AbstractPollingTask {
	
	protected NumberTypeRepository numberTypeRepository;
	
	public NumberPollingTask(String deviceURI, NumberTypeRepository numberTypeRepository) {
		super(deviceURI);
		this.numberTypeRepository = numberTypeRepository;
	}

	@Override
	protected void storePayload(String payload) {
		NumberType numberType = new NumberType();
		numberType.setDevice(deviceURI);
		numberType.setNumberValue(Integer.valueOf(payload));
		DateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
        Date date = new Date();
		numberType.setDateTime(dateFormat.format(date));
		numberTypeRepository.add(numberType);
	}

	@Override
	public abstract void run();

}
