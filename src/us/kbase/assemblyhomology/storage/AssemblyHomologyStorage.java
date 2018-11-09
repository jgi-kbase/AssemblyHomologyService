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

/** An interface for a storage system for assembly homology data.
 * 
 * Sequence metadata belongs to a namespace and a particular load within that namespace.
 * 
 * To load data:
 * - Generate a load id for the load (or reuse an old load id if you want to overwrite/append).
 * - With that load id, load the sequences with saveMeta
 *   - This does not affect the current load since it has a different load ID.
 * - Create or update the namespace, which switches the visible sequences to the new load.
 * 
 * The sequences with the old load id should (depending on the implementation) be removed from
 * the system after enough time to allow currently active searches to complete.
 * 
 * @author gaprice@lbl.gov
 *
 */
public interface AssemblyHomologyStorage {

	/** Save sequence metadata.
	 * @param namespaceID the namespace to which the metadata belongs.
	 * @param loadID the load ID to which the metadata belongs.
	 * @param seqmeta the sequence metadata.
	 * @throws AssemblyHomologyStorageException if the load fails.
	 */
	void saveSequenceMetadata(
			NamespaceID namespaceID,
			LoadID loadID,
			Collection<SequenceMetadata> seqmeta)
			throws AssemblyHomologyStorageException;
	
	/** Get sequence metadata from a namespace and the currently active load ID for that
	 * namespace.
	 * @param namespaceID the namespace containing the sequence metadata of interest.
	 * @param sequenceIDs the ids of the sequence metadata.
	 * @return the sequence metadata.
	 * @throws AssemblyHomologyStorageException if getting the sequence metadata from the
	 * storage system fails.
	 * @throws NoSuchSequenceException if the requested sequence id(s) does not exist within the
	 * namespace and its current load ID.
	 * @throws NoSuchNamespaceException if there is no namespace with the given ID.
	 */
	List<SequenceMetadata> getSequenceMetadata(NamespaceID namespaceID, List<String> sequenceIDs)
			throws AssemblyHomologyStorageException, NoSuchSequenceException,
				NoSuchNamespaceException;

	/** Get sequence metadata from a namespace and a load ID for that
	 * namespace.
	 * @param namespaceID the namespace containing the sequence metadata of interest.
	 * @param loadID the load ID containing the sequence metadata of interest.
	 * @param sequenceIDs the ids of the sequence metadata.
	 * @return the sequence metadata.
	 * @throws AssemblyHomologyStorageException if getting the sequence metadata from the
	 * storage system fails.
	 * @throws NoSuchSequenceException if the requested sequence id(s) does not exist within the
	 * namespace and its current load ID.
	 */
	List<SequenceMetadata> getSequenceMetadata(
			NamespaceID namespaceID,
			LoadID loadID,
			List<String> sequenceIDs)
			throws AssemblyHomologyStorageException, NoSuchSequenceException,
				NoSuchNamespaceException;
	
	// may need more specific methods later, like incrementing the seq count
	/** Create a namespace or replace the namespace if a namespace with the same ID already
	 * exists in the storage system.
	 * @param namespace the namespace to persist.
	 * @throws AssemblyHomologyStorageException if saving the namespace failed.
	 */
	void createOrReplaceNamespace(Namespace namespace) throws AssemblyHomologyStorageException;
	
	/** Get all the namespaces in the storage system.
	 * @return the namespaces.
	 * @throws AssemblyHomologyStorageException if getting the namespaces failed.
	 */
	Set<Namespace> getNamespaces() throws AssemblyHomologyStorageException;
	
	/** Get a specific namespace.
	 * @param namespaceID the ID of the namespace.
	 * @return the namespace.
	 * @throws AssemblyHomologyStorageException if getting the namespace fails.
	 * @throws NoSuchNamespaceException if there is no namespace with the given ID.
	 */
	Namespace getNamespace(NamespaceID namespaceID)
			throws AssemblyHomologyStorageException, NoSuchNamespaceException;
	
	/** Deletes a namespace and all data associated data, regardless of load ID.
	 * Calling this method during a load using the current loadID may lead to a namespace with
	 * incomplete data
	 * 
	 * @param namespaceID the namespaceID of the namespace to delete.
	 */
	void deleteNamespace(NamespaceID namespaceID);
	
}
