package us.kbase.assemblyhomology.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;

public class NamespaceID extends Name {

	//TODO JAVADOC
	//TODO TEST
	
	private static final String INVALID_CHARS_REGEX = "[^A-Za-z\\d_]+";
	private final static Pattern INVALID_CHARS = Pattern.compile(INVALID_CHARS_REGEX);
	
	public NamespaceID(final String id)
			throws MissingParameterException, IllegalParameterException {
		super(id, "namespaceID", 256);
		final Matcher m = INVALID_CHARS.matcher(id);
		if (m.find()) {
			throw new IllegalParameterException(String.format(
					"Illegal character in namespace id %s: %s", id, m.group()));
		}
	}
	
	public static void main(final String[] args) throws Exception {
		new NamespaceID("foo&bar");
	}
}
