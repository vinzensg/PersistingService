var THIS = this;

// Constants //////////////////////////////////////////////
var TIME_FORMAT = "HH:mm";
var DATE_FORMAT = "yyyy/MM/dd-HH:mm:ss";

var THERMOSTAT_TARGET = "coap://localhost:5685/apps/running/thermostat/temperature";
var TIMED_REQUEST = "coap://localhost:5685/apps/running/timed_request/tasks";

var URL_SUNRISE = "coap://localhost:5685/apps/running/weather/astronomy/sunrise";
var URL_SUNSET = "coap://localhost:5685/apps/running/weather/astronomy/sunset";

var THRESHOLD = 5*1000;
var FIFTEEN_MIN = 15*60+1000;
var THIRTY_MIN = 30*60*1000;
var TWENTY_FOUR_HOUR = 60*60*24*1000;

var TEMPERATURE_INCREASE = 1;
var DAY_START_BEFORE = 30;
var NIGHT_START_BEFORE = 15;

var DEFAULT_DAY_TEMP = 24;
var DEFAULT_NIGHT_TEMP = 20;
var DEFAULT_DAY_START = "07:00";
var DEFAULT_DAY_END = "19:00";

var TIMEOUT0 = 0;
var TIMEOUT1 = 1;
var TIMEOUT2 = 2;
var TIMEOUT3 = 3;

var DAYSTART_TASK = "tdnc_daystart";
var DAYEND_TASK = "tdnc_dayend";
var SUNRISE_TASK = "tdnc_sunrise";
var SUNSET_TASK = "tdnc_sunset";

// Variables ///////////////////////////////////////////////
var timeouts = new Array();

var sunrise = "00:00";
var sunset = "00:00";

var timeFormat = new SimpleDateFormat(TIME_FORMAT);
var dateFormat = new SimpleDateFormat(DATE_FORMAT);

// Add SubResource /////////////////////////////////////////
this.runningres = new RunningRes("running", "false");
this.sunres = new SunRes("sun");
this.tempres = new TempRes("temperature");
this.daytimesres = new DayTimesRes("day");

app.root.addSubResource(THIS.runningres.res);
app.root.addSubResource(THIS.sunres.res);
app.root.addSubResource(THIS.tempres.res);
app.root.addSubResource(THIS.daytimesres.res);

// Get Data from Weather App ///////////////////////////////

var now = new Date();
var calNow = Calendar.getInstance();
calNow.setTime(now);

var calNoon = todayWithTime("00:00");

