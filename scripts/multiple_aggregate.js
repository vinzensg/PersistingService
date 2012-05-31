var THIS = this;

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
		app.dump("Receive post");
		var payload = request.getPayloadString();
		var pp = new PayloadParser(payload);
		if (pp.has("resid") && pp.has("aggregatefunc")) {
			var sources = new Array();
			var counter = 1;
			while(pp.has("source"+counter)) {
				var source = pp.get("source"+counter++);
				sources.push(source);
			}
			var perform_task = new PerformTask(pp.get("resid"), sources, pp.get("aggregatefunc"));
			THISTasks.res.add(perform_task.res);
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
														   "(sourcex = ...\n" +
														   "aggregatefunc = ...");				
		}	
	}
}


function PerformTask(resid, sources, aggregatefunc) {
	var THISPerformTask = this;
	
	var resid = resid;
	perform_tasks[this.resid] = this;
	
	var aggregateFunc = getFunc(aggregatefunc);
		
	this.res = new JavaScriptResource(resid);
	
	// Add SubResources //////////////////////////////////
	this.devicesres = new Devices("sources", sources);
	this.aggregateres = new Aggregate("aggregate", sources, aggregateFunc);
	this.aggregatefuncres = new InfoRes("aggregatefunc", aggregatefunc);

	this.res.add(THISPerformTask.devicesres.res);
	this.res.add(THISPerformTask.aggregateres.res);
	this.res.add(THISPerformTask.aggregatefuncres.res);
	// Requests //////////////////////////////////////////
	this.res.ondelete = function(request) {
		delete perform_tasks[THISPerformTask.resid];
		THISPerformTask.res.remove();
		request.respond(CodeRegistry.RESP_DELETED);
	}
	
	function Aggregate(resid, sources, in_aggregateFunc) {
		var THISAggregate = this;
		
		this.aggregate_val = null;
		
		var aggregateFunc = in_aggregateFunc;
				
		for (var el in sources) {
			var request = new CoAPRequest();
			request.open("GET", sources[el]);
			request.setObserverOption();
			var respHandler = new RespHandler(this, aggregateFunc, parseInt(el)+1);
			request.onload = respHandler.handle
			request.send();
		}
		
		function RespHandler(in_aggregate_obj, in_aggregatefunc, in_device_nr) {
			var aggregate_obj = in_aggregate_obj;
			var aggregateFunc = in_aggregatefunc;
			var device_nr = in_device_nr;
			
			this.handle = function(request) {
				var payload = request.getPayloadString();
				var old_aggregate_val = aggregate_obj.aggregate_val;
				aggregate_obj.aggregate_val = aggregateFunc.perform(payload, device_nr)+";"+device_nr;
				if (old_aggregate_val != aggregate_obj.aggregate_val)
					THISPerformTask.res.changed();
			}
		}
				
		this.res = new JavaScriptResource(resid);
		this.res.isObservable(true);
		
		// Requests /////////////////////////////////////
		
		this.res.onget = function(request) {
			var withDevice = false;
			var query = request.getQuery();
			
			if (query != "") {
				var option = query.substring(1, query.length()).split('=');
				if (option[0]=="withdevice" && option[1]=="true")
					withDevice = true;
			}
			if (withDevice) {
				request.respond(CodeRegistry.RESP_CONTENT, getAggregateVal());
			} else {
				var val = getAggregateVal();
				if (val!="") request.respond(CodeRegistry.RESP_CONTENT, val.substring(0,val.indexOf(';')));
				else request.respond(CodeRegistry.RESP_CONTENT, "");
			}
		}
		
		// Functions ////////////////////////////////////
		
		function getAggregateVal() {
			if (THISAggregate.aggregate_val!=null) return THISAggregate.aggregate_val;
			else return "";
		}

	}
}

function Devices(resid, sources) {
	var THISDevice = this;
	
	this.devices = new Array();
	
	this.res = new JavaScriptResource(resid);
	
	var counter = 1;
	for (var el in sources) {
		var deviceres = new InfoRes("device"+counter++, sources[el]);
		THISDevice.devices.push(deviceres);
		this.res.add(deviceres.res);
	}
	
	this.res.onget = function(request) {
		var ret = "";
		var array = THISDevice.devices;
		for (var el in array) {
			ret += array[el].getInfo() + "\n";
		}
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
}

// Info Resource ////////////////////////////////////////

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

// Functions //////////////////////////////////////////////

function Sum() {
	var sum = 0;
	
	this.perform = function(value, device) {
		app.dump("Sum: " + sum);
		sum += parseFloat(value);
		return sum;
	}
	
	this.getStats = function() {
		return "sum";
	}
}

function Avg() {
	var sum = 0;
	var count = 0;
	
	this.perform = function(value, device) {
		count++;
		sum += parseFloat(value);
		return (sum / count);
	}
	
	this.getStats = function() {
		return "avg";
	}
}

function Max() {
	var max = -100000; // math min
	
	this.perform = function(value, device) {
		if (parseFloat(value) > max) {
			max = parseFloat(value);
		}
		return max;
	}
	
	this.getStats = function() {
		return "max";
	}
}

function Min() {
	var min = 100000; // math max
	
	this.perform = function(value, device) {
		if (parseFloat(value) < min) {
			min = parseFloat(value);
		}
		return min;
	}
	
	this.getStats = function() {
		return "min";
	}
}

function Newest() {
	
	this.perform = function(value, device) {
		return value;
	}
	
	this.getStats = function() {
		return "newest";
	}
}

function Own(evalFunc) {
	var func = "" + evalFunc;
	var storage = new Array();
	
	this.perform = function(in_value, device) {
		var ret = null;
		var value = null;
		if (isNaN(in_value)) value = in_value;
		else value = parseFloat(in_value);
		
		eval(func);

		return ret;
	}
	
	this.getStats = function() {
		return "own;;"+func+";;";
	}
}

function getFunc(func) {
	var fp = new FuncParser(func);
	var fu = fp.getFunc();
	if (fu=="sum") {
		app.dump("sum");
		return new Sum();
	} else if (fu=="avg") {
		app.dump("avg");
		return new Avg();
	} else if (fu=="max") {
		app.dump("max");
		return new Max();
	} else if (fu=="min") {
		app.dump("min");
		return new Min();
	} else if (fu=="newest"){
		app.dump("newest");
		return new Newest();
	} else if (fu=="own") {
		app.dump("own");
		if (fp.getParamCount()==1) {
			return new Own(fp.getParam(1));
		}
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
	this.func = fu[0].split(' ').join('');
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
function PayloadParser(payload) {
	this.map = new Object();
	
	var split_nl = payload.split('\n');
	for (var el=0; el<split_nl.length; el++) {
		if (split_nl[el].substring(0, split_nl[el].indexOf('=')-1)=="aggregatefunc") {
			var func = split_nl[el].substring(split_nl[el].indexOf('=')+1);
			while(func.indexOf(' ')==0) {
				func = func.substring(1);
			}
			if (func.indexOf("own")==0) {
				func += "\n";
				if (split_nl[el+1] && (""+split_nl[el+1]).indexOf(';;')==-1) {
					el++;
					while(split_nl[el] && (""+split_nl[el]).indexOf(';;')==-1){
						func += split_nl[el] + "\n";
						el++;
					}
					func += split_nl[el].substring(0, split_nl[el].indexOf(';;'));
				}
			}
			
			this.map["aggregatefunc"] = func;
		} else {
			var split_eq = split_nl[el].split(' ').join('').split('=');
			this.map[split_eq[0]] = split_eq[1];
		}
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