package us.kbase.assemblyhomology.core;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;

public class NamespaceID extends Name {

	//TODO JAVADOC
	//TODO TEST
	
	//TODO NOW ascii letters and numerals only
	
	public NamespaceID(final String id)
			throws MissingParameterException, IllegalParameterException {
		super(id, "namespaceID", 256);
	}
}
