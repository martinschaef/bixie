<?xml version="1.0" encoding="US-ASCII" ?>
<%@ page language="java" contentType="text/html; charset=US-ASCII"
	pageEncoding="US-ASCII"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://www.joogie.org/jsp/jstl/functions" prefix="funcs"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head profile="http://www.w3.org/2005/10/profile">
<link rel="icon" 
      type="image/ico" 
      href="img/favicon.ico">

<meta charset='utf-8'>
<meta http-equiv="X-UA-Compatible" content="chrome=1">
	<meta name="description"
		content="Bixie : Try it online">
		<title>Bixie : Try it online</title>

		<link rel="stylesheet" type="text/css" media="screen"
			href="css/stylesheet.css">
		<link rel="stylesheet" href="lib/codemirror/lib/codemirror.css" />
		<script src="lib/codemirror/lib/codemirror.js"></script>
		<script src="lib/codemirror/mode/clike/clike.js"></script>

		<link rel="stylesheet" href="lib/codemirror/addon/lint/lint.css">
		<style>
.CodeMirror {
	border: 2px inset #dee;
}

.line-error {
    background: #FBC2C4 !important;
    color: #8a1f11 !important;
}

div.center iframe{
    display: block;
    margin-left: auto;
    margin-right: auto;
}

.noframetable {
	border: none; 
	border-collapse: collapse; 
	width:100%
}
</style>
</head>
<body>

	<!-- HEADER -->
	<div id="header_wrap" class="outer">
		<header class="inner"> <a id="forkme_banner"
				href="https://github.com/martinschaef/bixie">View on GitHub</a>
	
			<h1 id="project_title">Try it online</h1>
	
			<section id="downloads"> <a class="zip_download_link"
				href="https://github.com/martinschaef/bixie/releases/download/v1.0/bixie.jar.zip">Download
				the .zip file</a> 
			</section> 
		</header>
	</div>

	<!-- MAIN CONTENT -->
	<div id="main_content_wrap" class="outer">
		<section id="main_content" class="inner">


		<script type="text/javascript">
		var editor = {};
		var example_idx =0;
		<c:if test="${null != requestScope.exampleIdx}">
			example_idx = ${requestScope.exampleIdx};
		</c:if>
		
		var examples = new Array();
				
		<c:if test="${null != requestScope.examples}">
			<c:forEach items="${requestScope.examples}" var="entry">
				examples.push("${entry}");
			</c:forEach>
		</c:if>
		
		function resetGutters() {
			editor.clearGutter("CodeMirror-lint-markers");
		}
		
		function loadnext(reload) {
			editor.clearHistory();
			resetGutters();
			example_idx=example_idx+1;
			if (example_idx>=examples.length) {
				example_idx =0;
			}
			document.getElementById('examplecounter').value = example_idx;
			editor.setValue(examples[example_idx]);
		 }

		
		
		function submit_form(e){
			document.getElementById('submitbutton').innerHTML = '<b>Loading</b>';
			document.getElementById('submitbutton').onclick = "";
			document.getElementById('loadnext').onclick = "";
			document.getElementById('form').submit();
		}
		
		</script>	
		<div id="codeblock" >			
			<div id="prompt">
				<img id="promptimg" alt="Write some Java code here!"
					src="img/prompt.png" />
			</div>

			<form id="form" action="bixie" method="post">
				<c:choose>
					<c:when test="${'POST' == pageContext.request.method}">
						<c:set var="code" value="${param.code}" />
					</c:when>
					<c:otherwise>
												
					</c:otherwise>
				</c:choose>
				<textarea id="code" name="code"><c:out value="${code}" /></textarea>
				
				<input type='hidden' id='examplecounter' name='examplecounter' value='0' />
				
				<p>
					<a href="javascript:void(0)" class="button" id="submitbutton"
						onclick="javascript:submit_form(this);">Ask <b>Bixie</b>!
					</a> &nbsp; <a href="javascript:void(0)" onclick="loadnext();" class="button" id="loadnext">Load Example</a>
				</p>
			</form>
		</div>

		<p class="inner">
			Enter some Java code or browse through our examples. Depending on your Internet connection,
			Bixie may take a few seconds to deliver results. Bixie results will be displayed as yellow
			warnings on the side of the code, compiler errors are red crosses. Bixie is only executed if your
			code has no compiler errors.
		</p>
		
	</section>
	