function start() {
	var difference = calNoon.getTimeInMillis() - calNow.getTimeInMillis();
	if (difference > 0) {
		timeouts[TIMEOUT1] = app.setTimeout(checkSun, difference);
	} else {
		sendRequest("GET", URL_SUNRISE, prepareDayTemp, "");
		
		sendRequest("GET", URL_SUNSET, prepareNightTemp, "");
		
		var nextCheck = TWENTY_FOUR_HOUR + difference;
		timeouts[TIMEOUT2] = app.setTimeout(checkSun, nextCheck);
	}
	
	var dayStart = dateFormat.format((todayWithTime(THIS.daytimesres.startres.getInfo())).getTime());
	var dayEnd = dateFormat.format((todayWithTime(THIS.daytimesres.endres.getInfo())).getTime());
	var sunRise = dateFormat.format((todayWithTime(sunrise)).getTime());
	var sunSet = dateFormat.format((todayWithTime(sunset)).getTime());
	var now = dateFormat.format(new Date());
	
	if (now < dayStart) {
		sendRequest("PUT", THERMOSTAT_TARGET, THIS.tempres.nightres.getInfo());
	} else if (now < dayEnd) {
		if (now < sunRise) {
			if (sunRise < dayStart) {
				sendRequest("PUT", THERMOSTAT_TARGET, THIS.tempres.dayres.getInfo());
			} else { // sunRise >= dayStart
				sendRequest("PUT", THERMOSTAT_TARGET, THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
			}
		} else if (now < sunSet) {
			sendRequest("PUT", THERMOSTAT_TARGET, THIS.tempres.dayres.getInfo());
		} else { // now >= sunSet
			if (sunSet < dayEnd) {
				sendRequest("PUT", THERMOSTAT_TARGET, THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
			} else {
				sendRequest("PUT", THERMOSTAT_TARGET, THIS.tempres.dayres.getInfo());
			}
		}
	} else { // now >= dayEnd
		sendRequest("PUT", THERMOSTAT_TARGET, THIS.tempres.nightres.getInfo());
	}
	
	
	function checkSun() {
		sendRequest("GET", URL_SUNRISE, prepareDayTemp, "");
		
		sendRequest("GET", URL_SUNSET, prepareNightTemp, "");
		
		timeouts[TIMEOUT3] = app.setTimeout(checkSun, TWENTY_FOUR_HOUR);
	}
}

function stop() {
	for (var timer in timeouts) {
		app.clearTimeout(timeouts[timer]);
	}
	
	// delete timed requests
	deleteTimedRequest(DAYSTART_TASK);
	deleteTimedRequest(DAYEND_TASK);
	deleteTimedRequest(SUNRISE_TASK);
	deleteTimedRequest(SUNSET_TASK);
}

function prepareDayTemp(request) {
	var payload = request.getPayloadString();
	sunrise = payload;
	THIS.sunres.riseres.setInfo(sunrise);
	app.dump("Sunrise: " + payload);
	setDaystart(sunrise);
}

function prepareNightTemp(request) {
	var payload = request.getPayloadString();
	sunset = payload;
	THIS.sunres.setres.setInfo(sunset);
	app.dump("Sunset: " + payload);
	setDayend(sunset);
}

//Functions ///////////////////////////////////////////////

function todayWithTime(time) {
	var calToday = Calendar.getInstance();
	
	var dateTime = timeFormat.parse(time);
	var cal = Calendar.getInstance();
	cal.setTime(dateTime);
	
	var dateToday = new Date();
	cal.set(Calendar.YEAR, calToday.get(Calendar.YEAR));
	cal.set(Calendar.MONTH, calToday.get(Calendar.MONTH));
	cal.set(Calendar.DAY_OF_YEAR, calToday.get(Calendar.DAY_OF_YEAR));
	
	return cal;
}

function setDaystart(sunrise) {
	var now = new Date();
	var calNow = Calendar.getInstance();
	calNow.setTime(now);
	
	var calSunrise = todayWithTime(sunrise);
	var calDaystart = todayWithTime(THIS.daytimesres.startres.getInfo());
	
	if (calSunrise.getTimeInMillis() >= (calDaystart.getTimeInMillis())) {
		// send before sunrise
		var calToday = calDaystart;
		
		if (calToday.getTimeInMillis() > (calNow.getTimeInMillis() + THRESHOLD)) {
			app.dump("send timed daystart");
			
			postTimedRequest(DAYSTART_TASK, THERMOSTAT_TARGET, "PUT", (THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE), dateFormat.format(calToday.getTime()));
		}
		// send after sunrise
		calToday = calSunrise;
		
		if (calToday.getTimeInMillis() > (calNow.getTimeInMillis() + THRESHOLD)) {
			app.sump("send timed sunrise");
			
			postTimedRequest(SUNRISE_TASK, THERMOSTAT_TARGET, "PUT", (THIS.tempres.dayres.getInfo()), dateFormat.format(calToday.getTime()));
		}
	} else {
		// send daystart - 30min 
		var calToday = calDaystart;
		calToday.add(Calendar.MINUTE, DAY_START_BEFORE);
		
		if (calToday.getTimeInMillis() > (calNow.getTimeInMillis() + THRESHOLD)) {
			app.dump("Send timed daystart");
			
			postTimedRequest(DAYSTART_TASK, THERMOSTAT_TARGET, "PUT", (THIS.tempres.dayres.getInfo()), dateFormat.format(calToday.getTime()));
		} else {
			// directly set temperature
		}
	}
}

function setDayend(sunset) {
	var now = new Date();
	var calNow = Calendar.getInstance();
	calNow.setTime(now);
	
	var calSunset = todayWithTime(sunset);
	var calDayend = todayWithTime(THIS.daytimesres.endres.getInfo());

	if (calSunset.getTimeInMillis() <= (calDayend.getTimeInMillis())) {
		calToday = calSunset;
		calToday.add(Calendar.MINUTE, NIGHT_START_BEFORE);
		
		if (calToday.getTimeInMillis() > (calNow.getTimeInMillis() + THRESHOLD)) {
			app.dump("send timed sunset: " + dateFormat.format(calToday.getTime()));
			
			postTimedRequest(SUNSET_TASK, THERMOSTAT_TARGET, "PUT", (THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE), dateFormat.format(calToday.getTime()));
		}
		
		// send after dayend
		var calToday = calDayend;
		
		if (calToday.getTimeInMillis() > (calNow.getTimeInMillis() + THRESHOLD)) {
			app.dump("send timed dayend: " + dateFormat.format(calToday.getTime()));
			
			postTimedRequest(DAYEND_TASK, THERMOSTAT_TARGET, "PUT", (THIS.tempres.nightres.getInfo()), dateFormat.format(calToday.getTime()));
		}
	} else {
		// send after dayend
		var calToday = calDayend;
		
		if (calToday.getTimeInMillis() > (calNow.getTimeInMillis() + THRESHOLD)) {
			app.dump("send timed dayend: " + dateFormat.format(calToday.getTime()));
			
			postTimedRequest(DAYEND_TASK, THERMOSTAT_TARGET, "PUT", (THIS.tempres.nightres.getInfo()), dateFormat.format(calToday.getTime()));
		}
	}
}

function sendRequest(operation, target, onloadFunction, payload) {
	app.dump("SEND REQUEST: " + operation + " " + target + " :: " + payload);
	var req = new CoAPRequest();
	req.open(operation, target);
	if (onloadFunction)
		req.onload = onloadFunction
	req.send(payload);
}

function postTimedRequest(resid, device, operation, payload, datetime) {
	var requestPayload = "resid = " + resid + "\n" +
		 "device = " + device + "\n" +
		 "operation = " + operation + "\n" +
		 "payload = " + payload + "\n" +
		 "datetime = " + datetime;
	sendRequest("POST", TIMED_REQUEST, null, requestPayload);
}

function changeTimedRequest(task, resid, change) {
	sendRequest("PUT", TIMED_REQUEST + "/" + task + "/" + resid, null, change);
}

function deleteTimeRequest(task) {
	sendRequest("DELETE", TIMED_REQUEST + "/" + task, null, "");
}

// Resources ///////////////////////////////////////////
function RunningRes(resid, in_running) {
	var running = in_running;
	
	this.res = new JavaScriptResource(resid);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, running);
	}
	
	this.res.onput = function(request) {
		var payload = request.getPayloadString();
		if (payload=="true") {
			if (running=="false") {
				running = payload;
				start();
			}
			request.respond(CodeRegistry.RESP_CHANGED);
		} else if (payload=="false") {
			if (running=="true") {
				running = payload;
				stop();
			}
			request.respond(CodeRegistry.RESP_CHANGED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST);
		}
	}
}

