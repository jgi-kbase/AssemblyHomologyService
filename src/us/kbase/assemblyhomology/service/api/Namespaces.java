package us.kbase.assemblyhomology.service.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.config.AssemblyHomologyConfig;
import us.kbase.assemblyhomology.core.AssemblyHomology;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.SequenceMatches;
import us.kbase.assemblyhomology.core.SequenceMatches.SequenceDistanceAndMetadata;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.service.Fields;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;

@javax.ws.rs.Path(ServicePaths.NAMESPACE_ROOT)
public class Namespaces {

	//TODO TEST
	//TODO JAVADOC
	
	private final AssemblyHomology ah;
	private final java.nio.file.Path tempDir;
	
	@Inject
	public Namespaces(final AssemblyHomology ah, final AssemblyHomologyConfig cfg) {
		this.ah = ah;
		this.tempDir = cfg.getPathToTemporaryFileDirectory();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<Map<String, Object>> getNamespaces() throws AssemblyHomologyStorageException {
		return ah.getNamespaces().stream().map(ns -> fromNamespace(ns))
				.collect(Collectors.toList());
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@javax.ws.rs.Path(ServicePaths.NAMESPACE_SELECT)
	public Map<String, Object> getNamespace(
			@PathParam(ServicePaths.NAMESPACE_SELECT_PARAM) final String namespace)
			throws NoSuchNamespaceException, MissingParameterException,
				IllegalParameterException, AssemblyHomologyStorageException {
		return fromNamespace(ah.getNamespace(new NamespaceID(namespace)));
	}

	private Map<String, Object> fromNamespace(final Namespace ns) {
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
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@javax.ws.rs.Path(ServicePaths.NAMESPACE_SEARCH)
	public Map<String, Object> searchNamespace(
			@Context HttpServletRequest request,
			@PathParam(ServicePaths.NAMESPACE_SELECT_PARAM) final String namespaces,
			@QueryParam("notstrict") final String notStrict,
			@QueryParam("max") final String max)
			throws IOException, NoSuchNamespaceException, IllegalParameterException,
			//TODO NOW CODE remove MinhashException when AssyHomol doesn't throw it
				MissingParameterException, AssemblyHomologyStorageException, MinHashException { 
		final int maxReturn = getMaxReturn(max);
		final boolean strict = notStrict == null;
		final Set<Namespace> nsids = ah.getNamespaces(getNamespaceIDs(namespaces));
		final Set<MinHashImplementationName> impls = nsids.stream().map(
				n -> n.getSketchDatabase().getImplementationName()).collect(Collectors.toSet());
		if (impls.size() != 1) {
			throw new IllegalParameterException(
					"Selected namespaces must have the same MinHash implementation");
		}
		final Optional<Path> expectedFileExtension =
				ah.getExpectedFileExtension(impls.iterator().next());
		String ext = ".tmp";
		if (expectedFileExtension.isPresent()) {
			ext += "." + expectedFileExtension.get().toString();
		}
		final SequenceMatches res;
		Path tempFile = null;
		// should catch IOException and do something with it?
		try (final InputStream is = request.getInputStream()) {
			tempFile = Files.createTempFile(tempDir, "assyhomol_input", ext);
			Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
			res = ah.measureDistance(
					nsids.stream().map(n -> n.getId()).collect(Collectors.toSet()),
					tempFile, maxReturn, strict);
		} finally {
			if (tempFile != null) {
				Files.delete(tempFile);
			}
		}
		final MinHashImplementationInformation impl = res.getImplementationInformation();
		final Map<String, Object> ret = new HashMap<>();
		ret.put(Fields.DIST_NAMESPACES, res.getNamespaces().stream().map(n -> fromNamespace(n))
				.collect(Collectors.toList()));
		ret.put(Fields.DIST_WARNINGS, res.getWarnings());
		ret.put(Fields.DIST_IMPLEMENTATION, impl.getImplementationName().getName());
		ret.put(Fields.DIST_IMPLEMENTATION_VERSION, impl.getImplementationVersion());
		ret.put(Fields.DISTANCES, res.getDistances().stream()
				.map(d -> fromDistance(d))
				.collect(Collectors.toList()));
		return ret;
	}

	private Set<NamespaceID> getNamespaceIDs(final String namespaces)
			throws MissingParameterException, IllegalParameterException {
		if (namespaces == null) {
			throw new MissingParameterException("namespaces");
		}
		final String[] ids = namespaces.split(",");
		final Set<NamespaceID> ret = new HashSet<>();
		for (final String id: ids) {
			ret.add(new NamespaceID(id.trim()));
		}
		return ret;
	}

	private int getMaxReturn(final String max) throws IllegalParameterException {
		if (max == null) {
			return -1;
		}
		try {
			return Integer.parseInt(max);
		} catch (NumberFormatException e) {
			throw new IllegalParameterException("Illegal value for max: " + max);
		}
	}

	private Map<String, Object> fromDistance(final SequenceDistanceAndMetadata dist) {
		final Map<String, Object> ret = new HashMap<>();
		ret.put(Fields.DIST_DISTANCE, dist.getDistance().getDistance());
		ret.put(Fields.DIST_RELATED_IDS, dist.getMetadata().getRelatedIDs());
		ret.put(Fields.DIST_SCI_NAME, dist.getMetadata().getScientificName().orNull());
		ret.put(Fields.DIST_SOURCE_ID, dist.getMetadata().getSourceID());
		ret.put(Fields.DIST_NAMESPACE_ID, dist.getNamespaceID().getName());
		return ret;
	}
	
}
