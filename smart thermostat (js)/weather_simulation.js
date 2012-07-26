var SUNRISE = "09:00";
var SUNSET = "17:00";

var sunriseRes = new Sun("sunrise", SUNRISE);
var sunsetRes = new Sun("sunset", SUNSET);

app.root.add(sunriseRes.res);
app.root.add(sunsetRes.res);


function Sun(resid, in_value) {
	
	var value = in_value;
	
	this.res = new JavaScriptResource(resid);
	
	this.res.onget = function(request) {
		request.respond(CodeRegistry.RESP_CONTENT, value);
	}
	
	this.res.onput = function(request) {
		payload = request.getPayloadString();
		value = payload;
		request.respond(CodeRegistry.RESP_CHANGED);
	}
}