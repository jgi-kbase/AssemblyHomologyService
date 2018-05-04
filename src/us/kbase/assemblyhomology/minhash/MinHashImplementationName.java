package us.kbase.assemblyhomology.minhash;

import us.kbase.assemblyhomology.core.Name;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;

public class MinHashImplementationName extends Name {

	//TODO JAVADOC
	//TODO TEST
	
	public MinHashImplementationName(final String id)
			throws MissingParameterException, IllegalParameterException {
		super(id, "minhash implementation name", 256);
	}
	
}
