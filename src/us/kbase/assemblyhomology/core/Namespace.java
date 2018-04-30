package us.kbase.assemblyhomology.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.isNullOrEmpty;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;

public class Namespace {
	
	//TODO JAVADOC
	//TODO TEST
	
	private final NamespaceID id;
	private final MinHashSketchDatabase sketchDatabase;
	private final LoadID loadID;
	private final DataSourceID dataSourceID;
	private final String sourceDatabaseID;
	private final Optional<String> description;

	private Namespace(
			final NamespaceID id,
			final MinHashSketchDatabase sketchDatabase,
			final LoadID loadID,
			final DataSourceID dataSourceID,
			final String sourceDatabaseID,
			final String description) {
		this.id = id;
		this.sketchDatabase = sketchDatabase;
		this.loadID = loadID;
		this.dataSourceID = dataSourceID;
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

	public String getSourceDatabaseID() {
		return sourceDatabaseID;
	}

	public Optional<String> getDescription() {
		return description;
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
			final DataSourceID dataSourceID) {
		return new Builder(id, sketchDatabase, loadID, dataSourceID);
	}
	
	public static class Builder {
		
		private final String DEFAULT = "default";
		
		private final NamespaceID id;
		private final MinHashSketchDatabase sketchDatabase;
		private final LoadID loadID;
		private final DataSourceID dataSourceID;
		private String sourceDatabaseID = DEFAULT;
		private String description = null;
		
		private Builder(
				final NamespaceID id,
				final MinHashSketchDatabase sketchDatabase,
				final LoadID loadID,
				final DataSourceID dataSourceID) {
			checkNotNull(id, "id");
			checkNotNull(sketchDatabase, "sketchDatabase");
			checkNotNull(loadID, "loadID");
			checkNotNull(dataSourceID, "dataSourceID");
			this.id = id;
			this.sketchDatabase = sketchDatabase;
			this.loadID = loadID;
			this.dataSourceID = dataSourceID;
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
			return new Namespace(id, sketchDatabase, loadID, dataSourceID, sourceDatabaseID,
					description);
		}
	}
}
