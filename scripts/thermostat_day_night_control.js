var THIS = this;

// Constants //////////////////////////////////////////////
var TIME_FORMAT = "HH:mm";
var DATE_FORMAT = "yyyy/MM/dd-HH:mm:ss";

var THERMOSTAT_TARGET = "coap://localhost:5685/apps/running/thermostat/temperature";
var TIMED_REQUEST = "coap://localhost:5685/apps/running/timed_request/tasks";

var URL_SUNRISE = "coap://localhost:5685/apps/running/weather/astronomy/sunrise";
var URL_SUNSET = "coap://localhost:5685/apps/running/weather/astronomy/sunset";

var THRESHOLD = 5;
var FIFTEEN_MIN = 15*60+1000;
var THIRTY_MIN = 30*60*1000;
var TWENTY_FOUR_HOUR = 60*60*24*1000;

var TEMPERATURE_INCREASE = 1;

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

app.root.add(THIS.runningres.res);
app.root.add(THIS.sunres.res);
app.root.add(THIS.tempres.res);
app.root.add(THIS.daytimesres.res);

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
		sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
	} else if (now < dayEnd) {
		if (now < sunRise) {
			if (sunRise < dayStart) {
				sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo());
			} else { // sunRise >= dayStart
				sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
			}
		} else if (now < sunSet) {
			sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo());
		} else { // now >= sunSet
			if (sunSet < dayEnd) {
				sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
			} else {
				sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo());
			}
		}
	} else { // now >= dayEnd
		sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
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
	var dayStart = dateFormat.format(todayWithTime(THIS.daytimesres.startres.getInfo()).getTime());
	var sunRise = dateFormat.format(todayWithTime(sunrise).getTime());
	var nowCal = Calendar.getInstance();
	nowCal.setTime(new Date())
	nowCal.add(Calendar.SECOND, THRESHOLD);
	var now = dateFormat.format(nowCal.getTime());
		
	app.dump("NOW Start: " + now);
	app.dump("Day Start: " + dayStart);
	if (sunRise >= dayStart) {		
		if (dayStart > now) {
			app.dump("send timed daystart");
			
			postTimedRequest(DAYSTART_TASK, THERMOSTAT_TARGET, "PUT", (THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE), dayStart);
		}
		
		if (sunRise > now) {
			app.sump("send timed sunrise");
			
			postTimedRequest(SUNRISE_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo(), sunRise);
		}
	} else { // runRise < dayStart	
		if (dayStart > now) {
			app.dump("Send timed daystart");
			
			postTimedRequest(DAYSTART_TASK, THERMOSTAT_TARGET, "PUT", (THIS.tempres.dayres.getInfo()), dayStart);
		}
	}
}

