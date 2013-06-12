if (typeof(flect) == "undefined") flect = {};
if (typeof(flect.app) == "undefined") flect.app = {};
if (typeof(flect.app.sqlsync) == "undefined") flect.app.sqlsync = {};

flect.app.sqlsync.SqlSync = function(dragged) {
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
		this.desc = tr.find("td:eq(2)").text();
		this.objectName = tr.find("td:eq(3)").text();
		this.externalId = tr.find("td:eq(4)").text();
		this.lastExecuted = strToDate(tr.find("td:eq(5)").text());
		this.nextExecute = strToDate(tr.find("td:eq(6)").text());
		this.sql = tr.attr("data-sql");
		this.seqNo = tr.attr("data-seqNo");
	}
	function setCurrentSqlInfo(info) {
		currentSqlInfo = info;
		$("#name").val(info.name);
		$("#desc").val(info.desc);
		$("#sql").val(info.sql);
		$("#objectName").val(info.objectName);
		$("#externalIdFieldName").val(info.externalId);
		$("#sql-datetime").val(dateToStr(info.nextExecute));
		
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
	function getRecordCount() {
		return $("#sql-table").find("tbody tr").length;
	}
	function error(str) {
		$("#error-msg").html(str).show();
	}
	function disableSync() {
		btnSync.attr("disabled", "disabled");
	}
	function setScheduledTime(time) {
		$("#error-msg").hide();
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
	}
	
	var currentSqlInfo = null,
		grid = new flect.util.SqlGrid({
			"modelPath" : "/sql/model",
			"dataPath" : "/sql/data",
			"div" : "#gridDiv",
			"error" : error
		}),
		btnSql = $("#btnSQL").click(function() {
			$("#error-msg").hide();
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
		btnUpdate = $("#btnUpdate").click(function() {
			$("#error-msg").hide();
			var data = formToHash();
			data.oldName = currentSqlInfo.name;
			data.seqNo = currentSqlInfo.seqNo;
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
			$("#error-msg").hide();
			var data = formToHash();
			data.seqNo = getRecordCount() + 1;
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
			$("#error-msg").hide();
			var data = formToHash();
			data.seqNo = currentSqlInfo.seqNo;
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
		btnScheduleAdd = $("#btnScheduleAdd").click(function() {
			var time = selSchedule.val();
			var selected = selSchedule2.find("option");
			if (selected.length > 0) {
				var ret = "";
				for (var i=0; i<selected.length; i++) {
					var cur = $(selected[i]).attr("value");
					if (ret.length > 0) {
						ret += ",";
					}
					if (time) {
						if (time == cur) {
							alert("その時刻はすでに設定されています");
							return;
						} else if (time && time < cur) {
							ret += time + ",";
							time = null;
						}
					}
					ret += cur;
				}
				if (time) {
					if (ret.length > 0) {
						ret += ",";
					}
					ret += time;
				}
				time = ret;
			}
			setScheduledTime(time);
		}),
		btnScheduleDel = $("#btnScheduleDel").click(function() {
			var selected = selSchedule2.val();
			if (!selected || selected.length == 0) {
				alert("削除する時刻を指定してください。");
				return;
			}
			var options = selSchedule2.find("option");
			var ret = "";
			for (var i=0; i<options.length; i++) {
				var cur = $(options[i]).attr("value");
				if ($.inArray(cur, selected) == -1) {
					if (ret.length > 0) {
						ret += ",";
					}
					ret += cur;
				}
			}
			setScheduledTime(ret);
		}),
		btnExport = $("#btnExport").click(function() {
			location.href = "/sync/export.json";
		}),
		btnImport = $("#btnImport").click(function() {
			
			$("#importFile").val(null).click();
		}),
		selSchedule = $("#scheduledTime"),
		selSchedule2 = $("#scheduledTime2");
	
	$("#importFile").change(function() {
		var filename = $(this).val();
		if (filename) {
			$("#importForm")[0].submit();
		}
	});
	
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
	
	var table = $("#sql-table");
	table.find("tbody tr").draggable({
		"axis" : "y",
		"containment" : table,
		"helper" : "clone"
	});
	table.find("tr").droppable({
		"drop" : function(e, ui) {
			var idx = 0,
				dropName = $(this).find("td:eq(0)").text(),
				dragName = $(ui.draggable).find("td:eq(0)").text(),
				records = table.find("tbody tr"),
				names = [];
			if (dragName == dropName) {
				return;
			}
			if (!dropName) {
				names.push(dragName);
			}
			for (var i=0; i<records.length; i++) {
				var curName = $(records[i]).find("td:eq(0)").text();
				if (curName != dragName) {
					names.push(curName);
					if (curName == dropName) {
						names.push(dragName);
					}
				}
			}
			$.ajax({
				"url" : "/sync/sort", 
				"type" : "POST",
				"data" : {
					"dragName" : dragName,
					"sortNames" : names
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
		}
	});
	table.find(".sql-enable").click(function() {
		var name = $(this).parents("tr").attr("data-name");
		var enabled = !($(this).attr("data-enabled") == "true");
		$.ajax({
			"url" : "/sync/setEnabled", 
			"type" : "POST",
			"data" : {
				"name" : name,
				"enabled" : enabled
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
	});
	table.find("button").click(function() {
		var name = $(this).parents("tr").attr("data-name");
		if (confirm(name + "を削除しますか？")) {
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
	});
	if (dragged) {
		var tr = table.find("tr[data-name='" + dragged + "']");
		tr.hide().addClass("error").fadeTo(2000, "1.0", function() {
			tr.removeClass("error");
		});
	}
}
