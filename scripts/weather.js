// Import Packages ////////////////////////////////////

var TODAY = 0;
var TOMORROW = 1;
var CITY = "ZŸrich";
var CITY_CODE = "12893366";

var TIME_FORMAT = "HH:mm";
var DATE_FORMAT = "yyyy/MM/dd-HH:mm:ss";

var timeFormat = new SimpleDateFormat(TIME_FORMAT);
var dateFormat = new SimpleDateFormat(DATE_FORMAT);

var THIS = this;
this.poll = 5*60*1000; // every 5 minutes
this.cont = true,

timeout = null;

// Add SubResource /////////////////////////////////////

this.cityres = new InfoRes("city", "ZŸrich");
this.pollres = new ChangeableInfoRes("poll", THIS.poll);
this.nowres = new NowRes("now");
this.todayres = new DayRes("today");
this.tomorrowres = new DayRes("tomorrow");
this.astronomyres = new AstronomyRes("astronomy");

app.root.add(THIS.cityres.res);
app.root.add(THIS.pollres.res);
app.root.add(THIS.nowres.res);
app.root.add(THIS.todayres.res);
app.root.add(THIS.tomorrowres.res);
app.root.add(THIS.astronomyres.res);


// Get Weather from Yahoo ///////////////////////////////

fetchWeatherData();

function fetchWeatherData() {
	app.dump("Request Weather");
	var rawWeather = "";
	
	var url = null;
	try {
		url = new URL("http://weather.yahooapis.com/forecastjson?w=" + CITY_CODE + "&u=c");
		
		var inreader = new BufferedReader(new InputStreamReader(url.openStream()));
		var inputLine;
		while ((inputLine = inreader.readLine()) != null) {
			 rawWeather += inputLine + "\n";
		}
		inreader.close();
	 
		app.dump(rawWeather);
		var jsonWeather = eval('(' + rawWeather + ')');
	
		THIS.nowres.tempres.setInfo(jsonWeather.condition.temperature);
	
		THIS.todayres.hightempres.setInfo(jsonWeather.forecast[TODAY].high_temperature);
		THIS.todayres.lowtempres.setInfo(jsonWeather.forecast[TODAY].low_temperature);
		
		THIS.tomorrowres.hightempres.setInfo(jsonWeather.forecast[TOMORROW].high_temperature);
		THIS.tomorrowres.lowtempres.setInfo(jsonWeather.forecast[TOMORROW].low_temperature);
		
		THIS.astronomyres.sunriseres.setInfo(jsonWeather.astronomy.sunrise);
		THIS.astronomyres.sunsetres.setInfo(jsonWeather.astronomy.sunset);
		
	} catch (e if e.javaException instanceof UnknownHostException) {
		app.dump("Unknown Host " + url + " - Reason: Probably no internet connection available.");
	}
	
	timeout = app.setTimeout(fetchWeatherData, THIS.pollres.getInfo());
}

// Now Resource ///////////////////////////////////////

function NowRes(resid) {
	var THISNowRes = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.tempres = new InfoRes("temperature", "0.0");
	
	this.res.add(THISNowRes.tempres.res);	
}

// Day Resource ////////////////////////////////////////

function DayRes(resid) {
	var THISDayRes = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.hightempres = new InfoRes("high_temperature", "0.0");
	this.lowtempres = new InfoRes("low_temperature", "0.0");
	
	this.res.add(THISDayRes.hightempres.res);
	this.res.add(THISDayRes.lowtempres.res);
}

// Astronomy Resource ///////////////////////////////////

function AstronomyRes(resid) {
	var THISAstronomyRes = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.sunriseres = new SunInfoRes("sunrise", "00:00");
	this.sunsetres = new SunInfoRes("sunset", "00:00");
	
	this.res.add(THISAstronomyRes.sunriseres.res);
	this.res.add(THISAstronomyRes.sunsetres.res);
	
	function SunInfoRes(resid, info) {
		var THISSunInfo = this;
		
		this.info = info;
		
		this.res = new JavaScriptResource(resid);
		this.res.isObservable(true);
		
		// Requests /////////////////////////////////////////
		this.res.onget = function(request) {
			var withDate = false;
			var query = request.getQuery();
			
			if (query != "") {
				var option = query.substring(1, query.length()).split('=');
				if (option[0]=="completedate" && option[1]=="true")
					withDate = true;
			}
			if (withDate) {
				request.respond(CodeRegistry.RESP_CONTENT, todayWithTime(THISSunInfo.info));
			} else {
				request.respond(CodeRegistry.RESP_CONTENT, THISSunInfo.info);
			}
		}
		
		this.getInfo = function() {
			return THISSunInfo.info;
		}
		
		this.setInfo = function(info) {
			var old = THISSunInfo.info;
			THISSunInfo.info = info;
			if (old != info)
				THISSunInfo.res.changed();
		}
		
		function todayWithTime(time) {
			var calToday = Calendar.getInstance();
			
			var dateTime = timeFormat.parse(time);
			var cal = Calendar.getInstance();
			cal.setTime(dateTime);
			
			var dateToday = new Date();
			cal.set(Calendar.YEAR, calToday.get(Calendar.YEAR));
			cal.set(Calendar.MONTH, calToday.get(Calendar.MONTH));
			cal.set(Calendar.DAY_OF_YEAR, calToday.get(Calendar.DAY_OF_YEAR));
			
			return dateFormat.format(cal.getTime());
		}
	}
}

//Info Resources ////////////////////////////////////////
function InfoRes(resid, in_info) {
	var THISInfo = this;
	
	var info = in_info;
	
	this.res = new JavaScriptResource(resid);
	this.res.isObservable(true);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, info);
	}
	
	this.getInfo = function() {
		return info;
	}
	
	this.setInfo = function(in_info) {
		if (in_info != info)
			THISInfo.res.changed();
		info = in_info;
	}
}

function ChangeableInfoRes(resid, in_info) {
	var THISChangeableInfo = this;
	
	var info = in_info;
	
	this.res = new JavaScriptResource(resid);
	this.res.isObservable(true);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, info);
	}
	
	this.res.onput = function(request) {
		var in_info = request.getPayloadString();
		if (in_info != info)
			THISChangeableInfo.res.changed();
		info = in_info;
		request.respond(CodeRegistry.RESP_CHANGED);
	}
	
	this.getInfo = function() {
		return info;
	}
	
	this.setInfo = function(in_info) {
		if (in_info != info)
			THISChangeableInfo.res.changed();
		info = in_info;
	}
}


// Clean up /////////////////////////////////////////////
app.onunload = function() {
	if (timeout)
		app.clearTimeout(timeout);
}