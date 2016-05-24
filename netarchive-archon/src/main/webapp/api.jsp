
<h1>API ARCHON REST resources. </h1>
<h2>Version:${pom.version}</h2>
<h2>Build time:${build.time}</h2>
<br>                                                       
<h2> SERVICE METHODS: </h2>
<br>


Remember to URL-encode slashes \ in file-path to %2F<br>
An ARC-file unique identifier is only the filename and not the full file-path.<br>

<table class="table" border="1">  
       <caption><strong>HTTP POST</strong></caption>
        <thead>  
          <tr>  
            <th>URL</th>  
            <th>Description</th>                
            <th>Output</th>
          </tr>  
        </thead>  
        <tbody>                                                 
          <tr>  
            <td>services/addARC/{arcPath}</td>  
            <td>
            Will add a new arc-fil to Archon. Will throw exception if the arc-id already exist<br>
            example: services/addARC/folder1%2Ffolder2%2Farcfile1.arc  
            </td>  
            <td>
             (none) 
            </td>
          </tr>                   
          <tr>  
            <td>services/addOrUpdateARC/{arcPath}</td>  
            <td>
            Will add a new file if it does not exist. If it exist the folder-path will be updated<br>
            example: services/addOrUpdateARC/folder1%2Ffolder2%2Farcfile1.arc  
            </td>  
            <td>
             (none) 
            </td>
          </tr>
             <tr>  
            <td>services/setARCPriority/{arcPath}/{priority}</td>  
            <td>
            Change the priority for the given arc-file<br>
            example: services/setARCPriority/folder1%2Ffolder2%2Farcfile1.arc/3  
            </td>  
            <td>
             (none) 
            </td>
          </tr>
             <tr>  
            <td>services/resetARCWithPriority/{arcPath}/{priority}</td>  
            <td>
            Reset state to NEW and shardid to null and set a priority
            example: services/resetARCWithPriority/folder1%2Ffolder2%2Farcfile1.arc/3  
            </td>  
            <td>
             (none) 
            </td>
          </tr>
          
        </tbody>  
</table>    

<br>
<table class="table" border="1">  
       <caption><strong>HTTP GET</strong></caption>
        <thead>  
          <tr>  
            <th>URL</th>  
            <th>Description</th>                
            <th>Output</th>
          </tr>  
        </thead>  
        <tbody>                     
            <tr>  
            <td>services/getShardIDs</td>  
            <td>
            List all shardIDs (completed and still runnning)<br>
            example: services/getShardIDs 
            </td>  
            <td>
             (none) 
            </td>
          </tr>                                                           
        </tbody>  
</table>    
        
        <br>


<table class="table" border="1">  
       <caption><strong>HTTP errors</strong></caption>
        <thead>  
          <tr>  
            <th>Error</th>  
            <th>Reason</th>                
          </tr>  
        </thead>  
        <tbody>  
          <tr>  
            <td>400 (Bad Request)</td>  
            <td>Caused by the input. Validation error etc.</td>    
          </tr>  
            <tr>  
            <td>500 (Internal Server Error)</td>  
            <td>Server side errors, nothing to do about it.</td>    
          </tr>
        </tbody>  
</table>    
