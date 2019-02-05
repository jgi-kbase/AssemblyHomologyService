package us.kbase.assemblyhomology.core;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;

/** An ID for a source of data. The maximum size is 256 Unicode code points.
 * @author gaprice@lbl.gov
 *
 */
public class DataSourceID extends Name {

	/** Create a new data source ID.
	 * @param dataSourceID the ID.
	 * @throws MissingParameterException if the id is null or the empty string.
	 * @throws IllegalParameterException if the id is too long or if the name contains
	 * control characters.
	 */
	public DataSourceID(final String dataSourceID)
			throws MissingParameterException, IllegalParameterException {
		super(dataSourceID, "dataSourceID", 256);
	}
}
