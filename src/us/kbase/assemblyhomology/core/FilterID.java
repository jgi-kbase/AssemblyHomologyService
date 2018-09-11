package us.kbase.assemblyhomology.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.minhash.MinHashDistanceFilter;

/** An ID for a Minhash distance filter. Valid IDs contain up to 20 lower case ASCII letters.
 * @see MinHashDistanceFilter
 * @author gaprice@lbl.gov
 *
 */
public class FilterID extends Name {

	private static final String INVALID_CHARS_REGEX = "[^a-z]+";
	private final static Pattern INVALID_CHARS = Pattern.compile(INVALID_CHARS_REGEX);

	public static final FilterID DEFAULT;
	static {
		try {
			DEFAULT = new FilterID("default");
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("Programming error: " + e, e);
		}
	}
	
	/** Create a new filter ID. 
	 * @param filterID the ID.
	 * @throws MissingParameterException if the id is null or the empty string.
	 * @throws IllegalParameterException if the id is too long or if the name contains
	 * illegal characters.
	 */
	public FilterID(final String filterID)
			throws MissingParameterException, IllegalParameterException {
		super(filterID, "filter id", 20);
		final Matcher m = INVALID_CHARS.matcher(super.getName());
		if (m.find()) {
			throw new IllegalParameterException(String.format(
					"Illegal character in filter id %s: %s", super.getName(), m.group()));
		}
	}
}
