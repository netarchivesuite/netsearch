package dk.statsbiblioteket.netarchivesuite.archon.service.exception;


public class ArchonPausedServiceException extends ArchonServiceException {
	
	private static final long serialVersionUID = 1L;
	
	public ArchonPausedServiceException() {
	        super();
	    }

	    public  ArchonPausedServiceException(String message) {
	        super(message);
	    }

	    public  ArchonPausedServiceException(String message, Throwable cause) {
	        super(message, cause);
	    }

	    public  ArchonPausedServiceException(Throwable cause) {
	        super(cause);
	    }	
}

