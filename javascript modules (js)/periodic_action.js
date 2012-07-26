var THIS = this;

/*
 * perform tasks keeps track of the tasks created.
 */
var perform_tasks = new Object();

// Add SubResources ////////////////////////////////////

this.tasksres = new Tasks("tasks");

app.root.add(THIS.tasksres.res);

// Tasks ///////////////////////////////////////////////
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
		if (pp.has("resid") && pp.has("target") && pp.has("operation") && (pp.has("period") || pp.has("periodfunc"))) {
			if (perform_tasks[pp.get("resid")] != null) {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "resid already exists.");
				return;
			}
			
			var operation = pp.get("operation");
			if (!(operation=="PUT" || operation=="POST" || operation=="DELETE")) {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Bad operation.");
				return;
			}
				
			var periodFunc = null;
			if (pp.has("periodfunc")) {
				periodFunc = getFunc(pp.get("periodfunc"));
				if (periodFunc==null) {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Bad periodfunc.");
					return;
				}
			}
			
			var finite = 0;
			if (pp.has("finite")) {
				finite = pp.get("finite");
			}
			
			var payload = "";
			if (pp.has("payload")) {
				payload = pp.get("payload");
			}
			
			var payloadFunc = null;
			if (pp.has("payloadfunc")) {
				payloadFunc = getFunc(pp.get("payloadfunc"));
				if (payloadFunc==null) {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Bad payloadfunc.");
					return;
				}
			}

			var perform_task = new PerformTask(pp.get("resid"), pp.get("target"), pp.get("operation"), pp.get("period"), periodFunc, finite, payload, payloadFunc);
			THISTasks.res.add(perform_task.res);
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
														   "target = ...\n" +
														   "operation = ...\n" +
														   "period = ...\n" +
														   "(periodfunc = ...)\n" +
														   "(finite = ...)\n" +
														   "(payload = ...)\n" +
														   "(payloadfunc = ...)");
		}
	}
}

/*
 * A single task is an instance of Perform Task.
 * 
 * Requests:
 * -	DELETE: delete the task.
 */
