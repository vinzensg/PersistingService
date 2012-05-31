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
		if (pp.has("resid") && pp.has("source") && pp.has("target") && pp.has("targetoperation") && pp.has("modifyfunc")) {
			if (pp.has("modifyfuncelse") && !pp.has("decisionfunc")) {
				request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide: decisionfunc = ...");
				return;
			}
			
			var decisionfunc = null;
			if (pp.has("decisionfunc")) {
				decisionfunc = pp.get("decisionfunc");
			}
			var modifyfuncelse = null;
			if (pp.has("modifyfuncelse")) {
				modifyfuncelse = pp.get("modifyfuncelse");
			}
			var perform_task = new PerformTask(pp.get("resid"), pp.get("source"), pp.get("target"), pp.get("targetoperation"), decisionfunc, pp.get("modifyfunc"), modifyfuncelse);
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


function PerformTask(resid, source, target, targetoperation, decisionfunc, modifyfunc, modifyfuncelse) {
	var THISPerformTask = this;
	
	this.resid = resid;
	
	perform_tasks[this.resid] = this;
	
	this.decisionFunc = null;
	if (decisionfunc) this.decisionFunc = getDecisionFunc(decisionfunc);
	
	this.modifyFunc = getModifyFunc(modifyfunc);
		
	this.modifyFuncElse = null;
	if (modifyfuncelse) this.modifyFuncElse = getModifyFunc(modifyfuncelse);
	
	// Add SubResources ////////////////////////////////
	
	this.res = new JavaScriptResource(resid);
	
	this.sourceres = new InfoRes("source", source);
	this.targetres = new InfoRes("target", target);
	this.modifyres = new FuncRes("modifyfunc", THISPerformTask.modifyFunc, getModifyFunc, target, targetoperation);
	
	this.res.add(THISPerformTask.sourceres.res);
	this.res.add(THISPerformTask.targetres.res);
	this.res.add(THISPerformTask.modifyres.res);
	
	if (decisionfunc) {
		this.decisionres = new FuncRes("decisionfunc", THISPerformTask.decisionFunc, getDecisionFunc);
		this.res.add(THISPerformTask.decisionres.res);
	}
	if (modifyfuncelse) {
		this.modifyelseres = new FuncRes("modifyfuncelse", THISPerformTask.modifyFuncElse, getModifyFunc);
		this.res.add(THISPerformTask.modifyelseres.res);
	}
	
	// Perform Control Loop //////////////////////////////
		
	var source_request = new CoAPRequest();
	source_request.open("GET", source);
	source_request.setObserverOption();
	var respHandler = new RespHandler(target, targetoperation);
	source_request.onload = respHandler.handle
	source_request.send();
	
	// Requests ////////////////////////////////////////
	this.res.ondelete = function (request) {
		delete perform_tasks[THISPerformTask.resid];
		request.respond(CodeRegistry.RESP_DELETED);
		THISPerformTask.res.remove();
	}
	
	// Function ////////////////////////////////////////////

	var old_payload = "";
	
	function modifyPayload(payload, target, target_operation) {
		old_payload = payload;
		var target_val = null;
		if (THISPerformTask.decisionFunc!=null && THISPerformTask.modifyFunc!=null && THISPerformTask.modifyFuncElse!=null) {
			app.dump("if else")
			if (THISPerformTask.decisionres.getFunc().check(payload)) target_val = THISPerformTask.modifyres.getFunc().perform(payload);
			else target_val = THISPerformTask.modifyelseres.getFunc().perform(payload);
		} else if (THISPerformTask.decisionFunc!=null && THISPerformTask.modifyFunc!=null) {
			app.dump("if");
			if (THISPerformTask.decisionres.getFunc().check(payload)) target_val = THISPerformTask.modifyres.getFunc().perform(payload);
		} else { // modifyfunc only
			app.dump("perform");
			target_val = THISPerformTask.modifyres.getFunc().perform(payload);
		}
		app.dump("target_val: " + target_val);
		if (target_val) {
			var target_request = new CoAPRequest();
			target_request.open(target_operation, target);
			target_request.send(target_val);
		}
	}
	
	// Response Handler /////////////////////////////////////
	
	function RespHandler(in_target, in_target_operation) {
		var target = in_target;
		var target_operation = in_target_operation;
		
		this.handle = function(request) {
			var payload = request.getPayloadString();
			modifyPayload(payload, target, target_operation);
		}
	}

	// Function Resource ////////////////////////////////////

	function FuncRes(resid, in_func_obj, in_getFunc, in_target, in_target_operation) {
		var THISFuncRes = this;
		
		var getFunc = in_getFunc;
		var target = in_target;
		var target_operation = in_target_operation;

		var func_obj = in_func_obj;
		
		this.res = new JavaScriptResource(resid);
		
		this.res.onget = function(request) {
			request.respond(CodeRegistry.RESP_CONTENT, func_obj.getStats());
		}
		
		this.res.onput = function(request) {
			var payload = request.getPayloadString();
			var new_func = getFunc(payload);
			if (new_func!=null) {
				func_obj = new_func;
				modifyPayload(old_payload, target, target_operation);
				request.respond(CodeRegistry.RESP_CHANGED);
			} else {
				request.respond(CodeRegistry.RESP_BAD_REQUEST);
			}
		}
		
		this.getFunc = function() {
			return func_obj;
		}
	}
}

//Info Resource ////////////////////////////////////////

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

function Equal(in_equal) {
	var equal = in_equal;
	
	this.check = function(value) {
		if (value==equal) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "equal: " + equal;
	}
}

function Greater(in_greater) {
	var greater = in_greater;
		
	this.check = function(value) {
		if (value>greater) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "greater: " + greater;
	}
}

function GreaterEqual(in_greatereq) {
	var greatereq = in_greatereq;
	
	this.check = function(value) {
		if (value>=greatereq) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "greaterequal: " + greatereq;
	}
}

function Less(in_less) {
	var less = in_less;
	
	this.check = function(value) {
		if (value<less) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "less: " + less;
	}
}

function LessEqual(in_lesseq) {
	var lesseq = in_lesseq;
	
	this.check = function(value) {
		if (value<=lesseq) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "lessequal: " + lesseq;
	}
}

function Contains(in_contains) {
	var contains = in_contains;
	
	this.check = function(value) {
		if (value.indexOf(contains)!=-1) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "contains: " + contains;
	}
}

function Prefix(in_prefix) {
	var prefix = in_prefix;
	
	this.check = function(value) {
		if (value.indexOf(prefix)==0) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "prefix: " + prefix;
	}
}

function Suffix(in_suffix) {
	var suffix = in_suffix;
	
	this.check = function(value) {
		if (value.lastIndexOf(suffix)==value.length-suffix.length) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "suffix: " + suffix;
	}
}

function Own(evalFunc) {
	var func = evalFunc;
	
	this.check = function(value) {
		if (eval(func)) return true;
		else return false;
	}
	
	this.getStats = function() {
		return "own: " + func;
	}
}

function getDecisionFunc(func) {
	var fp = new FuncParser(func);
	var fu = fp.getFunc();
	app.dump("FU: " + fu);
	if (fu=="equal") {
		app.dump("equal");
		if (fp.getParamCount()==1) return new Equal(fp.getParam(1));
		else return null;
	} else if (fu=="greater") {
		app.dump("greater");
		if (fp.getParamCount()==1) return new Greater(fp.getParam(1));
		else return null;
	} else if (fu=="greaterequal") {
		app.dump("greaterequal");
		if (fp.getParamCount()==1) return new GreaterEqual(fp.getParam(1));
		else return null;
	} else if (fu=="less") {
		app.dump("less");
		if (fp.getParamCount()==1) return new Less(fp.getParam(1));
		else return null;
	} else if (fu=="lessequal") {
		app.dump("lessequal");
		if (fp.getParamCount()==1) return new LessEqual(fp.getParam(1));
		else return null;
	} else if (fu=="contains") {
		app.dump("contains");
		if (fp.getParamCount()==1) return new Contains(fp.getParam(1));
		else return null;
	} else if (fu=="prefix") {
		app.dump("prefix");
		if (fp.getParamCount()==1) return new Prefix(fp.getParam(1));
		else return null;
	} else if (fu=="suffix") {
		app.dump("suffix");
		if (fp.getParamCount()==1) return new Suffix(fp.getParam(1));
		else return null;
	} else if (fu=="own") {
		app.dump("own");
		if (fp.getParamCount()==1) return new Own(fp.getParam(1));
		else return null;
	} else {
		return null;
	}
}

// Modify Functions //////////////////////////////////////////////

function Sum() {
	var sum = 0;
	
	this.perform = function(value) {
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
	var adder = in_adder;
	
	this.perform = function(value) {
		return parseFloat(value) + parseFloat(adder);
	}
	
	this.getStats = function() {
		return "add;;" + adder;
	}
}

function Subtract(in_sub) {
	var sub = in_sub;
	
	this.perform = function(value) {
		return parseFloat(value) - parseFloat(sub);
	}
	
	this.getStats = function() {
		return "subtract;;" + sub;
	}
}

function Multiply(in_multiply) {
	var multiply = in_multiply;
	
	this.perform = function(value) {
		return parseFloat(value) * parseFloat(multiply);
	}
	
	this.getStats = function() {
		return "multiply;;" + multiply;
	}
}

function Devide(in_devisor) {
	var divisor = in_divisor;
	
	this.perform = function(value) {
		return parseFloat(value) / parseFloat(divisor);
	}
	
	this.getStats = function() {
		return "divide;;" + divisor;
	}
}

function Modulo(in_modulo) {
	var modulo = in_modulo;
	
	this.perform = function(value) {
		return parseFloat(value) % parseFloat(modulo);
	}
	
	this.getStats = function() {
		return "modulo;;" + modulo;
	}
}

function Prefix(in_prefix) {
	var prefix = in_prefix;
	
	this.perform = function(value) {
		return prefix + value;
	}
	
	this.getStats = function() {
		return "prefix;;" + prefix;
	}
}

function Suffix(in_suffix) {
	var suffix = in_suffix;
	
	this.perform = function(value) {
		return value + suffix;
	}
	
	this.getStats = function() {
		return "suffix;;" + suffix;
	}
}

function Newest() {
	
	this.perform = function(value) {
		return value;
	}
	
	this.getStats = function() {
		return "newest";
	}
}

function Own(evalFunc) {
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

function getModifyFunc(func) {
	var fp = new FuncParser(func);
	var fu = fp.getFunc();
	app.dump("FU: " + fu);
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
	} else if (fu=="add") {
		app.dump("add");
		if (fp.getParamCount()==1) return new Add(fp.getParam(1));
		else return null;
	} else if (fu=="subtract") {
		app.dump("subtract");
		if (fp.getParamCount()==1) return new Subtract(fp.getParam(1));
		else return null;
	} else if (fu=="mulitply") {
		app.dump("multiply");
		if (fp.getParamCount()==1) return new Multiply(fp.getParam(1));
		else return null;
	} else if (fu=="devide") {
		app.dump("devide");
		if (fp.getParamCount()==1) return new Devide(fp.getParam(1));
		else return null;
	} else if (fu=="modulo") {
		app.dump("modulo");
		if (fp.getParamCount()==1) return new Modulo(fp.getParam(1));
		else return null;
	} else if (fu=="prefix") {
		app.dump("prefix");
		if (fp.getParamCount()==1) return new Prefix(fp.getParam(1));
		else return null;
	} else if (fu=="suffix") {
		app.dump("suffix");
		if (fp.getParamCount()==1) return new Suffix(fp.getParam(1));
		else return null;
	} else if (fu=="newest"){
		app.dump("newest");
		return new Newest();
	} else if (fu=="own") {
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
		var control_func = split_nl[el].substring(0, split_nl[el].indexOf('=')-1);

		if (control_func=="decisionfunc" || control_func=="modifyfunc" || control_func=="modifyfuncelse") {
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
			this.map[control_func] = func;
		} else {
			var no_space = split_nl[el].split(' ').join('');
			this.map[no_space.substring(0, no_space.indexOf('='))] = no_space.substring(no_space.indexOf('=')+1);
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
