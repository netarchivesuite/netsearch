package dk.statsbiblioteket.netarchivesuite.archon.service.exception;


public class InvalidArgumentServiceException extends ArchonServiceException {
	
	private static final long serialVersionUID = 1L;
	
	public  InvalidArgumentServiceException() {
	        super();
	    }

	    public InvalidArgumentServiceException(String message) {
	        super(message);
	    }

	    public InvalidArgumentServiceException(String message, Throwable cause) {
	        super(message, cause);
	    }

	    public InvalidArgumentServiceException(Throwable cause) {
	        super(cause);
	    }
	
	
}

