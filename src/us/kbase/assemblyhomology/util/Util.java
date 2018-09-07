package us.kbase.assemblyhomology.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;


/** Miscellaneous utility methods.
 * @author gaprice@lbl.gov
 *
 */
public class Util {
	
	/** Throw an exception if the given string is null or whitespace only.
	 * @param s the string to test.
	 * @param name the name of the string to include in the exception.
	 * @throws IllegalArgumentException if the string is null or whitespace only.
	 */
	public static void exceptOnEmpty(final String s, final String name)
			throws IllegalArgumentException {
		if (isNullOrEmpty(s)) {
			throw new IllegalArgumentException(name + " cannot be null or whitespace only");
		}
	}

	/** Check if a string is null or whitespace only.
	 * @param s the string to test.
	 * @return true if the string is null or whitespace only, false otherwise.
	 */
	public static boolean isNullOrEmpty(final String s) {
		return s == null || s.trim().isEmpty();
	}
	
	/** Check that a string is non-null and has at least one non-whitespace character.
	 * @param s the string to check.
	 * @param name the name of the string to use in any error messages.
	 * @throws MissingParameterException if the string fails the check.
	 */
	public static void checkString(final String s, final String name)
			throws MissingParameterException {
		try {
			checkString(s, name, -1);
		} catch (IllegalParameterException e) {
			throw new RuntimeException("Programming error: " +
					e.getMessage(), e);
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
		if (isNullOrEmpty(s)) {
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
	
	/** Check that the provided collection is not null and contains no null elements.
	 * @param col the collection to test.
	 * @param name the name of the collection to use in any error messages.
	 */
	public static <T> void checkNoNullsInCollection(final Collection<T> col, final String name) {
		checkNotNull(col, name);
		for (final T item: col) {
			if (item == null) {
				throw new NullPointerException("Null item in collection " + name);
			}
		}
	}
	
	/** Check that the provided collection is not null and contains no null or whitespace-only
	 * strings.
	 * @param strings the collection to check.
	 * @param name the name of the collection to use in any error messages.
	 */
	public static void checkNoNullsOrEmpties(final Collection<String> strings, final String name) {
		checkNotNull(strings, name);
		for (final String s: strings) {
			if (isNullOrEmpty(s)) {
				throw new IllegalArgumentException(
						"Null or whitespace only string in collection " + name);
			}
		}
	}
}
