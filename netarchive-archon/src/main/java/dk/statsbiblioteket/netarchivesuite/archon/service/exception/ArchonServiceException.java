package dk.statsbiblioteket.netarchivesuite.archon.service.exception;

public abstract class ArchonServiceException extends Exception{

	private static final long serialVersionUID = 1L;

	public  ArchonServiceException () {
	        super();
	    }

	    public  ArchonServiceException (String message) {
	        super(message);
	    }

	    public  ArchonServiceException (String message, Throwable cause) {
	        super(message, cause);
	    }

	    public  ArchonServiceException (Throwable cause) {
	        super(cause);
	    }
	
}
