// Import Packages /////////////////////////////////////
var THIS = this;

var DATE_FORMAT = "yyyy/MM/dd-HH:mm:ss";
var DATE_FORMAT_DAY = "yyyy/MM/dd";

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
		if (pp.has("resid") && pp.has("target") && pp.has("operation"), (pp.has("datetime") || pp.has("delay"))) {
			if (perform_tasks[pp.get("resid")] != null) {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "resid already exists.");
				return;
			}
			
			var payl = "";
			if (pp.has("payload")) {
				payl = pp.get("payload");
			}
			var date = null;
			if (pp.has("datetime")) {
				date = getDateFormatted(pp.get("datetime"));
			} else {
				date = getDateFromDelay(pp.get("delay"));
			}
			var perform_task = new PerformTask(pp.get("resid"), pp.get("target"), pp.get("operation"), payl, date);
			THISTasks.res.add(perform_task.res);
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
														   "target = ...\n" +
														   "operation = ...\n" +
														   "datetime = ...\n" +
														   "(payload = ...)");				
		}	
	}
}

// PerformTask ///////////////////////////////////////
/*
 * A single task is an instance of Perform Task.
 * 
 * Requests:
 * -	DELETE: delete the task.
 */
function PerformTask(resid, device, operation, payload, datetime) {
	var THISPerformTask = this;
	var timeout = null;
	
	this.resid = resid;
	
	var difference = calcDifference(datetime);
	if (difference > 0) { // only start the timed request, if date is in the future.
		perform_tasks[this.resid] = this;
		
		this.res = new JavaScriptResource(resid);
	
		// Add SubResource //////////////////////////////////
		this.deviceres = new InfoRes("target", device);
		this.operationres = new InfoRes("operation", operation);
		this.payloadres = new ChangeableInfoRes("payload", payload);
		this.datetimeres = new DateInfoRes("datetime", datetime, timeout);
		
		this.res.add(this.deviceres.res);
		this.res.add(this.operationres.res);
		this.res.add(this.payloadres.res);
		this.res.add(this.datetimeres.res);
		
		// Set Timeout //////////////////////////////////////
		timeout = app.setTimeout(performSend, difference);
		
		// Requests /////////////////////////////////////////
		this.res.ondelete = function(request) {
			app.dump("Delete task.");
			delete perform_tasks[THISPerformTask.resid];
			if (timeout)
				app.clearTimeout(timeout);
			request.respond(CodeRegistry.RESP_DELETED);
			THISPerformTask.res.remove();
		}
		
		// Functions ///////////////////////////////////////
		this.getTimeout = function() {
			return timeout;
		}
	}
	
	/*
	 * The date info resource offers an interface for the date-time of the timed request.
	 * 
	 * Requests:
	 * -	GET
	 */
	function DateInfoRes(resid, in_info, timeout) {
		var THISDateInfoRes = this;
		
		var timeout = timeout;
		
		var info = in_info;
		this.res = new JavaScriptResource(resid);
		
		// Rquests ////////////////////////////////////////
		this.res.onget = function(request) {
			request.respond(CodeRegistry.RESP_CONTENT, info);
		}
		
		this.res.onput = function(request) {
			var payload = request.getPayloadString();
			var difference = calcDifference(payload);
			if (difference > 0) {
				if (timeout)
					app.clearTimeout(timeout);
				THISDateInfoRes.setInfo(payload);
				timeout = app.setTimeout(performSend, difference);
				request.respond(CodeRegistry.RESP_CHANGED);
			} else {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "date not in future.");
			}
		}
		
		this.getInfo = function() {
			return info;
		}
		
		this.setInfo = function(in_info) {
			info = in_info;
		}
	}
	
	/*
	 * Send the request, when the time has elapsed.
	 */
	function performSend() {
		var client = new CoAPRequest();
		client.open(THISPerformTask.operationres.getInfo(), THISPerformTask.deviceres.getInfo());
		client.send(THISPerformTask.payloadres.getInfo());
		delete perform_tasks[THISPerformTask.resid];
		THISPerformTask.res.remove();
	}
	
	app.dump("New task created");
}

// Functions ////////////////////////////////////////////
/*
 * Compute the difference of the intended date-time and now. Return the result in milliseconds.
 */
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
 * -	PUT: change the information.
 */
function ChangeableInfoRes(resid, in_info) {
	var info = in_info;
	
	this.res = new JavaScriptResource(resid);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, info);
	}
	
	this.res.onput = function(request) {
		info = request.getPayloadString();
		request.respond(CodeRegistry.RESP_CHANGED);
	}
	
	this.getInfo = function() {
		return info;
	}
	
	this.setInfo = function(in_info) {
		info = in_info;
	}
}

//Create Standard Date Format ///////////////////////////
/*
 * Return the date formatted in the date-time format: yyyy/MM/dd-HH:mm:ss
 * date: yyyy/MM/dd-HH:mm:ss |Êyyyy/MM/dd | HH:mm:ss
 */
function getDateFormatted(date) {
	var newDate = "";
	switch(date.length) {
	case 19:
		newDate = date;
		break;
	case 10:
		newDate = date + "-00:00:00";
		break;
	case 8:
		var dateFormat = new SimpleDateFormat(DATE_FORMAT_DAY);
		var today = new Date();
		newDate = dateFormat.format(today) + "-" + date;
		break;
	default:
		newDate = false;
	}
		
	return newDate;
}

//Date from Delay ////////////////////////////////////////
/*
 * compute the date-time when a delay was passed: now + delay.
 */
function getDateFromDelay(delay) {
	var dateFormat = new SimpleDateFormat(DATE_FORMAT);
	
	var today = new Date();
	
	var calToday = Calendar.getInstance();
	calToday.setTime(today);
	calToday.add(Calendar.MILLISECOND, delay);
	
	return dateFormat.format(calToday.getTime());
}

//Payload Parser /////////////////////////////////////////
/*
 * The payload parser parses the payload and extracts the labels and their values.
 */
function PayloadParser(payload) {
	this.map = new Object();
	
	try {
		var split_nl = payload.split('\n');
		for (var el=0; el<split_nl.length; el++) {
			var label = split_nl[el].substring(0, split_nl[el].indexOf('=')-1);
			if (label=="payload"){
				var payl = split_nl[el].substring(split_nl[el].indexOf('=')+1);
				while (payl.indexOf(' ')==0) {
					payl = payl.substring(1);
				}
				if (payl.indexOf(';;')==0) {
					payl = payl.substring(2); // remove ';;'
					payl += "\n";
					if (split_nl[el+1] && !(""+split_nl[el+1]).indexOf(';;')==0) { // read all lines.
						el++;
						while(split_nl[el] && (""+split_nl[el]).indexOf(';;')==-1){ // read all the lines until the second ;;
							payl += split_nl[el] + "\n";
							el++;
						}		
						payl += split_nl[el].substring(0, split_nl[el].indexOf(';;'));
						payl = payl.replace(/;:;/g, ";;");
					}
				}
				this.map["payload"] = payl;
			} else {
				var no_space = split_nl[el].split(' ').join('');
				this.map[no_space.substring(0, no_space.indexOf('='))] = no_space.substring(no_space.indexOf('=')+1);
			}
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
			app.dump("Clear timeout: " + timeout);
			app.clearTimeout(timeout);
		}
	}
}