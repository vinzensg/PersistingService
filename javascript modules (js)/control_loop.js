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
		
		if (pp.has("resid") && pp.has("source") && pp.has("target") && pp.has("targetoperation") && pp.has("modifyfunc")) {
			if (perform_tasks[pp.get("resid")] != null) {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "resid already exists.");
				return;
			}
			
			var targetoperation = pp.get("targetoperation");
			if (!(targetoperation=="PUT" ||Êtargetoperation=="POST" || targetoperation=="DELETE")) {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Bad targetoperation.");
				return;
			}
			var decisionFunc = null;
			if (pp.has("decisionfunc")) {
				decisionFunc = getDecisionFunc(pp.get("decisionfunc"));
				if (decisionFunc==null) {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Bad decisionfunc.");
					return;
				}
			}
			var modifyFunc = getModifyFunc(pp.get("modifyfunc"));
			if (modifyFunc==null) {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Bad modifyfunc.");
				return;
			}
			var modifyFuncElse = null;
			if (pp.has("modifyfuncelse")) {
				modifyFuncElse = getModifyFunc(pp.get("modifyfuncelse"));
				if (modifyFuncElse==null) {
					request.respond(CodeRegistry.RESP_BAD_REQUEST, "Bad modifyelsefunc.");
					return;
				}
			}
			var perform_task = new PerformTask(pp.get("resid"), pp.get("source"), pp.get("target"), targetoperation, decisionFunc, modifyFunc, modifyFuncElse);
			THISTasks.res.add(perform_task.res);
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
														   "source = ... \n" +
														   "target = ... \n" +
														   "targetoperation = ... \n" +
														   "(decisionfunc = ... )\n" + 
														   "modifyfunc = ... \n" + 
														   "(modifyfuncelse = ... )");				
		}	
	}
}

/*
 * A single task is an instance of Perform Task.
 * 
 * Requests:
 * -	DELETE: delete the task.
 */
