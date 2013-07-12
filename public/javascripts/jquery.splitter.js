/*
if (typeof(flect) == "undefined") flect = {};
if (typeof(flect.html) == "undefined") flect.html = {};
if (typeof(flect.html.internal) == "undefined") flect.html.internal = {};

flect.html.internal.SplitterManager = {
	"splitters" : [],
	"current" : null,
	"initialized" : false
}

flect.html.internal.SplitterManager.init = function(splitter) {
	this.splitters.push(splitter);
	
	var self = this;
	if (this.initialized) {
		return;
	}
	this.initialized = true;
	
	var drag = false;
	var bgColor = null;
	var current = null;
	
	function doResize(current, ev) {
		var parent = current.parent;
		var div1 = current.div1;
		var div2 = current.div2;
		var horizontal = current.orientation() == "horizontal";
		var rbSize = 0;
		if (current.resizebar) {
			rbSize = horizontal ? current.resizebar.outerWidth(true) : current.resizebar.outerHeight(true);
		}
		var keepLeft = current.keepLeft();
		
		if (horizontal) {
			var cw = parent.width();
			var l, r;
			if (keepLeft) {
				l = div1.outerWidth(true);
				if (l > cw) {
					l = cw;
				}
				r = cw - l - rbSize;
				if (r < 0) {
					r = 0;
				}
			} else {
				r = div2.outerWidth(true);
				if (r > cw) {
					r = cw;
				}
				l = cw - r - rbSize;
				if (l < 0) {
					l = 0;
				}
			}
			current.position(l);
		} else {
			var ch = parent.height();
			var t, b;
			if (keepLeft) {
				t = div1.outerHeight(true);
				if (t > ch) {
					t = ch;
				}
				b = ch - t - rbSize;
				if (b < 0) {
					b = 0;
				}
			} else {
				b = div2.outerHeight(true);
				if (b > ch) {
					b = ch;
				}
				t = ch - b - rbSize;
				if (t < 0) {
					t = 0;
				}
			}
			current.position(t);
		}
		if (current._windowResized) {
			curent._windowResized(ev);
		}
	}
	
	$(document.documentElement).mousedown(function() {
		current = self.current;
		if (current != null) {
			bgColor = current.resizebar.css("background-color");
			current.resizebar.css("background-color", '#696969');
			$('<div class="splitterMask"></div>').insertAfter(current.parent);
			$('body').css('cursor', current.resizebar.css("cursor"));
			drag = true;
			return false;
		}
	}).mouseup(function() {
		if (current != null) {
			drag = false;
			$('div.splitterMask').remove();
			current.resizebar.css("background-color", bgColor);
			$('body').css('cursor', 'auto');
		}
	}).mousemove(function(ev) {
		if (current == null || !drag) {
			return;
		}
		var horizontal = current.orientation() == "horizontal";
		if (horizontal) {
			var pw = current.parent.width();
			var x = ev.clientX - current.parent.offset().left;
			if (x < 0) {
				x = 0;
			} else if (x > pw) {
				x = pw;
			}
			current.position(x);
		} else {
			var ph = current.parent.height();
			var y = ev.clientY - current.parent.offset().top;
			if (y < 0) {
				y = 0;
			} else if (y > ph) {
				y = ph;
			}
			current.position(y);
		}
		if (current._paneResized) {
			current._paneResized(ev);
		}
		for (var i=0; i<self.splitters.length; i++) {
			var pane = self.splitters[i];
			if (pane != current) {
				doResize(pane, ev);
			}
		}
	});
	
	$(window).resize(function(ev){
		for (var i=0; i<self.splitters.length; i++) {
			doResize(self.splitters[i], ev);
		}
	});
}

flect.html.internal.SplitterManager.release = function(splitter) {
	var idx = -1;
	for (var i=0; i<this.splitters.length; i++) {
		if (this.splitters[i] == splitter) {
			idx = i;
			break;
		}
	}
	if (idx >= 0) {
		this.splitters.splice(idx, 1);
	}
}

flect.html.Splitter = function(parent, div1, div2, horizontal) {
	var self = this;
	flect.html.internal.SplitterManager.init(this);
	
	this.parent = $(parent);
	this.div1 = $(div1);
	this.div2 = $(div2);
	
	this.resizebar = $("<div class='splitter-resizebar'></div>").appendTo(this.parent);
	this.resizebar.css({
		"background-color" : "#a9a9a9",
		"position" : "absolute",
		"z-index" : 20,
		"overflow" : "hidden"
	}).mouseenter(function() {
		flect.html.internal.SplitterManager.current = self;
	}).mouseleave(function() {
		flect.html.internal.SplitterManager.current = null;
	});
	if (horizontal) {
		var w1 = this.div1.outerWidth(true);
		var w2 = this.parent.width() - w1;
		this.div1.css({
			"position" : "absolute",
			"overflow-x" : "auto",
			"left" : 0,
			"width" : w1
		});
		this.resizebar.css({
			"left" : w1,
			"width" : 4,
			"height" : "100%",
			"cursor" : "col-resize"
		});
		var w3 = this.resizebar.outerWidth(true);
		this.div2.css({
			"position" : "absolute",
			"overflow-x" : "auto",
			"right" : 0,
			"width" : w2 - w3
		});
	} else {
		var h1 = this.div1.outerHeight(true);
		var h2 = this.div2.outerHeight(true);
		this.div1.css({
			"position" : "absolute",
			"overflow-y" : "auto",
			"top" : 0,
			"height" : h1
		});
		this.resizebar.css({
			"top" : h1,
			"width" : "100%",
			"height" : 4,
			"cursor" : "row-resize"
		});
		var h3 = this.resizebar.outerHeight(true);
		this.div2.css({
			"position" : "absolute",
			"overflow-y" : "auto",
			"bottom" : 0,
			"height" : h2 - h3
		});
	}
	this._keepLeft = true;
	this._limit = 0;
	this._paneResized = null;
	this._windowResized = null;
}

flect.html.Splitter.prototype.orientation = function() {
	var cursor = this.resizebar.css("cursor").toLowerCase();
	return cursor == "col-resize" ? "horizontal" : "vertical";
}

flect.html.Splitter.prototype.paneResized = function(func) {
	if (func === undefined) {
		return this._paneResized;
	} else {
		this._paneResized = func;
	}
}

flect.html.Splitter.prototype.windowResized = function(func) {
	if (func === undefined) {
		return this._windowResized;
	} else {
		this._windowResized = func;
	}
}

flect.html.Splitter.prototype.keepLeft = function(b) {
	if (b === undefined) {
		return this._keepLeft;
	} else {
		this._keepLeft = b;
	}
}

flect.html.Splitter.prototype.limit = function(n) {
	if (n === undefined) {
		return this._limit;
	} else {
		this._limit = n;
	}
}


flect.html.Splitter.prototype.position = function(n) {
	if (this.orientation() == "horizontal") {
		if (n === undefined) {
			return this.resizebar.css("left");
		} else {
			var low = this._limit;
			var high = this.parent.width() - this._limit;
			if (n < low) {
				n = low;
			} else if (n > high) {
				n = high;
			}
			this.resizebar.css("left", n);
			this.div1.css("width", n);
			this.div2.css("width", this.parent.width() - this.resizebar.outerWidth(true) - n);
		}
	} else {
		if (n === undefined) {
			return this.resizebar.css("top");
		} else {
			var low = this._limit;
			var high = this.parent.height() - this._limit;
			if (n < low) {
				n = low;
			} else if (n > high) {
				n = high;
			}
			this.resizebar.css("top", n);
			this.div1.css("height", n);
			this.div2.css("height", this.parent.height() - this.resizebar.outerHeight(true) - n);
		}
	}
}

flect.html.Splitter.prototype.release = function() {
	flect.html.internal.SplitterManager.release(this);
}
flect.html.FixedDiv = function(parent, div1, div2, horizontal, keepLeft) {
	var self = this;
	flect.html.internal.SplitterManager.init(this);
	this.parent = $(parent);
	this.div1 = $(div1);
	this.div2 = $(div2);
	
	function parentWidth() {
		if (self.parent.get(0) == document.body) {
			return document.documentElement.clientWidth;
		} else {
			return self.parent.innerWidth();
		}
	}
	function parentHeight() {
		if (self.parent.get(0) == document.body) {
			return document.documentElement.clientHeight;
		} else {
			return self.parent.innerHeight();
		}
	}
	function position(n) {
		if (horizontal) {
			if (n === undefined) {
				return self.div1.width();
			} else {
				var w1, w2;
				if (keepLeft) {
					w1 = self.div1.width();
					w2 = parentWidth() - w1;
					
					self.div1.css({
						"position" : "absolute",
						"top" : 0,
						"left" : 0,
						"height" : "100%",
						"width" : w1
					});
					self.div2.css({
						"position" : "absolute",
						"top" : 0,
						"left" : w1,
						"height" : "100%",
						"width" : w2
					});
				} else {
					w2 = self.div2.width();
					w1 = parentWidth() - w2;
					
					self.div1.css({
						"position" : "absolute",
						"top" : 0,
						"left" : 0,
						"height" : "100%",
						"width" : w1
					});
					self.div2.css({
						"position" : "absolute",
						"top" : 0,
						"right" : 0,
						"height" : "100%",
						"width" : w2
					});
				}
			}
		} else {
			if (n === undefined) {
				return self.div1.height();
			} else {
				var h1, h2;
				if (keepLeft) {
					h1 = self.div1.height();
					h2 = parentHeight() - h1;
					
					self.div1.css({
						"position" : "absolute",
						"top" : 0,
						"left" : 0,
						"width" : "100%",
						"height" : h1
					});
					self.div2.css({
						"position" : "absolute",
						"top" : h1,
						"left" : 0,
						"width" : "100%",
						"height" : h2
					});
				} else {
					h2 = self.div2.height();
					h1 = parentHeight() - h2;
					
					self.div1.css({
						"position" : "absolute",
						"top" : 0,
						"left" : 0,
						"width" : "100%",
						"height" : h1
					});
					self.div2.css({
						"position" : "absolute",
						"bottom" : 0,
						"left" : 0,
						"width" : "100%",
						"height" : h2
					});
				}
			}
		}
	}
	if (horizontal) {
		position(this.div1.width());
	} else {
		position(this.div1.height());
	}
	this._windowResized = null;
	$.extend(this, {
		"orientation" : function() {
			return horizontal ? "horizontal" : "vertical";
		},
		"keepLeft" : function() {
			return keepLeft;
		},
		"windowResized" : function(func) {
			if (func === undefined) {
				return self._windowResized;
			} else {
				self._windowResized = func;
			}
		},
		"position" : position
	});
}

flect.html.FixedDiv.prototype.release = function() {
	flect.html.internal.SplitterManager.release(this);
};
*/


