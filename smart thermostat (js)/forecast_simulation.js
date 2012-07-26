var NOW = 22;
var TOMORROW = 12;

var nowTopres = new Top("now", (new Temp("temperature", NOW)).res);
var tomorrowTopres = new Top("tomorrow", (new Temp("low_temperature", TOMORROW)).res);

app.root.add(nowTopres.res);
app.root.add(tomorrowTopres.res);

function Temp(resid, in_value) {
	var THISTemp = this;
	
	var value = in_value;
	
	this.res = new JavaScriptResource(resid);
	this.res.isObservable(true);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, value);
	}
	
	this.res.onput = function(request) {
		payload = request.getPayloadString();
		value = payload;
		THISTemp.res.changed();
		request.respond(CodeRegistry.RESP_CHANGED);
	}
}

function Top(resid, sub_res) {
	
	this.res = new JavaScriptResource(resid);
	
	this.res.add(sub_res);
}