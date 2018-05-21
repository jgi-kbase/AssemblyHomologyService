package us.kbase.assemblyhomology.core;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;

/** An ID for a particular data load. The maximum size is 256 Unicode code points.
 * @author gaprice@lbl.gov
 *
 */
public class LoadID extends Name {

	/** Create a new load ID.
	 * @param loadID the ID.
	 * @throws MissingParameterException if the id is null or the empty string.
	 * @throws IllegalParameterException if the id is too long or if the name contains
	 * control characters.
	 */
	public LoadID(final String loadID)
			throws MissingParameterException, IllegalParameterException {
		super(loadID, "loadID", 256);
	}
}
