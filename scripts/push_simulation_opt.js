// Import Packages /////////////////////////////////////
var THIS = this;

THIS.poll = 4000;

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
		if (pp.has("resid") && pp.has("device")) {
			var options = "";
			if (pp.has("options")) {
				options = pp.get("options");
			}
			var perform_task = new PerformTask(pp.get("resid"), pp.get("device"), options);
			THISTasks.res.addSubResource(perform_task.res);
			perform_tasks.push(perform_task);
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
														   "device = ...\n" +
														   "options = ...");				
		}	
	}
}

function PerformTask(resid, device, options) {
	var THISPerformTask = this;
	
	var timeout = null;
	
	this.res = new JavaScriptResource(resid);
	
	// add resources for device, options
	this.deviceres = new InfoRes("device", device);
	this.optionsres = new InfoRes("options", options);
	this.pollres = new ChangeableInfoRes("poll", THIS.poll);
	this.valueres = new Value("value");
	
	this.res.addSubResource(THISPerformTask.deviceres.res);
	this.res.addSubResource(THISPerformTask.optionsres.res);
	this.res.addSubResource(THISPerformTask.pollres.res);
	this.res.addSubResource(THISPerformTask.valueres.res);
	
	function Value(resid) {
		var THISValue = this;
		
		var value;
		
		this.res = new JavaScriptResource(resid);
		
		this.res.isObservable(true);
		this.res.onget = function(request) {
			request.respond(CodeRegistry.RESP_CONTENT, value);
		}
		
		performPoll();
		
		function performPoll() {
			if (timeout)
				timeouts.pop(timeout);
			app.dump("perform poll");
			var coapreq = new CoAPRequest();
			coapreq.open("GET", THISPerformTask.deviceres.getRawInfo());
			coapreq.onload = handleValue
			coapreq.send();
			
			timeout = app.setTimeout(performPoll, THISPerformTask.pollres.getRawInfo());
			timeouts.push(timeout);
		}
		
		function handleValue(request) {
			var payload = request.getPayloadString();
			if (payload != value) {
				value = payload;
				THISValue.res.changed();
			}
		}
	}
	
	// Requests /////////////////////////////////////////
	this.res.ondelete = function(request) {
		perform_tasks.pop(this);
		if (timeout)
			app.clearTimeout(timeout);
		THISPerformTask.res.remove();
		request.respond(CodeRegistry.RESP_DELETED);
	}
	
	// Functions ////////////////////////////////////////
	this.getTimeout = function() {
		return timeout;
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
	
	this.setInfo = function(info) {
		THISInfo.info = info;
	}
	
	this.getRawInfo = function() {
		return THISInfo.info;
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
	this.setInfo = THISChangeableInfo_prot.setInfo
	this.getInfo = THISChangeableInfo_prot.getInfo
	this.getRawInfo = THISChangeableInfo_prot.getRawInfo
	
	THISChangeableInfo.res.onput = function(request) {
		var payload = request.getPayloadString();
		THISChangeableInfo_prot.info = payload;
		THISChangeableInfo.info = payload;
		request.respond(CodeRegistry.RESP_CHANGED);
	}
}

//Payload Parser ////////////////////////////////////////////////////
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