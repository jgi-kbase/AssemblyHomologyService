package us.kbase.assemblyhomology.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.isNullOrEmpty;

import java.time.Instant;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;

/** A namespace containing a MinHash sketch database. A namespace contains the sketch database
 * and information about the source of the database. A namespace also contains a load ID that
 * separates subsequent data loads into the same namespace from one another and allows for
 * instantaneous switching from one load to another.
 * 
 * @author gaprice@lbl.gov
 *
 */
public class Namespace {
	
	private final NamespaceID id;
	private final MinHashSketchDatabase sketchDatabase;
	private final LoadID loadID;
	private final DataSourceID dataSourceID;
	private final Instant creation;
	private final String sourceDatabaseID;
	private final Optional<String> description;

	private Namespace(
			final NamespaceID id,
			final MinHashSketchDatabase sketchDatabase,
			final LoadID loadID,
			final DataSourceID dataSourceID,
			final Instant creation,
			final String sourceDatabaseID,
			final String description) {
		this.id = id;
		this.sketchDatabase = sketchDatabase;
		this.loadID = loadID;
		this.dataSourceID = dataSourceID;
		this.creation = creation;
		this.sourceDatabaseID = sourceDatabaseID;
		this.description = Optional.fromNullable(description);
	}

	/** Get the namespace ID.
	 * @return the ID.
	 */
	public NamespaceID getID() {
		return id;
	}

	/** Get the sketch database associated with the namespace.
	 * @return the sketch database.
	 */
	public MinHashSketchDatabase getSketchDatabase() {
		return sketchDatabase;
	}

	/** Get the current load ID for the namespace. Note that this value is often persisted in
	 * a database and therefore is stale as soon as it is retrieved.
	 * @return the load ID.
	 */
	public LoadID getLoadID() {
		return loadID;
	}

	/** Get the ID of the data's source - often an institution like JGI, EMBL, etc.
	 * @return the data source ID.
	 */
	public DataSourceID getSourceID() {
		return dataSourceID;
	}

	/** Get the time this namespace was created.
	 * @return the creation time.
	 */
	public Instant getCreation() {
		return creation;
	}

	/** Get the ID of the database within the data source where the data from which the
	 * sketch database was created originates.
	 * @return the source database ID.
	 */
	public String getSourceDatabaseID() {
		return sourceDatabaseID;
	}

	/** Get a description of the namespace and the data contained within it, if any.
	 * @return the description or absent.
	 */
	public Optional<String> getDescription() {
		return description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creation == null) ? 0 : creation.hashCode());
		result = prime * result + ((dataSourceID == null) ? 0 : dataSourceID.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((loadID == null) ? 0 : loadID.hashCode());
		result = prime * result + ((sketchDatabase == null) ? 0 : sketchDatabase.hashCode());
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
		Namespace other = (Namespace) obj;
		if (creation == null) {
			if (other.creation != null) {
				return false;
			}
		} else if (!creation.equals(other.creation)) {
			return false;
		}
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
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (loadID == null) {
			if (other.loadID != null) {
				return false;
			}
		} else if (!loadID.equals(other.loadID)) {
			return false;
		}
		if (sketchDatabase == null) {
			if (other.sketchDatabase != null) {
				return false;
			}
		} else if (!sketchDatabase.equals(other.sketchDatabase)) {
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

	/** Get a {@link Namespace} builder.
	 * @param id the ID of the namespace.
	 * @param sketchDatabase the sketch database associated with the namespace.
	 * @param loadID the load ID for the sketch database and associated data.
	 * @param creation the creation time of the namespace.
	 * @return a {@link Namespace} builder.
	 */
	public static Builder getBuilder(
			final NamespaceID id,
			final MinHashSketchDatabase sketchDatabase,
			final LoadID loadID,
			final Instant creation) {
		return new Builder(id, sketchDatabase, loadID, creation);
	}
	
	/** A {@link Namespace} builder.
	 * @author gaprice@lbl.gov
	 *
	 */
	public static class Builder {
		
		private static final String DEFAULT = "default";
		private static final DataSourceID DEFAULT_DS_ID;
		static {
			try {
				DEFAULT_DS_ID = new DataSourceID("KBase");
			} catch (IllegalParameterException | MissingParameterException e) {
				throw new RuntimeException("Well this is unexpected.", e);
			}
		}
		
		private final NamespaceID id;
		private final MinHashSketchDatabase sketchDatabase;
		private final LoadID loadID;
		private DataSourceID dataSourceID = DEFAULT_DS_ID;
		private Instant creation;
		private String sourceDatabaseID = DEFAULT;
		private String description = null;

		private Builder(
				final NamespaceID id,
				final MinHashSketchDatabase sketchDatabase,
				final LoadID loadID,
				final Instant creation) {
			checkNotNull(id, "id");
			checkNotNull(sketchDatabase, "sketchDatabase");
			checkNotNull(loadID, "loadID");
			checkNotNull(creation, "creation");
			if (!id.getName().equals(sketchDatabase.getName().getName())) {
				// code smell here. Think about this later.
				throw new IllegalArgumentException("Namespace ID must equal sketch DB ID");
			}
			this.id = id;
			this.sketchDatabase = sketchDatabase;
			this.loadID = loadID;
			this.creation = creation;
		}
		
		/** Add a data source ID. If the data source is null, the data source is reset to the
		 * default, "KBase".
		 * @param dataSourceID the ID of the source of the data.
		 * @return this builder.
		 */
		public Builder withNullableDataSourceID(final DataSourceID dataSourceID) {
			if (dataSourceID == null) {
				this.dataSourceID = DEFAULT_DS_ID;
			} else {
				this.dataSourceID = dataSourceID;
			}
			return this;
		}
		
		/** Add an ID for the database within the data source where the data originated. If null
		 * or whitespace, the ID is reset to the default, "default".
		 * @param sourceDatabaseID the ID of the source database.
		 * @return this builder.
		 */
		public Builder withNullableSourceDatabaseID(final String sourceDatabaseID) {
			if (isNullOrEmpty(sourceDatabaseID)) {
				this.sourceDatabaseID = DEFAULT;
			} else {
				this.sourceDatabaseID = sourceDatabaseID;
			}
			return this;
		}
		
		/** Add a description of the namespace. If null or whitespace, the description is set to
		 * null.
		 * @param description the namespace description.
		 * @return this builder.
		 */
		public Builder withNullableDescription(final String description) {
			if (isNullOrEmpty(description)) {
				this.description = null;
			} else {
				this.description = description;
			}
			return this;
		}
		
		/** Build the {@link Namespace}.
		 * @return the new {@link Namespace}.
		 */
		public Namespace build() {
			return new Namespace(id, sketchDatabase, loadID, dataSourceID, creation,
					sourceDatabaseID, description);
		}
	}
}
