// Import Packages /////////////////////////////////////
var THIS = this;

THIS.poll = 4000;

var perform_tasks = new Object();

// Add SubResources ///////////////////////////////////

this.tasksres = new Tasks("tasks");

app.root.add(THIS.tasksres.res);

// Tasks /////////////////////////////////////////////

function Tasks(resid) {
	var THISTasks = this;
	
	this.res = new JavaScriptResource(resid);
	
	this.res.onget = function(request) {
		var ret = "";
		for (var el in perform_tasks) {
			ret += el + "\n";
		}
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}

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
			THISTasks.res.add(perform_task.res);
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
	
	this.resid = resid;
	
	perform_tasks[this.resid] = this;
	
	var timeout = null;
	
	this.res = new JavaScriptResource(resid);
	
	// add resources for device, options
	this.deviceres = new InfoRes("device", device);
	this.optionsres = new InfoRes("options", options);
	this.pollres = new ChangeableInfoRes("poll", THIS.poll);
	this.valueres = new Value("value");
	
	this.res.add(THISPerformTask.deviceres.res);
	this.res.add(THISPerformTask.optionsres.res);
	this.res.add(THISPerformTask.pollres.res);
	this.res.add(THISPerformTask.valueres.res);
	
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
			app.dump("perform poll");
			var coapreq = new CoAPRequest();
			coapreq.open("GET", THISPerformTask.deviceres.getInfo());
			coapreq.onload = handleValue
			coapreq.send();
			
			timeout = app.setTimeout(performPoll, THISPerformTask.pollres.getInfo());
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
		delete perform_tasks[THISPerformTask.resid];
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