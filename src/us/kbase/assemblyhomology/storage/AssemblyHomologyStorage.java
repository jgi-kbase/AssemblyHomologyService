package us.kbase.assemblyhomology.storage;

import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;

public interface AssemblyHomologyStorage {

	//TODO JAVADOC
	
	/*
	void loadSequenceMetadata(NamespaceID namespaceID, List<SequenceMetadata> seqmeta);
	
	List<SequenceMetadata> getSequenceMetadata(NamespaceID namespaceID, List<String> sequenceIDs);
	*/
	
	void createOrReplaceNamespace(Namespace namespace, boolean updateParameters);
	
	Namespace getNamespace(NamespaceID namespace);
	
	// deletes namespace and all data associated with that namespace's current loadID
	// calling this method during a load using the current loadID may lead to a namespace with
	// incomplete data
	void deleteNamespace(NamespaceID namespace);
	
}
