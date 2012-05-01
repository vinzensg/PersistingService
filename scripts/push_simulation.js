// Imports ////////////////////////////////////////////////////

var THIS = this;
this.poll = 4000;

this.subs = new Array();

// Add SubResources ///////////////////////////////////////////

this.stringres = new StringRes("string");
this.numberres = new NumberRes("number");

app.root.addSubResource(this.stringres.res);
app.root.addSubResource(this.numberres.res);

// Push ///////////////////////////////////////////////////////

function TypeRes(resid) {
	var THISTypeRes = this;
	
	this.name = resid;
	
	this.res = new JavaScriptResource(resid);
	
	// Requests ///////////////////////////////////////////////
	this.res.onget = function(request) {
		var ret = "";
		for (el in THIS.subs) {
			var sub = THIS.subs[el];
			if (this.name == sub.name) {
				ret += sub.getInfo();
			}
		}
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
}

function StringRes(resid) {
	var THISString = this;
	this.prototype = new TypeRes(resid);
	var THISString_prot = this.prototype;
		
	this.res = THISString_prot.res;
	this.name = THISString_prot.name;
	
	// Requests /////////////////////////////////////////////
	
	this.res.onpost = function(request) {
		var payload = request.getPayloadString();
		var pp = new PayloadParser(payload);
		if (pp.has("resid") && pp.has("device")) {
			if (pp.has("poll")) {
				THISString.addRes(pp.get("resid"), pp.get("device"), pp.get("poll"));
			} else {
				THISString.addRes(pp.get("resid"), pp.get("device"), THIS.poll);
			}
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
					   									   "device = ...");
		}
		
	}
	
	this.addRes = function(resid, device, poll) {
		var pollingTask = new PollingString(resid, device, poll);
		this.res.addSubResource(pollingTask.res);
		pollingTask.valueres.run();
		THIS.subs.push(pollingTask);
	}
}

function NumberRes(resid) {
	var THISNumber = this;
	this.prototype = new TypeRes(resid);
	var THISNumber_prot = this.prototype;
		
	this.res = THISNumber_prot.res;
	this.name = THISNumber_prot.name;
	
	// Requests ///////////////////////////////////////////
	
	this.res.onpost = function(request) {
		var payload = request.getPayloadString();
		var pp = new PayloadParser(payload);
		if (pp.has("resid") && pp.has("device")) {
			if (pp.has("poll")) {
				THISNumber.addRes(pp.get("resid"), pp.get("device"), pp.get("poll"));
			} else {
				THISNumber.addRes(pp.get("resid"), pp.get("device"), THIS.poll);
			}
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
					   									   "device = ...");
		}	
	}
	
	this.addRes = function(resid, device, poll) {
		var pollingTask = new PollingNumber(resid, device, poll);
		this.res.addSubResource(pollingTask.res);
		pollingTask.valueres.run();
		THIS.subs.push(pollingTask);
	}
}

// Polling Task //////////////////////////////////////////////

function Polling(resid, device, poll) {
	var THISPolling = this;
	
	this.name = resid;
	
	this.cont = true;
		
	this.res = new JavaScriptResource(resid);

	// Add SubResources //////////////////////////////////////////////
	this.pollres = new ChangeableInfoRes("poll", poll);
	this.deviceres = new InfoRes("device", device);
	
	this.res.addSubResource(THISPolling.pollres.res);
	this.res.addSubResource(THISPolling.deviceres.res);
	
	// Requests ////////////////////////////////////////////////////

	this.res.ondelete = function(request) {
		THISPolling.cont = false;
		app.dump("Polling cont: " + THISPolling.cont);
		request.respond(CodeRegistry.RESP_DELETED);
		try {
			java.lang.Thread.sleep(1000);
		} catch (e if e.javaException instanceof InterruptedException) {
			app.dump("Sleeping was interrupted");
		}
		this.remove();
	}
	
	this.getInfo = function() {
		return this.name + ": " +
				"device = " + this.deviceres.info + "; " +
				"poll = " + this.pollres.info; 
	}
}

function PollingString(resid, device, poll) {
	var THISPollingString = this;
	this.prototype = new Polling(resid, device, poll);
	var THISPollingString_prot = this.prototype;
		
	this.res = THISPollingString_prot.res;
	this.name = THISPollingString_prot.name;
	this.pollres = THISPollingString_prot.pollres;
	this.getInfo = THISPollingString_prot.getInfo

	// Add SubResources //////////////////////////////////////////////
	this.valueres = new StringValue("value", device, this);
	
	this.res.addSubResource(THISPollingString.valueres.res);
}

function PollingNumber(resid, device, poll) {
	var THISPollingNumber = this;
	this.prototype = new Polling(resid, device, poll);
	var THISPollingNumber_prot = this.prototype;
		
	this.res = THISPollingNumber_prot.res;
	this.name = THISPollingNumber_prot.name;
	this.pollres = THISPollingNumber_prot.pollres;
	this.getInfo = THISPollingNumber_prot.getInfo
	
	// Add SubResources //////////////////////////////////////////////
	this.valueres = new NumberValue("value", device, this);
	
	this.res.addSubResource(THISPollingNumber.valueres.res);
}

// Value Rsources ///////////////////////////////////////
function Value(resid, device, pollingres) {
	var THISValue = this;
		
	this.pollingres = pollingres;
	
	this.device = device;
	
	this.value = null;
	
	this.res = new JavaScriptResource(resid);
	this.res.isObservable(true);
	
	// Requests /////////////////////////////////////////////////
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, THISValue.value);
	}
	
	// Functions ////////////////////////////////////////////////////
	this.coapreq = new CoAPRequest();
	
	this.run = function() {
		new Thread(function() {
			while(THISValue.pollingres.prototype.cont) {
				app.dump("Execute GET");
				THISValue.coapreq.open("GET", THISValue.device);
				THISValue.coapreq.send();
				try {
					java.lang.Thread.sleep(THISValue.pollingres.pollres.info);
				} catch (e if e.javaException instanceof InterruptedException) {
					app.dump("Spleeping was interrupted");
				}
			}
		}).start();
	}
}

function StringValue(resid, device, pollingres) {
	var THISStringValue = this;
	this.prototype = new Value(resid, device, pollingres);
	var THISStringValue_prot = this.prototype;
	
	this.res = THISStringValue_prot.res;
	this.coapreq = THISStringValue_prot.coapreq;
	this.run = THISStringValue_prot.run
	
	this.coapreq.onload = setvalue
	
	function setvalue() {
		app.dump("Receive value");
		if (THISStringValue.coapreq.status == THISStringValue.coapreq.Content) {
			var resp = THISStringValue.coapreq.responseText;
			if (THISStringValue_prot.value != resp) {
				THISStringValue_prot.value = resp;
				THISStringValue.res.changed();
			}
		}
	}
}

function NumberValue(resid, device, pollingres) {
	var THISNumberValue = this;
	this.prototype = new Value(resid, device, pollingres);
	var THISNumberValue_prot = this.prototype;
	
	this.res = THISNumberValue_prot.res;
	this.coapreq = THISNumberValue_prot.coapreq;
	this.run = THISNumberValue_prot.run
		
	this.coapreq.onload = setvalue
	
	function setvalue() {
		app.dump("Receive value");
		if (THISNumberValue.coapreq.status == THISNumberValue.coapreq.Content) {
			var resp = parseFloat(THISNumberValue.coapreq.responseText);
			if (THISNumberValue_prot.value != resp) {
				THISNumberValue_prot.value = resp;
				THISNumberValue.res.changed();
			}
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