function SunRes(resid) {
	var THISSunRes = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.riseres = new InfoRes("rise", "");
	this.setres = new InfoRes("set", "");
	
	this.res.addSubResource(THISSunRes.riseres.res);
	this.res.addSubResource(THISSunRes.setres.res);
}

function TempRes(resid) {
	var THISTempRes = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.dayres = new ChangeableInfoRes("day", DEFAULT_DAY_TEMP, function(oldInfo, newInfo) {
		var newTemp = newInfo;
		var dayStart = dateFormat.format((todayWithTime(THIS.daytimesres.startres.getInfo())).getTime());
		var dayEnd = dateFormat.format((todayWithTime(THIS.daytimesres.endres.getInfo())).getTime());
		var sunRise = dateFormat.format((todayWithTime(sunrise)).getTime());
		var sunSet = dateFormat.format((todayWithTime(sunset)).getTime());
		var now = dateFormat.format(new Date());
		
		if (now < dayStart) {
			if (sunRise < dayStart) {
				if (sunRise < now) {
					changeTimedRequest(DAYSTART_TASK, "payload", newTemp);
				} else { // sunRise >= now
					changeTimedRequest(DAYSTART_TASK, "payload", newTemp + TEMPERATURE_INCREASE);
				}
			} else { // sunRise >= dayStart
				if (sunRise < now) {
					sendRequest("PUT", THERMOSTAT_TARGET, null, newTemp + TEMPERATURE_INCREASE);
					changeTimedRequest(DAYSTART_TASK, "payload", newTemp);
				} else { // sunRise >= now
					changeTimedRequest(DAYSTART_TASK, "payload", newTemp + TEMPERATURE_INCREASE);
					changeTimedRequest(SUNRISE_TASK, "payload", newTemp);
				}
			}
			if (sunSet < dayEnd) {
				changeTimedRequest(SUNSET_TASK, "payload", newTemp + TEMPERATURE_INCREASE);
			} else { // sunSet >= dayEnd
				return;
			}
		} else if (now < dayEnd) {
			if (sunRise > dayStart) {
				if (sunRise < now) {
					return;
				} else { // sunRise >= now
					sendRequest("PUT", THERMOSTAT_TARGET, null, newTemp + TEMPERATURE_INCREASE);
					changeTimedRequest(SUNRISE_TASK, "payload", newTemp);
				}
			}
			if (sunSet < dayEnd) {
				if (sunSet < now) {
					sendRequest("PUT", THERMOSTAT_TARGET, null, newTemp + TEMPERATURE_INCREASE);
				} else { // sunSet >= now
					changeTimedRequest(SUNSET_TASK, "payload", newTemp + TEMPERATURE_INCREASE);
				}
			} else { // sunSet >= dayEnd
				return;
			}
		} else { // now >= dayEnd
			return;
		}
	});
	
	this.nightres = new ChangeableInfoRes("night", DEFAULT_NIGHT_TEMP, function(oldInfo, newInfo) {
		var newTemp = newInfo;
		var dayStart = dateFormat.format((todayWithTime(THIS.daytimesres.startres.getInfo())).getTime());
		var dayEnd = dateFormat.format((todayWithTime(THIS.daytimesres.endres.getInfo())).getTime());
		var now = dateFormat.format(new Date());
		
		if (now < dayStart) {
			sendRequest("PUT", THERMOSTAT_TARGET, null, newTemp);
			changeTimedRequest(DAYEND_TASK, "payload", newTemp);
		} else if (now < dayEnd) {
			changeTimedRequest(DAYEND_TASK, "payload", newTemp);
		} else { // now >= dayEnd
			sendRequest("PUT", THERMOSTAT_TARGET, null, newTemp);
		}
	});
	
	this.res.addSubResource(THISTempRes.dayres.res);
	this.res.addSubResource(THISTempRes.nightres.res);
}

