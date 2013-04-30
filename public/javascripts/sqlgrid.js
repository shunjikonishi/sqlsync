if (typeof(flect) == "undefined") flect = {};
if (typeof(flect.util) == "undefined") flect.util = {};

/*
setting {
	"colModelPath": "",
	"dataPath" : "",
	"
}
*/
flect.util.SQLGrid = function(setting) {
	function execute(sql) {
	}
	$.extend(this, {
		"execute" : execute
	});
}
