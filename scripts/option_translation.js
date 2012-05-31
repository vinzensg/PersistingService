app.root.onput = function(request) {
	var payload = request.getPayloadString().split((';'));
	if (payload[2])	app.dump("Translation: Request: " + payload[0] + " URI: " + payload[1] + " Payload: " + payload[2]);
	else app.dump("Translation: Request: " + payload[0] + " URI: " + payload[1]);
	
	if (payload.length > 0) {
		var optionReq = new CoAPRequest();
		optionReq.open(payload[0], payload[1]);
		optionReq.async = false;
		if (payload[2]) optionReq.send(payload[2]);
		else optionReq.send();
		
		var response = optionReq.responseText;
		app.dump("Response: " + response);
		request.respond(CodeRegistry.RESP_CONTENT, response);
	} else {
		request.respond(CodeRegistry.RESP_BAD_REQUEST);
	}
}