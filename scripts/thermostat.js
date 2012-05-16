var sensor = new Sensor("sensor");
var power = new Power("power", false);
var wheel = new Wheel("wheel", 0);
var heater = new Heater("heater", 22);

app.root.addSubResource(sensor.res);
app.root.addSubResource(power.res);
app.root.addSubResource(wheel.res);
app.root.addSubResource(heater.res);

function Sensor(resid) {
	var THIS = this;
	
	this.data = new Array(20, 21, 23, 24, 24, 24, 25, 23, 22, 20, 19, 18, 20, 21, 22);
	this.counter = 0;
	
	this.res = new JavaScriptResource(resid);
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

app.root.addSubResource(temperature.res);

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

