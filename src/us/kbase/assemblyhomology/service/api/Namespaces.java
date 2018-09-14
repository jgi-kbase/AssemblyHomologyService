package us.kbase.assemblyhomology.service.api;

import static us.kbase.assemblyhomology.util.Util.isNullOrEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.config.AssemblyHomologyConfig;
import us.kbase.assemblyhomology.core.AssemblyHomology;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.NamespaceView;
import us.kbase.assemblyhomology.core.SequenceMatches;
import us.kbase.assemblyhomology.core.SequenceMatches.SequenceDistanceAndMetadata;
import us.kbase.assemblyhomology.core.Token;
import us.kbase.assemblyhomology.core.exceptions.AuthenticationException;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.IncompatibleAuthenticationException;
import us.kbase.assemblyhomology.core.exceptions.IncompatibleNamespacesException;
import us.kbase.assemblyhomology.core.exceptions.IncompatibleSketchesException;
import us.kbase.assemblyhomology.core.exceptions.InvalidSketchException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashDistanceFilterException;
import us.kbase.assemblyhomology.service.Fields;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;

/** Handler for the endpoints under the {@link ServicePaths#NAMESPACE_ROOT} endpoints.
 * @author gaprice@lbl.gov
 *
 */
@javax.ws.rs.Path(ServicePaths.NAMESPACE_ROOT)
public class Namespaces {

	private final AssemblyHomology ah;
	private final java.nio.file.Path tempDir;
	
	/** Construct the handler. This is typically done by the Jersey framework.
	 * @param ah an instance of the core assembly homology class.
	 * @param cfg the configuration for the assembly homology service.
	 */
	@Inject
	public Namespaces(final AssemblyHomology ah, final AssemblyHomologyConfig cfg) {
		this.ah = ah;
		this.tempDir = cfg.getPathToTemporaryFileDirectory();
	}