</div>

	<div id="footer_wrap" class="footer">
		<p >
			Bixie is maintained by <a href="https://github.com/martinschaef" target="_blank">martinschaef</a>
		</p>
	</div>

	<script type="text/javascript">
            var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
            document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
          </script>
	<script type="text/javascript">
            try {
              var pageTracker = _gat._getTracker("UA-20025374-5");
            pageTracker._trackPageview();
            } catch(err) {}
    </script>


	<script>
	
		editor = CodeMirror.fromTextArea(document.getElementById("code"), {
			lineNumbers : true,
			indentUnit: 4, 
			tabSize: 4,
			matchBrackets : true,
			mode : "text/x-java",
			gutters: ["CodeMirror-linenumbers", "CodeMirror-lint-markers"]
		});
		
		<c:if test="${'POST' != pageContext.request.method}">
		editor.setValue(examples[example_idx]);
		</c:if>
		
		function makeParserError(line, msg) {
			// var info = cm.lineInfo(line);	
			var severity = "error";
			var tip = document.createElement("div");
			tip.className = "CodeMirror-lint-message-" + severity;
			tip.appendChild(document.createTextNode(msg));
			editor.setGutterMarker(line, "CodeMirror-lint-markers", makeMarker(tip, severity,
					false, true));
		}

		function makeBixieWarning(line, msg) {
			// var info = cm.lineInfo(line);	
			var severity = "warning";
			var tip = document.createElement("div");			
			tip.className = "CodeMirror-lint-message-" + severity;
			tip.appendChild(document.createTextNode(msg));
			editor.addLineClass(line, 'background', 'line-error');
			editor.setGutterMarker(line, "CodeMirror-lint-markers", makeMarker(tip, severity,
					false, true));
		}

	  var GUTTER_ID = "CodeMirror-lint-markers";

	  function showTooltip(e, content) {
	    var tt = document.createElement("div");
	    tt.className = "CodeMirror-lint-tooltip";
	    tt.appendChild(content.cloneNode(true));
	    document.body.appendChild(tt);

	    function position(e) {
	      if (!tt.parentNode) return CodeMirror.off(document, "mousemove", position);
	      tt.style.top = Math.max(0, e.clientY - tt.offsetHeight - 5) + "px";
	      tt.style.left = (e.clientX + 5) + "px";
	    }
	    CodeMirror.on(document, "mousemove", position);
	    position(e);
	    if (tt.style.opacity != null) tt.style.opacity = 1;
	    return tt;
	  }
	  function rm(elt) {
	    if (elt.parentNode) elt.parentNode.removeChild(elt);
	  }
	  function hideTooltip(tt) {
	    if (!tt.parentNode) return;
	    if (tt.style.opacity == null) rm(tt);
	    tt.style.opacity = 0;
	    setTimeout(function() { rm(tt); }, 600);
	  }

	  function showTooltipFor(e, content, node) {
	    var tooltip = showTooltip(e, content);
	    function hide() {
	      CodeMirror.off(node, "mouseout", hide);
	      if (tooltip) { hideTooltip(tooltip); tooltip = null; }
	    }
	    var poll = setInterval(function() {
	      if (tooltip) for (var n = node;; n = n.parentNode) {
	        if (n == document.body) return;
	        if (!n) { hide(); break; }
	      }
	      if (!tooltip) return clearInterval(poll);
	    }, 400);
	    CodeMirror.on(node, "mouseout", hide);
	  }

	  function LintState(cm, options, hasGutter) {
	    this.marked = [];
	    this.options = options;
	    this.timeout = null;
	    this.hasGutter = hasGutter;
	    this.onMouseOver = function(e) { onMouseOver(cm, e); };
	  }

	  function parseOptions(cm, options) {
	    if (options instanceof Function) return {getAnnotations: options};
	    if (!options || options === true) options = {};
	    if (!options.getAnnotations) options.getAnnotations = cm.getHelper(CodeMirror.Pos(0, 0), "lint");
	    if (!options.getAnnotations) throw new Error("Required option 'getAnnotations' missing (lint addon)");
	    return options;
	  }

	  function clearMarks(cm) {
	    var state = cm.state.lint;
	    if (state.hasGutter) cm.clearGutter(GUTTER_ID);
	    for (var i = 0; i < state.marked.length; ++i)
	      state.marked[i].clear();
	    state.marked.length = 0;
	  }

	  function makeMarker(labels, severity, multiple, tooltips) {
	    var marker = document.createElement("div"), inner = marker;
	    marker.className = "CodeMirror-lint-marker-" + severity;
	    if (multiple) {
	      inner = marker.appendChild(document.createElement("div"));
	      inner.className = "CodeMirror-lint-marker-multiple";
	    }

	    if (tooltips != false) CodeMirror.on(inner, "mouseover", function(e) {
	      showTooltipFor(e, labels, inner);
	    });

	    return marker;
	  }

	  function getMaxSeverity(a, b) {
	    if (a == "error") return a;
	    else return b;
	  }

	  function groupByLine(annotations) {
	    var lines = [];
	    for (var i = 0; i < annotations.length; ++i) {
	      var ann = annotations[i], line = ann.from.line;
	      (lines[line] || (lines[line] = [])).push(ann);
	    }
	    return lines;
	  }

	  function annotationTooltip(ann) {
	    var severity = ann.severity;
	    if (!severity) severity = "error";
	    var tip = document.createElement("div");
	    tip.className = "CodeMirror-lint-message-" + severity;
	    tip.appendChild(document.createTextNode(ann.message));
	    return tip;
	  }

	  function startLinting(cm) {
	    var state = cm.state.lint, options = state.options;
	    if (options.async)
	      options.getAnnotations(cm, updateLinting, options);
	    else
	      updateLinting(cm, options.getAnnotations(cm.getValue(), options.options));
	  }

	  function updateLinting(cm, annotationsNotSorted) {
	    clearMarks(cm);
	    var state = cm.state.lint, options = state.options;

	    var annotations = groupByLine(annotationsNotSorted);

	    for (var line = 0; line < annotations.length; ++line) {
	      var anns = annotations[line];
	      if (!anns) continue;

	      var maxSeverity = null;
	      var tipLabel = state.hasGutter && document.createDocumentFragment();

	      for (var i = 0; i < anns.length; ++i) {
	        var ann = anns[i];
	        var severity = ann.severity;
	        if (!severity) severity = "error";
	        maxSeverity = getMaxSeverity(maxSeverity, severity);

	        if (options.formatAnnotation) ann = options.formatAnnotation(ann);
	        if (state.hasGutter) tipLabel.appendChild(annotationTooltip(ann));

	        if (ann.to) state.marked.push(cm.markText(ann.from, ann.to, {
	          className: "CodeMirror-lint-mark-" + severity,
	          __annotation: ann
	        }));
	      }

	      if (state.hasGutter)
	        cm.setGutterMarker(line, GUTTER_ID, makeMarker(tipLabel, maxSeverity, anns.length > 1,
	                                                       state.options.tooltips));
	    }
	    if (options.onUpdateLinting) options.onUpdateLinting(annotationsNotSorted, annotations, cm);
	  }

	  function onChange(cm) {
	    var state = cm.state.lint;
	    clearTimeout(state.timeout);
	    state.timeout = setTimeout(function(){startLinting(cm);}, state.options.delay || 500);
	  }

	  function popupSpanTooltip(ann, e) {
	    var target = e.target || e.srcElement;
	    showTooltipFor(e, annotationTooltip(ann), target);
	  }

	  // When the mouseover fires, the cursor might not actually be over
	  // the character itself yet. These pairs of x,y offsets are used to
	  // probe a few nearby points when no suitable marked range is found.
	  var nearby = [0, 0, 0, 5, 0, -5, 5, 0, -5, 0];

	  function onMouseOver(cm, e) {
	    if (!/\bCodeMirror-lint-mark-/.test((e.target || e.srcElement).className)) return;
	    for (var i = 0; i < nearby.length; i += 2) {
	      var spans = cm.findMarksAt(cm.coordsChar({left: e.clientX + nearby[i],
	                                                top: e.clientY + nearby[i + 1]}, "client"));
	      for (var j = 0; j < spans.length; ++j) {
	        var span = spans[j], ann = span.__annotation;
	        if (ann) return popupSpanTooltip(ann, e);
	      }
	    }
	  }

	  CodeMirror.defineOption("lint", false, function(cm, val, old) {
	    if (old && old != CodeMirror.Init) {
	      clearMarks(cm);
	      cm.off("change", onChange);
	      CodeMirror.off(cm.getWrapperElement(), "mouseover", cm.state.lint.onMouseOver);
	      delete cm.state.lint;
	    }

	    if (val) {
	      var gutters = cm.getOption("gutters"), hasLintGutter = false;
	      for (var i = 0; i < gutters.length; ++i) if (gutters[i] == GUTTER_ID) hasLintGutter = true;
	      var state = cm.state.lint = new LintState(cm, parseOptions(cm, val), hasLintGutter);
	      cm.on("change", onChange);
	      if (state.options.tooltips != false)
	        CodeMirror.on(cm.getWrapperElement(), "mouseover", state.onMouseOver);

	      startLinting(cm);
	    }
	  });
	</script>

	
		<c:if test="${null != requestScope.parsererror}">
			<c:forEach items="${requestScope.parsererror}" var="entry">
				<script type="text/javascript">
						makeParserError(${entry.key}-1, " ${entry.value}" );
					</script>
			</c:forEach>
		</c:if>
	
		<c:if test="${null != requestScope.report}">
			<c:forEach items="${requestScope.report}" var="entry">
				<script type="text/javascript">
					makeBixieWarning(${entry.key}-1, " ${entry.value}" );
				</script>
			</c:forEach>
		</c:if>

</body>
</html>
