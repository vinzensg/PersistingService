var THIS = this;

/*
 * perform tasks keeps track of the tasks created.
 */
var perform_tasks = new Object();

// Add SubResources ///////////////////////////////////

this.tasksres = new Tasks("tasks");
this.singlecastres = new SingleCast("single");

app.root.add(THIS.tasksres.res);
app.root.add(THIS.singlecastres.res);

// Single Cast ///////////////////////////////////////
/*
 * Resource to perform a single multicast without having to create an instance.
 */
function SingleCast(resid) {
	var THISSingleCast = this;
	
	this.res = new JavaScriptResource(resid);
	
	// Requests ////////////////////////////////////
	this.res.onpost = function(request) {
		var answer = performMulticast(request, "POST");
		if (answer=="") request.respond(CodeRegistry.RESP_VALID);
		else request.respond(CodeRegistry.RESP_BAD_REQUEST, answer);
	}
	
	this.res.onput = function(request) {
		var answer = performMulticast(request, "PUT");
		if (answer=="") request.respond(CodeRegistry.RESP_VALID);
		else request.respond(CodeRegistry.RESP_BAD_REQUEST, answer);
	}
	
	this.res.ondelete = function(request) {
		var answer = performMulticast(request, "DELETE");
		if (answer=="") request.respond(CodeRegistry.RESP_VALID);
		else request.respond(CodeRegistry.RESP_BAD_REQUEST, answer);
	}
	
	/*
	 * Perform the single multicast depending on the operation passed.
	 */
	function performMulticast(request, operation) {
		var payload = request.getPayloadString();
		var pp = new PayloadParser(payload);
		if (!(pp.has("target1"))) {
			return "Provide:\n" +
				   "target1 = ...\n" + 
				   "(targetX = ...)";
		}
										 
		var targets = new Array();
		var counter = 1;
		while(pp.has("target"+counter)) {
			var target = pp.get("target"+counter++);
			targets.push(target);
		}
		var payl = "";
		if (pp.has("payload"))
			payl = pp.get("payload");
		
		for (var el in targets) {
			var req = new CoAPRequest();
			req.open(operation, targets[el]);
			req.send(payl);
		}
		return "";
	}
}

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
		app.dump("Create new Task");
		var payload = request.getPayloadString();
		var pp = new PayloadParser(payload);
		if (pp.has("resid") && pp.has("target1")) {	
			if (perform_tasks[pp.get("resid")] != null) {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "resid already exists.");
				return;
			}
			
			var targets = new Array();
			var counter = 1;
			while(pp.has("target"+counter)) {
				var target = pp.get("target"+counter++);
				targets.push(target);
			}
			
			var targetdecisions = new Array();
			for (var decision_counter = 1;decision_counter < counter; decision_counter++) {
				if (pp.has("targetdecision"+decision_counter)) {
					var decisionFunc = getFunc(pp.get("targetdecision"+decision_counter));
					if (decisionFunc!=null) {
						targetdecisions.push(decisionFunc);
					} else {
						request.respond(CodeRegistry.RESP_BAD_REQUEST, "Bad targetdecision function (Nr: " + decision_counter + ")");
						return;
					}
				} else {
					targetdecisions.push(null);
				}
			}
			
			var perform_task = new PerformTask(pp.get("resid"), targets, targetdecisions);
			THISTasks.res.add(perform_task.res);
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
														   "target1 = ...\n" +
														   "(targetdecision1 = ...)\n" +
														   "(targetX = ...)\n" +
														   "(targetdecisionX = ...)");				
		}	
	}
}

/*
 * A single task is an instance of Perform Task.
 * 
 * Requests:
 * -	DELETE: delete the task.
 */
