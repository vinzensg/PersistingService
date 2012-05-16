// Imports ////////////////////////////////////////////////////

var THIS = this;

this.subs = new Array();

// Add SubResources ///////////////////////////////////////////

this.tasksres = new Tasks("tasks");

app.root.addSubResource(this.tasksres.res);

// Tasks /////////////////////////////////////////////////////

function Tasks(resid) {
	var THISTasks = this;
	
	this.res = new JavaScriptResource(resid);
	
	// Add SubResources //////////////////////////////////////
	this.taskspostres = new TasksPost_Put("post", "POST");
	this.tasksputres = new TasksPost_Put("put", "PUT");
	this.tasksdeleteres = new TasksDelete("delete", "DELETE");
	
	this.res.addSubResource(this.taskspostres.res);
	this.res.addSubResource(this.tasksputres.res);
	this.res.addSubResource(this.tasksdeleteres.res);	
}

function SubTask(resid, operation) {
	var THISSubTask = this;
	
	this.operation = operation;
		
	this.res = new JavaScriptResource(resid);
	
	// Requests //////////////////////////////////////////
	
	this.res.onget = function(request) {
		var ret = "";
		for (var i=0, e=THIS.subs.length; i<e; i++) {
			var sub = THIS.subs[i];
			app.dump("SUB: " + sub.operation);
			app.dump("THISSub: " + THISSubTask.operation);
			if (sub.operation == THISSubTask.operation) {
				ret += sub.getInfo();
			}
		}
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
}

function TasksPost_Put(resid, operation) {
	var THISTasksPostPut = this;
	this.prototype = new SubTask(resid, operation);
	var THISTasksPostPut_prot = this.prototype;
	
	this.res = THISTasksPostPut_prot.res;
	this.operation = THISTasksPostPut_prot.operation;
	
	this.res.onpost = function(request) {
		var payload = request.getPayloadString();
		var pp = new PayloadParser(payload);
		if (pp.has("resid") && pp.has("device") && pp.has("period")) {
			var finite = 0;
			if (pp.has("finite")) {
				finite = pp.get("finite");
			}
			if (pp.has("payload")) {
				THISTasksPostPut.addRes(pp.get("resid"), pp.get("device"), pp.get("period"), finite, pp.get("payload"), null);
				request.respond(CodeRegistry.RESP_CREATED);
			} else if (pp.has("payloadfunc")){
				THISTasksPostPut.addRes(pp.get("resid"), pp.get("device"), pp.get("period"), finite, "", pp.get("payloadfunc"));
				request.respond(CodeRegistry.RESP_CREATED);
			} else {
				THISTasksPostPut.addRes(pp.get("resid"), pp.get("device"), pp.get("period"), finite, "", null);
				request.respond(CodeRegistry.RESP_CREATED);
			}
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
														   "device = ...\n" +
														   "period = ...\n" +
														   "(finite = ...)\n" +
														   "(payload = ...)\n" +
														   "(payloadfunc = ...)");
		}
	}
	
	// Functions /////////////////////////////////////////
	
	this.addRes = function(name, device, period, finite, payload, payloadfunc) {
		var periodicPostPut = new PeriodicPost_Put(this.operation, name, device, period,finite,  payload, payloadfunc);
		this.res.addSubResource(periodicPostPut.res);
		periodicPostPut.run();
		THIS.subs.push(periodicPostPut);
	}
}

function TasksDelete(resid, operation) {
	var THISTasksDelete = this;
	this.prototype = new SubTask(resid, operation);
	var THISTasksDelete_prot = this.prototype;
	
	this.res = THISTasksDelete_prot.res;
	this.operation = THISTasksDelete_prot.operation;
	
	this.res.onpost = function(request) {
		var payload = request.getPayloadString();
		var pp = new PayloadParser(payload);
		if (pp.has("resid") && pp.has("device") && pp.has("period")) {
			var finite = 0;
			if (pp.has("finite")) {
				finite = pp.get("finite");
			}
			THISTasksDelete.addRes(pp.get("resid"), pp.get("device"), pp.get("period"), finite);
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
														   "device = ...\n" +
														   "period = ...\n" +
														   "(finite = ...)");
		}
	}
	
	// Functions /////////////////////////////////////////
	this.addRes = function(resid, device, period, finite) {
		var periodicDelete = new PeriodicDelete("DELETE", resid, device, period, finite);
		this.res.addSubResource(periodicDelete.res);
		periodicDelete.run();
		THIS.subs.push(periodicDelete);
	}
}

