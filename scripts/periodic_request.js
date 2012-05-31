var THIS = this;

var perform_tasks = new Object();

// Add SubResources ////////////////////////////////////

this.tasksres = new Tasks("tasks");

app.root.add(THIS.tasksres.res);

// Tasks ///////////////////////////////////////////////

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
		if (pp.has("resid") && pp.has("device") && pp.has("operation") && (pp.has("period") || pp.has("periodfunc"))) {
			var periodfunc = null;
			if (pp.has("periodfunc")) {
				periodfunc = pp.get("periodfunc");
			}
			var finite = 0;
			if (pp.has("finite")) {
				finite = pp.get("finite");
			}
			var payload = "";
			if (pp.has("payload")) {
				payload = pp.get("payload");
			}
			var payloadfunc = null;
			if (pp.has("payloadfunc")) {
				payloadfunc = pp.get("payloadfunc");
			}
			var perform_task = new PerformTask(pp.get("resid"), pp.get("device"), pp.get("operation"), pp.get("period"), periodfunc, finite, payload, payloadfunc);
			THISTasks.res.add(perform_task.res);
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
														   "device = ...\n" +
														   "operation = ...\n" +
														   "period = ...\n" +
														   "(finite = ...)\n" +
														   "(payload = ...)\n" +
														   "(payloadfunc = ...)");
		}
	}
}
	
function PerformTask(resid, device, operation, period, periodfunc, finite, payload, payloadfunc) {
	var THISPerformTask = this;
	
	this.resid = resid;
	
	perform_tasks[this.resid] = this;
	
	var timeout = null;
	
	var periodFunc = null;
	if (periodfunc)
		periodFunc = getFunc(periodfunc);
	
	var payloadFunc = null;
	if (payloadfunc)
		payloadFunc = getFunc(payloadfunc);
	
	this.res = new JavaScriptResource(resid);
	
	// Add SubResources ////////////////////////////////////
	this.deviceres = new InfoRes("device", device);
	this.runningres = new RunRes("running", "true");
	this.operationres = new InfoRes("operation", operation);
	this.periodres = new ChangeableInfoRes("period", period);
	this.periodfuncres = new InfoRes("periodfunc", periodfunc);
	this.finiteres = new InfoRes("finite", finite);
	this.remainingres = new InfoRes("remaining", finite);
	this.payloadres = new ChangeableInfoRes("payload", payload);
	this.payloadfuncres = new InfoRes("payloadfunc", payloadfunc);
	
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
	
	function performRequest(remaining) {
		app.dump("Perform Request");
		var coapreq = new CoAPRequest();
		coapreq.open(THISPerformTask.operationres.getInfo(), THISPerformTask.deviceres.getInfo());
		if (payloadFunc!=null) {
			coapreq.send(payloadFunc.perform());
		} else {
			coapreq.send(THISPerformTask.payloadres.getInfo());
		}
		if (remaining >= 0) {
			app.dump("Remaining: " + remaining);
			if (remaining > 0) {
				if (periodFunc!=null) {
					timeout = app.setTimeout(performRequest, periodFunc.perform(), (remaining - 1));
				} else {
					timeout = app.setTimeout(performRequest, THISPerformTask.periodres.getInfo(), (remaining - 1));
				}
				THISPerformTask.remainingres.setInfo(remaining);
			} else {
				THISPerformTask.runningres.setInfo("false");
			}
		} else {
			if (periodFunc!=null) {
				timeout = app.setTimeout(performRequest, periodFunc.perform(), -1);
			} else {
				timeout = app.setTimeout(performRequest, THISPerformTask.periodres.getInfo(), -1);
			}
		}
	}
		
	// Requests /////////////////////////////////////////////
	this.res.ondelete = function(request) {
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
					if (periodFunc!=null) periodFunc.reset();
					var finite = THISPerformTask.finiteres.getInfo();
					if (finite > 0) {
						performRequest(finite-1);
						THISPerformTask.remainingres.setInfo(finite);						
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
		return "Increaser: start = " + start + "; step = " + step + "; end = " + end; 
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
		var ret = "{" + set[0];
		for (var i=1; i<set.length; i++) {
			ret += ", " + set[i];
		}
		ret += "}";
		return "Set Interator: " + ret;
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
}

function getFunc(func) {
	var fp = new FuncParser(func);
	var fu = fp.getFunc();
	if (fu=="inc") {
		app.dump("inc found");
		if (fp.getParamCount() == 3) {
			app.dump("increaser");
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

// Payload Parser /////////////////////////////////////////
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
