var THIS = this;

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
		if (pp.has("resid") && pp.has("aggregatefunc")) {
			if (perform_tasks[pp.get("resid")] != null) {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "resid already exists.");
				return;
			}
			
			var sources = new Array();
			var counter = 1;
			while(pp.has("source"+counter)) {
				var source = pp.get("source"+counter++);
				sources.push(source);
			}
			var aggregateFunc = getFunc(pp.get("aggregatefunc"));
			if (aggregateFunc==null) {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Bad aggregatefunc.");
				return;
			}
			var perform_task = new PerformTask(pp.get("resid"), sources, aggregateFunc);
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

/*
 * A single task is an instance of Perform Task.
 * 
 * Requests:
 * -	DELETE: delete the task.
 */
function PerformTask(resid, sources, aggregateFunc) {
	var THISPerformTask = this;
	
	this.resid = resid;
	perform_tasks[this.resid] = this;
			
	this.res = new JavaScriptResource(resid);
	
	// Add SubResources //////////////////////////////////
	this.devicesres = new Devices("sources", sources);
	this.aggregateres = new Aggregate("aggregate", sources);
	this.aggregatefuncres = new FuncRes("aggregatefunc", aggregateFunc);

	this.res.add(THISPerformTask.devicesres.res);
	this.res.add(THISPerformTask.aggregateres.res);
	this.res.add(THISPerformTask.aggregatefuncres.res);
	
	// Requests //////////////////////////////////////////
	this.res.ondelete = function(request) {
		app.dump("Delete task.");
		unregisterAsObserver();
		delete perform_tasks[THISPerformTask.resid];
		THISPerformTask.res.remove();
		request.respond(CodeRegistry.RESP_DELETED);
	}
	
	// Functions /////////////////////////////////////////
	/*
	 * Unregister from all sources, when not needed anymore.
	 */
	function unregisterAsObserver() {
		var array = THISPerformTask.devicesres.devices;
		for (var el in array) {
			var unregister = new CoAPRequest();
			unregister.open("GET", array[el].getInfo());
			unregister.async = true;
			unregister.send();
		}
	}
	
	/*
	 * The aggregate resource offers an interface to access the aggregate value.
	 * 
	 * Requests:
	 * -	GET: get the aggregate value.
	 * 			Option: withdevice=true: request the aggregate with the source device number.
	 */
	function Aggregate(resid, sources) {
		var THISAggregate = this;
		
		this.aggregate_val = null;
						
		for (var el in sources) {
			var request = new CoAPRequest();
			request.open("GET", sources[el]);
			request.setObserverOption();
			var respHandler = new RespHandler(this, parseInt(el)+1);
			request.onload = respHandler.handle
			request.send();
		}
		
		/*
		 * The response handler reads the incoming value and aggregates it. It sets the new value. 
		 * If the new value differs from the old one, it notifies the observers.
		 */
		function RespHandler(in_aggregate_obj, in_device_nr) {
			var aggregate_obj = in_aggregate_obj;
			var device_nr = in_device_nr;
			
			this.handle = function(request) {
				var payload = request.getPayloadString();
				var old_aggregate_val = aggregate_obj.aggregate_val;
				if (!payload.empty) aggregate_obj.aggregate_val = THISPerformTask.aggregatefuncres.getFunc().perform(payload, device_nr)+";"+device_nr;
				if (old_aggregate_val != aggregate_obj.aggregate_val)
					THISAggregate.res.changed();
			}
		}
				
		this.res = new JavaScriptResource(resid);
		this.res.isObservable(true);
		
		// Requests /////////////////////////////////////
		
		var a_query = "";
		
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
	
	app.dump("New task created.");
}

/*
 * The devices resource is the top resource for all the sources.
 * 
 * Requests:
 * -	GET: return all the sources.
 */
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

// Function Resource ////////////////////////////////////
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
		var new_func = getFunc(payload);
		if (new_func!=null) {
			func_obj = new_func;
			request.respond(CodeRegistry.RESP_CHANGED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST);
		}
	}
	
	this.getFunc = function() {
		return func_obj;
	}
}

// Info Resource ////////////////////////////////////////
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

// Functions //////////////////////////////////////////////
/*
 * All aggregate functions:
 * -	sum
 * -	average
 * -	maximum
 * -	minimum
 * -	add
 * -	subtract
 * -	multiply
 * -	divide
 * -	modulo
 * -	prefix
 * -	suffix
 * -	newest
 * -	own
 */
function Sum() {
	var sum = 0;
	
	this.perform = function(value, device) {
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
	var max = Number.MIN_VALUE;
	
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
	var min = Number.MAX_VALUE;
	
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

function Add(in_adder) {
	var adder = parseFloat(in_adder);
	
	this.perform = function(value, device) {
		return parseFloat(value) + adder;
	}
	
	this.getStats = function() {
		return "add;;" + adder;
	}
}

function Subtract(in_sub) {
	var sub = parseFloat(in_sub);
	
	this.perform = function(value, device) {
		return parseFloat(value) - sub;
	}
	
	this.getStats = function() {
		return "subtract;;" + sub;
	}
}

function Multiply(in_multiply) {
	var multiply = parseFloat(in_multiply);
	
	this.perform = function(value, device) {
		return parseFloat(value) * multiply;
	}
	
	this.getStats = function() {
		return "multiply;;" + multiply;
	}
}

function Divide(in_divisor) {
	var divisor = parseFloat(in_divisor);
	
	this.perform = function(value, device) {
		return parseFloat(value) / divisor;
	}
	
	this.getStats = function() {
		return "divide;;" + divisor;
	}
}

function Modulo(in_modulo) {
	var modulo = parseFloat(in_modulo);
	
	this.perform = function(value, device) {
		return parseFloat(value) % modulo;
	}
	
	this.getStats = function() {
		return "modulo;;" + modulo;
	}
}

function Prefix(in_prefix) {
	var prefix = in_prefix;
	
	this.perform = function(value, device) {
		return ""+prefix + value;
	}
	
	this.getStats = function() {
		return "prefix;;" + prefix;
	}
}

function Suffix(in_suffix) {
	var suffix = in_suffix;
	
	this.perform = function(value, device) {
		return ""+value + suffix;
	}
	
	this.getStats = function() {
		return "suffix;;" + suffix;
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
/*
 * Returns the funciton object depending on the function-string passed (from the payload).
 */
function getFunc(func) {
	var fp = new FuncParser(func);
	var fu = fp.getFunc();
	app.dump("fu: " + fu);
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
/*
 * The payload parser parses the payload and extracts the labels and their values.
 */
function FuncParser(func) {
	var THISFuncParser = this;
	
	app.dump("func: " + func);
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
	
	try {
		var split_nl = payload.split('\n');
		for (var el=0; el<split_nl.length; el++) {
			if (split_nl[el].substring(0, split_nl[el].indexOf('=')-1)=="aggregatefunc") { // special treatment for the functions (aggregatefunc = own;;FUNCTION;;)
				var func = split_nl[el].substring(split_nl[el].indexOf('=')+1);
				while(func.indexOf(' ')==0) { // remove spaces after the equal sign: '   own' -> 'own'
					func = func.substring(1);
				}
				if (func.indexOf("own")==0) { // if own was specified for the function
					func += "\n";
					if (split_nl[el+1] && !(""+split_nl[el+1]).indexOf(';;')==0) { // read multiple lines
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
				
				this.map["aggregatefunc"] = func;
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

/*
 * Unregister as observer from sources.
 */
app.onunload = function() {
	for (var el in perform_tasks) {
		app.dump("Unregister from sources");
		perform_tasks[el].unregisterFromObserver();
	}
}