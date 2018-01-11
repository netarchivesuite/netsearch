package dk.statsbiblioteket.netarchivesuite.archon.service.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ArchonPausedServiceExceptionMapper implements ExceptionMapper<ArchonPausedServiceException> {
    private static final Response.Status responseStatus =  Response.Status.SERVICE_UNAVAILABLE;    //503

    @Override
    public Response toResponse(ArchonPausedServiceException exc) {

        return (exc.getMessage() != null)
                ? Response.status(responseStatus).entity(exc.getMessage()).type("text/plain").build()
                : Response.status(responseStatus).build();
    }
}