// Periodic Tasks ///////////////////////////////////////////

function Periodic(operation, resid, device, period, finite) {
	var THISPeriodic = this;
	
	this.operation = operation;
	
	this.name = resid;
	
	this.cont = true;
	
	this.res = new JavaScriptResource(resid);
	
	// Add SubResources ////////////////////////////////////
	
	this.deviceres = new InfoRes("device", device);
	this.periodres = new ChangeableInfoRes("period", period);
	this.finiteres = new InfoRes("finite", finite);
	this.remainres = new InfoRes("remain", finite);
	
	this.res.addSubResource(THISPeriodic.deviceres.res);
	this.res.addSubResource(THISPeriodic.periodres.res);
	this.res.addSubResource(THISPeriodic.finiteres.res);
	this.res.addSubResource(THISPeriodic.remainres.res);
	
	// Requests ////////////////////////////////////////////
	this.res.onget = function(request) {
		var ret = "device = " + THISPeriodic.deviceres.getInfo() + "\n" +
				  "period = " + THISPeriodic.periodres.getInfo() + "\n" +
				  "finite = " + THISPeriodic.finiteres.getInfo() + "\n" +
				  "remaining = " + THISPeriodic.remainres.getInfo();
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
	
	this.res.ondelete = function(request) {
		THISPeriodic.cont = false;
		request.respond(CodeRegistry.RESP_DELETED);
		try {
			java.lang.Thread.sleep(1000);
		} catch (e if e.javaException instanceof InterruptedException) {
			app.dump("Sleeping was interrupted");
		}
		THIS.subs.pop(this);
		this.remove();
	}
}

function PeriodicPost_Put(operation, resid, device, period, finite, payload, payloadfunc) {
	var THISPeriodicPostPut = this;
	this.prototype = new Periodic(operation, resid, device, period, finite);
	var THISPeriodicPostPut_prot = this.prototype;
	
	app.dump(THISPeriodicPostPut_prot.periodres.info);
		
	this.res = THISPeriodicPostPut_prot.res;
	this.operation = THISPeriodicPostPut_prot.operation;
	
	// Add SubResources ////////////////////////////////////
	this.payloadres = new InfoRes("payload", payload);
	this.payloadfuncres = new InfoRes("payloadfunc", payloadfunc);
	
	this.res.addSubResource(this.payloadres.res);
	this.res.addSubResource(this.payloadfuncres.res);
	
	// Set Real Payload Function ///////////////////////////
	
	if (payloadfunc != null) {
		this.payloadfunc = getFunc(payloadfunc);
	} else {
		this.payloadfunc = new EmptyFunc();
	}
	
	// Requests ////////////////////////////////////////////
	this.res.onget = function(request) {
		var ret = "device = " + THISPeriodicPostPut_prot.deviceres.getInfo() + "\n" +
				  "period = " + THISPeriodicPostPut_prot.periodres.getInfo() + "\n" +
				  "finite = " + THISPeriodicPostPut_prot.finiteres.getInfo() + "\n" +
				  "remaining = " + THISPeriodicPostPut_prot.remainres.getInfo() + "\n" +
				  "payload = " + THISPeriodicPostPut_prot.payloadres.getInfo() + "\n" +
				  "payloadfunc = " + THISPeriodicPostPut.payloadfunc.getStats();
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
	
	// Functions //////////////////////////////////////////
	this.run = function() {
		if (THISPeriodicPostPut_prot.finiteres.info > 0) {
			new Thread(function() {
				var cycles = THISPeriodicPostPut_prot.finiteres.info;
				while(cycles > 0) {
					loop();
					cycles--;
					THISPeriodicPostPut_prot.remainres.info--;
				}
				THISPeriodicPost.res.remove();
			}).start();
		} else {
			new Thread(function() {
				var cont = true;
				while(THISPeriodicPostPut_prot.cont) {
					loop();
				}
			}).start();
		}
	}
	
	var loop = function() {
		app.dump("Perform Periodic Task");
		var coapreq = new CoAPRequest();
		coapreq.open(THISPeriodicPostPut_prot.operation, THISPeriodicPostPut_prot.deviceres.info);
		if (payloadfunc != null) {
			coapreq.send(THISPeriodicPostPut.payloadfunc.perform());
		} else {
			coapreq.send(THISPeriodicPostPut_prot.payloadres.info);
		}
		
		try {
			java.lang.Thread.sleep(THISPeriodicPostPut_prot.periodres.info);
		} catch (e if e.javaException instanceof InterruptedException) {
			app.dump("Spleeping was interrupted");
		}
	}
	
	this.getInfo = function() {
		return THISPeriodicPostPut_prot.operation + ": " +
				"name = " + THISPeriodicPostPut_prot.name + "; " +
				"device = " + THISPeriodicPostPut_prot.deviceres.getInfo() + "; " +
				"period = " + THISPeriodicPostPut_prot.periodres.getInfo() + "; " +
				"finite = " + THISPeriodicPostPut_prot.finiteres.getInfo() + "; " +
				"payload = " + THISPeriodicPostPut.payloadres.getInfo() + "; " +
				"payloadfunc = " + THISPeriodicPostPut.payloadfunc.getStats() + "\n";
	}
		
}

function PeriodicDelete(operation, resid, device, period, finite) {
	var THISPeriodicDelete = this;
	this.prototype = new Periodic(operation, resid, device, period, finite);
	var THISPeriodicDelete_prot = this.prototype;
	
	this.res = THISPeriodicDelete_prot.res;
	this.operation = THISPeriodicDelete_prot.operation;
	
	// Functions //////////////////////////////////////////
	this.run = function() {
		if (THISPeriodicDelete_prot.finiteres.info > 0) {
			new Thread(function() {
				var cycles = THISPeriodicDelete_prot.finiteres.info;
				while(cycles > 0) {
					loop();
					cycles--;
					THISPeriodicDelete_prot_remainres.info--;
				}
				THISPeriodicDelete.res.remove();
			}).start();
		} else {
			new Thread(function() {
				while(THISPeriodicDelete_prot.cont) {
					loop();
				}
			}).start();
		}
	}
	
	var loop = function() {
		app.dump("Perform Periodic Task");
		var coapreq = new CoAPRequest();
		coapreq.open(THISPeriodicDelete_prot.operation, THISPeriodicDelete_prot.deviceres.info);
		coapreq.send();
		
		try {
			app.dump("Period: " + THISPeriodicDelete_prot.periodres.info);
			java.lang.Thread.sleep(THISPeriodicDelete_prot.periodres.info);
		} catch (e if e.javaException instanceof InterruptedException) {
			app.dump("Spleeping was interrupted");
		}
	}
	
	this.getInfo = function() {
		return THISPeriodicPostPut_prot.operation + ": " +
				"name = " + THISPeriodicDelete_prot.name + "; " +
				"device = " + THISPeriodicDelete_prot.deviceres.getInfo() + "; " +
				"period = " + THISPeriodicDelete_prot.periodres.getInfo() + "; " +
				"finite = " + THISPeriodicDelete_prot.finiteres.getInfo();
	}
}

// Info Resources ////////////////////////////////////////
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
		THISChangeableInfo_prot.info = payload;
		THISChangeableInfo.info = payload;
		request.respond(CodeRegistry.RESP_CHANGED);
	}
}

