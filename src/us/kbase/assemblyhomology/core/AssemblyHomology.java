package us.kbase.assemblyhomology.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.checkNoNullsInCollection;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.core.SequenceMatches.SequenceDistanceAndMetadata;
import us.kbase.assemblyhomology.core.exceptions.IncompatibleAuthenticationException;
import us.kbase.assemblyhomology.core.exceptions.IncompatibleNamespacesException;
import us.kbase.assemblyhomology.core.exceptions.IncompatibleSketchesException;
import us.kbase.assemblyhomology.core.exceptions.InvalidSketchException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchSequenceException;
import us.kbase.assemblyhomology.core.exceptions.NoTokenProvidedException;
import us.kbase.assemblyhomology.minhash.DefaultDistanceCollector;
import us.kbase.assemblyhomology.minhash.DefaultDistanceFilter;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashDistanceCollector;
import us.kbase.assemblyhomology.minhash.MinHashDistanceFilter;
import us.kbase.assemblyhomology.minhash.MinHashImplementation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationFactory;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;
import us.kbase.assemblyhomology.minhash.exceptions.NotASketchException;
import us.kbase.assemblyhomology.storage.AssemblyHomologyStorage;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;

/** The core class in the AssemblyHomology software. Handles integrating the data from the
 * storage system with the data returned when matching a query sequence against a sketch database
 * from a namespace.
 * @author gaprice@lbl.gov
 *
 */
public class AssemblyHomology {
	
	private static final int DEFAULT_RETURN = 10;
	private static final int MAX_RETURN = 100;
	
	private final AssemblyHomologyStorage storage;
	private final Map<String, MinHashImplementationFactory> impls = new HashMap<>();
	private final Map<FilterID, MinHashDistanceFilterFactory> filters = new HashMap<>();
	private final Path tempFileDirectory;
	private final int minhashTimeoutSec;
	
	/** Create a new AssemblyHomology class.
	 * @param storage the storage system to be used by the class.
	 * @param implementationFactories the factories for the various MinHash implementations to
	 * be used by the class.
	 * @param filterFactories the factories for the filters required by the namespaces in storage.
	 * @param tempFileDirectory a directory for storing temporary files.
	 * @param minhashTimeoutSec the timeout for any minhash processes in seconds.
	 */
	public AssemblyHomology(
			final AssemblyHomologyStorage storage,
			final Collection<MinHashImplementationFactory> implementationFactories,
			final Collection<MinHashDistanceFilterFactory> filterFactories,
			final Path tempFileDirectory,
			final int minhashTimeoutSec) {
		// probably needs a builder
		checkNotNull(storage, "storage");
		checkNoNullsInCollection(implementationFactories, "implementationFactories");
		checkNoNullsInCollection(filterFactories, "filterFactories");
		checkNotNull(tempFileDirectory, "tempFileDirectory");
		if (minhashTimeoutSec < 1) {
			throw new IllegalArgumentException("minhashTimeout must be > 0");
		}
		this.storage = storage;
		this.tempFileDirectory = tempFileDirectory;
		this.minhashTimeoutSec = minhashTimeoutSec;
		for (final MinHashImplementationFactory fac: implementationFactories) {
			final String impl = fac.getImplementationName().getName().toLowerCase();
			if (impls.containsKey(impl)) {
				throw new IllegalArgumentException("Duplicate implementation: " + impl);
			}
			impls.put(impl, fac);
		}
		for (final MinHashDistanceFilterFactory fac: filterFactories) {
			if (filters.containsKey(fac.getID())) {
				throw new IllegalArgumentException("Duplicate filter: " + fac.getID().getName());
			}
			filters.put(fac.getID(), fac);
		}
	}
	
	// should this not expose some of the stuff in the namespace class? Load ID, sketch DB path
	/** Get all the namespaces available.
	 * @return the namespaces.
	 * @throws AssemblyHomologyStorageException if an error occurred contacting the storage
	 * system.
	 */
	public Set<Namespace> getNamespaces() throws AssemblyHomologyStorageException {
		return storage.getNamespaces();
	}
	
