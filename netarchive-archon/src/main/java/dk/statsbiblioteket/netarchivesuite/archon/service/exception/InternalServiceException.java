package dk.statsbiblioteket.netarchivesuite.archon.service.exception;


public class InternalServiceException extends ArchonServiceException {

	   private static final long serialVersionUID = 1L;
	
	   public InternalServiceException() {
	        super();
	    }

	    public  InternalServiceException(String message) {
	        super(message);
	    }

	    public  InternalServiceException(String message, Throwable cause) {
	        super(message, cause);
	    }

	    public  InternalServiceException(Throwable cause) {
	        super(cause);
	    }
	
	
}


