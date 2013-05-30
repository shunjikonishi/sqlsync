if (typeof(flect) == "undefined") flect = {};
if (typeof(flect.app) == "undefined") flect.app = {};
if (typeof(flect.app.sqlsync) == "undefined") flect.app.sqlsync = {};

flect.app.sqlsync.SqlSync = function(scheduledTime) {
	function strToDate(str) {
		var y = parseInt(str.substring(0, 4)),
			m = parseInt(str.substring(5, 7)),
			d = parseInt(str.substring(8, 10)),
			h = parseInt(str.substring(11, 13)),
			mi = parseInt(str.substring(14, 16)),
			s = parseInt(str.substring(17, 19)),
			ms = 0;
		return new Date(y, m - 1, d, h, mi, s, ms);
	}
	function dateToStr(d) {
		function pad(n) {
			return n < 10 ? "0" + n : "" + n;
		}
		return d.getFullYear() + "-" +
			pad(d.getMonth() + 1) + "-" +
			pad(d.getDate()) + " " + 
			pad(d.getHours()) + ":" +
			pad(d.getMinutes()) + ":" +
			pad(d.getSeconds());
	}
	function SQLInfo(rowid, tr) {
		this.rowid = rowid;
		this.name = tr.find("td:eq(0)").text();
		this.desc = tr.find("td:eq(1)").text();
		this.objectName = tr.find("td:eq(2)").text();
		this.externalId = tr.find("td:eq(3)").text();
		this.lastExecuted = strToDate(tr.find("td:eq(4)").text());
		this.nextExecute = strToDate(tr.find("td:eq(5)").text());
		this.sql = tr.attr("data-sql");
	}
	function setCurrentSqlInfo(info) {
		currentSqlInfo = info;
		$("#name").val(info.name);
		$("#desc").val(info.desc);
		$("#sql").val(info.sql);
		$("#objectName").val(info.objectName);
		$("#externalIdFieldName").val(info.externalId);
		$("#sql-datetime").val(dateToStr(info.nextExecute));
		
		btnDelete.removeAttr("disabled");
		btnUpdate.removeAttr("disabled");
		btnSync.removeAttr("disabled");
	}
	function formToHash() {
		var ret = {};
		$("#formList").find(":input").each(function(idx, el) {
			var name = $(el).attr("name");
			var value = $(el).val();
			ret[name] = value;
		});
		return ret;
	}
	function error(str) {
		$("#error-msg").html(str);
	}
	function disableSync() {
		btnSync.attr("disabled", "disabled");
	}
	
	var currentSqlInfo = null,
		grid = new flect.util.SQLGrid({
			"modelPath" : "/sql/model",
			"dataPath" : "/sql/data",
			"div" : "#gridDiv",
			"error" : error
		}),
		btnSql = $("#btnSQL").click(function() {
			var sql = $("#sql").val(),
				date = $("#sql-datetime").val();
			if (!sql) {
				alert("SQLを入力してください");
				return;
			}
			if (!date) {
				alert("対象日時を設定してください");
				return;
			}
			var params = [
				{
					"name" : "date",
					"type" : "datetime",
					"value" : date
				}
			];
			grid.execute(sql, params);
		}),
		btnDelete = $("#btnDelete").click(function() {
			if (confirm("削除しますか？")) {
				var name = currentSqlInfo.name;
				$.ajax({
					"url" : "/sync/delete", 
					"type" : "POST",
					"data" : {
						"name" : name
					},
					"success" : function(data) {
						location.reload();
					},
					"error" : function(xhr) {
						error(xhr.responseText);
					}
				});
			}
		}),
		btnUpdate = $("#btnUpdate").click(function() {
			var data = formToHash();
			data.oldName = currentSqlInfo.name;
			$.ajax({
				"url" : "/sync/update", 
				"type" : "POST",
				"data" : data,
				"success" : function(data) {
					if (data == "OK") {
						location.reload();
					} else {
						error(data);
					}
				},
				"error" : function(xhr) {
					error(xhr.responseText);
				}
			});
		}),
		btnAdd = $("#btnAdd").click(function() {
			var data = formToHash();
			$.ajax({
				"url" : "/sync/add", 
				"type" : "POST",
				"data" : data,
				"success" : function(data) {
					if (data == "OK") {
						location.reload();
					} else {
						error(data);
					}
				},
				"error" : function(xhr) {
					error(xhr.responseText);
				}
			});
		}),
		btnSync = $("#btnSync").click(function() {
			var data = formToHash();
			$.ajax({
				"url" : "/sync/execute", 
				"type" : "POST",
				"data" : data,
				"success" : function(data) {
					if (data == "OK") {
						location.reload();
					} else {
						error(data);
					}
				},
				"error" : function(xhr) {
					error(xhr.responseText);
				}
			});
		}),
		btnSchedule = $("#btnSchedule").click(function() {
			var time = selSchedule.val();
			$.ajax({
				"url" : "/sync/setScheduleTime", 
				"type" : "POST",
				"data" : {
					"scheduledTime" : time
				},
				"success" : function(data) {
					if (data == "OK") {
						location.reload();
					} else {
						error(data);
					}
				},
				"error" : function(xhr) {
					error(xhr.responseText);
				}
			});
		}),
		selSchedule = $("#scheduledTime");
	
	$("#name").change(disableSync);
	$("#desc").change(disableSync);
	$("#sql").change(disableSync);
	$("#objectName").change(disableSync);
	$("#externalIdFieldName").change(disableSync);
	$(".sql-data").change(disableSync);
	$("#sql-datetime").datetimepicker({
		"dateFormat" : "yy-mm-dd",
		"timeFormat" : "HH:mm:ss"
	});
	$("#sql-table").on("click", ".sqlinfo", function() {
		var tr = $(this);
		tr.parent().find("tr").removeClass("error");
		tr.addClass("error");
		var idx = tr.parent().find("tr").index(tr);
		setCurrentSqlInfo(new SQLInfo(idx, tr));
	});
	selSchedule.val(scheduledTime);
}
