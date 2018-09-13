package us.kbase.assemblyhomology.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import us.kbase.assemblyhomology.core.MinHashDistanceFilterFactory;

/** A configuration for a {@link MinHashDistanceFilterFactory}.
 * @author gaprice@lbl.gov
 *
 */
public class FilterConfiguration {

	private final String classname;
	private final Map<String, String> config;

	/** Create the filter configuration.
	 * @param classname the java class name of the filter factory.
	 * @param config the configuration of the filter factory in key / value pairs.
	 */
	public FilterConfiguration(final String classname, final Map<String, String> config) {
		exceptOnEmpty(classname, "classname");
		checkNotNull(config, "config");
		for (final String key: config.keySet()) {
			exceptOnEmpty(key, "config key");
			// null / whitespace values are ok.
		}
		this.classname = classname;
		this.config = Collections.unmodifiableMap(new HashMap<>(config));
	}

	/** Get the java class name of the filter factory.
	 * @return the class name.
	 */
	public String getFactoryClassName() {
		return classname;
	}

	/** Get the configuration of the filter factory.
	 * @return the configuration.
	 */
	public Map<String, String> getConfig() {
		return config;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((classname == null) ? 0 : classname.hashCode());
		result = prime * result + ((config == null) ? 0 : config.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FilterConfiguration other = (FilterConfiguration) obj;
		if (classname == null) {
			if (other.classname != null) {
				return false;
			}
		} else if (!classname.equals(other.classname)) {
			return false;
		}
		if (config == null) {
			if (other.config != null) {
				return false;
			}
		} else if (!config.equals(other.config)) {
			return false;
		}
		return true;
	}
}