function PerformTask(resid, device, operation, period, periodFunc, finite, payload, payloadFunc) {
	var THISPerformTask = this;
	
	this.resid = resid;
	
	perform_tasks[this.resid] = this;
	
	var timeout = null;
	
	this.res = new JavaScriptResource(resid);
	
	// Add SubResources ////////////////////////////////////
	this.deviceres = new InfoRes("target", device);
	this.runningres = new RunRes("running", "true");
	this.operationres = new InfoRes("operation", operation);
	this.periodres = new ChangeableInfoRes("period", period);
	this.periodfuncres = new FuncRes("periodfunc", periodFunc);
	this.finiteres = new InfoRes("finite", finite);
	this.remainingres = new InfoRes("remaining", finite);
	this.payloadres = new ChangeableInfoRes("payload", payload);
	this.payloadfuncres = new FuncRes("payloadfunc", payloadFunc);
	
	this.res.add(THISPerformTask.deviceres.res);
	this.res.add(THISPerformTask.runningres.res);
	this.res.add(THISPerformTask.operationres.res);
	this.res.add(THISPerformTask.periodres.res);
	this.res.add(THISPerformTask.periodfuncres.res);
	this.res.add(THISPerformTask.finiteres.res);
	this.res.add(THISPerformTask.remainingres.res);
	this.res.add(THISPerformTask.payloadres.res);
	this.res.add(THISPerformTask.payloadfuncres.res);
		
	// Start Perform Task /////////////////////////////////
	if (finite > 0) {
		performRequest(finite - 1);
	} else {
		performRequest(-1);
	}
	
	/*
	 * Perform the periodic request.
	 * If remaining >= 0: perform finite periodic request.
	 * Otherwise (remaining < 0): perform infinite periodic request.
	 */
	function performRequest(remaining) {
		app.dump("Perform request");
		var coapreq = new CoAPRequest();
		coapreq.open(THISPerformTask.operationres.getInfo(), THISPerformTask.deviceres.getInfo());
		if (THISPerformTask.payloadfuncres.getFunc()!=null) {
			coapreq.send(THISPerformTask.payloadfuncres.getFunc().perform());
		} else {
			coapreq.send(THISPerformTask.payloadres.getInfo());
		}
		if (remaining >= 0) {
			if (remaining > 0) {
				if (THISPerformTask.periodfuncres.getFunc()!=null) {
					timeout = app.setTimeout(performRequest, THISPerformTask.periodfuncres.getFunc().perform(), (remaining - 1));
				} else {
					timeout = app.setTimeout(performRequest, THISPerformTask.periodres.getInfo(), (remaining - 1));
				}
				THISPerformTask.remainingres.setInfo(remaining);
			} else {
				THISPerformTask.remainingres.setInfo(remaining);
				THISPerformTask.runningres.setRunning("false");
			}
		} else {
			if (THISPerformTask.periodfuncres.getFunc()!=null) {
				timeout = app.setTimeout(performRequest, THISPerformTask.periodfuncres.getFunc().perform(), -1);
			} else {
				timeout = app.setTimeout(performRequest, THISPerformTask.periodres.getInfo(), -1);
			}
		}
	}
		
	// Requests /////////////////////////////////////////////
	this.res.ondelete = function(request) {
		app.dump("Delete task.")
		delete perform_tasks[THISPerformTask.resid];
		if (timeout)
			app.clearTimeout(timeout);
		THISPerformTask.res.remove();
		request.respond(CodeRegistry.RESP_DELETED);
	}
	
	// Functions ////////////////////////////////////////////
	this.getTimeout = function() {
		return timeout;
	}
	
	// Running Resource /////////////////////////////////////
	/*
	 * Resource to keep track, if the periodic request is running or not.
	 * 
	 * Requests:
	 * -	GET: return running status.
	 * -	PUT: change the running status.
	 * 			false: stop periodic request.
	 * 			true: restart periodic request.
	 * 			true;continue: continue periodic request.
	 */
	function RunRes(resid, in_running) {
		var running = in_running;
		
		this.res = new JavaScriptResource(resid);
		
		this.res.onget = function(request) {
			request.respond(CodeRegistry.RESP_CONTENT, running);
		}
		
		this.res.onput = function(request) {
			var payload = request.getPayloadString().split(';');
			var run = payload[0];
			if (run=="true") {
				if (payload[1] && payload[1]=="continue") {
					var finite = THISPerformTask.finiteres.getInfo();
					if (finite > 0) {
						performRequest(THISPerformTask.remainingres.getInfo()-1);
					} else {
						performRequest(-1);
					}
				} else {
					if (THISPerformTask.periodfuncres.getFunc()!=null) THISPerformTask.periodfuncres.getFunc().reset();
					if (THISPerformTask.payloadfuncres.getFunc()!=null) THISPerformTask.payloadfuncres.getFunc().reset();
					var finite = THISPerformTask.finiteres.getInfo();
					if (finite > 0) {
						performRequest(finite-1);
						THISPerformTask.remainingres.setInfo(finite-1);						
					} else {
						performRequest(-1);
					}
				}
				running = run;
				request.respond(CodeRegistry.RESP_CHANGED);
			} else if (run=="false") {
				app.dump("remaining: " + THISPerformTask.remainingres.getInfo());
				app.clearTimeout(timeout);
				running = run;
				request.respond(CodeRegistry.RESP_CHANGED);
			} else {
				request.respond(CodeRegistry.RESP_BAD_REQUEST);
			}
		}
		
		this.setRunning = function(in_running) {
			running = in_running;
		}
	}
	
	app.dump("New task created.");
}

//Function Resource ////////////////////////////////////
/*
 * A resource specially for the functions.
 * 
 * Requests:
 * -	GET: get the function type.
 * -	PUT: change the function.	
 */
