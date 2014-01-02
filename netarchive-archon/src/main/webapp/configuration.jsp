<%@ page import="
    java.util.*,
    dk.statsbiblioteket.netarchivesuite.archon.persistence.*,
    dk.statsbiblioteket.netarchivesuite.archon.service.*,
    dk.statsbiblioteket.netarchivesuite.archon.*"%>

<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
    <title>Archon</title>
    <script type="text/javascript" src="js/jquery-1.8.3.js"></script>
    <script type="text/javascript" src="js/bootstrap.min.js"></script>
    <link href="css/bootstrap.min.css" rel="stylesheet" media="screen" />    
</head>
<body>

<%
   H2Storage storage = H2Storage.getInstance();   
%>

<script>
    function save(type){
        document.configurationForm.event.value=type;
        document.configurationForm.submit();
    }

</script>

<h1>Archon</h1>

<ul class="nav nav-tabs" id="configTab">
    <li class="active"><a href="#latest">Latest ARCs added</a></li>
    <li><a href="#running">All Running</a></li>
</ul>

<%@ include file="message.jsp" %>

<form name="configurationForm" class="well" action="configurationServlet" method="POST">
    <input type="hidden" name="event" />
    <input type="hidden" name="typeName" />

    <div class="tab-content">
        <div class="tab-pane active" id="latest">
            <%@ include file="latest.jsp" %>
        </div>
        <div class="tab-pane" id="running">
            <%@ include file="running.jsp" %>
        </div>       
    </div>
</form>

<script>
    $('#configTab a').click(function (e) {
        e.preventDefault();
        $(this).tab('show');
    })
</script>

<%
    //Show correct tab (by number 0,1,2,3,..)
    String tab = (String) request.getAttribute("tab");
    if (tab != null){%>
<script>
    $('#configTab li:eq(<%=tab%>) a').tab('show');
</script>
<%}%>

</body>
</html>