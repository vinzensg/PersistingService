app.root.onput = function() {
	var req = new CoAPRequest();
	req.open("GET", "coap://localhost:5684/persistingservice/tasks/general/eval/history/all");
	req.setObserverOption();
	req.onload = function(request) {
		app.dump("Data receiving...");
		app.dump(request.getPayloadString());
	}
	
	req.send();
}