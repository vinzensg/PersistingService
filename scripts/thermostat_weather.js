// Import Packages ////////////////////////////////////

var TODAY = 0;
var TOMORROW = 1;
var CITY = "ZŸrich";
var CITY_CODE = "12893366";

var THIS = this;
this.poll = 60*60*60*1000; // every hour
this.cont = true,

timeout = null;

// Add SubResource /////////////////////////////////////

this.cityres = new InfoRes("city", "ZŸrich");
this.pollres = new ChangeableInfoRes("poll", THIS.poll);
this.nowres = new NowRes("now");
this.todayres = new DayRes("today");
this.tomorrowres = new DayRes("tomorrow");
this.astronomyres = new AstronomyRes("astronomy");

app.root.addSubResource(THIS.cityres.res);
app.root.addSubResource(THIS.pollres.res);
app.root.addSubResource(THIS.nowres.res);
app.root.addSubResource(THIS.todayres.res);
app.root.addSubResource(THIS.tomorrowres.res);
app.root.addSubResource(THIS.astronomyres.res);


// Get Weather from Yahoo ///////////////////////////////

timeout = app.setTimeout(fetchWeatherData, this.pollres.getRawInfo());

function fetchWeatherData() {
			var rawWeather = "";
			
			var url = new URL("http://weather.yahooapis.com/forecastjson?w=" + CITY_CODE + "&u=c");
				
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
			
			timeout = app.setTimeout(fetchWeatherData, THIS.pollres.getRawInfo());
}

// Now Resource ///////////////////////////////////////

function NowRes(resid) {
	var THISNowRes = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.tempres = new InfoRes("temperature", "0.0");
	
	this.res.addSubResource(THISNowRes.tempres.res);	
}

// Day Resource ////////////////////////////////////////

function DayRes(resid) {
	var THISDayRes = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.hightempres = new InfoRes("high_temperature", "0.0");
	this.lowtempres = new InfoRes("low_temperature", "0.0");
	
	this.res.addSubResource(THISDayRes.hightempres.res);
	this.res.addSubResource(THISDayRes.lowtempres.res);
}

// Astronomy Resource ///////////////////////////////////

function AstronomyRes(resid) {
	var THISAstronomyRes = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.sunriseres = new InfoRes("sunrise", "0:00");
	this.sunsetres = new InfoRes("sunset", "0:00");
	
	this.res.addSubResource(THISAstronomyRes.sunriseres.res);
	this.res.addSubResource(THISAstronomyRes.sunsetres.res);
}

//Info Resources ////////////////////////////////////////

function InfoRes(resid, info) {
	var THISInfo = this;
	
	this.info = info;
	
	this.prototype.res = new JavaScriptResource(resid);
	this.prototype.res.isObservable(true);
	
	// Requests /////////////////////////////////////////
	this.prototype.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, THISInfo.getInfo());
	}
	
	this.prototype.getInfo = function() {
		if (THISInfo.info == "" ||ÊTHISInfo.info == 0) {
			return "EMPTY";
		} else {
			return THISInfo.info;
		}
	}
	
	this.prototype.getRawInfo = function() {
		return THISInfo.info;
	}
	
	this.prototype.setInfo = function(info) {
		THISInfo.info = info;
		THISInfo.prototype.res.changed();
	}
}

function ChangeableInfoRes(resid, info) {
	var THISChangeableInfo = this;
	this.prototype = new InfoRes(resid, info);
	var THISChangeableInfo_prot = this.prototype;
	
	this.info = THISChangeableInfo_prot.info;
	
	THISChangeableInfo.res.onput = function(request) {
		var payload = parseInt(request.getPayloadString());
		THISChangeableInfo.setInfo(payload);
		request.respond(CodeRegistry.RESP_CHANGED);
	}
}

app.onunload() {
	if (timeout)
		app.clearTimeout(timeout);
}