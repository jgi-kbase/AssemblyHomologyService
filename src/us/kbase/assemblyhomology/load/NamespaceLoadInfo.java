package us.kbase.assemblyhomology.load;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.load.ParseHelpers.getString;
import static us.kbase.assemblyhomology.load.ParseHelpers.fromYAML;

import java.io.InputStream;
import java.time.Instant;
import java.util.Map;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.core.DataSourceID;
import us.kbase.assemblyhomology.core.FilterID;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.load.exceptions.LoadInputParseException;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;

/** Represents load information for a namespace instantiated from an YAML or JSON input.
 * 
 * Example input:
 * 
 * <pre>
 * id: mynamespace
 * datasource: KBase
 * sourcedatabase: CI Refdata
 * description: some reference data
 * filterid: myfilter
 * <pre>
 * 
 * @author gaprice@lbl.gov
 *
 */
public class NamespaceLoadInfo {
	
	private final NamespaceID id;
	private final DataSourceID dataSourceID;
	private final Optional<String> sourceDatabaseID;
	private final Optional<String> description;
	private final Optional<FilterID> filterID;

	/** Generate load information for a namespace.
	 * @param input the input to parse.
	 * @param sourceInfo information about the source, often a file name.
	 * @throws LoadInputParseException if the input could not be parsed.
	 */
	public NamespaceLoadInfo(final InputStream input, final String sourceInfo)
			throws LoadInputParseException {
		final Object predata = fromYAML(input, sourceInfo);
		if (!(predata instanceof Map)) {
			throw new LoadInputParseException(
					"Expected mapping at / in " + sourceInfo);
		}
		@SuppressWarnings("unchecked")
		final Map<String, Object> data = (Map<String, Object>) predata;
		id = getID(data, "id", sourceInfo);
		dataSourceID = getDataSourceID(data, "datasource", sourceInfo);
		filterID = getFilterID(data, "filterid", sourceInfo);
		sourceDatabaseID = Optional.fromNullable(
				getString(data, "sourcedatabase", sourceInfo, true));
		description = Optional.fromNullable(getString(data, "description", sourceInfo, true));
	}

	/** Get the namespace ID.
	 * @return the namespace ID.
	 */
	public NamespaceID getId() {
		return id;
	}

	/** Get the ID of the data source from whence the data associated with this namespace came.
	 * @return the data source ID.
	 */
	public DataSourceID getDataSourceID() {
		return dataSourceID;
	}

	/** Get the ID of the database within the data source from whence the data came.
	 * @return the source database ID, or absent if absent.
	 */
	public Optional<String> getSourceDatabaseID() {
		return sourceDatabaseID;
	}

	/** Get the description of the namespace.
	 * @return the description, or absent if absent.
	 */
	public Optional<String> getDescription() {
		return description;
	}
	
	/** Get the ID of the filter to be used with the namespace.
	 * @return the filter ID, or absent if absent.
	 */
	public Optional<FilterID> getFilterID() {
		return filterID;
	}

	private NamespaceID getID(
			final Map<String, Object> data,
			final String key,
			final String sourceInfo)
			throws LoadInputParseException {
		final String nsid = getString(data, key, sourceInfo, false);
		try {
			return new NamespaceID(nsid);
		} catch (IllegalParameterException e) {
			throw new LoadInputParseException("Illegal namespace ID: " + nsid, e);
		} catch (MissingParameterException e) {
			throw new RuntimeException("this should be impossible", e);
		}
	}
	
	private Optional<FilterID> getFilterID(
			final Map<String, Object> data,
			final String key,
			final String sourceInfo)
			throws LoadInputParseException {
		final String fid = getString(data, key, sourceInfo, true);
		if (fid == null) {
			return Optional.absent();
		}
		try {
			return Optional.of(new FilterID(fid));
		} catch (IllegalParameterException e) {
			throw new LoadInputParseException("Illegal filter ID: " + fid, e);
		} catch (MissingParameterException e) {
			throw new RuntimeException("this should be impossible", e);
		}
	}
	
	private DataSourceID getDataSourceID(
			final Map<String, Object> data,
			final String key,
			final String sourceInfo)
			throws LoadInputParseException {
		final String dsid = getString(data, key, sourceInfo, false);
		try {
			return new DataSourceID(dsid);
		} catch (IllegalParameterException e) {
			throw new LoadInputParseException("Illegal data source ID: " + dsid, e);
		} catch (MissingParameterException e) {
			throw new RuntimeException("this should be impossible", e);
		}
	}
	
	/** Create a namespace from the load info.
	 * @param sketchDB the sketch database associated with the namespace.
	 * @param loadID the load ID for the load associated with the namespace.
	 * @param creation the time the namespace was created.
	 * @return the new namespace.
	 */
	public Namespace toNamespace(
			final MinHashSketchDatabase sketchDB,
			final LoadID loadID,
			final Instant creation) {
		checkNotNull(sketchDB, "sketchDB");
		checkNotNull(loadID, "loadID");
		checkNotNull(creation, "creation");
		return Namespace.getBuilder(id, sketchDB, loadID, creation)
				.withNullableDataSourceID(dataSourceID)
				.withNullableSourceDatabaseID(sourceDatabaseID.orNull())
				.withNullableDescription(description.orNull())
				.withNullableFilterID(filterID.orNull())
				.build();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataSourceID == null) ? 0 : dataSourceID.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((filterID == null) ? 0 : filterID.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((sourceDatabaseID == null) ? 0 : sourceDatabaseID.hashCode());
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
		NamespaceLoadInfo other = (NamespaceLoadInfo) obj;
		if (dataSourceID == null) {
			if (other.dataSourceID != null) {
				return false;
			}
		} else if (!dataSourceID.equals(other.dataSourceID)) {
			return false;
		}
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
		if (filterID == null) {
			if (other.filterID != null) {
				return false;
			}
		} else if (!filterID.equals(other.filterID)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (sourceDatabaseID == null) {
			if (other.sourceDatabaseID != null) {
				return false;
			}
		} else if (!sourceDatabaseID.equals(other.sourceDatabaseID)) {
			return false;
		}
		return true;
	}
}