function FuncRes(resid, in_func_obj) {
	var THISFuncRes = this;
	
	var func_obj = in_func_obj;
	
	this.res = new JavaScriptResource(resid);
	
	this.res.onget = function(request) {
		if (func_obj) request.respond(CodeRegistry.RESP_CONTENT, func_obj.getStats());
		else request.respond(CodeRegistry.RESP_CONTENT, "");
	}
	
	this.res.onput = function(request) {
		var payload = request.getPayloadString();
		if (payload=="remove") {
			func_obj = null;
			request.respond(CodeRegistry.RESP_CHANGED);
		} else {
			var new_func = getFunc(payload);
			if (new_func!=null) {
				func_obj = new_func;
				request.respond(CodeRegistry.RESP_CHANGED);
			} else {
				request.respond(CodeRegistry.RESP_BAD_REQUEST);
			}
		}	
	}
	
	this.getFunc = function() {
		return func_obj;
	}
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

// Predefiend Functions //////////////////////////////////
/*
 * All period and payload functions:
 * -	increaser
 * -	set
 * -	own
 */
function Increaser(start, step, end) {
	var initial_start = parseFloat(start);
	
	var ret = initial_start;
	var start = initial_start;
	var end = parseFloat(end);
	var step = parseFloat(step);
		
	this.perform = function() {
		var tmp = ret;
		ret = ret + step;
		if (ret > end && step > 0) {
			ret = start + (ret - (end + 1));
		} else if (ret < end && step < 0) {
			ret = start - ((end - 1) - ret);
		}
		return tmp;
	}
	
	this.getStats = function() {
		return "inc;;" + start + ";;" + step + ";;" + end; 
	}
	
	this.reset = function() {
		ret = initial_start;
	}
}

function Set(set) {
	var set = set;
	var counter = 0;
		
	this.perform = function() {
		var tmp = counter;
		counter = (counter + 1)%set.length;
		return set[tmp];
	}
		
	this.getStats = function() {
		var ret = set[0];
		for (var i=1; i<set.length; i++) {
			ret += ";;" + set[i];
		}
		return "set;;" + ret;
	}
	
	this.reset = function() {
		counter = 0;
	}
}

function Own(evalFunc) {
	var func = evalFunc;
	var storage = new Array();
	
	this.perform = function() {
		var ret = null;
		
		eval(func);
		
		return ret;
	}
	
	this.getStats = function() {
		return "own;;" + func + ";;";
	}
}

/*
 * Returns the function object depending on the function-string passed (from the payload).
 */
function getFunc(func) {
	var fp = new FuncParser(func);
	var fu = fp.getFunc();
	if (fu=="inc") {
		app.dump("inc");
		if (fp.getParamCount() == 3) {
			return new Increaser(fp.getParam(1), fp.getParam(2), fp.getParam(3));
		}
		return null;
	} else if (fu=="set") {
		app.dump("set");
		if (fp.getParamCount() > 0) {
			var set = new Array();
			for (var i=1; i<=fp.getParamCount(); i++) {
				set.push(fp.getParam(i));
			}
			return new Set(set);
		}
		return null;
	} else if (fu=="own"){
		app.dump("own");
		if (fp.getParamCount()==1) return new Own(fp.getParam(1));
		else return null;
	} else {
		return null;
	}
}

// Function Parser ////////////////////////////////////////
/*
 * The function parser extracts the information from the function-string (from the paylod).
 */
function FuncParser(func) {
	var THISFuncParser = this;
	
	this.param = new Array();
	
	var fu = func.split(';;');
	this.func = fu[0];
	for (var i=1; i<fu.length; i++) {
		this.param.push(fu[i]);
	}
		
	this.getFunc = function() {
		return THISFuncParser.func;
	}
		
	this.getParam = function(number) {
		return THISFuncParser.param[number-1];
	}
		
	this.getParamCount = function() {
		return THISFuncParser.param.length;
	}
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
			if (label=="periodfunc" || label=="payloadfunc") { // special treatment for the functions (...func = own;;FUNCTION;;)
				var func = split_nl[el].substring(split_nl[el].indexOf('=')+1);
				while(func.indexOf(' ')==0) { // remove spaces after the equal sign: '   own' -> 'own'
					func = func.substring(1);
				}
				if (func.indexOf("own")==0) { // if own was specified for the function
					func += "\n";
					if (func.lastIndexOf(';;')>4) func = func.substring(0, func.lastIndexOf(';;')); // only one line: own;;...;;
					else if (split_nl[el+1] && !(""+split_nl[el+1]).indexOf(';;')==0) { // read all lines.
						el++;
						while(split_nl[el] && (""+split_nl[el]).indexOf(';;')==-1){ // read all the lines until the second ;;
							func += split_nl[el] + "\n";
							el++;
						}
						if (split_nl[el].indexOf(';;;')!=-1) { // the last line may contain three ;;;: own;;ret = 10;;;
							func += split_nl[el].substring(0, split_nl[el].indexOf(';;;')+1);
						} else {
							func += split_nl[el].substring(0, split_nl[el].indexOf(';;'));
						}
					}
				}
				
				this.map[label] = func;
			} else if (label=="payload"){
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
 * Clean up all the pending timeouts.
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
