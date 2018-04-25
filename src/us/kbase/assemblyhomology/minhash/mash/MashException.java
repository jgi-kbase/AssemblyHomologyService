package us.kbase.assemblyhomology.minhash.mash;

import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;

@SuppressWarnings("serial")
public class MashException extends MinHashException {

	//TODO JAVADOC
	//TODO TEST
	
	public MashException(final String message) {
		super(message);
	}
	
	public MashException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