function PerformTask(resid, targets, targetdecisions) {
	var THISPerformTask = this;
	
	this.resid = resid;
	perform_tasks[this.resid] = this;
	
	this.res = new JavaScriptResource(resid);
	
	// Add SubResources //////////////////////////////////
	this.devicesres = new Devices("targets", targets);
	this.decisionsres = new Decisions("targetdecisions", targetdecisions);
	this.castres = new Cast("cast", targets);
	
	this.res.add(THISPerformTask.devicesres.res);
	this.res.add(THISPerformTask.decisionsres.res);
	this.res.add(THISPerformTask.castres.res);
	
	// Requests //////////////////////////////////////////
	this.res.ondelete = function(request) {
		delete perform_tasks[THISPerformTask.resid];
		THISPerformTask.res.remove();
		request.respond(CodeRegistry.RESP_DELETED);
	}

	/*
	 * The cast resource lets the user perform the multicast for a specified multicast task.
	 */
	function Cast(resid, in_targets) {
		var THISCast = this;
		
		var targets = in_targets;
		
		this.res = new JavaScriptResource(resid);
		
		// Requests ////////////////////////////////////
		this.res.onpost = function(request) {
			performMulticast(request, "POST");
			request.respond(CodeRegistry.RESP_VALID);
		}
		
		this.res.onput = function(request) {
			performMulticast(request, "PUT");
			request.respond(CodeRegistry.RESP_VALID);
		}
		
		this.res.ondelete = function(request) {
			performMulticast(request, "DELETE");
			request.respond(CodeRegistry.RESP_VALID);
		}
	
		/*
		 * Perform the multicast depending on the operation passed.
		 */
		function performMulticast(request, operation) {
			var payload = request.getPayloadString();
			for (var el in targets) {
				var decisionFunc = THISPerformTask.decisionsres.decisions[el].getFunc()
				if (decisionFunc==null) {
					var postrequest = new CoAPRequest();
					postrequest.open(operation, targets[el]);
					postrequest.send(payload);
				} else if (decisionFunc.check(payload)) {
						var postrequest = new CoAPRequest();
						postrequest.open(operation, targets[el]);
						postrequest.send(payload);
				}
			}
		}
	}
	
	app.dump("New task created.");
}

/*
 * The devices resource is a resource for all the targets.
 * 
 * Requests:
 * -	GET: return all the targets.
 */
