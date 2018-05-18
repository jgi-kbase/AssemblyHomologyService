package us.kbase.assemblyhomology.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.isNullOrEmpty;

import java.time.Instant;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;

public class Namespace {
	
	//TODO JAVADOC
	//TODO TEST
	
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

	public NamespaceID getId() {
		return id;
	}

	public MinHashSketchDatabase getSketchDatabase() {
		return sketchDatabase;
	}

	public LoadID getLoadID() {
		return loadID;
	}

	public DataSourceID getSourceID() {
		return dataSourceID;
	}

	public Instant getCreation() {
		return creation;
	}

	public String getSourceDatabaseID() {
		return sourceDatabaseID;
	}

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

	@Override
	public String toString() {
		StringBuilder builder2 = new StringBuilder();
		builder2.append("Namespace [id=");
		builder2.append(id);
		builder2.append(", sketchDatabase=");
		builder2.append(sketchDatabase);
		builder2.append(", loadID=");
		builder2.append(loadID);
		builder2.append(", dataSourceID=");
		builder2.append(dataSourceID);
		builder2.append(", creation=");
		builder2.append(creation);
		builder2.append(", sourceDatabaseID=");
		builder2.append(sourceDatabaseID);
		builder2.append(", description=");
		builder2.append(description);
		builder2.append("]");
		return builder2.toString();
	}

	public static Builder getBuilder(
			final NamespaceID id,
			final MinHashSketchDatabase sketchDatabase,
			final LoadID loadID,
			final Instant creation) {
		return new Builder(id, sketchDatabase, loadID, creation);
	}
	
	public static class Builder {
		
		private final String DEFAULT = "default";
		
		private final NamespaceID id;
		private final MinHashSketchDatabase sketchDatabase;
		private final LoadID loadID;
		private DataSourceID dataSourceID;
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
			try {
				this.dataSourceID = new DataSourceID("KBase");
			} catch (IllegalParameterException | MissingParameterException e) {
				throw new RuntimeException("Well this is unexpected.", e);
			}
		}
		
		public Builder withNullableDataSourceID(final DataSourceID dataSourceID) {
			if (dataSourceID != null) {
				this.dataSourceID = dataSourceID;
			}
			return this;
		}
		
		// default = "default"
		public Builder withNullableSourceDatabaseID(final String sourceDatabaseID) {
			if (isNullOrEmpty(sourceDatabaseID)) {
				this.sourceDatabaseID = DEFAULT;
			} else {
				this.sourceDatabaseID = sourceDatabaseID;
			}
			return this;
		}
		
		public Builder withNullableDescription(final String description) {
			if (isNullOrEmpty(description)) {
				this.description = null;
			} else {
				this.description = description;
			}
			return this;
		}
		
		public Namespace build() {
			return new Namespace(id, sketchDatabase, loadID, dataSourceID, creation,
					sourceDatabaseID, description);
		}
	}
}
