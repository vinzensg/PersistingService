app.root.onput = function(request_1) {
	var payload = request_1.getPayloadString();
	
	var req = new CoAPRequest();
	req.open("GET", payload);
	req.setObserverOption();
	req.onload = function(request_2) {
		app.dump("Data receiving...");
		app.dump(request_2.getPayloadString());
	}
	
	req.send();
	
	request_1.respond(CodeRegistry.RESP_VALID);
}