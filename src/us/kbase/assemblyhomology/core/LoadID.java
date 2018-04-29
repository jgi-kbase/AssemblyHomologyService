package us.kbase.assemblyhomology.core;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;

public class LoadID extends Name {

	//TODO JAVADOC
	//TODO TEST
	
	public LoadID(final String id)
			throws MissingParameterException, IllegalParameterException {
		super(id, "loadID", 256);
	}
}
