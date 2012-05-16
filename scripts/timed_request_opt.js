// Import Packages /////////////////////////////////////
var THIS = this;

var DATE_FORMAT = "yyyy/MM/dd-HH:mm:ss";
var DATE_FORMAT_DAY = "yyyy/MM/dd";

var perform_tasks = new Array();

// Add SubResources ///////////////////////////////////

this.tasksres = new Tasks("tasks");

app.root.addSubResource(THIS.tasksres.res);

// Tasks /////////////////////////////////////////////

function Tasks(resid) {
	var THISTasks = this;
	
	this.res = new JavaScriptResource(resid);

	this.res.onpost = function(request) {
		app.dump("Recieve post");
		var payload = request.getPayloadString();
		var pp = new PayloadParser(payload);
		if (pp.has("resid") && pp.has("device") && pp.has("operation"), pp.has("datetime")) {
			var payl = "";
			if (pp.has("payload")) {
				payl = pp.get("payload");
			}
			var date = new DateFormatter(pp.get("datetime")).getFormated();
			var perform_task = new PerformTask(pp.get("resid"), pp.get("device"), pp.get("operation"), payl, date);
			THISTasks.res.addSubResource(perform_task.res);
			perform_tasks.push(perform_task);
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
														   "device = ...\n" +
														   "operation = ...\n" +
														   "datetime = ...\n" +
														   "payload = ...");				
		}	
	}
}

// PerformTask ///////////////////////////////////////

function PerformTask(resid, device, operation, payload, datetime) {
	var THISPerformTask = this;
	var timeout = null;
	
	var difference = calcDifference(datetime);
	if (difference > 0) {
		this.res = new JavaScriptResource(resid);
	
		// Set Timeout //////////////////////////////////////
		app.dump("Difference: " + difference);
		timeout = app.setTimeout(performSend, difference, device, operation, payload, this.res);
	
	
		// Add SubResource //////////////////////////////////
		this.deviceres = new InfoRes("device", device);
		this.operationres = new InfoRes("operation", operation);
		this.payloadres = new InfoRes("payload", payload);
		this.datetimeres = new DateInfoRes("datetime", datetime, timeout);
		
		this.res.addSubResource(this.deviceres.res);
		this.res.addSubResource(this.operationres.res);
		this.res.addSubResource(this.payloadres.res);
		this.res.addSubResource(this.datetimeres.res);
		
		// Requests /////////////////////////////////////////
		this.res.ondelete = function(request) {
			perform_tasks.pop(this);
			if (timeout)
				app.clearTimeout(timeout);
			request.respond(CodeRegistry.RESP_DELETED);
			THISPerformTask.res.remove();
		}
		
		// Functions ///////////////////////////////////////
		this.getTimeout() {
			return timeout;
		}
	}
	
	function DateInfoRes(resid, info, timeout) {
		var THISDateInfoRes = this;
		this.prototype = new InfoRes(resid, info);
		var THISDateInfoRes_prot = this.prototype;
		
		var timeout = timeout;
		
		this.info = THISDateInfoRes_prot.info;
		this.res = THISDateInfoRes_prot.res;
		this.getInfo = THISDateInfoRes_prot.getInfo
		
		this.res.onput = function(request) {
			var payload = request.getPayloadString();
			THISDateInfoRes_prot.info = payload;
			THISDateInfoRes.info = payload;
			if (timeout)
				app.clearTimeout(timeout);
			var difference = calcDifference(payload);
			if (difference > 0) {
				app.dump("Updated Difference: " + difference);
				timeout = app.setTimeout(performSend, difference, THISPerformTask.deviceres.info, THISPerformTask.operationres.info, THISPerformTask.payloadres.info, THISPerformTask.res);
			}
			
			request.respond(CodeRegistry.RESP_CHANGED);		
		}
	}
}

// Functions ////////////////////////////////////////////

function performSend(device, operation, payload, resource) {
	var client = new CoAPRequest();
	client.open(operation , device);
	client.send(payload);
	resource.remove();
}

function calcDifference(datetime) {
	var dateFormat = new SimpleDateFormat(DATE_FORMAT);
	var date = dateFormat.parse(datetime);
	var today = new Date();
		
	var calDate = Calendar.getInstance();
	calDate.setTime(date);
	var calToday = Calendar.getInstance();
	calToday.setTime(today);
		
	return calDate.getTimeInMillis() - calToday.getTimeInMillis();
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
		var payload = request.getPayloadString();
		THISChangeableInfo_prot.info = payload;
		THISChangeableInfo.info = payload;
		request.respond(CodeRegistry.RESP_CHANGED);
	}
}

//Create Standard Date Format ///////////////////////////
//Format: yyyy/MM/dd-HH:mm:ss
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

//Clean Up ////////////////////////////////////////////////////////
app.onunload = function() {
	for (var el in perform_tasks) {
		var timeout = perform_tasks[el].getTimeout();
		if (timeout) {
			app.dump("clear timeout: " + timeout);
			app.clearTimeout(timeout);
		}
	}
}