function setDayend(sunset) {
	var dayEnd = dateFormat.format(todayWithTime(THIS.daytimesres.endres.getInfo()).getTime());
	var sunSet = dateFormat.format(todayWithTime(sunset).getTime());
	var nowCal = Calendar.getInstance();
	nowCal.setTime(new Date())
	nowCal.add(Calendar.SECOND, THRESHOLD);
	var now = dateFormat.format(nowCal.getTime());

	app.dump("NOW End: " + now);
	if (sunSet <= dayEnd) {
		if (sunSet > now) {
			app.dump("send timed sunset");
			
			postTimedRequest(SUNSET_TASK, THERMOSTAT_TARGET, "PUT", (THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE), sunSet);
		}
				
		if (dayEnd > now) {
			app.dump("send timed dayend");
			
			postTimedRequest(DAYEND_TASK, THERMOSTAT_TARGET, "PUT", (THIS.tempres.nightres.getInfo()), dayEnd);
		}
	} else { // sunSet > dayEnd	
		if (dayEnd > now) {
			app.dump("send timed dayend");
			
			postTimedRequest(DAYEND_TASK, THERMOSTAT_TARGET, "PUT", (THIS.tempres.nightres.getInfo()), dayEnd);
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

function deleteTimedRequest(task) {
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
	
	this.isRunning = function() {
		return running;
	}
}

function SunRes(resid) {
	var THISSunRes = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.riseres = new InfoRes("rise", sunrise);
	this.setres = new InfoRes("set", sunset);
	
	this.res.add(THISSunRes.riseres.res);
	this.res.add(THISSunRes.setres.res);
}

function TempRes(resid) {
	var THISTempRes = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.dayres = new ChangeableInfoRes("day", DEFAULT_DAY_TEMP, function(oldInfo, newInfo) {
		if (oldInfo == newInfo)
			return;
		
		var newTemp = newInfo;
		var dayStart = dateFormat.format((todayWithTime(THIS.daytimesres.startres.getInfo())).getTime());
		var dayEnd = dateFormat.format((todayWithTime(THIS.daytimesres.endres.getInfo())).getTime());
		var sunRise = dateFormat.format((todayWithTime(sunrise)).getTime());
		var sunSet = dateFormat.format((todayWithTime(sunset)).getTime());
		var now = dateFormat.format(new Date());
		
		if (now < dayStart) {
			if (sunRise < dayStart) {
				if (sunRise < now) {
					app.dump("temp_day::update==1");
					changeTimedRequest(DAYSTART_TASK, "payload", newTemp);
				} else { // sunRise >= now
					app.dump("temp_day::update==2");
					changeTimedRequest(DAYSTART_TASK, "payload", newTemp + TEMPERATURE_INCREASE);
				}
			} else { // sunRise >= dayStart
				if (sunRise < now) {
					app.dump("temp_day::update==3");
					sendRequest("PUT", THERMOSTAT_TARGET, null, newTemp + TEMPERATURE_INCREASE);
					changeTimedRequest(DAYSTART_TASK, "payload", newTemp);
				} else { // sunRise >= now
					app.dump("temp_day::update==4");
					changeTimedRequest(DAYSTART_TASK, "payload", newTemp + TEMPERATURE_INCREASE);
					changeTimedRequest(SUNRISE_TASK, "payload", newTemp);
				}
			}
			if (sunSet < dayEnd) {
				app.dump("temp_day::update==5");
				changeTimedRequest(SUNSET_TASK, "payload", newTemp + TEMPERATURE_INCREASE);
			} else { // sunSet >= dayEnd
				app.dump("temp_day::update==6");
				return;
			}
		} else if (now < dayEnd) {
			if (sunRise > dayStart) {
				if (sunRise < now) {
					app.dump("temp_day::update==7");
					return;
				} else { // sunRise >= now
					app.dump("temp_day::update==8");
					sendRequest("PUT", THERMOSTAT_TARGET, null, newTemp + TEMPERATURE_INCREASE);
					changeTimedRequest(SUNRISE_TASK, "payload", newTemp);
				}
			}
			if (sunSet < dayEnd) {
				if (sunSet < now) {
					app.dump("temp_day::update==9");
					sendRequest("PUT", THERMOSTAT_TARGET, null, newTemp + TEMPERATURE_INCREASE);
				} else { // sunSet >= now
					app.dump("temp_day::update==10");
					changeTimedRequest(SUNSET_TASK, "payload", newTemp + TEMPERATURE_INCREASE);
				}
			} else { // sunSet >= dayEnd
				app.dump("temp_day::update==11");
				return;
			}
		} else { // now >= dayEnd
			app.dump("temp_day::update==12");
			return;
		}
	});
	
	this.nightres = new ChangeableInfoRes("night", DEFAULT_NIGHT_TEMP, function(oldInfo, newInfo) {
		if (oldInfo == newInfo)
			return;
		
		var newTemp = newInfo;
		var dayStart = dateFormat.format((todayWithTime(THIS.daytimesres.startres.getInfo())).getTime());
		var dayEnd = dateFormat.format((todayWithTime(THIS.daytimesres.endres.getInfo())).getTime());
		var now = dateFormat.format(new Date());
		
		if (now < dayStart) {
			app.dump("temp_night::update==1");
			sendRequest("PUT", THERMOSTAT_TARGET, null, newTemp);
			changeTimedRequest(DAYEND_TASK, "payload", newTemp);
		} else if (now < dayEnd) {
			app.dump("temp_night::update==2");
			changeTimedRequest(DAYEND_TASK, "payload", newTemp);
		} else { // now >= dayEnd
			app.dump("temp_night::update==3");
			sendRequest("PUT", THERMOSTAT_TARGET, null, newTemp);
		}
	});
	
	this.res.add(THISTempRes.dayres.res);
	this.res.add(THISTempRes.nightres.res);
}

function DayTimesRes(resid) {
	var THISDayRes = this;
	
	this.res = new JavaScriptResource(resid);

	this.startres = new ChangeableInfoRes("start", DEFAULT_DAY_START, function(oldInfo, newInfo) {
		if (oldInfo == newInfo)
			return;
		
		var newDate = dateFormat.format((todayWithTime(newInfo)).getTime());
		var oldDate = dateFormat.format((todayWithTime(oldInfo)).getTime());
		var sunRise = dateFormat.format((todayWithTime(sunrise)).getTime());
		var now = dateFormat.format(new Date());
		
		if (sunRise < oldDate) {
			if (now < sunRise) {
				if (newDate < sunRise) {
					if (newDate < now) {
						app.dump("day_start::update==1");
						sendRequest("PUT", THERMOSTART_TARGET, null, THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
						deleteRequest(DAYSTART_TASK);
					} else { // newDate >= now
						app.dump("day_start::update==2");
						changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
						changeTimedRequest(DAYSTART_TASK, "payload", THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
						postTimedRequest(SUNRISE_TASK, THERMOSTART_TARGET, "PUT", THIS.tempres.dayres.getInfo(), sunRise);
					}
				} else if (newDate < oldDate) {
					app.dump("day_start::update==3");
					changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
				} else { // newDate >= oldDate
					app.dump("day_start::update==4");
					changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
				}
			} else if (now < oldDate) {
				if (newDate < sunRise) {
					app.dump("day_start::update==5");
					changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
					changeTimedRequest(DAYSTART_TASK, "payload", THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
					postTimedRequest(SUNRISE_TASK, THERMOSTART_TARGET, "PUT", THIS.tempres.dayres.getInfo(), sunRise);
				} else if (newDate < oldDate) {
					if (newDate < now) {
						app.dump("day_start::update==6");
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo());
						deleteRequest(DAYSTART_TASK);
					} else { // newDate >= now
						app.dump("day_start::update==7");
						changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
					}
				} else { // newDate >= oldDate
					app.dump("day_start::update==8");
					changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
				}
			} else { // now >= oldDate
				if (newDate < sunRise) {
					app.dump("day_start::update==9");
					return;
				} else if (newDate < oldDate) {
					app.dump("day_start::update==10");
					return;
				} else { // newDate >= oldDate
					if (newDate < now) {
						app.dump("day_start::update==11");
						return;
					} else { // newDate >= now
						app.dump("day_start::update==12");
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
						postTimedRequest(DAYSTART_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo(), newDate);
					}
				}
			}
		} else { // sunRise >= oldDate
			if (now < oldDate) {
				if (newDate < oldDate) {
					if (newDate < now) {
						app.dump("day_start::update==13");
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo());
						deleteTimedRequest(DAYSTART_TASK);
					} else { // newDate >= now
						app.dump("day_start::update==14");
						changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
					}
				} else if (newDate < sunRise) {
					app.dump("day_start::update==15");
					changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
				} else { // newDate >= sunRise
					app.dump("day_start::update==16");
					changeTimedRequest(DAYSTART_TASK, "datetime", newDate);
					deleteTimedRequest(SUNRISE_TASK);
				}
			} else if (now < sunRise) {
				if (newDate < oldDate) {
					app.dump("day_start::update==17");
					return;
				} else if (newDate < sunRise) {
					if (newDate < now) {
						app.dump("day_start::update==18");
						return;
					} else { // newDate >= now
						app.dump("day_start::update==19");
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
						postTimedRequest(DAYSTART_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo(), newDate);
					}
				} else { // newDate >= sunRise
					app.dump("day_start::update==20");
					deleteTimedRequest(SUNRISE_TASK);
					postTimedRequest(DAYSTART_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo(), newDate);
				}
			} else { // now >= sunRise
				if (newDate < oldDate) {
					app.dump("day_start::update==21");
					return;
				} else if (newDate < sunRise) {
					app.dump("day_start::update==22");
					return;
				} else { // newDate >= sunRise
					if (newDate < now) {
						app.dump("day_start::update==23");
						return;
					} else { // newDate >= now
						app.dump("day_start::update==24");
						postTimedRequest(DAYSTART_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo(), newDate);
					}
				}
			}
		}
	});

	this.endres = new ChangeableInfoRes("end", DEFAULT_DAY_END, function(oldInfo, newInfo) {
		if (oldInfo == newInfo)
			return;
		
		var newDate = dateFormat.format((todayWithTime(newInfo)).getTime());
		var oldDate = dateFormat.format((todayWithTime(oldInfo)).getTime());
		var sunSet = dateFormat.format((todayWithTime(sunset)).getTime());
		var now = dateFormat.format(new Date());
		
		if (sunSet < oldDate) {
			if (now < sunSet) {
				if (newDate < sunSet) {
					if (newDate < now) {
						app.dump("day_end::update==1");
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
						deleteTimedRequest(SUNSET_TASK);
						deleteTimedRequest(DAYEND_TASK);
					} else { // newDate >= now
						app.dump("day_end::update==2");
						changeTimedRequest(DAYEND_TASK, "datetime", newDate);
						deleteTimedRequest(SUNSET_TASK);
					}
				} else if (newDate < oldDate) {
					app.dump("day_end::update==3");
					changeTimedRequest(DAYEND_TASK, "datetime", newDate);
				} else { // newDate >= oldDate
					app.dump("day_end::update==4");
					changeTimedRequest(DAYEND_TASK, "datetime", newDate);
				}
			} else if (now < oldDate) {
				if (newDate < sunSet) {
					app.dump("day_end::update==5");
					sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
					deleteTimedRequest(SUNSET_TASK);
					deleteTimedRequest(DAYEND_TASK);
				} else if (newDate < oldDate) {
					if (newDate < now) {
						app.dump("day_end::update==6");
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
						deleteTimedRequest(DAYEND_TASK);
					} else { // newDate >= now
						app.dump("day_end::update==7");
						changeTimedRequest(DAYEND_TASK, "datetime", newDate);
					}
				} else { // newDate >= oldDate
					app.dump("day_end::update==8");
					changeTimeRequest(DAYEND_TASK, "datetime", newDate);
				}
			} else { // now >= oldDate
				if (newDate < sunSet) {
					app.dump("day_end::update==9");
					return;
				} else if (newDate < oldDate) {
					app.dump("day_end::update==10");
					return;
				} else { // newDate >= oldDate
					if (newDate < now) {
						app.dump("day_end::update==11");
						return;
					} else { // newDate >= now
						if (now < sunSet) {
							app.dump("day_end::update==12");
							postTimedRequest(DAYEND_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.nightres.getInfo(), newDate);
							postTimedRequest(SUNSET_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo() + 1, sunset);
						} else { // now >= sunSet
							app.dump("day_end::update==13");
							sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
							postTimedRequest(DAYEND_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.nightres.getInfo(), newDate);
						}
					}
				}
			}
		} else { // sunSet >= oldDate
			if (now < oldDate) {
				if (newDate < oldDate) {
					if (newDate < now) {
						app.dump("day_end::update==14");
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.nightres.getInfo());
						deleteTimedRequest(DAYEND_TASK);
					} else { // newDate >= now
						app.dump("day_end::update==15");
						changeTimedRequest(DAYEND_TASK, "datetime", newDate);
					}
				} else if (newDate < sunSet) {
					app.dump("day_end::update==16");
					changeTimedRequest(DAYEND_TASK, "datetime", newDate);
				} else { // newDate >= sunSet
					app.dump("day_end::update==17");
					changeTimedRequest(DAYEND_TASK, "datetime", newDate);
					postTimedRequest(SUNSET_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE, sunSet);
				}
			} else if (now < sunSet) {
				if (newDate < oldDate) {
					app.dump("day_end::update==18");
					return;
				} else if (newDate < sunSet) {
					if (newDate < now) {
						app.dump("day_end::update==19");
						return;
					} else { // newDate >= now
						app.dump("day_end::update==20");
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
						postTimedRequest(DAYEND_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.nightres.getInfo(), newDate);
					}
				} else { // newDate >= sunSet
					app.dump("day_end::update==21");
					postTimedRequest(SUNSET_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE, sunSet);
					postTimedRequest(DAYEND_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.nightres.getInfo(), newDate);
				}
			} else { // now >= sunSet
				if (newDate < oldDate) {
					app.dump("day_end::update==22");
					return;
				} else if (newDate < sunSet) {
					app.dump("day_end::update==23");
					return;
				} else { // newDate >= sunSet
					if (newDate < now) {
						app.dump("day_end::update==24");
						return;
					} else { // newDate >= now
						app.dump("day_end::update==25");
						sendRequest("PUT", THERMOSTAT_TARGET, null, THIS.tempres.dayres.getInfo() + TEMPERATURE_INCREASE);
						postTimedRequest(DAYEND_TASK, THERMOSTAT_TARGET, "PUT", THIS.tempres.nightres.getInfo(), newDate);
					}
				}
			}
		}
	});
	
	this.res.add(THISDayRes.startres.res);
	this.res.add(THISDayRes.endres.res);
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
	
	this.setInfo = function(in_info) {
		info = in_info;
	}
}

// Unload ////////////////////////////////////

app.onunload = function() {
	app.dump("Clear Timeouts");
	for (var timer in timeouts) {
		app.clearTimeout(timeouts[timer]);
	}
	
	app.dump("Delete all timed requests");
	// delete timed requests
	deleteTimedRequest(DAYSTART_TASK);
	deleteTimedRequest(DAYEND_TASK);
	deleteTimedRequest(SUNRISE_TASK);
	deleteTimedRequest(SUNSET_TASK);
}