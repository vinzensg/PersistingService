var sensor = new Sensor("sensor");
var power = new Power("power", false);
var wheel = new Wheel("wheel", 0);
var heater = new Heater("heater", 22);

app.root.add(sensor.res);
app.root.add(power.res);
app.root.add(wheel.res);
app.root.add(heater.res);

function Sensor(resid) {
	var THIS = this;
	
	this.data = new Array(20, 21, 23, 24, 24, 24, 25, 23, 22, 20, 19, 18, 20, 21, 22);
	this.counter = 0;
	
	this.res = new JavaScriptResource(resid);
	this.res.isObservable(true);
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, ""+THIS.data[(THIS.counter++)%15]);
	};
}

function Power(resid, power) {
	var THIS = this;
	
	this.power = power;
	
	this.res = new JavaScriptResource(resid);
	this.res.isObservable(true);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, "power = " + THIS.power);
	};
	
	this.res.onput = function(request) {		
		var payload = request.getPayloadString();
		if (payload == "true" ||Êpayload == "false") {
			THIS.power = payload;
			request.respond(CodeRegistry.RESP_CONTENT, "Power was set: power = " + THIS.power);
		} else {
			request.respond(CodeRegistry.RESP_CONTENT, "Invalid post payload.");
		}
		
	};
}

function Wheel(resid, value) {
	var THIS = this;
	
	this.value = value;
	
	this.res = new JavaScriptResource(resid);
	this.res.isObservable(true);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, THIS.value);
	};
	
	this.res.onput = function(request) {
		var payload = request.getPayloadString();
		if (payload >= 0 && payload <= 50) {
			THIS.value = payload;
			request.respond(CodeRegistry.RESP_CONTENT, "Value was set: value = " + THIS.value);
		} else {
			request.respond(CodeRegistry.RESP_CONTENT, "Invalid post payload.");
		}
	};
}

function Heater(resid, heat) {
	var THIS = this;
	
	this.heat = heat;
	
	this.res = new JavaScriptResource(resid);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, "heat = " + THIS.heat);
	};
	
	this.res.onput = function(request) {
		var payload = request.getPayloadString();
		if (!isNaN(payload)) {
			THIS.heat = payload;
			request.respond(CodeRegistry.RESP_CONTENT, "Heat was set: heat = " + THIS.heat);
		}
	};
}


var temperature = new Temperature("temperature", 23);
var temperature2 = new Temperature("temperature2", 20);

app.root.add(temperature.res);
app.root.add(temperature2.res);

function Temperature(resid, temp) {
	var THIS = this;
	
	this.temp = temp;
	
	this.res = new JavaScriptResource(resid);
	this.res.isObservable(true);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, THIS.temp);
	}
	
	this.res.onput = function(request) {
		var payload = request.getPayloadString();
		THIS.temp = payload;
		THIS.res.changed();
		request.respond(CodeRegistry.RESP_CHANGED);
	}
}

var info = new InfoRes("info", "0");

app.root.add(info.res);

//Info Resources ////////////////////////////////////////
function InfoRes(resid, in_info) {
	var THISInfo = this;
	
	var info = in_info;
	
	this.res = new JavaScriptResource(resid);
	this.res.isObservable(true);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, info);
	}
	
	this.res.onput = function(request) {
		var payload = request.getPayloadString();
		THISInfo.setInfo(payload);
	}
	
	this.getInfo = function() {
		return info;
	}
	
	this.setInfo = function(in_info) {
		app.dump("==NEW==");
		app.dump("in_info: " + in_info);
		app.dump("info: " + info);
		app.dump("in_info != info: " + (in_info != info));
		if (in_info != info) {
			app.dump("call changed");
			THISInfo.res.changed();
		}
		info = in_info;
	}
}

var realtemperature = new RealTemperature("realtemperature", 22.0);

app.root.add(realtemperature.res);

var TEMP_INTERVAL = 10*60*1000;

function RealTemperature(resid, temp) {
	var THISRealTemp = this;
	
	var timeout = null;
	
	this.temp = temp;
	
	this.res = new JavaScriptResource(resid);
	this.res.isObservable(true);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, THISRealTemp.temp);
	}
	
	this.res.onput = function(request) {
		var payload = request.getPayloadString();
		if (isNumber(payload)) {
			if (THISRealTemp.temp!=payload) {
				if (timeout) app.clearTimeout(timeout);
				timeout = app.setTimeout(updateTemp, TEMP_INTERVAL, parseFloat(THISRealTemp.temp), parseFloat(payload));				
			}
			request.respond(CodeRegistry.RESP_CHANGED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST);
		}
		
	}
	
	function updateTemp(current, target) {
		if (target-current==0) return;
		
		if (Math.abs(target-current)<0.5) {
			THISRealTemp.temp = target;
			THISRealTemp.res.changed();
			return;
		}
		
		if (target-current > 0) {
			THISRealTemp.temp = (current+0.5) + "";
		} else { // target-current < 0
			THISRealTemp.temp = (current-0.5) + "";
		}
		THISRealTemp.res.changed();
		
		timeout = app.setTimeout(updateTemp, TEMP_INTERVAL, parseFloat(THISRealTemp.temp), target);
	}
	
	function isNumber(n) {
		return !isNaN(parseFloat(n)) && isFinite(n);
	}
}

