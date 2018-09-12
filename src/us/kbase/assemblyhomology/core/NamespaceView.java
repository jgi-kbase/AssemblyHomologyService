package us.kbase.assemblyhomology.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;

/** Presents a view of a namespace integrating data from multiple sources aimed towards a user
 * of a {@link AssemblyHomology} instance. Omits data that is not useful to a general user.
 * @author gaprice@lbl.gov
 *
 */
public class NamespaceView {

	//TODO NOW JAVADOC
	
	private final Optional<String> authsource;
	private final NamespaceID namespaceID;
	private final Optional<String> description;
	private final DataSourceID sourceID;
	private final Instant modification;
	private final String sourceDatabaseID;
	private final MinHashImplementationName implementationName;
	private final MinHashParameters parameterSet;
	private final int sequenceCount;

	/** Create the view.
	 * @param namespace the namespace that will be part of the view.
	 */
	public NamespaceView(final Namespace namespace) {
		checkNotNull(namespace, "namespace");
		this.authsource = Optional.absent();
		this.namespaceID = namespace.getID();
		this.description = namespace.getDescription();
		this.sourceID = namespace.getSourceID();
		this.modification = namespace.getModification();
		this.sourceDatabaseID = namespace.getSourceDatabaseID();
		this.implementationName = namespace.getSketchDatabase().getImplementationName();
		this.parameterSet = namespace.getSketchDatabase().getParameterSet();
		this.sequenceCount = namespace.getSketchDatabase().getSequenceCount();
	}
	
	/** Create the view.
	 * @param namespace the namespace that will be part of the view.
	 * @param filter the filter associated with the namespace.
	 */
	public NamespaceView(final Namespace namespace, final MinHashDistanceFilterFactory filter) {
		checkNotNull(namespace, "namespace");
		checkNotNull(filter, "filter");
		if (!namespace.getFilterID().isPresent() ||
				!namespace.getFilterID().get().equals(filter.getID())) {
			throw new IllegalArgumentException(
					"The namespace filter ID and the filter's ID do not match");
		}
		this.authsource = filter.getAuthSource();
		this.namespaceID = namespace.getID();
		this.description = namespace.getDescription();
		this.sourceID = namespace.getSourceID();
		this.modification = namespace.getModification();
		this.sourceDatabaseID = namespace.getSourceDatabaseID();
		this.implementationName = namespace.getSketchDatabase().getImplementationName();
		this.parameterSet = namespace.getSketchDatabase().getParameterSet();
		this.sequenceCount = namespace.getSketchDatabase().getSequenceCount();
	}

	/** Get the authorization source (e.g. KBase, JGI, etc.) associated with the namespace. This
	 * source will be used to filter sequence data.
	 * @return the authorization source.
	 */
	public Optional<String> getAuthsource() {
		return authsource;
	}

	/** Get the ID of the namespace.
	 * @return the ID.
	 */
	public NamespaceID getNamespaceID() {
		return namespaceID;
	}

	/** Get a free-text description of the namespace, if any.
	 * @return the description, or absent() if none.
	 */
	public Optional<String> getDescription() {
		return description;
	}

	/** Get the ID of the source of the data.
	 * @return the source ID.
	 */
	public DataSourceID getSourceID() {
		return sourceID;
	}

	/** Get the last modification date of the namespace.
	 * @return the modification date.
	 */
	public Instant getModification() {
		return modification;
	}

	/** Get the ID of the sequence database at the source. 
	 * @return the source database ID.
	 */
	public String getSourceDatabaseID() {
		return sourceDatabaseID;
	}

	/** Get the implementation name of the Minhash implementation associated with the namespace.
	 * @return the implementation name.
	 */
	public MinHashImplementationName getImplementationName() {
		return implementationName;
	}

	/** Get the set of parameters used to create the sketch database associated with the namespace.
	 * @return the parameter set.
	 */
	public MinHashParameters getParameterSet() {
		return parameterSet;
	}

	/** Get the number of sequences in the sketch database associated with the namespace.
	 * @return the sequence count.
	 */
	public int getSequenceCount() {
		return sequenceCount;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authsource == null) ? 0 : authsource.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((implementationName == null) ? 0 : implementationName.hashCode());
		result = prime * result + ((modification == null) ? 0 : modification.hashCode());
		result = prime * result + ((namespaceID == null) ? 0 : namespaceID.hashCode());
		result = prime * result + ((parameterSet == null) ? 0 : parameterSet.hashCode());
		result = prime * result + sequenceCount;
		result = prime * result + ((sourceDatabaseID == null) ? 0 : sourceDatabaseID.hashCode());
		result = prime * result + ((sourceID == null) ? 0 : sourceID.hashCode());
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
		NamespaceView other = (NamespaceView) obj;
		if (authsource == null) {
			if (other.authsource != null) {
				return false;
			}
		} else if (!authsource.equals(other.authsource)) {
			return false;
		}
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
		if (implementationName == null) {
			if (other.implementationName != null) {
				return false;
			}
		} else if (!implementationName.equals(other.implementationName)) {
			return false;
		}
		if (modification == null) {
			if (other.modification != null) {
				return false;
			}
		} else if (!modification.equals(other.modification)) {
			return false;
		}
		if (namespaceID == null) {
			if (other.namespaceID != null) {
				return false;
			}
		} else if (!namespaceID.equals(other.namespaceID)) {
			return false;
		}
		if (parameterSet == null) {
			if (other.parameterSet != null) {
				return false;
			}
		} else if (!parameterSet.equals(other.parameterSet)) {
			return false;
		}
		if (sequenceCount != other.sequenceCount) {
			return false;
		}
		if (sourceDatabaseID == null) {
			if (other.sourceDatabaseID != null) {
				return false;
			}
		} else if (!sourceDatabaseID.equals(other.sourceDatabaseID)) {
			return false;
		}
		if (sourceID == null) {
			if (other.sourceID != null) {
				return false;
			}
		} else if (!sourceID.equals(other.sourceID)) {
			return false;
		}
		return true;
	}
}
