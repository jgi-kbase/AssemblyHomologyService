package us.kbase.assemblyhomology.service.api;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import us.kbase.assemblyhomology.core.AssemblyHomology;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.service.Fields;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;

@Path(ServicePaths.NAMESPACE_ROOT)
public class Namespaces {

	//TODO TEST
	//TODO JAVADOC
	
	private final AssemblyHomology ah;
	
	@Inject
	public Namespaces(final AssemblyHomology ah) {
		this.ah = ah;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path(ServicePaths.NAMESPACE_SELECT)
	public Map<String, Object> getNamespace(
			@PathParam(ServicePaths.NAMESPACE_SELECT_PARAM) final String namespace)
			throws NoSuchNamespaceException, MissingParameterException,
				IllegalParameterException, AssemblyHomologyStorageException {
		final Namespace ns = ah.getNamespace(new NamespaceID(namespace));
		final MinHashSketchDatabase db = ns.getSketchDatabase();
		final MinHashParameters params = db.getParameterSet();
		final Map<String, Object> ret = new HashMap<>();
		ret.put(Fields.NAMESPACE_DESCRIPTION, ns.getDescription().orNull());
		ret.put(Fields.NAMESPACE_ID, ns.getId().getName());
		ret.put(Fields.NAMESPACE_IMPLEMENTATION, db.getImplementationName().getName());
		ret.put(Fields.NAMESPACE_SEQ_COUNT, db.getSequenceCount());
		ret.put(Fields.NAMESPACE_KMER_SIZE, params.getKmerSize());
		ret.put(Fields.NAMESPACE_SCALING, params.getScaling().orNull());
		ret.put(Fields.NAMESPACE_SKETCH_SIZE, params.getSketchSize().orNull());
		ret.put(Fields.NAMESPACE_DB_ID, ns.getSourceDatabaseID());
		ret.put(Fields.NAMESPACE_DATA_SOURCE_ID, ns.getSourceID().getName());
		return ret;
	}
	
}