// Predefiend Functions //////////////////////////////////
function EmptyFunc() {
	this.getStats = function() {
		return "EMPTY";
	}
}

function Increaser(start, step, end) {
	var ret = parseFloat(start);
	var start = parseFloat(start);
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
}

function Set(set) {
	var set = set;
	var counter = 0;
	
	this.perform = function() {
		var tmp = counter;
		counter = (counter + 1)%set.length;
		app.dump("tmp: " + tmp + "  counter: " + counter + "  set: " + set[tmp]);
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
}

function getFunc(func) {
	var fp = new FuncParser(func);
	var fu = fp.getFunc();
	if (fu == "inc") {
		app.dump("inc found");
		if (fp.getParamCount() == 3) {
			app.dump("return increaser");
			return new Increaser(fp.getParam(1), fp.getParam(2), fp.getParam(3));
		}
	} else if (fu == "set") {
		app.dump("set found");
		if (fp.getParamCount() > 0) {
			var set = new Array();
			for (var i=1; i<=fp.getParamCount(); i++) {
				set.push(fp.getParam(i));
			}
			return new Set(set);
		}
	}
}

// Function Parser ////////////////////////////////////////
function FuncParser(func) {
	var THISFuncParser = this;
	
	this.param = new Array();
	
	var fu = func.split(';');
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