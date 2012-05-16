// Import Packages /////////////////////////////////////
var THIS = this;

var periodic_tasks = new Array();

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
		if (pp.has("resid") && pp.has("device") && pp.has("operation") && pp.has("period")) {
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
			var periodic_task = new Periodic(pp.get("resid"), pp.get("device"), pp.get("operation"), pp.get("period"), finite, payload, payloadfunc);
			THISTasks.res.addSubResource(periodic_task.res);
			periodic_tasks.push(periodic_task);
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
	
function Periodic(resid, device, operation, period, finite, payload, payloadfunc) {
	var THISPeriodic = this;
	
	var timeout = null;
	var payloadFunc = null 
	if (payloadfunc)
		payloadFunc = getFunc(payloadfunc);
	
	this.res = new JavaScriptResource(resid);
	
	// Add SubResources ////////////////////////////////////
	this.deviceres = new InfoRes("device", device);
	this.operationres = new InfoRes("operation", operation);
	this.periodres = new ChangeableInfoRes("period", period);
	this.finiteres = new InfoRes("finite", finite);
	this.remainingres = new InfoRes("remaining", finite);
	this.payloadres = new ChangeableInfoRes("payload", payload);
	this.payloadfuncres = new InfoRes("payloadfunc", payloadFunc);
	
	this.res.addSubResource(THISPeriodic.deviceres.res);
	this.res.addSubResource(THISPeriodic.operationres.res);
	this.res.addSubResource(THISPeriodic.periodres.res);
	this.res.addSubResource(THISPeriodic.finiteres.res);
	this.res.addSubResource(THISPeriodic.remainingres.res);
	this.res.addSubResource(THISPeriodic.payloadres.res);
	this.res.addSubResource(THISPeriodic.payloadfuncres.res);
	
	// Start Periodic Task /////////////////////////////////
	if (finite > 0) {
		performRequest(finite - 1);
	} else {
		performRequest(-1);
	}
	
	function performRequest(remaining) {
		app.dump("Perform Request");
		var coapreq = new CoAPRequest();
		coapreq.open(THISPeriodic.operationres.getRawInfo(), THISPeriodic.deviceres.getRawInfo());
		if (payloadFunc) {
			coapreq.send(payloadFunc.perform());
		} else {
			coapreq.send(THISPeriodic.payloadres.getRawInfo());
		}
		if (remaining >= 0) {
			app.dump("Remaining: " + remaining);
			if (remaining > 0) {
				timeout = app.setTimeout(performRequest, THISPeriodic.periodres.getRawInfo(), (remaining - 1));
				THISPeriodic.remainingres.setInfo(remaining - 1);
			} else {
				THISPeriodic.res.remove();
			}
		} else {
			timeout = app.setTimeout(performRequest, THISPeriodic.periodres.getRawInfo(), -1);
		}
	}
		
	// Requests //////////////////////////////////////////
	this.res.ondelete = function(request) {
		periodic_tasks.pop(this);
		if (timeout)
			app.clearTimeout(timeout);
		THISPeriodic.res.remove();
		request.respond(CodeRegistry.RESP_DELETED);
	}
	
	// Functions /////////////////////////////////////////
	this.getTimeout = function() {
		return timeout;
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
	} else {
		return null;
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

//Clean Up ////////////////////////////////////////////////////////
app.onunload = function() {
	for (var el in periodic_tasks) {
		var timeout = periodic_tasks[el].getTimeout();
		if (timeout) {
			app.dump("clear timeout: " + timeout);
			app.clearTimeout(timeout);
		}
	}
}