(function($) {
	function SplitterManager() {
		var splitters = [],
			current = null,
			drag = false,
			bgColor = null,
			current = null,
			dragObj = null;
		
		function doResize(current, ev) {
			var parent = current.parent;
			var div1 = current.div1;
			var div2 = current.div2;
			var horizontal = current.orientation() == "horizontal";
			var rbSize = 0;
			if (current.resizebar) {
				rbSize = horizontal ? current.resizebar.outerWidth(true) : current.resizebar.outerHeight(true);
			}
			var keepLeft = current.keepLeft();
			
			if (horizontal) {
				var cw = parent.width();
				var l, r;
				if (keepLeft) {
					l = div1.outerWidth(true);
					if (l > cw) {
						l = cw;
					}
					r = cw - l - rbSize;
					if (r < 0) {
						r = 0;
					}
				} else {
					r = div2.outerWidth(true);
					if (r > cw) {
						r = cw;
					}
					l = cw - r - rbSize;
					if (l < 0) {
						l = 0;
					}
				}
				current.position(l);
			} else {
				var ch = parent.height();
				var t, b;
				if (keepLeft) {
					t = div1.outerHeight(true);
					if (t > ch) {
						t = ch;
					}
					b = ch - t - rbSize;
					if (b < 0) {
						b = 0;
					}
				} else {
					b = div2.outerHeight(true);
					if (b > ch) {
						b = ch;
					}
					t = ch - b - rbSize;
					if (t < 0) {
						t = 0;
					}
				}
				current.position(t);
			}
			if (current.windowResized()) {
				curent.windowResized()(ev);
			}
		}
		
		$(document.documentElement).mousedown(function() {
			dragObj = current;
			if (dragObj != null) {
				bgColor = dragObj.resizebar.css("background-color");
				dragObj.resizebar.css("background-color", '#696969');
				$('<div class="splitterMask"></div>').insertAfter(dragObj.parent);
				$('body').css('cursor', dragObj.resizebar.css("cursor"));
				drag = true;
				return false;
			}
		}).mouseup(function() {
			if (dragObj != null) {
				drag = false;
				$('div.splitterMask').remove();
				dragObj.resizebar.css("background-color", bgColor);
				$('body').css('cursor', 'auto');
			}
		}).mousemove(function(ev) {
			if (dragObj == null || !drag) {
				return;
			}
			var horizontal = dragObj.orientation() == "horizontal";
			if (horizontal) {
				var pw = dragObj.parent.width();
				var x = ev.clientX - dragObj.parent.offset().left;
				if (x < 0) {
					x = 0;
				} else if (x > pw) {
					x = pw;
				}
				dragObj.position(x);
			} else {
				var ph = dragObj.parent.height();
				var y = ev.clientY - dragObj.parent.offset().top;
				if (y < 0) {
					y = 0;
				} else if (y > ph) {
					y = ph;
				}
				dragObj.position(y);
			}
			if (dragObj.paneResized()) {
				dragObj.paneResized()(ev);
			}
			for (var i=0; i<splitters.length; i++) {
				var pane = splitters[i];
				if (pane != current) {
					doResize(pane, ev);
				}
			}
		});
		
		$(window).resize(function(ev){
			for (var i=0; i<splitters.length; i++) {
				doResize(splitters[i], ev);
			}
		});
		function setCurrent(c) {
			current = c;
		}
		function add(splitter) {
			splitters.push(splitter);
		}
		function release(splitter) {
			var idx = -1;
			for (var i=0; i<splitters.length; i++) {
				if (splitters[i] == splitter) {
					idx = i;
					break;
				}
			}
			if (idx >= 0) {
				splitters.splice(idx, 1);
			}
		}
		$.extend(this, {
			"setCurrent" : setCurrent,
			"add" : add,
			"release" : release
		});
	}
	/**
	 * divをリサイズするSplitter
	 * @param parrent 親要素
	 * @param div1 親要素内で上(または左)に位置する要素
	 * @param div1 親要素内で下(または右)に位置する要素
	 * @param horizontal trueの場合横分割、falseの場合は縦分割
	 */
	function Splitter(parent, div1, div2, horizontal) {
		if (manager == null) {
			manager = new SplitterManager();
		}
		manager.add(this);
		var self = this,
			keepLeft = true,
			limit = 0,
			paneResized = null,
			windowResized = null,
			parent = $(parent),
			div1 = $(div1),
			div2 = $(div2),
			resizebar = $("<div class='splitter-resizebar'></div>").appendTo(parent);
		
		resizebar.css({
			"background-color" : "#a9a9a9",
			"position" : "absolute",
			"z-index" : 20,
			"overflow" : "hidden"
		}).mouseenter(function() {
			manager.setCurrent(self);
		}).mouseleave(function() {
			manager.setCurrent(null);
		});
		if (horizontal) {
			var w1 = div1.outerWidth(true);
			var w2 = parent.width() - w1;
			div1.css({
				"position" : "absolute",
				"overflow-x" : "auto",
				"left" : 0,
				"width" : w1
			});
			resizebar.css({
				"left" : w1,
				"width" : 4,
				"height" : "100%",
				"cursor" : "col-resize"
			});
			var w3 = resizebar.outerWidth(true);
			div2.css({
				"position" : "absolute",
				"overflow-x" : "auto",
				"right" : 0,
				"width" : w2 - w3
			});
		} else {
			var h1 = div1.outerHeight(true);
			var h2 = div2.outerHeight(true);
			div1.css({
				"position" : "absolute",
				"overflow-y" : "auto",
				"top" : 0,
				"height" : h1
			});
			resizebar.css({
				"top" : h1,
				"width" : "100%",
				"height" : 4,
				"cursor" : "row-resize"
			});
			var h3 = resizebar.outerHeight(true);
			div2.css({
				"position" : "absolute",
				"overflow-y" : "auto",
				"bottom" : 0,
				"height" : h2 - h3
			});
		}
		$.extend(this, {
			"parent" : parent,
			"div1" : div1,
			"div2" : div2,
			"resizebar" : resizebar,
			"orientation" : function() {
				var cursor = self.resizebar.css("cursor").toLowerCase();
				return cursor == "col-resize" ? "horizontal" : "vertical";
			},
			"paneResized" : function(func) {
				if (func === undefined) {
					return paneResized;
				} else {
					paneResized = func;
				}
			},
			"windowResized" : function(func) {
				if (func === undefined) {
					return windowResized;
				} else {
					windowResized = func;
				}
			},
			"keepLeft" : function(b) {
				if (b === undefined) {
					return keepLeft;
				} else {
					keepLeft = b;
				}
			},
			"limit" : function(n) {
				if (n === undefined) {
					return limit;
				} else {
					limit = n;
				}
			},
			"position" : function(n) {
				if (self.orientation() == "horizontal") {
					if (n === undefined) {
						return resizebar.css("left");
					} else {
						var low = limit;
						var high = parent.width() - limit;
						if (n < low) {
							n = low;
						} else if (n > high) {
							n = high;
						}
						resizebar.css("left", n);
						div1.css("width", n);
						div2.css("width", parent.width() - resizebar.outerWidth(true) - n);
					}
				} else {
					if (n === undefined) {
						return resizebar.css("top");
					} else {
						var low = limit;
						var high = parent.height() - limit;
						if (n < low) {
							n = low;
						} else if (n > high) {
							n = high;
						}
						resizebar.css("top", n);
						div1.css("height", n);
						div2.css("height", parent.height() - resizebar.outerHeight(true) - n);
					}
				}
			},
			"release" : function() {
				manager.release(self);
			}
		});
	}
	/**
	 * divをリサイズするSplitter
	 * @param parrent 親要素
	 * @param div1 親要素内で上(または左)に位置する要素
	 * @param div1 親要素内で下(または右)に位置する要素
	 * @param horizontal trueの場合横分割、falseの場合は縦分割
	 * @param keepLeft trueの場合div1のサイズをキープ、falseの場合はdiv2のサイズをキープ
	 */
	function FixedDiv(parent, div1, div2, horizontal, keepLeft) {
		if (manager == null) {
			manager = new SplitterManager();
		}
		manager.add(this);
		var self = this,
			parent = $(parent),
			div1 = $(div1),
			div2 = $(div2),
			windowResized = null;
		
		function parentWidth() {
			if (parent.get(0) == document.body) {
				return document.documentElement.clientWidth;
			} else {
				return parent.innerWidth();
			}
		}
		function parentHeight() {
			if (parent.get(0) == document.body) {
				return document.documentElement.clientHeight;
			} else {
				return parent.innerHeight();
			}
		}
		function position(n) {
			if (horizontal) {
				if (n === undefined) {
					return div1.width();
				} else {
					var w1, w2;
					if (keepLeft) {
						w1 = div1.width();
						w2 = parentWidth() - w1;
						
						div1.css({
							"position" : "absolute",
							"top" : 0,
							"left" : 0,
							"height" : "100%",
							"width" : w1
						});
						div2.css({
							"position" : "absolute",
							"top" : 0,
							"left" : w1,
							"height" : "100%",
							"width" : w2
						});
					} else {
						w2 = div2.width();
						w1 = parentWidth() - w2;
						
						div1.css({
							"position" : "absolute",
							"top" : 0,
							"left" : 0,
							"height" : "100%",
							"width" : w1
						});
						div2.css({
							"position" : "absolute",
							"top" : 0,
							"right" : 0,
							"height" : "100%",
							"width" : w2
						});
					}
				}
			} else {
				if (n === undefined) {
					return div1.height();
				} else {
					var h1, h2;
					if (keepLeft) {
						h1 = div1.height();
						h2 = parentHeight() - h1;
						
						div1.css({
							"position" : "absolute",
							"top" : 0,
							"left" : 0,
							"width" : "100%",
							"height" : h1
						});
						div2.css({
							"position" : "absolute",
							"top" : h1,
							"left" : 0,
							"width" : "100%",
							"height" : h2
						});
					} else {
						h2 = div2.height();
						h1 = parentHeight() - h2;
						
						div1.css({
							"position" : "absolute",
							"top" : 0,
							"left" : 0,
							"width" : "100%",
							"height" : h1
						});
						div2.css({
							"position" : "absolute",
							"bottom" : 0,
							"left" : 0,
							"width" : "100%",
							"height" : h2
						});
					}
				}
			}
		}
		if (horizontal) {
			position(div1.width());
		} else {
			position(div1.height());
		}
		$.extend(this, {
			"orientation" : function() {
				return horizontal ? "horizontal" : "vertical";
			},
			"keepLeft" : function() {
				return keepLeft;
			},
			"windowResized" : function(func) {
				if (func === undefined) {
					return windowResized;
				} else {
					windowResized = func;
				}
			},
			"position" : position,
			"release" : function() {
				manager.release(self);
			}
		});
	}
	var manager = null;
	
	$.fn.splitter = function(options) {
		var div1 = this.children().first();
		var div2 = div1.next();
		var settings = {
			"div1" : div1,
			"div2" : div2,
			"orientation" : "horizontal",
			"limit" : 0,
			"keepLeft" : true,
			"paneResized" : null,
			"windowResized" : null
		};
		if (options) {
			$.extend(settings, options);
		}
		var splitter = new Splitter(this, settings.div1, settings.div2, settings.orientation == "horizontal");
		splitter.limit(settings.limit);
		splitter.keepLeft(settings.keepLeft);
		splitter.paneResized(settings.paneResized);
		splitter.windowResized(settings.windowResized);
		return splitter;
	}
	$.fn.fixedDiv = function(options) {
		var div1 = this.children().first();
		var div2 = div1.next();
		var settings = {
			"div1" : div1,
			"div2" : div2,
			"orientation" : "horizontal",
			"keepLeft" : true,
			"windowResized" : null
		};
		if (options) {
			$.extend(settings, options);
		}
		var fixedDiv = new FixedDiv(this, settings.div1, settings.div2, settings.orientation == "horizontal", settings.keepLeft);
		fixedDiv.windowResized(settings.windowResized);
		return fixedDiv;
	}
})(jQuery);
