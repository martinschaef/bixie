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
		content="Bixie : Find inconsistencies in Java code">
		<title>Bixie : Find inconsistencies in Java code</title>
		<link rel="stylesheet" type="text/css" media="screen" href="css/stylesheet.css"/>

</head>
<body>

	<!-- HEADER -->
	<div id="header_wrap" class="outer">
		<header class="inner"> <a id="forkme_banner"
				href="https://github.com/martinschaef/bixie">View on GitHub</a>
	
			<h1 id="project_title">Find inconsistencies in Java code</h1>
	

		</header>
	</div>

	<!-- MAIN CONTENT -->
	<div id="main_content_wrap" class="outer">
		<section id="main_content" class="inner">

	<p>
		Bixie is a static checker that detects <b>inconsistencies</b> in Java code. Inconsistent Code
		is code that only occurs on paths that contain inconsistent assumptions. That is, it is either unreachable
		or any of its executions must lead to a runtime exception.
		While inconsistencies are not automatically bugs, they have a bad smell as they represent code that 
		cannot be executed safely. The Java compiler, for example, treats certain instances of inconsistent code 
		as errors, like the use of uninitialized variables or inevitable null-pointer dereferences. 
		Bixie uses deductive verification to find inconsistencies that the Java compiler missed. 
	</p>

	
</div>

	<div id="footer_wrap" class="footer">
		<p >
			Bixie is maintained by <a href="https://github.com/martinschaef" target="_blank">martinschaef</a>
		</p>
	</div>



</body>
</html>
