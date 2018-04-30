package us.kbase.assemblyhomology.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;

public class Util {
	
	//TODO JAVADOC
	//TODO TEST

	public static void exceptOnEmpty(final String s, final String name)
			throws IllegalArgumentException {
		if (isNullOrEmpty(s)) {
			throw new IllegalArgumentException(name);
		}
	}

	public static boolean isNullOrEmpty(final String s) {
		return s == null || s.trim().isEmpty();
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
	
	public static <T> void checkNoNullsInCollection(final Collection<T> col, final String name) {
		checkNotNull(col, name);
		for (final T item: col) {
			if (item == null) {
				throw new NullPointerException("Null item in collection " + name);
			}
		}
	}
	
	public static void checkNoNullsOrEmpties(final Collection<String> strings, final String name) {
		checkNotNull(strings, name);
		for (final String s: strings) {
			if (isNullOrEmpty(s)) {
				throw new IllegalArgumentException("Null or empty string in collection " + name);
			}
		}
	}
}
