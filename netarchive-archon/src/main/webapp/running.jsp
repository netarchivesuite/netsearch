
<%
String paused = request.getParameter("paused");

if (paused != null){ //Update status 
  boolean isPaused = Boolean.valueOf(paused);
  ArchonFacade.setPaused(isPaused);
}
boolean isPaused = ArchonFacade.isPaused(); 

%>



<script>
    function stop(){
    	var newloc = location.origin+location.pathname+'?paused=true&tab=1';    	
    	location.replace(newloc);
    }
    
    function start(){
    	var newloc = location.origin+location.pathname+'?paused=false&tab=1';    	
    	location.replace(newloc);
    }
        
</script>
<% if (isPaused){%>
  <h1>Archon is ON HALT! Press start to resume...</h1>
<%} else { %>
  <h1>Archon is ACTIVE. Press stop to halt all new jobs...</h1>
<%}%>



 
<% if (isPaused){%>
  <button type="button" class="btn btn-success " onclick="javascript: start();">Start</button>
<%} else { %>
  <button type="button" class="btn btn-danger " onclick="javascript: stop();">Stop</button>
<%}%>
<br>



<%     
   List<ArcVO> allRunningArcs = ArchonFacade.getAllRunningArcs();
%>

All Running ARCs(<%=allRunningArcs.size()%> currently running)



  <table class="table table-striped table-condensed table-hover">
   <thead>
   <tr>
    <th>ArcID</th>
    <th>ShardId</th>
    <th>State</th>
    <th>Priority</th>
    <th>createdTime</th>
    <th>modifiedTime</th>
    <th>Running for</th>
   </tr>
   </thead>
   <tbody>
<% for ( ArcVO current : allRunningArcs ){%>
   <tr>
      <td><%=current.getFileName()%></td>
      <td><%=current.getShardId()%></td>
      <td><%=current.getArcState()%></td>
      <td><%=current.getPriority()%></td>
      <td><%=current.getCreatedTime()%></td>
      <td><%=current.getModifiedTime()%></td>
      <td><%=current.calculateTimeSinceLastModified()%></td>   
  </tr>
<%}%>
   </tbody>
</table>