	/** Get the extant namespaces.
	 * @return the namespaces in the system.
	 * @throws AssemblyHomologyStorageException if an error occurs contacting the storage system.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Set<Map<String, Object>> getNamespaces() throws AssemblyHomologyStorageException {
		return ah.getNamespaces().stream().map(ns -> fromNamespace(ns))
				.collect(Collectors.toSet());
	}
	
	/** Get a particular namespace.
	 * @param namespace the ID of the namespace.
	 * @return the namespace.
	 * @throws NoSuchNamespaceException if there is no such namespace.
	 * @throws MissingParameterException if the ID is missing or white space only.
	 * @throws IllegalParameterException if the ID is not a valid namespace ID.
	 * @throws AssemblyHomologyStorageException if an error occurs contacting the storage system.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@javax.ws.rs.Path(ServicePaths.NAMESPACE_SELECT)
	public Map<String, Object> getNamespace(
			@PathParam(ServicePaths.NAMESPACE_SELECT_PARAM) final String namespace)
			throws NoSuchNamespaceException, MissingParameterException,
				IllegalParameterException, AssemblyHomologyStorageException {
		return fromNamespace(ah.getNamespace(new NamespaceID(namespace)));
	}

	private Map<String, Object> fromNamespace(final NamespaceView ns) {
		final Map<String, Object> ret = new HashMap<>();
		final MinHashParameters params = ns.getParameterSet();
		ret.put(Fields.NAMESPACE_AUTH_SOURCE, ns.getAuthsource().orNull());
		ret.put(Fields.NAMESPACE_DESCRIPTION, ns.getDescription().orNull());
		ret.put(Fields.NAMESPACE_ID, ns.getID().getName());
		ret.put(Fields.NAMESPACE_LASTMOD, ns.getModification().toEpochMilli());
		ret.put(Fields.NAMESPACE_IMPLEMENTATION, ns.getImplementationName().getName());
		ret.put(Fields.NAMESPACE_SEQ_COUNT, ns.getSequenceCount());
		ret.put(Fields.NAMESPACE_KMER_SIZE, Arrays.asList(params.getKmerSize()));
		ret.put(Fields.NAMESPACE_SCALING, params.getScaling().orNull());
		ret.put(Fields.NAMESPACE_SKETCH_SIZE, params.getSketchSize().orNull());
		ret.put(Fields.NAMESPACE_DB_ID, ns.getSourceDatabaseID());
		ret.put(Fields.NAMESPACE_DATA_SOURCE_ID, ns.getSourceID().getName());
		return ret;
	}
	
	/** Search one or more namespaces. Expects a sketch database file in the request body.
	 * @param request the incoming servlet request.
	 * @param namespaces a comma delimited string of namespace IDs.
	 * @param notStrict if non null, MinHash searches will continue if possible if the query
	 * sketch database parameters do not match the target database parameters.
	 * @param max the maximum number of matches to return. If missing, < 1, or > 100 the maximum
	 * is set to 10.
	 * @return the matches.
	 * @throws IOException if an error occurs retrieving the sketch database file from the
	 * request or saving the file to a temporary file.
	 * @throws NoSuchNamespaceException if one of the requested namespaces does not exist.
	 * @throws IncompatibleSketchesException if the provided sketch parameters are incompatible
	 * with one or more of the target sketches.
	 * @throws MissingParameterException if the namespace IDs parameter is missing.
	 * @throws AssemblyHomologyStorageException if an error occurs contacting the storage system.
	 * @throws InvalidSketchException if the sketch file provided in the request body is not
	 * a sketch.
	 * @throws IncompatibleNamespacesException if the selected namespaces have incompatible
	 * MinHash implementations.
	 * @throws IllegalParameterException if one or more of the namespace IDs are illegal, or if
	 * max is not an integer if provided.
	 * @throws IncompatibleAuthenticationException if namespaces with different authentication
	 * sources are requested.
	 * @throws MinHashDistanceFilterException if a filter exception occurs.
	 * @throws AuthenticationException if an authentication error occurs.
	 */
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@javax.ws.rs.Path(ServicePaths.NAMESPACE_SEARCH)
	public Map<String, Object> searchNamespaces(
			@Context HttpServletRequest request,
			@HeaderParam("Authorization") final String auth,
			@PathParam(ServicePaths.NAMESPACE_SELECT_PARAM) final String namespaces,
			@QueryParam("notstrict") final String notStrict,
			@QueryParam("max") final String max)
			throws IOException, NoSuchNamespaceException, IncompatibleSketchesException,
				MissingParameterException, AssemblyHomologyStorageException,
				InvalidSketchException, IncompatibleNamespacesException,
				IllegalParameterException, IncompatibleAuthenticationException,
				AuthenticationException, MinHashDistanceFilterException { 
		final int maxReturn = getMaxReturn(max);
		final boolean strict = notStrict == null;
		final Set<NamespaceView> nss = ah.getNamespaces(getNamespaceIDs(namespaces));
		final Set<MinHashImplementationName> impls = nss.stream().map(
				n -> n.getImplementationName()).collect(Collectors.toSet());
		if (impls.size() != 1) {
			throw new IncompatibleNamespacesException(
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
					nss.stream().map(n -> n.getID()).collect(Collectors.toSet()),
					tempFile, maxReturn, strict, getToken(auth));
		} finally {
			if (tempFile != null) {
				Files.delete(tempFile);
			}
		}
		final MinHashImplementationInformation impl = res.getImplementationInformation();
		final Map<String, Object> ret = new HashMap<>();
		ret.put(Fields.DIST_NAMESPACES, res.getNamespaces().stream().map(n -> fromNamespace(n))
				.collect(Collectors.toSet()));
		ret.put(Fields.DIST_WARNINGS, res.getWarnings());
		ret.put(Fields.DIST_IMPLEMENTATION, impl.getImplementationName().getName());
		ret.put(Fields.DIST_IMPLEMENTATION_VERSION, impl.getImplementationVersion());
		ret.put(Fields.DISTANCES, res.getDistances().stream()
				.map(d -> fromDistance(d))
				.collect(Collectors.toList()));
		return ret;
	}

	private Token getToken(final String auth) throws MissingParameterException {
		return isNullOrEmpty(auth) ? null : new Token(auth);
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
