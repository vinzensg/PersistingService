function sendRequest(operation, target, payload) {
    if (window.XMLHttpRequest)
    {// code for IE7+, Firefox, Chrome, Opera, Safari
         xmlhttp=new XMLHttpRequest();
    }
    else
    {// code for IE6, IE5
        xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
    }
    xmlhttp.open(operation,"http://localhost:8080/proxy?" + target,false);
    xmlhttp.send(payload);
    return xmlhttp.responseText;
}

function sendAsyncRequest(operation, target, payload, callback) {
    if (window.XMLHttpRequest)
    {// code for IE7+, Firefox, Chrome, Opera, Safari
        xmlhttp=new XMLHttpRequest();
    }
    else
    {// code for IE6, IE5
        xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
    }
    xmlhttp.open(operation,"http://localhost:8080/proxy?" + target, true);
    if (callback)
        xmlhttp.onload = function() {callback(xmlhttp)};
    xmlhttp.send(payload);
}

function dateFromString(date_string) {
    var date_string_split = date_string.split("-");
    var date = date_string_split[0];
    var time = date_string_split[1];
    
    var date_split = date.split("/");
    var time_split = time.split(":");
    
    var year = date_split[0];
    var month = date_split[1];
    var day = date_split[2];
    
    var hour = time_split[0];
    var minute = time_split[1];
    var second = time_split[2];
            
    return new Date(year, month, day, hour, minute, second);
}

function dateToString(date) {
    var ret = date.getFullYear() + "/";
    
    if (hasMinDigits(date.getMonth(), 2)) ret += date.getMonth() + "/";
    else ret += "0" + (date.getMonth() + 1) + "/";
    
    if (hasMinDigits(date.getDate(), 2)) ret += date.getDate() + "-";
    else ret += "0" + date.getDate() + "-";
    
    if (hasMinDigits(date.getHours(), 2)) ret += date.getHours() + ":";
    else ret += "0" + date.getHours() + ":";
    
    if (hasMinDigits(date.getMinutes(), 2)) ret += date.getMinutes() + ":";
    else ret += "0" + date.getMinutes() + ":";
    
    if (hasMinDigits(date.getSeconds(), 2)) ret += date.getSeconds();
    else ret += "0" + date.getSeconds();
    
    return ret;
}

function hasMinDigits(num, digits) {
    var min = Math.pow(10,digits-1);
    return num>=min;
}