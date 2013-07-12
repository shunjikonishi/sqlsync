$(function() {
	//IE6, 7, 8はスキップ
	if(!jQuery.support.opacity){
		return;
	}
	
	var body = $("body");
	
	//section id
	function calcSectionId(el) {
		var prev = $(el).prevAll("section");
		var ret = prev.length + 1;
		var parent = $(el).parents("section:first");
		if (parent.length > 0) {
			ret = calcSectionId(parent) + "-" + ret;
		}
		return ret;
	}
	$("section").each(function() {
		var id = $(this).attr("id");
		if (!id) {
			id = calcSectionId(this);
			$(this).attr("id", id);
		}
	});
	//Make nav
	function makeNav(el, ol) {
		var li = $("<li><a href='#" + el.attr("id") + "'>" + el.find("h1:first").text() + "</a></li>");
		var children = el.children("section");
		if (children.length > 0) {
			var childOl = $("<ol></ol>");
			children.each(function() {
				makeNav($(this), childOl);
			});
			li.append(childOl);
		}
		ol.append(li);
	}
	
	var sections = body.children("section")
	var nav = $("nav");
	if (nav.length == 0) {
		nav = $("<nav></nav>");
	}
	var ol = $("<ol></ol>");
	sections.each(function() {
		makeNav($(this), ol);
	});
	nav.css({
		"height" : "100%",
		"width" : "20%",
		"overflow" : "auto"
	});
	nav.append(ol);
	
	//Make splitter
	var sectionHolder = $("<div id='section-holder' style='overflow:auto;height:100%;width:80%;'></div>");
	sectionHolder.append(sections);
	
	var contentHolder = $("<div id='content-holder'></div>")
		.append(nav)
		.append(sectionHolder);
	
	var header = $("header");
	var footer = $("footer");
	if (header.length > 0) {
		header.after(contentHolder);
	} else if (footer.length > 0) {
		footer.before(contentHolder);
	} else {
		body.append(contentHolder);
	}
	if (header.length > 0) {
		//header.css("height", header.height());
		var div = $("<div style='width:100%;position:absolute;'></div>")
			.append(header.nextAll())
			.appendTo(body);
		body.fixedDiv({
			"orientation" : "vertical"
		});
	}
	if (footer.length > 0) {
		footer.parent().fixedDiv({
			"orientation" : "vertical",
			"keepLeft" : false
		});
	}
	contentHolder.splitter();
	
	var hash = location.hash;
	if (hash) {
		//FireFoxが初回表示の時にhashに飛んでくれないのに対応する
		setTimeout(function() {
			location.href = hash;
		}, 0);
	}
})
