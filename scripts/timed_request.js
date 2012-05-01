// Import Packages /////////////////////////////////////
importPackage(Packages.java.text);
importPackage(Packages.java.util);

var THIS = this;

var DATE_FORMAT = "yyyy/MM/dd-HH:mm:ss";
var DATE_FORMAT_DAY = "yyyy/MM/dd";

// Add SubResources ///////////////////////////////////

this.tasksres = new Tasks("task");

app.root.addSubResource(THIS.tasksres.res);

// Tasks /////////////////////////////////////////////

function Tasks(resid) {
	var THISTasks = this;
	
	this.res = new JavaScriptResource(resid);

	this.postres = new RequestTask("post", "POST", true);
	this.putres = new RequestTask("put", "PUT", true);
	this.deleteres = new RequestTask("delete", "DELETE", false);
	
	this.res.addSubResource(THISTasks.postres.res);
	this.res.addSubResource(THISTasks.putres.res);
	this.res.addSubResource(THISTasks.deleteres.res);
}

// Timed Task ///////////////////////////////////////

function RequestTask(resid, operation, has_payload) {
	var THISRequestTask = this;
	
	this.operation = operation;
	
	this.res = new JavaScriptResource(resid);
	
	this.res.onpost = function(request) {
		var payload = request.getPayloadString();
		var pp = new PayloadParser(payload);
		if (pp.has("resid") && pp.has("device") && pp.has("datetime")) {
			var payl = "";
			if (!has_payload) {
				payl = null;
			} else if (pp.has("payload")) {
				payl = pp.get("payload");
			}
			var date = new DateFormatter(pp.get("datetime")).getFormated();
			THISRequestTask.addRes(pp.get("resid"), pp.get("device"), payl, date);
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			if (has_payload) {
				request.respond("Provide:\n" +
							"resid = ...\n" +
							"device = ...\n" +
							"datetime = ..." +
							"(payload = ...\n)");
			} else {
				request.respond("Provide:\n" +
						"resid = ...\n" +
						"device = ...\n" +
						"datetime = ...");
			}
							
		}
	}
	
	this.addRes = function(resid, device, payload, datetime) {
		var performTask = new Perform(THISRequestTask.operation, resid, device, payload, datetime);
		THISRequestTask.res.addSubResource(performTask.res);
		performTask.run();
	}
}

// Perform Task //////////////////////////////////////////

function Perform(operation, resid, device, payload, datetime) {
	var THISPerform = this;
	
	this.thread = null;
	
	this.operation = operation;
	
	this.res = new JavaScriptResource(resid);
	
	this.deviceres = new InfoRes("device", device);
	this.datetimeres = new InfoRes("datetime", datetime);

	this.res.addSubResource(THISPerform.deviceres.res);
	this.res.addSubResource(THISPerform.datetimeres.res);

	if (payload != null) {
		this.payloadres = new InfoRes("payload", payload);
		this.res.addSubResource(THISPerform.payloadres.res);
	}
	
	// Requests //////////////////////////////////////////
	this.res.ondelete = function(request) {
		THISPerform.thread.interrupt();
		request.respond(CodeRegistry.RESP_DELETED);
	}
	
	// Functions /////////////////////////////////////////
	
	this.run = function() {
		THISPerform.thread = new Thread(function() {
			var dateFormat = new SimpleDateFormat(DATE_FORMAT);
			var date = dateFormat.parse(THISPerform.datetimeres.info);
			var today = new Date();
			
			var calDate = Calendar.getInstance();
			calDate.setTime(date);
			var calToday = Calendar.getInstance();
			calToday.setTime(today);
			
			var difference = calDate.getTimeInMillis() - calToday.getTimeInMillis();
			if (difference > 0) {
				try {
					app.dump("Difference: " + difference);
					java.lang.Thread.sleep(difference);
					THISPerform.performSend();
				} catch (e if e.javaException instanceof InterruptedException) {
					app.dump("Spleeping was interrupted");
				}
			}
			THISPerform.res.remove();
		});
		THISPerform.thread.start();
	}
	
	this.performSend = function() {
		var client = new CoAPRequest();
		client.open(THISPerform.operation , THISPerform.deviceres.info);
		if (THISPerform.payloadres) {
			client.send(THISPerform.payloadres.info);
		} else {
			client.send();
		}
	}
}

//Info Resources ////////////////////////////////////////
function InfoRes(resid, info) {
	var THISInfo = this;
	
	this.info = info;
	
	this.res = new JavaScriptResource(resid);
	
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
}

function ChangeableInfoRes(resid, info) {
	var THISChangeableInfo = this;
	this.prototype = new InfoRes(resid, info);
	var THISChangeableInfo_prot = this.prototype;
	
	this.info = THISChangeableInfo_prot.info;
	this.res = THISChangeableInfo_prot.res;
	this.getInfo = THISChangeableInfo_prot.getInfo
	
	THISChangeableInfo.res.onput = function(request) {
		var payload = parseInt(request.getPayloadString());
		THISChangeableInfo.info = payload;
		request.respond(CodeRegistry.RESP_CHANGED);
	}
}

// Create Standard Date Format ///////////////////////////
// Format: yyyy/MM/dd-HH:mm:ss
function DateFormatter(date) {
	this.date = date;
	
	this.getFormated = function() {
		var newDate = "";
		app.dump("Date length: " + date.length);
		switch(date.length) {
		case 19:
			newDate = date;
			break;
		case 10:
			newDate = date + "-00:00:00";
			break;
		case 8:
			var dateFormat = new SimpleDateFormat(DATE_FORMAT_DAY)
			var today = new Date();
			newDate = dateFormat.format(today) + "-" + date;
			break;
		default:
			newDate = false;
		}
		
		return newDate;
	}
}

//Payload Parser /////////////////////////////////////////
function PayloadParser(payload) {
	this.map = new Object();
	
	var pl = payload.split(' ').join('');
	var split_nl = pl.split('\n');
	for (el in split_nl) {
		var split_eq = split_nl[el].split('=');
		this.map[split_eq[0]] = split_eq[1];
	}
	
	this.get = function(element) {
		return this.map[element];
	}
	
	this.has = function(element) {
		if (this.map[element]) {
			return true;
		} else {
			return false;
		}
	}
}