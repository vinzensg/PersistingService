var THIS = this;

// Constants //////////////////////////////////////////////
var TIME_FORMAT = "HH:mm";
var DATE_FORMAT = "yyyy/MM/dd-HH:mm:ss";

var THERMOSTAT_TARGET = "coap://localhost:5685/apps/running/thermostat/temperature";
var TIMED_REQUEST = "coap://localhost:5685/apps/running/timed_request/tasks";

var URL_SUNRISE = "coap://localhost:5685/apps/running/weather/astronomy/sunrise";
var URL_SUNSET = "coap://localhost:5685/apps/running/weather/astronomy/sunset";

var TWENTY_FOUR = 60*60*24*1000;

var DEFAULT_DAY_TEMP = 24;
var DEFAULT_NIGHT_TEMP = 20;
var DEFAULT_DAY_START = "07:00";
var DEFAULT_DAY_END = "21:08";

var TIMEOUT0 = 0;
var TIMEOUT1 = 1;
var TIMEOUT2 = 2;
var TIMEOUT3 = 3;

// Variables ///////////////////////////////////////////////
var timeouts = new Array();

var sunrise = "00:00";
var sunset = "00:00";

var timeFormat = new SimpleDateFormat(TIME_FORMAT);
var dateFormat = new SimpleDateFormat(DATE_FORMAT);

// Add SubResource /////////////////////////////////////////

this.tempres = new TempRes("temperature");
this.daytimesres = new DayTimesRes("day");

app.root.addSubResource(THIS.tempres.res);
app.root.addSubResource(THIS.daytimesres.res);

// Get Data from Weather App ///////////////////////////////

var now = new Date();
var calNow = Calendar.getInstance();
calNow.setTime(now);

var calNoon = todayWithTime("21:06");

var difference = calNoon.getTimeInMillis() - calNow.getTimeInMillis();
if (difference > 0) {
	timeouts[TIMEOUT1] = app.setTimeout(checkSun, difference);
} else {
	var getRise = new CoAPRequest();
	app.dump("GET SUNRISE diff");
	getRise.open("GET", URL_SUNRISE);
	getRise.onload = prepareDayTemp
	getRise.send();
	
	var getSet = new CoAPRequest();
	app.dump("GET SUNSET diff");
	getSet.open("GET", URL_SUNSET);
	getSet.onload = prepareNightTemp
	getSet.send();
	
	var nextCheck = TWENTY_FOUR + difference;
	timeouts[TIMEOUT2] = app.setTimeout(checkSun, nextCheck);
}

function checkSun() {
	var getRise = new CoAPRequest();
	app.dump("GET SUNRISE diff");
	getRise.open("GET", URL_SUNRISE);
	getRise.onload = prepareDayTemp
	getRise.send();
	
	var getSet = new CoAPRequest();
	app.dump("GET SUNSET diff");
	getSet.open("GET", URL_SUNSET);
	getSet.onload = prepareNightTemp
	getSet.send();
	
	timeouts[TIMEOUT3] = app.setTimeout(checkSun, TWENTY_FOUR);
}

function prepareDayTemp(request) {
	var payload = request.getPayloadString();
	sunrise = payload;
	updateDaystart(sunrise);
	app.dump("Sunrise: " + payload);
}