function PerformTask(resid, source, target, targetoperation, decisionFunc, modifyFunc, modifyFuncElse) {
	var THISPerformTask = this;
	
	this.resid = resid;
	
	var old_payload = "";
	
	perform_tasks[this.resid] = this;
	
	// Add SubResources ////////////////////////////////
	
	this.res = new JavaScriptResource(resid);
	
	this.sourceres = new InfoRes("source", source);
	this.targetres = new InfoRes("target", target);
	this.targetoperationres = new InfoRes("targetoperation", targetoperation);
	this.outputres = new OutputRes("output", source, target, targetoperation);
	this.decisionres = new FuncRes("decisionfunc", decisionFunc, getDecisionFunc, target, targetoperation);
	this.modifyres = new FuncRes("modifyfunc", modifyFunc, getModifyFunc, target, targetoperation);
	this.modifyelseres = new FuncRes("modifyfuncelse", modifyFuncElse, getModifyFunc, target, targetoperation);
	
	this.res.add(THISPerformTask.sourceres.res);
	this.res.add(THISPerformTask.targetres.res);
	this.res.add(THISPerformTask.targetoperationres.res);
	this.res.add(THISPerformTask.outputres.res);
	this.res.add(THISPerformTask.decisionres.res);
	this.res.add(THISPerformTask.modifyres.res);
	this.res.add(THISPerformTask.modifyelseres.res);
	
	// Requests ////////////////////////////////////////
	this.res.ondelete = function (request) {
		app.dump("Delete task.");
		unregisterAsObserver();
		delete perform_tasks[THISPerformTask.resid];
		request.respond(CodeRegistry.RESP_DELETED);
		THISPerformTask.res.remove();
	}
	
	// Functions ////////////////////////////////////////////
	/*
	 * Unregister from source, when not needed anymore.
	 */
	function unregisterAsObserver() {
		var unregister = new CoAPRequest();
		unregister.open("GET", THISPerformTask.sourceres.getInfo());
		unregister.async = true;
		unregister.send();
	}
	
	/*
	 * A resource which stores the last known output to the target device.
	 * 
	 * This resource is observable.
	 */
	function OutputRes(resid, source, target, targetoperation) {
		var THISOutputRes = this;
		
		this.res = new JavaScriptResource(resid);
		this.res.isObservable(true);
		
		var target_val = null;
		
		// Requests //////////////////////////////////////////
		this.res.onget = function(request) {
			if (target_val) request.respond(CodeRegistry.RESP_CONTENT, target_val);
			else request.respond(CodeRegistry.RESP_CONTENT, "");
		}
		
		// Perform Control Loop //////////////////////////////
		var source_request = new CoAPRequest();
		source_request.open("GET", source);
		source_request.setObserverOption();
		var respHandler = new RespHandler(target, targetoperation);
		source_request.onload = respHandler.handle
		source_request.send();
		
		/*
		 * Modify the incoming value depending on the decision function, the modification function and the modification-else function.
		 * 
		 * After the modification, the new value is sent to the target.
		 */
		this.modifyPayload = function(payload, target, target_operation) {
			old_payload = payload;
			if (THISPerformTask.decisionres.getFunc()!=null && THISPerformTask.modifyres.getFunc()!=null && THISPerformTask.modifyelseres.getFunc()!=null) {
				if (THISPerformTask.decisionres.getFunc().check(payload)) target_val = THISPerformTask.modifyres.getFunc().perform(payload);
				else target_val = THISPerformTask.modifyelseres.getFunc().perform(payload);
			} else if (THISPerformTask.decisionres.getFunc()!=null && THISPerformTask.modifyres.getFunc()!=null) {
				if (THISPerformTask.decisionres.getFunc().check(payload)) target_val = THISPerformTask.modifyres.getFunc().perform(payload);
				else return;
			} else { // modifyfunc only
				target_val = THISPerformTask.modifyres.getFunc().perform(payload);
			}
			if (target_val!=null) {
				var target_request = new CoAPRequest();
				target_request.open(target_operation, target);
				target_request.send(target_val);
				THISOutputRes.res.changed();
			}
		}
		
		// Response Handler /////////////////////////////////////
		/*
		 * The response handler gets the value from the source and calls modifyPayload.
		 */
		function RespHandler(in_target, in_target_operation) {
			var target = in_target;
			var target_operation = in_target_operation;
			
			this.handle = function(request) {
				var payload = request.getPayloadString();
				THISOutputRes.modifyPayload(payload, target, target_operation);
			}
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
	function FuncRes(in_resid, in_func_obj, in_getFunc, in_target, in_target_operation) {
		var THISFuncRes = this;
		var resid = in_resid;
		
		var getFunc = in_getFunc;
		var target = in_target;
		var target_operation = in_target_operation;

		var func_obj = in_func_obj;
		
		this.res = new JavaScriptResource(resid);
		
		this.res.onget = function(request) {
			if (func_obj) request.respond(CodeRegistry.RESP_CONTENT, func_obj.getStats());
			else request.respond(CodeRegistry.RESP_CONTENT, "");
		}
		
		this.res.onput = function(request) {
			var payload = request.getPayloadString();
			if (payload=="remove" && resid!="modifyfunc") {
				func_obj = null;
				request.respond(CodeRegistry.RESP_CHANGED);
			} else {
				var new_func = getFunc(payload);
				if (new_func!=null) {
					func_obj = new_func;
					THISPerformTask.outputres.modifyPayload(old_payload, target, target_operation);
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
	
	app.dump("New task created.");
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


// Decision Functions ////////////////////////////////////////////
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

function DecisionPrefix(in_prefix) {
	var prefix = ""+in_prefix;
	
	this.check = function(value) {
		if ((""+value).indexOf(prefix)==0) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "prefix;;" + prefix;
	}
}

function DecisionSuffix(in_suffix) {
	var suffix = ""+in_suffix;
	
	this.check = function(value) {		
		if ((""+value).lastIndexOf(suffix)==((""+value).length-suffix.length)) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "suffix;;" + suffix;
	}
}

function DecisionOwn(evalFunc) {
	var func = evalFunc;
	
	this.check = function(value) {
		if (eval(func)) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "own;;" + func + ";;";
	}
}

/*
 * Returns the function object depeding on the funcion-string passed (from the payload).
 */
function getDecisionFunc(func) {
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
		if (fp.getParamCount()==1) return new DecisionPrefix(fp.getParam(1));
		else return null;
	} else if (fu=="suffix") {
		if (fp.getParamCount()==1) return new DecisionSuffix(fp.getParam(1));
		else return null;
	} else if (fu=="own") {
		if (fp.getParamCount()==1) return new DecisionOwn(fp.getParam(1));
		else return null;
	} else {
		return null;
	}
}

// Modify Functions //////////////////////////////////////////////
/*
 * All modification functions available:
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
 * -	none
 * -	own
 */
function Sum() {
	var sum = 0;
	
	this.perform = function(value) {
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
	
	this.perform = function(value) {
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
	
	this.perform = function(value) {
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
	
	this.perform = function(value) {
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
	
	this.perform = function(value) {
		return parseFloat(value) + adder;
	}
	
	this.getStats = function() {
		return "add;;" + adder;
	}
}

function Subtract(in_sub) {
	var sub = parseFloat(in_sub);
	
	this.perform = function(value) {
		return parseFloat(value) - sub;
	}
	
	this.getStats = function() {
		return "subtract;;" + sub;
	}
}

function Multiply(in_multiply) {
	var multiply = parseFloat(in_multiply);
	
	this.perform = function(value) {
		return parseFloat(value) * multiply;
	}
	
	this.getStats = function() {
		return "multiply;;" + multiply;
	}
}

function Divide(in_divisor) {
	var divisor = parseFloat(in_divisor);
	
	this.perform = function(value) {
		return parseFloat(value) / divisor;
	}
	
	this.getStats = function() {
		return "divide;;" + divisor;
	}
}

function Modulo(in_modulo) {
	var modulo = parseFloat(in_modulo);
	
	this.perform = function(value) {
		return parseFloat(value) % modulo;
	}
	
	this.getStats = function() {
		return "modulo;;" + modulo;
	}
}

function ModifyPrefix(in_prefix) {
	var prefix = in_prefix;
	
	this.perform = function(value) {
		return ""+prefix + value;
	}
	
	this.getStats = function() {
		return "prefix;;" + prefix;
	}
}

function ModifySuffix(in_suffix) {
	var suffix = in_suffix;
	
	this.perform = function(value) {
		return ""+value + suffix;
	}
	
	this.getStats = function() {
		return "suffix;;" + suffix;
	}
}

function None() {
	
	this.perform = function(value) {
		return ""+value;
	}
	
	this.getStats = function() {
		return "none";
	}
}

function ModifyOwn(evalFunc) {
	var func = "" + evalFunc;
	var storage = new Array();
	
	this.perform = function(in_value) {
		var ret = null;
		var value = null;
		if (isNaN(in_value)) value = ""+in_value;
		else value = parseFloat(in_value);
		
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
function getModifyFunc(func) {
	var fp = new FuncParser(func);
	var fu = fp.getFunc();
	app.dump("Function: " + fu);
	if (fu=="sum") {
		return new Sum();
	} else if (fu=="avg") {
		return new Avg();
	} else if (fu=="max") {
		return new Max();
	} else if (fu=="min") {
		return new Min();
	} else if (fu=="add") {
		if (fp.getParamCount()==1) return new Add(fp.getParam(1));
		else return null;
	} else if (fu=="subtract") {
		if (fp.getParamCount()==1) return new Subtract(fp.getParam(1));
		else return null;
	} else if (fu=="multiply") {
		if (fp.getParamCount()==1) return new Multiply(fp.getParam(1));
		else return null;
	} else if (fu=="divide") {
		if (fp.getParamCount()==1) return new Divide(fp.getParam(1));
		else return null;
	} else if (fu=="modulo") {
		if (fp.getParamCount()==1) return new Modulo(fp.getParam(1));
		else return null;
	} else if (fu=="prefix") {
		if (fp.getParamCount()==1) return new ModifyPrefix(fp.getParam(1));
		else return null;
	} else if (fu=="suffix") {
		if (fp.getParamCount()==1) return new ModifySuffix(fp.getParam(1));
		else return null;
	} else if (fu=="none"){
		return new None();
	} else if (fu=="own") {
		if (fp.getParamCount()==1) return new ModifyOwn(fp.getParam(1));
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
			var control_func = split_nl[el].substring(0, split_nl[el].indexOf('=')-1);
	
			if (control_func=="decisionfunc" || control_func=="modifyfunc" || control_func=="modifyfuncelse") { // special treatment for the functions (...func = own;;FUNCTION;;)
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
						if (split_nl[el].indexOf(';;;')!=-1) { // the last line may contain three ;;;: own;;ret = 10;;;
							func += split_nl[el].substring(0, split_nl[el].indexOf(';;;')+1);
						} else {
							func += split_nl[el].substring(0, split_nl[el].indexOf(';;'));
						}
					}
				}
				this.map[control_func] = func;
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
		app.dump("Unregister from source");
		perform_tasks[el].unregisterAsObserver();
	}
}