function Devices(resid, targets) {
	var THISDevice = this;
		
	this.devices = new Array();
		
	this.res = new JavaScriptResource(resid);
	
	var counter = 1;
	for (var el in targets) {
		var deviceres = new InfoRes("device"+counter++, targets[el]);
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
	
/*
 * The decisions resource is a resource for all the decisions for the targets
 * 
 *  Requests:
 *  -	GET: returns all the decisions for the targets with the number of the target
 */
function Decisions(resid, targetdecisions) {
	var THISDecision = this;
		
	this.decisions = new Array();
		
	this.res = new JavaScriptResource(resid);
		
	var counter = 1;
	for (var el in targetdecisions) {
		var decisionres = new FuncRes("decision"+counter++, targetdecisions[el]);
		THISDecision.decisions.push(decisionres);
		this.res.add(decisionres.res);
	}
		
	this.res.onget = function(request) {
		var ret = "";
		var array = THISDecision.decisions;
		for (var el in array) {
			ret += (parseInt(el)+1)+";";
			if (array[el].getFunc()) ret += array[el].getFunc().getStats();
			ret += "\n";
		}
		request.respond(CodeRegistry.RESP_CONTENT, ret);
	}
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

//Info Resource ////////////////////////////////////////
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

//Decision Functions ////////////////////////////////////////////
/*
 * All decision functions:
 * -	equal
 * -	not equal
 * -	greater
 * -	greater equal
 * -	less
 * -	less equal
 * -	contains
 * -	prefix
 * -	suffix
 * -	own
 */
function Equal(in_equal) {
	var equal = null;
	if (isNaN(in_equal)) equal = ""+in_equal;
	else equal = parseFloat(in_equal);
	
	this.check = function(in_value) {
		var value = null;
		if (isNaN(in_value)) value = ""+in_value;
		else value = parseFloat(in_value);
		if (value==equal) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "equal;;" + equal;
	}
}

function NotEqual(in_not_equal) {
	var not_equal = null;
	if (isNaN(in_not_equal)) not_equal = ""+in_not_equal;
	else not_equal = parseFloat(in_not_equal),
	
	this.check = function(in_value) {
		var value = null;
		if (isNaN(in_value)) value = ""+in_value;
		else value = parseFloat(in_value);
		if (value!=not_equal) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "notequal;;" + not_equal;
	}
}

function Greater(in_greater) {
	var greater = parseFloat(in_greater);
		
	this.check = function(in_value) {
		var value = parseFloat(in_value);
		if (value>greater) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "greater;;" + greater;
	}
}

function GreaterEqual(in_greatereq) {
	var greatereq = parseFloat(in_greatereq);
	
	this.check = function(in_value) {
		var value = parseFloat(in_value);
		if (value>=greatereq) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "greaterequal;;" + greatereq;
	}
}

function Less(in_less) {
	var less = parseFloat(in_less);
	
	this.check = function(in_value) {
		var value = parseFloat(in_value);
		if (value<less) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "less;;" + less;
	}
}

function LessEqual(in_lesseq) {
	var lesseq = parseFloat(in_lesseq);
	
	this.check = function(in_value) {
		var value = parseFloat(in_value);
		if (value<=lesseq) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "lessequal;;" + lesseq;
	}
}

function Contains(in_contains) {
	var contains = ""+in_contains;
	
	this.check = function(value) {
		if ((""+value).indexOf(contains)!=-1) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "contains;;" + contains;
	}
}

function Prefix(in_prefix) {
	var prefix = ""+in_prefix;
	
	this.check = function(value) {
		if ((""+value).indexOf(prefix)==0) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "prefix;;" + prefix;
	}
}

function Suffix(in_suffix) {
	var suffix = ""+in_suffix;
	
	this.check = function(value) {		
		if ((""+value).lastIndexOf(suffix)==((""+value).length-suffix.length)) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "suffix;;" + suffix;
	}
}

function Own(evalFunc) {
	var func = evalFunc;
	
	this.check = function(value) {
		if (eval(func)) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "own;;" + func;
	}
}

/*
 * Returns the function object depeding on the funcion-string passed (from the payload).
 */
function getFunc(func) {
	var fp = new FuncParser(func);
	var fu = fp.getFunc();
	app.dump("Function: " + fu);
	if (fu=="equal") {
		if (fp.getParamCount()==1) return new Equal(fp.getParam(1));
		else return null;
	} else if (fu=="notequal") {
		if (fp.getParamCount()==1) return new NotEqual(fp.getParam(1));
		else return null;
	} else if (fu=="greater") {
		if (fp.getParamCount()==1) return new Greater(fp.getParam(1));
		else return null;
	} else if (fu=="greaterequal") {
		if (fp.getParamCount()==1) return new GreaterEqual(fp.getParam(1));
		else return null;
	} else if (fu=="less") {
		if (fp.getParamCount()==1) return new Less(fp.getParam(1));
		else return null;
	} else if (fu=="lessequal") {
		if (fp.getParamCount()==1) return new LessEqual(fp.getParam(1));
		else return null;
	} else if (fu=="contains") {
		if (fp.getParamCount()==1) return new Contains(fp.getParam(1));
		else return null;
	} else if (fu=="prefix") {
		if (fp.getParamCount()==1) return new Prefix(fp.getParam(1));
		else return null;
	} else if (fu=="suffix") {
		if (fp.getParamCount()==1) return new Suffix(fp.getParam(1));
		else return null;
	} else if (fu=="own") {
		if (fp.getParamCount()==1) return new Own(fp.getParam(1));
		else return null;
	} else {
		return null;
	}
}

//Function Parser ////////////////////////////////////////
/*
 * The function parser extracts the information from the function-string (from the paylod).
 */
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
/*
 * The payload parser parses the payload and extracts the labels and their values.
 */
function PayloadParser(payload) {
	this.map = new Object();
	
	try {
		var split_nl = payload.split('\n');
		for (var el=0; el<split_nl.length; el++) {
			var decision_func = split_nl[el].substring(0, split_nl[el].indexOf('=')-1);
	
			if (decision_func.indexOf("targetdecision")==0) { // special treatment for the functions (decision_func = own;;FUNCTION;;)
				var func = split_nl[el].substring(split_nl[el].indexOf('=')+1);
				while(func.indexOf(' ')==0) { // remove spaces after the equal sign: '   own' -> 'own'
					func = func.substring(1);
				}
				if (func.indexOf("own")==0) { // if own was specified for the function
					func += "\n";
					if (func.lastIndexOf(';;')>4) func = func.substring(0, func.lastIndexOf(';;')); // only one line: own;;...;;
					else if (split_nl[el+1] && !(""+split_nl[el+1]).indexOf(';;')==0) { // read multiple lines
						el++;
						while(split_nl[el] && (""+split_nl[el]).indexOf(';;')==-1){ // read all the lines until the second ;;
							func += split_nl[el] + "\n";
							el++;
						}
						func += split_nl[el].substring(0, split_nl[el].indexOf(';;'));
					}
				}
				this.map[decision_func] = func;
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