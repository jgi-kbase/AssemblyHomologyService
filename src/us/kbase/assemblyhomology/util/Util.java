package us.kbase.assemblyhomology.util;


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
	
}
