package us.kbase.assemblyhomology.minhash;

import us.kbase.assemblyhomology.core.Name;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;

/** The name of a MinHash implementation, like mash or sourmash.
 * @author gaprice@lbl.gov
 *
 */
public class MinHashImplementationName extends Name {

	/** Create the name.
	 * @param id the name.
	 * @throws MissingParameterException if the name is null or whitespace only.
	 * @throws IllegalParameterException if the name is too long or contains control characters.
	 */
	public MinHashImplementationName(final String id)
			throws MissingParameterException, IllegalParameterException {
		super(id, "minhash implementation name", 256);
	}
	
}
