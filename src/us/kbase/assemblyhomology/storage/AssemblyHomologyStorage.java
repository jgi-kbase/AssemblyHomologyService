package us.kbase.assemblyhomology.storage;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchSequenceException;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;

/* Loading:
 * * Generate a load id for the load (or reuse an old load id if you want to overwrite/append).
 * * With that load id, load the sequences with saveMeta
 *   * This does not affect the current load since it has a different load ID.
 * * Create or update the namespace, which switches the visible sequences to the new load.
 */

public interface AssemblyHomologyStorage {

	//TODO JAVADOC
	
	void saveSequenceMetadata(
			NamespaceID namespaceID,
			LoadID loadID,
			Collection<SequenceMetadata> seqmeta)
			throws AssemblyHomologyStorageException;
	
	// fetches load id from current namespace
	List<SequenceMetadata> getSequenceMetadata(NamespaceID namespaceID, List<String> sequenceIDs)
			throws AssemblyHomologyStorageException, NoSuchSequenceException,
				NoSuchNamespaceException;
	
	List<SequenceMetadata> getSequenceMetadata(
			NamespaceID namespaceID,
			LoadID loadID,
			List<String> sequenceIDs)
			throws AssemblyHomologyStorageException, NoSuchSequenceException,
				NoSuchNamespaceException;
	
	// may need more specific methods later, like incrementing the seq count
	void createOrReplaceNamespace(Namespace namespace) throws AssemblyHomologyStorageException;
	
	Set<Namespace> getNamespaces() throws AssemblyHomologyStorageException;
	
	Namespace getNamespace(NamespaceID namespace)
			throws AssemblyHomologyStorageException, NoSuchNamespaceException;
	
	// deletes namespace and all data associated with that namespace's current loadID
	// calling this method during a load using the current loadID may lead to a namespace with
	// incomplete data
	void deleteNamespace(NamespaceID namespace);
	
}
