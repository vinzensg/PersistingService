var THIS = this;

var perform_tasks = new Object();

// Add SubResources ///////////////////////////////////

this.tasksres = new Tasks("tasks");
this.singlecastres = new SingleCast("singlecast");

app.root.add(THIS.tasksres.res);
app.root.add(THIS.singlecastres.res);

// Single Cast ///////////////////////////////////////

function SingleCast(resid) {
	var THISSingleCast = this;
	
	this.res = new JavaScriptResource(resid);
	
	// Requests ////////////////////////////////////
	this.res.onpost = function(request) {
		performBroadcast(request, "POST");
		request.respond(CodeRegistry.RESP_VALID);		
	}
	
	this.res.onput = function(request) {
		performBroadcast(request, "PUT");
		request.respond(CodeRegistry.RESP_VALID);
	}
	
	this.res.ondelete = function(request) {
		performBroadcast(request, "DELETE");
		request.respond(CodeRegistry.RESP_VALID);
	}
	
	function performBroadcast(request, operation) {
		var payload = request.getPayloadString();
		var pp = new PayloadParser(payload);
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
	}
}

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
		if (pp.has("resid")) {
			var targets = new Array();
			var counter = 1;
			while(pp.has("target"+counter)) {
				var target = pp.get("target"+counter++);
				targets.push(target);
			}
			var perform_task = new PerformTask(pp.get("resid"), targets);
			THISTasks.res.add(perform_task.res);
			request.respond(CodeRegistry.RESP_CREATED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST, "Provide:\n" +
														   "resid = ...\n" +
														   "(targetx = ...)");				
		}	
	}
}


function PerformTask(resid, targets) {
	var THISPerformTask = this;
	
	var resid = resid;
	perform_tasks[this.resid] = this;
	
	this.res = new JavaScriptResource(resid);
	
	// Add SubResources //////////////////////////////////
	this.devicesres = new Devices("targets", targets);
	this.castres = new Cast("cast", targets);
	
	this.res.add(THISPerformTask.devicesres.res);
	this.res.add(THISPerformTask.castres.res);
	
	// Requests //////////////////////////////////////////
	this.res.ondelete = function(request) {
		delete perform_tasks[THISPerformTask.resid];
		THISPerformTask.res.remove();
		request.respond(CodeRegistry.RESP_DELETED);
	}
	
	
}

function Cast(resid, in_targets) {
	var THISCast = this;
	
	var targets = in_targets;
	
	this.res = new JavaScriptResource(resid);
	
	// Requests ////////////////////////////////////
	this.res.onpost = function(request) {
		performBroadcast(request, "POST");
		request.respond(CodeRegistry.RESP_VALID);
	}
	
	this.res.onput = function(request) {
		performBroadcast(request, "PUT");
		request.respond(CodeRegistry.RESP_VALID);
	}
	
	this.res.ondelete = function(request) {
		performBroadcast(request, "DELETE");
		request.respond(CodeRegistry.RESP_VALID);
	}

	function performBroadcast(request, operation) {
		var payload = request.getPayloadString();
		for (var el in targets) {
			var postrequest = new CoAPRequest();
			postrequest.open(operation, targets[el]);
			postrequest.send(payload);
		}
	}
}

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

//Payload Parser /////////////////////////////////////////
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