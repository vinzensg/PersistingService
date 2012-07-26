// Import Packages /////////////////////////////////////
var THIS = this;

THIS.poll = 1000; // 1 second

/*
 * perform tasks keeps track of the tasks created.
 */
var perform_tasks = new Object();

// Add SubResources ///////////////////////////////////

this.tasksres = new Tasks("tasks");

app.root.add(THIS.tasksres.res);

// Tasks /////////////////////////////////////////////
/*
 * The tasks resource holds all the tasks created.
 * 
 * Requests:
 * -	GET: get a list of all the tasks.
 * -	POST: create a new task.
 */
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
		app.dump("Create new task.");
		var payload = request.getPayloadString();
		var pp = new PayloadParser(payload);
		if (pp.has("resid") && pp.has("source")) {
			if (perform_tasks[pp.get("resid")] != null) {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "resid already exists.");
				return;
			}
			
			var poll = THIS.poll;
			if (pp.has("poll")) {
				poll = pp.get("poll");
			}
			var options = "";
			if (pp.has("options")) {
				options = pp.get("options");
			}
			var perform_task = new PerformTask(pp.get("resid"), pp.get("source"), poll, options);
			THISTasks.res.add(perform_task.res);
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
														   "source = ...\n" +
														   "(poll = ...)\n" +
														   "(options = ...)");				
		}	
	}
}

/*
 * A single task is an instance of Perform Task.
 * 
 * Requests:
 * -	DELETE: delete the task.
 */
function PerformTask(resid, device, poll, options) {
	var THISPerformTask = this;
	
	this.resid = resid;
	
	perform_tasks[this.resid] = this;
	
	var timeout = null;
	
	this.res = new JavaScriptResource(resid);
	
	// add resources for device, options
	this.deviceres = new InfoRes("source", device);
	this.optionsres = new InfoRes("options", options);
	this.pollres = new ChangeableInfoRes("poll", poll);
	this.valueres = new Value("value");
	
	this.res.add(THISPerformTask.deviceres.res);
	this.res.add(THISPerformTask.optionsres.res);
	this.res.add(THISPerformTask.pollres.res);
	this.res.add(THISPerformTask.valueres.res);
	
	/*
	 * The value resource offers an interface to access the polled value.
	 * 
	 * -	GET: return the polled value.
	 */
	function Value(resid) {
		var THISValue = this;
		
		var value;
		
		this.res = new JavaScriptResource(resid);
		
		this.res.isObservable(true);
		this.res.onget = function(request) {
			request.respond(CodeRegistry.RESP_CONTENT, value);
		}
		
		performPoll();
		
		/*
		 * poll in the defined poll interval.
		 */
		function performPoll() {
			var coapreq = new CoAPRequest();
			coapreq.open("GET", THISPerformTask.deviceres.getInfo());
			coapreq.onload = handleValue
			coapreq.send();
			
			timeout = app.setTimeout(performPoll, THISPerformTask.pollres.getInfo());
		}
		
		/*
		 * Incoming values are compared with the aold value.
		 * If the new value is different from the old value change it and send it to all observers.
		 */
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
		app.dump("Delete task.");
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
	
	app.dump("New task created.");
}

//Info Resources ////////////////////////////////////////
/*
 * A resource for all information resources.
 * 
 * -	GET: get the information.
 */
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

/*
 * A resource for all information resources.
 * 
 * -	GET: get the information.
 * -	PUT: change the information
 */
function ChangeableInfoRes(resid, in_info) {
	var THISChangeableInfo = this;
	var info = in_info;
	
	this.res = new JavaScriptResource(resid);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, info);
	}
	
	this.res.onput = function(request) {
		var payload = request.getPayloadString();
		THISChangeableInfo.setInfo(payload);
		
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
/*
 * The payload parser parses the payload and extracts the labels and their values.
 */
function PayloadParser(payload) {
	this.map = new Object();
	
	try {
		var split_nl = payload.split('\n');
		for (var el=0; el<split_nl.length; el++) {			
			var no_space = split_nl[el].split(' ').join('');
			this.map[no_space.substring(0, no_space.indexOf('='))] = no_space.substring(no_space.indexOf('=')+1);
		}
	} catch (e if e.javaException instanceof StringIndexOutOfBoundsException) {
		app.dump("Invalid Payload: could not be parsed.");
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
/*
 * Clean up the pending timeouts.
 */
app.onunload = function() {
	for (var el in perform_tasks) {
		var timeout = perform_tasks[el].getTimeout();
		if (timeout) {
			app.dump("clear timeout: " + timeout);
			app.clearTimeout(timeout);
		}
	}
}