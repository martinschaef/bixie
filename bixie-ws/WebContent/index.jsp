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
	
			<section id="downloads"> <a class="zip_download_link"
				href="https://github.com/martinschaef/bixie/releases/download/v1.0/bixie.jar.zip">Download
				the .zip file</a> 
			</section> 
		</header>
	</div>

	<!-- MAIN CONTENT -->
	<div id="main_content_wrap" class="outer">
		<section id="main_content" class="inner">

	<p>
		Bixie is a static checker that detects <b>inconsistencies</b> in Java code. Inconsistent Code
		is code that only occurs on paths that contain inconsistent assumptions. That is, it is either unreachable
		or any of its executions must lead to an uncaught exception.
		While inconsistencies are not automatically bugs, they have a bad smell as they represent code that 
		cannot be executed safely. The Java compiler, for example, treats certain instances of inconsistent code 
		as errors, like the use of uninitialized variables or inevitable null-pointer dereferences. 
		Bixie uses deductive verification to find inconsistencies that the Java compiler missed. 
	</p>

	<div >
		<div id="prompt">
		<a href="./bixie" target="_blank">
		<img id="screenshot" alt="Write some Java code here!"
						src="img/screen.png" />
		</a>
		</div>
		<br/>
		<p> 
			The easiest way to understand what Bixie does is to
			try it <a href="./bixie" target="_blank">online</a>. Click on the picture
			on the right and browse through some examples of inconsistent Java code
			or type your own program and check it.
		</p>
		
	</div>
	<h3>Demo Video</h3>
	<div id="thevideo" class="center">
	<iframe width="420" height="315" src="//www.youtube.com/embed/1_M35R1wUm4" frameborder="0" allowfullscreen></iframe>	
	<br/>
	<hr/>
	</div>
	
	<h3>Inconsistent Code found by Bixie</h3>
		<p>
		We keep running Bixie on open-source projects and report our findings. 
		In order to avoid spamming developers, we inspect each warning manually to make sure that it is relevant.
		We only create pull requests if the warning does not involve code that is generated, 
		deliberately unreachable (e.g., a debug constant disables it), or the code has a comment that 
		says it is supposed to be unreachable. 
		<br/>
		Here are some instances of inconsistent code that were found and fixed by Bixie in popular projects: 
		</p>
		<ul>
		<li>Apache Cassandra: <a href="https://github.com/apache/cassandra/pull/46" target="_blank">see pull request</a></li>
		<li>Apache Hive: <a href="https://github.com/apache/hive/pull/23" target="_blank">see pull request</a></li>
		<li>Apache jMeter: <a href="https://github.com/apache/jmeter/pull/10" target="_blank">see pull request</a></li>
		<li>Apache Maven: <a href="https://github.com/apache/maven/pull/30" target="_blank">see pull request</a></li>
		<li>Apache Tomcat: <a href="https://github.com/apache/tomcat/pull/13" target="_blank">see pull request</a></li>
		<li>Bouncy Castle: <a href="https://github.com/bcgit/bc-java/pull/87" target="_blank">see pull request</a></li>
		<li> Soot: see pull request <a href="https://github.com/Sable/soot/pull/244" target="_blank"> 1</a> and 
		<a href="https://github.com/Sable/soot/pull/261" target="_blank">2</a> and 
		<a href="https://github.com/Sable/soot/pull/260" target="_blank">3</a> </li>
		</ul>
	During the development of Bixie we also found a case where the Java compiler generates
	unreachable bytecode (see <a href="http://stackoverflow.com/questions/25615417/try-with-resources-introduce-unreachable-bytecode" target="_blank">here</a>).

	<h3>Download and Usage</h3>
	<p>Before you start, check your Java version. 
	You need at least  <a href="http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html" target="_blank">JDK 7</a> 
	to run Bixie. Please check:
	</p>
	<pre>java -version</pre>
	<p>If the result is not <code>1.7.0</code> or higher please update your Java version. 
	If you just want to play with Bixie, use our <a href="./bixie" target="_blank">web tester</a>.
	</p> 
	
	<p> 
	Bixie uses <a href="http://www.sable.mcgill.ca/soot/" target="_blank">Soot</a> to parse Java (byte)code. There is a big difference between Soot 2.5.0 and the latest version of Soot.
	Hence, we provide two versions of Bixie, depending on if you plan to analyze source code or bytecode:
	<ul> 
	<li><b>bixie_latestSoot.jar</b> which uses the latest version of Soot. Runs stable on bytecode and jar files but Soot may throw exceptions on source files. This version is used for our experiments.</li>
	<li><b>bixie_soot2.5.jar</b> which uses Soot 2.5.0. Runs well on source code, but can be unstable on bytecode. This version is used for the web tester.</li>	
	</ul>
	Download the <a href="https://github.com/martinschaef/bixie/releases">latest release</a> from GitHub and unzip it. Pick one of the Bixie jar files depending on if you want to analyze source code or bytecode. 
	In the following, we use <b>bixie_soot2.5.jar</b> for demonstration, but <b>bixie_latestSoot.jar</b> can be used with the exact same parameters if you have class files.
	</p>	

    <pre>java -jar bixie_soot2.5.jar -j [input] -cp [classpath] -o [output] </pre>
    <p>
	Where <code>[input]</code> is either a (debug compiled) Jar file, or the root folder of your class or source files.
	</p>
    <p>
	For <code>[classpath]</code> use the classpath that you would also use to run the code 
	that you passed as <code>[input]</code>. If no special classpath is required, use the same
	value as <code>[input]</code>. 
	</p>

    <p>
	For <code>[out]</code> use the name of the text file where Bixie should write its report to. 
	</p>
	
	<p>
	For larger programs we highly recommend to start Bixie with lots of resources. In our experiments, we use the
	following setup (which requires a 64bit JDK):
	</p>
	<pre>java -Xmx2g -Xms2g -Xss4m -jar bixie.jar ... </pre>
	<p>For 32bit installations of Java use -Xmx1g -Xms1g -Xss4m.</p> 
	
	<h5>Example</h5>
	<p>
	To check if everything is working properly, we test Bixie on Demo.java that comes with the download. Now go to that folder and run:</p>
    <pre>java -jar bixie_soot2.5.jar -j ./ -cp ./ -o report.txt </pre>	 
	<p>Your result.txt file should look somewhat like this:</p>
	<pre>In file: ./Demo.java
 		   Inconsistency detected between the following lines:
				4(else-block), 7
	</pre>
	
	<h3>Previous Tools and Papers</h3>
	<p>
	Bixie is the successor of our  <a href="http://www.joogie.org" target="_blank">Joogie</a> tool. 
	While Joogie was already doing a good job in detecting inconsistent code in Java bytecode, it produced 
	a substantial amount of false alarms because not all inconsistencies in the bytecode are
	also inconsistencies in Java code. In Bixie, we use a novel technique to translate bytecode into logic
	that allows our checker to suppress almost all false alarms. At the same time, we improved
	handling of loops and library functions to further increase the detection rate.
	For more details on the new features of Bixie, check our 
	<a href="https://github.com/martinschaef/jar2bpl/wiki" target="_blank">wiki</a>.
	</p>
	<p>
	The papers below describe how the actual checking for inconsistent code is implemented in Bixie:</p>
	<ul> 
	<li><a href="http://iist.unu.edu/publication/theory-control-flow-graph-exploration" target="_blank">A Theory for Control-Flow Graph Exploration</a>,  S. Arlt, P. R&uuml;mmer, M. Sch&auml;f, ATVA 2013 </li>
	<li><a href="http://www.informatik.uni-freiburg.de/~schaef/joogie.pdf" target="_blank">Joogie: Infeasible Code Detection for Java</a>,  S. Arlt, M. Sch&auml;f, CAV 2012 </li>
	<li><a href="http://cs.nyu.edu/~wies/publ/doomed_program_points.pdf" target="_blank">It's doomed; we can prove it</a>,  J. Hoenicke, R. Leino, A. Podelski, M. Sch&auml;f, T. Wies, FM 2009 </li>
	</ul>
	<p>
	Bixie also includes an interpolation-based fault localization to explain inconsistent code (see our papers below). Previous tools on inconsistent code 
	could only detect single statements by proving that they do not occur on feasible paths. Bixie is the first tool that computes actual error messages for inconsistent 
	code.
	<ul> 
	<li><a href="http://iist.unu.edu/publication/explaining-inconsistent-code" target="_blank">Explaining Inconsistent Code</a>,  M. Sch&auml;f, D. Schwartz-Narbonne, T. Wies, FSE 2013 </li>
	<li><a href="http://iist.unu.edu/publication/flow-sensitive-fault-localization" target="_blank">Flow-sensitive Fault Localization</a>,   J. Christ, E. Ermis, M. Sch&auml;f, T. Wies, VMCAI 2013 </li>
	<li><a href="http://iist.unu.edu/publication/error-invariants" target="_blank">Error Invariants</a>,  E. Ermis, M. Sch&auml;f, T. Wies, FM 2012 </li>
	</ul>
	</p>
	<p>
	Bixie uses the following components:
	<ul> 
	<li><a href="https://github.com/martinschaef/gravy" target="_blank">GraVy</a>,  A tool to compute feasible path covers for Boogie programs. </li>
	<li><a href="https://github.com/martinschaef/jar2bpl" target="_blank">Jar2Bpl</a>,   A tool to translate Java Bytecode into Boogie based on Soot. </li>
	</ul>
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


</body>
</html>
