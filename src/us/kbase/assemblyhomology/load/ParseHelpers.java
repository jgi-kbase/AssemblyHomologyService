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

public class ParseHelpers {
	
	//TODO TEST
	//TODO JAVADOC

	public static Map<String, Object> fromYAML(final InputStream input, final String sourceInfo)
			throws LoadInputParseException {
		checkNotNull(input, "input");
		return fromYaml(i -> fromYaml((InputStream) i), input, sourceInfo);
	}
	
	public static Map<String, Object> fromYAML(final String input, final String sourceInfo)
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

	private static <R> Map<String, Object> fromYaml(
			final Function<Object, R> yamlProcessor,
			final R input,
			final String sourceInfo)
			throws LoadInputParseException {
		exceptOnEmpty(sourceInfo, "sourceInfo");
		final Object predata;
		try {
			predata = yamlProcessor.apply(input);
		} catch (Exception e) {
			// wtf snakeyaml authors, not using checked exceptions is bad enough, but not
			// documenting any exceptions and overriding toString so you can't tell what
			// exception is being thrown is something else
			throw new LoadInputParseException(String.format("Error parsing source %s: %s %s",
					sourceInfo, e.getClass(), e.getMessage()), e);
		}
		if (!(predata instanceof Map)) {
			throw new LoadInputParseException(
					"Expected mapping in top level YAML in " + sourceInfo);
		}
		@SuppressWarnings("unchecked")
		final Map<String, Object> data = (Map<String, Object>) predata;
		return data;
	}
	
	public static String getString(
			final Map<?, Object> map,
			final Object key,
			final String sourceInfo,
			final boolean optional)
			throws LoadInputParseException {
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
