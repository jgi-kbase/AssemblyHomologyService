package us.kbase.assemblyhomology.minhash.exceptions;

@SuppressWarnings("serial")
public class MinHashException extends Exception {
	
	//TODO JAVADOC
	//TODO TEST
	
	public MinHashException(final String message) {
		super(message);
	}
	
	public MinHashException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
