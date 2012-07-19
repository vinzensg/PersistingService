var THIS = this;

var APP_NAME = "thermostat_vacation_control";

var MULTIPLE_AGGREGATE = "coap://localhost:5685/apps/running/multiple_aggregate/tasks";
var CONTROL_LOOP = "coap://localhost:5685/apps/running/control_loop/tasks";

var CHANGE_CONTROL_LOOP = "coap://localhost:5685/apps/running/control_loop/tasks/"+APP_NAME+"/modifyfunc";  

var FORECAST_RESOURCE = "coap://localhost:5685/apps/running/weather/tomorrow/low_temperature";
var TEMPERATURE_RESOURCE = "coap://localhost:5685/apps/running/weather/now/temperature";

var THERMOSTAT_TARGET = "coap://localhost:5685/apps/running/thermostat/temperature";

var OUTDOOR_THRESHOLD = 0;
var INDOOR_MAX = 20;
var INDOOR_MIN = 10;

// Add SubResources ////////////////////////////////

this.outdoorthresholdres = new ChangeableInfoRes("outdoor_threshold", OUTDOOR_THRESHOLD);
this.indoormaxres = new ChangeableInfoRes("indoor_max", INDOOR_MAX);
this.indoorminres = new ChangeableInfoRes("indoor_min", INDOOR_MIN);
this.runningres = new RunningRes("running", "false");

app.root.add(THIS.outdoorthresholdres.res);
app.root.add(THIS.indoormaxres.res);
app.root.add(THIS.indoorminres.res);
app.root.add(THIS.runningres.res);

function computeModifyfunc() {
	return "own;;" +
	"var temp_device = value.split(';');" + "\n" +
	"if (!storage[1]) storage[1]=Number.MAX;" + "\n" +
	"if (!storage[2]) storage[2]=Number.MAX;" + "\n" +
	"storage[temp_device[1]] = temp_device[0];" + "\n" +
	"if (storage[1]<"+THIS.outdoorthresholdres.getInfo()+") {" + "\n" +
	"	ret = "+THIS.indoormaxres.getInfo()+";" + "\n" +
	"} else if (storage[2]<"+THIS.outdoorthresholdres.getInfo()+") {" + "\n" +
	"	ret = "+(THIS.indoormaxres.getInfo()+THIS.indoorminres.getInfo())/2+";" + "\n" +
	"} else {" + "\n" +
	"	ret = "+THIS.indoorminres.getInfo()+";" + "\n" +
	"}" + "\n" +
	";;";
}

function start() {
	var aggregate_payload = "resid = " + APP_NAME + "\n"+ 
							"source1 = " + TEMPERATURE_RESOURCE + "\n" +
							"source2 = " + FORECAST_RESOURCE + "\n" +
							"aggregatefunc = newest";
	
	var aggregate_request = new CoAPRequest();
	aggregate_request.open("POST", MULTIPLE_AGGREGATE);
	aggregate_request.async = false;
	aggregate_request.send(aggregate_payload);
	
	var response = aggregate_request.responseText; // block
		
	var control_payload = "resid = " + APP_NAME + "\n" + 
						  "source = " + MULTIPLE_AGGREGATE + "/" + APP_NAME + "/aggregate?withdevice=true" + "\n" +
						  "target = " + THERMOSTAT_TARGET + "\n" +
						  "targetoperation = PUT" + "\n" +
						  "modifyfunc = " + computeModifyfunc();
	
	var control_request = new CoAPRequest();
	control_request.open("POST", CONTROL_LOOP);
	control_request.async = false;
	control_request.send(control_payload);
}

function stop() {
	var aggregate_request = new CoAPRequest();
	aggregate_request.open("DELETE", MULTIPLE_AGGREGATE+"/"+APP_NAME);
	aggregate_request.send();
	
	var control_request = new CoAPRequest();
	control_request.open("DELETE", CONTROL_LOOP+"/"+APP_NAME);
	control_request.send();
}

// Running Resource ////////////////////////////////
function RunningRes(resid, in_running) {
	
	var running = in_running;
	
	this.res = new JavaScriptResource(resid);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, running);
	}
	
	this.res.onput = function(request) {
		var payload = request.getPayloadString();
		if (payload=="true") {
			if (running!=payload) {
				start();
				running = "true";
			}
			request.respond(CodeRegistry.RESP_CHANGED);
		} else if (payload=="false") {
			if (running != payload) {
				stop();
				running = "false;"
			}
			request.respond(CodeRegistry.RESP_CHANGED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST);
		}
	}
}

// Info Resource ///////////////////////////////////

function ChangeableInfoRes(resid, in_info, in_change_target) {
	var info = in_info;
		
	this.res = new JavaScriptResource(resid);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, info);
	}
	
	this.res.onput = function(request) {
		in_info = request.getPayloadString();
		if (info != in_info) {
			info = in_info;
			var changerequest = new CoAPRequest();
			changerequest.open("PUT", CHANGE_CONTROL_LOOP);
			changerequest.send(computeModifyfunc());
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

// Clean up ////////////////////////////////////////
app.onunload = function() {
	var aggregate_request = new CoAPRequest();
	aggregate_request.open("DELETE", MULTIPLE_AGGREGATE+"/"+APP_NAME);
	aggregate_request.send();
	
	var control_request = new CoAPRequest();
	control_request.open("DELETE", CONTROL_LOOP+"/"+APP_NAME);
	control_request.send();
}