package us.kbase.assemblyhomology.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;

/** An ID for a namespace. Valid IDs contain upper and lower case ASCII letters and numbers and
 * the underscore and are no longer than 256 characters.
 * @author gaprice@lbl.gov
 *
 */
public class NamespaceID extends Name {

	private static final String INVALID_CHARS_REGEX = "[^A-Za-z\\d_]+";
	private final static Pattern INVALID_CHARS = Pattern.compile(INVALID_CHARS_REGEX);
	
	/** Create a new namespace ID. 
	 * @param namespaceID the ID.
	 * @throws MissingParameterException if the id is null or the empty string.
	 * @throws IllegalParameterException if the id is too long or if the name contains
	 * illegal characters.
	 */
	public NamespaceID(final String namespaceID)
			throws MissingParameterException, IllegalParameterException {
		super(namespaceID, "namespaceID", 256);
		final Matcher m = INVALID_CHARS.matcher(super.getName());
		if (m.find()) {
			throw new IllegalParameterException(String.format(
					"Illegal character in namespace id %s: %s", super.getName(), m.group()));
		}
	}
}