	// should this not expose some of the stuff in the namespace class? Load ID, sketch DB path
	/** Get a set of namespaces.
	 * @param ids the IDs of the namespaces to get.
	 * @return the namespaces.
	 * @throws NoSuchNamespaceException if one of the IDs does not exist in the system.
	 * @throws AssemblyHomologyStorageException if an error occurred contacting the storage
	 * system.
	 */
	public Set<Namespace> getNamespaces(final Set<NamespaceID> ids)
			throws NoSuchNamespaceException, AssemblyHomologyStorageException {
		checkNoNullsInCollection(ids, "ids");
		final Set<Namespace> namespaces = new HashSet<>();
		for (final NamespaceID id: ids) {
			// add a bulk method if this proves too slow
			namespaces.add(storage.getNamespace(id));
		}
		return namespaces;
	}
	
	// should this not expose some of the stuff in the namespace class? Load ID, sketch DB path
	/** Get a namespace.
	 * @param namespaceID the ID of the namespace to get.
	 * @return the namespace.
	 * @throws NoSuchNamespaceException if the ID does not exist in the system.
	 * @throws AssemblyHomologyStorageException if an error occurred contacting the storage
	 * system.
	 */
	public Namespace getNamespace(final NamespaceID namespaceID)
			throws NoSuchNamespaceException, AssemblyHomologyStorageException {
		checkNotNull(namespaceID, "namespaceID");
		return storage.getNamespace(namespaceID);
	}
	
	/** Get the file extension expected by a particular implementation.
	 * @param impl the name of the implementation of interest.
	 * @return the expected file extension or absent if there is none.
	 */
	public Optional<Path> getExpectedFileExtension(final MinHashImplementationName impl) {
		checkNotNull(impl, "impl");
		final String implLower = impl.getName().toLowerCase();
		if (!impls.containsKey(implLower)) {
			throw new IllegalArgumentException("No such implementation: " + impl.getName());
		}
		return impls.get(implLower).getExpectedFileExtension();
	}
	
	/** Measure the MinHash distance from a single query sequence to the sequences in one or
	 * more namespaces.
	 * @param namespaceIDs the namespace IDs for the namespaces of interest.
	 * @param sketchDB the input query sketch that will be measured against the sketch databases
	 * associated with the given sequences.
	 * @param returnCount the number of measurements to return. If < 1 or > 100, 10 measurements
	 * will be returned.
	 * @param strict true to enforce an exact match between sketch parameters. If false, 
	 * differences in the parameters will be ignored if the MinHash implementation allows it.
	 * @param token a token to use for namespaces that require authentication. Pass null if
	 * no namespaces require authentication and no token is available.
	 * @return the sequence matches.
	 * @throws NoSuchNamespaceException if one of the namespace IDs doesn't exist in the system.
	 * @throws AssemblyHomologyStorageException if an error occurred contacting the storage
	 * system.
	 * @throws InvalidSketchException if the input sketch is invalid.
	 * @throws IncompatibleNamespacesException if the selected namespaces have different
	 * MinHash implementations.
	 * @throws IncompatibleSketchesException if the input sketch's parameters are not
	 * compatible with any of the selected namespaces' sketch databases.
	 * @throws NoTokenProvidedException if a namespace requires authentication but no token
	 * was provided.
	 * @throws IncompatibleAuthenticationException if namespaces with different authentication
	 * sources are requested.
	 */
	public SequenceMatches measureDistance(
			final Set<NamespaceID> namespaceIDs,
			final Path sketchDB,
			int returnCount,
			final boolean strict,
			final Token token)
			throws NoSuchNamespaceException, AssemblyHomologyStorageException,
				InvalidSketchException, IncompatibleNamespacesException,
				IncompatibleSketchesException, NoTokenProvidedException,
				IncompatibleAuthenticationException {
		// may need a builder here, only 1st 2 arguments are always required
		checkNoNullsInCollection(namespaceIDs, "namespaceIDs");
		checkNotNull(sketchDB, "sketchDB");
		if (namespaceIDs.isEmpty()) {
			throw new IllegalArgumentException("No namespace IDs provided");
		}
		if (returnCount > MAX_RETURN || returnCount < 1) {
			returnCount = DEFAULT_RETURN;
		}
		
		final Set<Namespace> namespaces = getNamespaces(namespaceIDs);
		final Map<String, Namespace> idToNS = namespaces.stream()
				.collect(Collectors.toMap(n -> n.getID().getName(), n -> n));
		final MinHashImplementation impl = getImplementation(namespaces);
		final DistReturn distret = getDistances(
				namespaces, sketchDB, impl, returnCount, strict, Optional.fromNullable(token));
		final Map<Namespace, Map<String, SequenceMetadata>> idToSeq = 
				getSequenceMetadata(idToNS, distret.dists);
		final List<SequenceDistanceAndMetadata> distNMeta = new LinkedList<>();
		for (final MinHashDistance d: distret.dists) {
			final Namespace ns = idToNS.get(d.getReferenceDBName().getName());
			final SequenceMetadata seq = idToSeq.get(ns).get(d.getSequenceID());
			distNMeta.add(new SequenceDistanceAndMetadata(ns.getID(), d, seq));
		}
		
		// return query id?
		return new SequenceMatches(
				namespaces, impl.getImplementationInformation(), distNMeta, distret.warnings);
	}

