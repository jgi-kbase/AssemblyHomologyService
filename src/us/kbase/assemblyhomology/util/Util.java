package us.kbase.assemblyhomology.util;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;

public class Util {
	
	//TODO JAVADOC
	//TODO TEST

	public static void exceptOnEmpty(
			final String s,
			final String name)
			throws IllegalArgumentException {
		if (s == null || s.trim().isEmpty()) {
			throw new IllegalArgumentException(name);
		}
	}
	
	
	/** Check that a string is non-null, has at least one non-whitespace character, and is below
	 * a specified length (not including surrounding whitespace).
	 * @param s the string to check.
	 * @param name the name of the string to use in any error messages.
	 * @param max the maximum number of code points in the string. If 0 or less, the length is not
	 * checked.
	 * @throws MissingParameterException if the string is null or contains only whitespace
	 * characters.
	 * @throws IllegalParameterException if the string is too long.
	 */
	public static void checkString(
			final String s,
			final String name,
			final int max)
			throws MissingParameterException, IllegalParameterException {
		if (s == null || s.trim().isEmpty()) {
			throw new MissingParameterException(name);
		}
		
		if (max > 0 && codePoints(s.trim()) > max) {
			throw new IllegalParameterException(
					name + " size greater than limit " + max);
		}
	}
	
	private static int codePoints(final String s) {
		return s.codePointCount(0, s.length());
	}
}