function prepareNightTemp(request) {
	var payload = request.getPayloadString();
	sunset = payload;
	updateDayend(sunset);
	app.dump("Sunset: " + payload);
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

function updateDaystart(sunrise) {
	var now = new Date();
	var calNow = Calendar.getInstance();
	calNow.setTime(now);
	
	var calSunrise = todayWithTime(sunrise);
	var calDaystart = todayWithTime(THIS.daytimesres.startres.getRawInfo());
	
	app.dump("Sunrise: " + calSunrise.getTimeInMillis());
	app.dump("Daystart: " + calDaystart.getTimeInMillis());
	if (calSunrise.getTimeInMillis() >= (calDaystart.getTimeInMillis() + (60*30*1000))) {
		// send before sunrise
		var calTomorrow = calDaystart;
		calTomorrow.add(Calendar.DAY_OF_YEAR, 1);
		calTomorrow.add(Calendar.MINUTE, -30);
		
		if (calTomorrow.getTimeInMillis() > (calNow.getTimeInMillis() + (5*1000))) {
			var setDaystart = new CoAPRequest();
			setDaystart.open("POST", TIMED_REQUEST);
			var payloadDaystart = "resid = daystart\n" +
						  	 	  "device = " + THERMOSTAT_TARGET + "\n" +
						  	 	  "operation = PUT\n" +
						  	 	  "payload = " + (THIS.tempres.dayres.getRawInfo() + 1) + "\n" +
						  	 	  "datetime = " + dateFormat.format(calTomorrow.getTime());
			app.dump("Send daystart");
			setDaystart.send(payloadDaystart);
		}
		// send after sunrise
		calTomorrow = calSunrise;
		calTomorrow.add(Calendar.DAY_OF_YEAR, 1);
		
		if (calTomorrow.getTimeInMillis() > (calNow.getTimeInMillis() + (5*1000))) {
			var setSunrise = new CoAPRequest();
			setSunrise.open("POST", TIMED_REQUEST);
			var payloadSunrise = "resid = sunrise\n" +
						  		 "device = " + THERMOSTAT_TARGET + "\n" +
						  		 "operation = PUT\n" +
						  		 "payload = " + THIS.tempres.dayres.getRawInfo() + "\n" +
						  		 "datetime = " + dateFormat.format(calTomorrow.getTime());
			app.dump("Send sunrise");
			setSunrise.send(payloadSunrise);
		}
	} else {
		// send daystart - 30min 
		var calTomorrow = calDaystart;
		calTomorrow.add(Calendar.DAY_OF_YEAR, 1);
		
		if (calTomorrow.getTimeInMillis() > (calNow.getTimeInMillis() + (5*1000))) {
			var setDaystart = new CoAPRequest();
			setDaystart.open("POST", TIMED_REQUEST);
			var payloadDaystart = "resid = daystart\n" +
						  		  "device = " + THERMOSTAT_TARGET + "\n" +
						  		  "payload = " + (THIS.tempres.dayres.getRawInfo()) + "\n" +
						  		  "operation = PUT\n" +
						  		  "datetime = " + dateFormat.format(calTomorrow.getTime());
			app.dump("Send daystart");
			setDaystart.send(payloadDaystart);
		}
	}
}

function updateDayend(sunset) {
	var now = new Date();
	var calNow = Calendar.getInstance();
	calNow.setTime(now);
	
	var calSunset = todayWithTime(sunset);
	var calDayend = todayWithTime(THIS.daytimesres.endres.getRawInfo());
	
	app.dump("Sunset: " + calSunset.getTimeInMillis());
	app.dump("Dayend: " + calDayend.getTimeInMillis());
	if ((60*15*1000) + calSunset.getTimeInMillis() <= (calDayend.getTimeInMillis())) {
		// send before sunset -15min
		calToday = calSunset;
		calToday.add(Calendar.MINUTE, -15);
		
		if (calToday.getTimeInMillis() > (calNow.getTimeInMillis() + (5*1000))) {
			var setSunset = new CoAPRequest();
			setSunset.open("POST", TIMED_REQUEST);
			var payloadSunset = "resid = sunset\n" +
						  		"device = " + THERMOSTAT_TARGET + "\n" +
						  		"operation = PUT\n" +
						  		"payload = " + (THIS.tempres.dayres.getRawInfo() + 1) + "\n" +
						  		"datetime = " + dateFormat.format(calToday.getTime());
			app.dump("Send sunset");
			setSunset.send(payloadSunset);
		}
		
		// send after dayend
		var calToday = calDayend;
		
		if (calToday.getTimeInMillis() > (calNow.getTimeInMillis() + (5*1000))) {
			var setDayend = new CoAPRequest();
			setDayend.open("POST", TIMED_REQUEST);
			var payloadDayend = "resid = dayend\n" +
						  	 	 "device = " + THERMOSTAT_TARGET + "\n" +
						  	 	 "operation = PUT\n" +
						  	 	 "payload = " + THIS.tempres.nightres.getRawInfo() + "\n" +
						  	 	 "datetime = " + dateFormat.format(calToday.getTime());
			app.dump("Send dayend");
			setDayend.send(payloadDayend);
		}
	} else {
		// send after dayend
		var calToday = calDayend;
		
		app.dump("Day end: " + dateFormat.format(calToday.getTime()));
		if (calToday.getTimeInMillis() > (calNow.getTimeInMillis() + (5*1000))) {
			var setDayend = new CoAPRequest();
			setDayend.open("POST", TIMED_REQUEST);
			var payloadDayend = "resid = dayend\n" +
						  		"device = " + THERMOSTAT_TARGET + "\n" +
						  		"operation = PUT\n" +
						  		"payload = " + (THIS.tempres.nightres.getRawInfo()) + "\n" +
						  		"datetime = " + dateFormat.format(calToday.getTime());
			app.dump("Send dayend");
			setDayend.send(payloadDayend);
		}
	}
}

// Resources ///////////////////////////////////////////

function TempRes(resid) {
	var THISTempRes = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.dayres = new ChangeableInfoRes("day", DEFAULT_DAY_TEMP);
	this.nightres = new ChangeableInfoRes("night", DEFAULT_NIGHT_TEMP);
	
	this.res.addSubResource(THISTempRes.dayres.res);
	this.res.addSubResource(THISTempRes.nightres.res);
}

function DayTimesRes(resid) {
	var THISDayRes = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.startres = new ChangeableInfoRes("start", DEFAULT_DAY_START);
	this.endres = new ChangeableInfoRes("end", DEFAULT_DAY_END);
	
	this.res.addSubResource(THISDayRes.startres.res);
	this.res.addSubResource(THISDayRes.endres.res);
}


//Info Resources ////////////////////////////////////////

function InfoRes(resid, info) {
	var THISInfo = this;
	
	this.info = info;
	
	this.res = new JavaScriptResource(resid);
	this.res.isObservable(true);
	
	// Requests /////////////////////////////////////////
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, THISInfo.getInfo());
	}
	
	this.getInfo = function() {
		if (THISInfo.info == "" ||ÊTHISInfo.info == 0) {
			return "EMPTY";
		} else {
			return THISInfo.info;
		}
	}
	
	this.getRawInfo = function() {
		return THISInfo.info;
	}
	
	this.setInfo = function(info) {
		THISInfo.info = info;
	}
}

function ChangeableInfoRes(resid, info) {
	var THISChangeableInfo = this;
	this.prototype = new InfoRes(resid, info);
	var THISChangeableInfo_prot = this.prototype;
	
	this.info = THISChangeableInfo_prot.info;
	this.res = THISChangeableInfo_prot.res;
	this.getInfo = THISChangeableInfo_prot.getInfo
	this.getRawInfo = THISChangeableInfo_prot.getRawInfo
	
	THISChangeableInfo.res.onput = function(request) {
		var payload = request.getPayloadString();
		THISChangeableInfo_prot.info = payload;
		THISChangeableInfo.info = payload;
		
		/*
		updateDaystart(sunrise);
		updateDayend(sunset);
		*/
		
		request.respond(CodeRegistry.RESP_CHANGED);
	}
}

// Unload ////////////////////////////////////

app.onunload = function() {
	app.dump("Clear Timeouts");
	for (var timer in timeouts) {
		app.clearTimeout(timeouts[timer]);
	}
}