function DayTimesRes(resid) {
	var THISDayRes = this;
	
	this.res = new JavaScriptResource(resid);

	this.startres = new ChangeableInfoRes("start", DEFAULT_DAY_START, function(oldInfo, newInfo) {
		var newDate = dateFormat.format((todayWithTime(newInfo)).getTime());
		var oldDate = dateFormat.format((todayWithTime(oldInfo)).getTime());
		var sunRise = dateFormat.format((todayWithTime(sunrise)).getTime());
		var now = dateFormat.format(new Date());
		
		if (sunRise < oldDate) {
			if (now < sunRise) {
				if (newDate < sunRise) {
					if (newDate < now) {
						sendRequest("PUT", THERMOSTART_TARGET, null, THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
						deleteRequest(DAYSTART_TASK);
					} else { // newDate >= now
						changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
						changeTimedRequest(DAYSTART_TASK, "payload", THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
						postTimedRequest(SUNRISE_TASK, THERMOSTART_TARGET, "PUT", THIS.tempres.dayres.getInfo(), sunRise);
					}
				} else if (newDate < oldDate) {
					changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
				} else { // newDate >= oldDate
					changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
				}
			} else if (now < oldDate) {
				if (newDate < sunRise) {
					changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
					changeTimedRequest(DAYSTART_TASK, "payload", THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
					postTimedRequest(SUNRISE_TASK, THERMOSTART_TARGET, "PUT", THIS.tempres.dayres.getInfo(), sunRise);
				} else if (newDate < oldDate) {
					if (newDate < now) {
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo());
						deleteRequest(DAYSTART_TASK);
					} else { // newDate >= now
						changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
					}
				} else { // newDate >= oldDate
					changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
				}
			} else { // now >= oldDate
				if (newDate < sunRise) {
					return;
				} else if (newDate < oldDate) {
					return;
				} else { // newDate >= oldDate
					if (newDate < now) {
						return;
					} else { // newDate >= now
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
						postTimedRequest(DAYSTART_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo(), newDate);
					}
				}
			}
		} else { // sunRise >= oldDate
			if (now < oldDate) {
				if (newDate < oldDate) {
					if (newDate < now) {
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo());
						deleteTimedRequest(DAYSTART_TASK);
					} else { // newDate >= now
						changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
					}
				} else if (newDate < sunRise) {
					changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
				} else { // newDate >= sunRise
					changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
					deleteTimedRequest(SUNRISE_TASK);
				}
			} else if (now < sunRise) {
				if (newDate < oldDate) {
					return;
				} else if (newDate < sunRise) {
					if (newDate < now) {
						return;
					} else { // newDate >= now
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
						postTimedRequest(DAYSTART_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo(), newDate);
					}
				} else { // newDate >= sunRise
					deleteTimedRequest(SUNRISE_TASK);
					postTimedRequest(DAYSTART_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo(), newDate);
				}
			} else { // now >= sunRise
				if (newDate < oldDate) {
					return;
				} else if (newDate < sunRise) {
					return;
				} else { // newDate >= sunRise
					if (newDate < now) {
						return;
					} else { // newDate >= now
						postTimedRequest(DAYSTART_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo(), newDate);
					}
				}
			}
		}
	});

	this.endres = new ChangeableInfoRes("end", DEFAULT_DAY_END, function(oldInfo, newInfo) {
		var newDate = dateFormat.format((todayWithTime(newInfo)).getTime());
		var oldDate = dateFormat.format((todayWithTime(oldInfo)).getTime());
		var sunSet = dateFormat.format((todayWithTime(sunset)).getTime());
		var now = dateFormat.format(new Date());
		
		if (sunSet < oldDate) {
			if (now < sunSet) {
				if (newDate < sunSet) {
					if (newDate < now) {
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
						deleteTimedRequest(SUNSET_TASK);
						deleteTimedRequest(DAYEND_TASK);
					} else { // newDate >= now
						changeTimedRequest(DAYEND_TASK, "datetime", newDate);
						deleteTimedRequest(SUNSET_TASK);
					}
				} else if (newDate < oldDate) {
					changeTimedRequest(DAYEND_TASK, "datetime", newDate);
				} else { // newDate >= oldDate
					changeTimedRequest(DAYEND_TASK, "datetime", newDate);
				}
			} else if (now < oldDate) {
				if (newDate < sunSet) {
					sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
					deleteTimedRequest(SUNSET_TASK);
					deleteTimedRequest(DAYEND_TASK);
				} else if (newDate < oldDate) {
					if (newDate < now) {
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
						deleteTimedRequest(DAYEND_TASK);
					} else { // newDate >= now
						changeTimedRequest(DAYEND_TASK, "datetime", newDate);
					}
				} else { // newDate >= oldDate
					changeTimeRequest(DAYEND_TASK, "datetime", newDate);
				}
			} else { // now >= oldDate
				if (newDate < sunRise) {
					return;
				} else if (newDate < oldDate) {
					return;
				} else { // newDate >= oldDate
					if (newDate < now) {
						return;
					} else { // newDate >= now
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
						postTimedRequest(DAYEND_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.nightres.getInfo(), newDate);
					}
				}
			}
		} else { // sunSet >= oldDate
			if (now < oldDate) {
				if (newDate < oldDate) {
					if (newDate < now) {
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
						deleteTimedRequest(DAYEND_TASK);
					} else { // newDate >= now
						changeTimedRequest(DAYEND_TASK, "datetime", newDate);
					}
				} else if (newDate < sunSet) {
					changeTimedRequest(DAYEND_TASK, "datetime", newDate);
				} else { // newDate >= sunSet
					changeTimedRequest(DAYEND_TASK, "datetime", newDate);
					postTimedRequest(SUNSET_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE, sunSet);
				}
			} else if (now < sunSet) {
				if (newDate < oldDate) {
					return;
				} else if (newDate < sunSet) {
					if (newDate < now) {
						return;
					} else { // newDate >= now
						sendRequest("PUT", THERMOSTAT_TARGET, THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
						postTimedRequest(DAYEND_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.nightres.getInfo(), newDate);
					}
				} else { // newDate >= sunSet
					postTimedRequest(SUNSET_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE, sunSet);
					changeTimedRequest(DAYEND_TASK, "datetime", newDate);
				}
			} else { // now >= sunSet
				if (newDate < oldDate) {
					return;
				} else if (newDate < sunSet) {
					return;
				} else { // newDate >= sunSet
					if (newDate < now) {
						return;
					} else { // newDate >= now
						sendRequest("PUT", THERMOSTAT_TARGET, THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
						postTimedRequest(DAYEND_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.nightres.getInfo(), newDate);
					}
				}
			}
		}
	});
	
	this.res.addSubResource(THISDayRes.startres.res);
	this.res.addSubResource(THISDayRes.endres.res);
}


//Info Resources ////////////////////////////////////////
function InfoRes(resid, in_info) {
	var info = in_info;
	
	this.res = new JavaScriptResource(resid);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, info);
	}
	
	this.getInfo = function() {
		return info;
	}
	
	this.setInfo = function(in_info) {
		info = in_info;
	}
}

function ChangeableInfoRes(resid, in_info, in_func) {
	var info = in_info;
	var updateFunc = in_func;
	
	this.res = new JavaScriptResource(resid);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, info);
	}
	
	this.res.onput = function(request) {
		var payload = request.getPayloadString();
		if (payload != info) {
			if (updateFunc)
				updateFunc(info, payload);
			info = payload;
		}
		request.respond(CodeRegistry.RESP_CHANGED);
	}
	
	this.getInfo = function() {
		return info;
	}
	
	this.setInfo = function(info) {
		info = info;
	}
}

// Unload ////////////////////////////////////

app.onunload = function() {
	app.dump("Clear Timeouts");
	for (var timer in timeouts) {
		app.clearTimeout(timeouts[timer]);
	}
}