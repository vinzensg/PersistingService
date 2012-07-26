var realtemperature = new RealTemperature("temperature", 100.0);

app.root.add(realtemperature.res);

var TEMP_INTERVAL = 2*60*1000; // 10*60*1000 = 10min

function RealTemperature(resid, temp) {
	var THISRealTemp = this;
	
	var timeout = null;
	
	this.temp = temp;
	
	this.res = new JavaScriptResource(resid);
	this.res.isObservable(true);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, THISRealTemp.temp);
	}
	
	this.res.onput = function(request) {
		var payload = request.getPayloadString();
		app.dump("Set to temperature: " + payload);
		if (isNumber(payload)) {
			if (THISRealTemp.temp!=payload) {
				if (timeout) app.clearTimeout(timeout);
				timeout = app.setTimeout(updateTemp, TEMP_INTERVAL, parseFloat(THISRealTemp.temp), parseFloat(payload));				
			}
			THISRealTemp.res.changed();
			request.respond(CodeRegistry.RESP_CHANGED);
		} else {
			request.respond(CodeRegistry.RESP_BAD_REQUEST);
		}
		
	}
	
	function updateTemp(current, target) {
		app.dump("update temperature (current = " + current + " ; target = " + target + ")");
		if (target-current==0) return;
		
		if (Math.abs(target-current)<0.5) {
			THISRealTemp.temp = target;
			THISRealTemp.res.changed();
			return;
		}
		
		if (target-current > 0) {
			THISRealTemp.temp = (current+0.5) + "";
		} else { // target-current < 0
			THISRealTemp.temp = (current-0.5) + "";
		}
		THISRealTemp.res.changed();
		
		timeout = app.setTimeout(updateTemp, TEMP_INTERVAL, parseFloat(THISRealTemp.temp), target);
	}
	
	function isNumber(n) {
		return !isNaN(parseFloat(n)) && isFinite(n);
	}
}