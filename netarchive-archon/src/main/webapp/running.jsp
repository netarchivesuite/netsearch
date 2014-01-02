All Running ARCs:

<%     
   List<ArcVO> allRunningArcs = storage.getAllRunningArcs();
%>

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
