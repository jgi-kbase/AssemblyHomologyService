package us.kbase.assemblyhomology.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.minhash.MinHashDistanceCollector;

/** An ID for a Minhash distance collector. Valid IDs contain up to 20 lower case ASCII letters.
 * @see MinHashDistanceCollector
 * @author gaprice@lbl.gov
 *
 */
public class CollectorID extends Name {

	private static final String INVALID_CHARS_REGEX = "[^a-z]+";
	private final static Pattern INVALID_CHARS = Pattern.compile(INVALID_CHARS_REGEX);
	
	/** Create a new collector ID. 
	 * @param collectorID the ID.
	 * @throws MissingParameterException if the id is null or the empty string.
	 * @throws IllegalParameterException if the id is too long or if the name contains
	 * illegal characters.
	 */
	public CollectorID(final String collectorID)
			throws MissingParameterException, IllegalParameterException {
		super(collectorID, "collectorID", 20);
		final Matcher m = INVALID_CHARS.matcher(super.getName());
		if (m.find()) {
			throw new IllegalParameterException(String.format(
					"Illegal character in collector id %s: %s", super.getName(), m.group()));
		}
	}
}
