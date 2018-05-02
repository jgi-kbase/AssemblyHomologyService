package us.kbase.assemblyhomology.load.exceptions;

@SuppressWarnings("serial")
public class LoadInputParseException extends Exception {

	//TODO TEST
	//TODO JAVADOC
	
	public LoadInputParseException(final String message) {
		super(message);
	}
	
	public LoadInputParseException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