	private Map<Namespace, Map<String, SequenceMetadata>> getSequenceMetadata(
			final Map<String, Namespace> idToNS,
			final Set<MinHashDistance> dists)
			throws NoSuchNamespaceException, AssemblyHomologyStorageException {
		final Map<Namespace, List<String>> ids = new HashMap<>();
		for (final MinHashDistance dist: dists) {
			final Namespace ns = idToNS.get(dist.getReferenceDBName().getName());
			if (!ids.containsKey(ns)) {
				ids.put(ns, new LinkedList<>());
			}
			ids.get(ns).add(dist.getSequenceID());
		}
		final Map<Namespace, Map<String, SequenceMetadata>> ret = new HashMap<>();
		for (final Entry<Namespace, List<String>> e: ids.entrySet()) {
			final List<SequenceMetadata> meta;
			try {
				meta = storage.getSequenceMetadata(
						e.getKey().getID(), e.getKey().getLoadID(), e.getValue());
			} catch (NoSuchSequenceException err) {
				throw new IllegalStateException(String.format(
						"Database is corrupt. Unable to find sequences from sketch file for " +
						"namespace %s: %s", e.getKey().getID().getName(), err.getMessage()), err);
			}
			ret.put(e.getKey(), meta.stream().collect(Collectors.toMap(s -> s.getID(), s -> s)));
		}
		return ret;
	}

	private static class DistReturn {
		
		private final Set<MinHashDistance> dists;
		private final Set<String> warnings;
		
		private DistReturn(final Set<MinHashDistance> set, final Set<String> warnings) {
			this.dists = set;
			this.warnings = warnings;
		}
	}
	
	private DistReturn getDistances(
			final Set<Namespace> namespaces,
			final Path sketchDB,
			final MinHashImplementation impl,
			int returnCount,
			final boolean strict,
			final Optional<Token> token)
			throws InvalidSketchException, IncompatibleSketchesException,
				NoTokenProvidedException, IncompatibleAuthenticationException {
		final MinHashSketchDatabase query = getQueryDB(sketchDB, impl);
		final Set<String> warnings = new HashSet<>();
		for (final Namespace ns: namespaces) {
			try {
				warnings.addAll(ns.getSketchDatabase().checkIsQueriableBy(query, strict).stream()
						.map(s -> "Namespace " + ns.getID().getName() + ": " + s)
						.collect(Collectors.toList()));
			} catch (
					us.kbase.assemblyhomology.minhash.exceptions.IncompatibleSketchesException e) {
				throw new IncompatibleSketchesException(String.format(
						"Unable to query namespace %s with input sketch: %s",
						ns.getID().getName(), e.getMessage()), e);
			}
		}
		final MinHashDistanceCollector distCol = new DefaultDistanceCollector(returnCount);
		final Map<MinHashSketchDatabase, MinHashDistanceFilter> dbs =
				setUpDistanceFilters(namespaces, distCol, token);
		try {
			// ignore returned warnings since we gather them above
			impl.computeDistance(query, dbs, strict);
		} catch (MinHashException e) {
			/* At this point minhash should work, and the user input should be ok.
			 * Hard to know how to respond to exceptions. For now just bail.
			 * when failure modes are better understood / logged, could expand
			 * the exception hierarchy and responses.
			 */
			throw new IllegalStateException("Unexpected error running MinHash implementation " +
					impl.getImplementationInformation().getImplementationName().getName(), e);
		}
		return new DistReturn(distCol.getDistances(), warnings);
	}

