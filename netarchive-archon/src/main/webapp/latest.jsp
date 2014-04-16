<%     
   List<ArcVO> latest1000Arcs = storage.getLatest1000Arcs();
   int totalNumberOfArcFiles = storage.getArcCount();
%>

<Strong>TotalNumber of Arc-files: <%=totalNumberOfArcFiles%>  </Strong>
<br>
Latest 1000 arcs added:<br>

  <table class="table table-striped table-condensed table-hover">
   <thead>
   <tr>
    <th>ArcID</th>
    <th>ShardId</th>
    <th>State</th>
    <th>Priority</th>
    <th>createdTime</th>
    <th>modifiedTime</th>
   </tr>
   </thead>
   <tbody>
<% for ( ArcVO current : latest1000Arcs ){%>
   <tr>
      <td><%=current.getFileName()%></td>
      <td><%=current.getShardId()%></td>
      <td><%=current.getArcState()%></td>
      <td><%=current.getPriority()%></td>
      <td><%=current.getCreatedTime()%></td>
      <td><%=current.getModifiedTime()%></td>
  </tr>
<%}%>
   </tbody>
</table>
