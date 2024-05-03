package us.kbase.assemblyhomology.load;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;
import static us.kbase.assemblyhomology.util.Util.isNullOrEmpty;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import us.kbase.assemblyhomology.load.exceptions.LoadInputParseException;

/** Utility methods for parsing and extracting data from YAML documents.
 * @author gaprice@lbl.gov
 *
 */
public class ParseHelpers {
	
	/** Parse a YAML document into a object. The object will consist of maps, lists,
	 * and primitives.
	 * @param input the input YAML document.
	 * @param sourceInfo information about the source of the document, typically a file name.
	 * @return the parsed YAML document.
	 * @throws LoadInputParseException if the input could not be parsed.
	 */
	public static Object fromYAML(final InputStream input, final String sourceInfo)
			throws LoadInputParseException {
		checkNotNull(input, "input");
		return fromYaml(i -> fromYaml((InputStream) i), input, sourceInfo);
	}
	
	/** Parse a YAML document into a object. The object will consist of maps, lists,
	 * and primitives.
	 * @param input the input YAML document.
	 * @param sourceInfo information about the source of the document, typically a file name.
	 * @return the parsed YAML document.
	 * @throws LoadInputParseException if the input could not be parsed.
	 */
	public static Object fromYAML(final String input, final String sourceInfo)
			throws LoadInputParseException {
		exceptOnEmpty(input, "input");
		return fromYaml(i -> fromYaml((String) i), input, sourceInfo);
	}
	
	private static Object fromYaml(final String source) {
		return new Yaml(new SafeConstructor()).load(source);
	}
	
	private static Object fromYaml(final InputStream source) {
		return new Yaml(new SafeConstructor()).load(source);
	}

	private static <R> Object fromYaml(
			final Function<Object, R> yamlProcessor,
			final R input,
			final String sourceInfo)
			throws LoadInputParseException {
		exceptOnEmpty(sourceInfo, "sourceInfo");
		try {
			return yamlProcessor.apply(input);
		} catch (Exception e) {
			// wtf snakeyaml authors, not using checked exceptions is bad enough, but not
			// documenting any exceptions and overriding toString so you can't tell what
			// exception is being thrown is something else
			throw new LoadInputParseException(String.format("Error parsing source %s: %s %s",
					sourceInfo, e.getClass(), e.getMessage()), e);
		}
	}
	
	/** Get a string value from a map.
	 * @param map the map from which to get the string.
	 * @param key the key of the value to get.
	 * @param sourceInfo information about the source of the map, usually a file name.
	 * @param optional if true, a null or whitespace only value is acceptable. If false, an
	 * exception will be thrown.
	 * @return the string.
	 * @throws LoadInputParseException if the value is not a string or is null or whitespace only
	 * with optional set to false.
	 */
	public static String getString(
			final Map<?, Object> map,
			final Object key,
			final String sourceInfo,
			final boolean optional)
			throws LoadInputParseException {
		checkNotNull(map, "map");
		final Object value = map.get(key);
		if (value == null) {
			if (optional) {
				return null;
			}
			throw new LoadInputParseException("Missing value at " + key + "." + fmt(sourceInfo));
		}
		if (!(value instanceof String)) {
			throw new LoadInputParseException(
					String.format("Expected string, got %s at %s.%s",
							value, key, fmt(sourceInfo)));
		}
		if (isNullOrEmpty((String) value)) {
			if (optional) {
				return null;
			}
			throw new LoadInputParseException("Missing value at " + key + "." + fmt(sourceInfo));
		}
		return (String) value;
	}
	
	private static String fmt(final String sourceInfo) {
		return sourceInfo == null ? "" :
			sourceInfo.trim().isEmpty() ? "" : " Source: " + sourceInfo;
	}
	
}
