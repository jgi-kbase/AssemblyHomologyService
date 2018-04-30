package us.kbase.assemblyhomology.storage;

import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;

public interface AssemblyHomologyStorage {

	//TODO JAVADOC
	
	/*
	void loadSequenceMetadata(NamespaceID namespaceID, List<SequenceMetadata> seqmeta);
	
	List<SequenceMetadata> getSequenceMetadata(NamespaceID namespaceID, List<String> sequenceIDs);
	*/
	
	void createOrReplaceNamespace(Namespace namespace) throws AssemblyHomologyStorageException;
	
	Namespace getNamespace(NamespaceID namespace)
			throws AssemblyHomologyStorageException, NoSuchNamespaceException;
	
	// deletes namespace and all data associated with that namespace's current loadID
	// calling this method during a load using the current loadID may lead to a namespace with
	// incomplete data
	void deleteNamespace(NamespaceID namespace);
	
}