	private Map<MinHashSketchDatabase, MinHashDistanceFilter> setUpDistanceFilters(
			final Set<Namespace> namespaces,
			final MinHashDistanceCollector distCol,
			final Optional<Token> token)
			throws NoTokenProvidedException, IncompatibleAuthenticationException {
		final MinHashDistanceFilter defaultFilter = new DefaultDistanceFilter(distCol);
		final Map<MinHashSketchDatabase, MinHashDistanceFilter> dbs = new HashMap<>();
		NamespaceID authns = null;
		String auth = null;
		for (final Namespace ns: namespaces) {
			if (ns.getFilterID().isPresent()) {
				if (!filters.containsKey(ns.getFilterID().get())) {
					throw new IllegalStateException(String.format(
							"Application is misconfigured. Namespace %s requires filter %s but " +
							"it is not configured.",
							ns.getID().getName(), ns.getFilterID().get().getName()));
				}
				final MinHashDistanceFilterFactory fac = filters.get(ns.getFilterID().get());
				if (fac.getAuthSource().isPresent()) {
					if (!token.isPresent()) {
						throw new NoTokenProvidedException(String.format(
								"Namespace %s requires %s authentication, but no " +
								"token was provided",
								ns.getID().getName(), fac.getAuthSource().get()));
					}
					if (auth == null) {
						auth = fac.getAuthSource().get();
						authns = ns.getID();
					} else {
						if (!auth.equals(fac.getAuthSource().get())) {
							throw new IncompatibleAuthenticationException(String.format(
									"Namespace %s requires %s authentication, " +
									"namespace %s requires %s authentication",
									authns.getName(), auth,
									ns.getID().getName(), fac.getAuthSource().get()));
						}
					}
					dbs.put(ns.getSketchDatabase(), fac.getFilter(distCol, token.get()));
				} else {
					dbs.put(ns.getSketchDatabase(), fac.getFilter(distCol));
				}
			} else {
				dbs.put(ns.getSketchDatabase(), defaultFilter);
			}
		}
		return dbs;
	}

	private MinHashSketchDatabase getQueryDB(final Path sketchDB, final MinHashImplementation impl)
			throws InvalidSketchException {
		final MinHashSketchDatabase query;
		try {
			query = impl.getDatabase(
					new MinHashSketchDBName("<query>"),
					new MinHashDBLocation(sketchDB));
		} catch (NotASketchException e) {
			if (e.getMinHashErrorOutput().isPresent()) {
				LoggerFactory.getLogger(getClass()).error(
						"minhash implementation stderr:\n{}", e.getMinHashErrorOutput().get());
			}
			throw new InvalidSketchException("The input sketch is not a valid sketch.", e);
		} catch (MinHashException e) {
			// there may be other error types that are not the user's fault here, but hard to
			// know without lots of experiments. Deal with them as they come up. For now we
			// assume something broke badly.
			throw new IllegalStateException(
					"Error loading query sketch database: " + e.getMessage(), e);
		}
		if (query.getSequenceCount() != 1) {
			throw new InvalidSketchException(
					"Query sketch database must have exactly one sketch");
		}
		return query;
	}

	private MinHashImplementation getImplementation(final Set<Namespace> ns)
			throws IncompatibleNamespacesException {
		final Set<MinHashImplementationName> implnames = ns.stream()
				.map(n -> n.getSketchDatabase().getImplementationName())
				.collect(Collectors.toSet());
		if (implnames.size() != 1) {
			throw new IncompatibleNamespacesException(
					"The selected namespaces must share the same Minhash implementation");
		}
		final String impl = implnames.iterator().next().getName().toLowerCase();
		if (!impls.containsKey(impl)) {
			throw new IllegalStateException(String.format(
					"Application is misconfigured. Implementation %s stored in database but " +
					"not available.", impl));
		}
		try {
			return impls.get(impl).getImplementation(tempFileDirectory, minhashTimeoutSec);
		} catch (MinHashInitException e) {
			throw new IllegalStateException(String.format("Application is misconfigured. " +
					"Error attempting to build the %s MinHash implementation.", impl), e);
		}
	}
}
