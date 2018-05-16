package us.kbase.assemblyhomology.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.checkNoNullsInCollection;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
import com.mongodb.MongoClient;

import us.kbase.assemblyhomology.core.SequenceMatches.SequenceDistanceAndMetadata;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.InvalidSketchException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchSequenceException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashDistanceSet;
import us.kbase.assemblyhomology.minhash.MinHashImplementation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationFactory;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.IncompatibleSketchesException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;
import us.kbase.assemblyhomology.minhash.exceptions.NotASketchException;
import us.kbase.assemblyhomology.minhash.mash.MashFactory;
import us.kbase.assemblyhomology.storage.AssemblyHomologyStorage;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;
import us.kbase.assemblyhomology.storage.mongo.MongoAssemblyHomologyStorage;

public class AssemblyHomology {
	
	//TODO TEST
	//TODO JAVADOC
	
	private static final int DEFAULT_RETURN = 10;
	private static final int MAX_RETURN = 100;
	
	private final AssemblyHomologyStorage storage;
	private final Map<String, MinHashImplementationFactory> impls = new HashMap<>();
	private final Path tempFileDirectory;
	
	public AssemblyHomology(
			final AssemblyHomologyStorage storage,
			final Collection<MinHashImplementationFactory> implementationFactories,
			final Path tempFileDirectory) {
		checkNotNull(storage, "storage");
		checkNoNullsInCollection(implementationFactories, "implementationFactories");
		checkNotNull(tempFileDirectory, "tempFileDirectory");
		this.storage = storage;
		this.tempFileDirectory = tempFileDirectory;
		for (final MinHashImplementationFactory fac: implementationFactories) {
			final String impl = fac.getImplementationName().getName().toLowerCase();
			if (impls.containsKey(impl)) {
				throw new IllegalArgumentException("Duplicate implementation: " + impl);
			}
			impls.put(impl, fac);
		}
	}
	
	// should this not expose some of the stuff in the namespace class? Load ID, sketch DB path
	public Set<Namespace> getNamespaces() throws AssemblyHomologyStorageException {
		return storage.getNamespaces();
	}
	
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
	public Namespace getNamespace(final NamespaceID namespaceID)
			throws NoSuchNamespaceException, AssemblyHomologyStorageException {
		checkNotNull(namespaceID, "namespaceID");
		return storage.getNamespace(namespaceID);
	}
	
	public Optional<Path> getExpectedFileExtension(final MinHashImplementationName impl) {
		checkNotNull(impl, "impl");
		final String implLower = impl.getName().toLowerCase();
		if (!impls.containsKey(implLower)) {
			throw new IllegalArgumentException("No such implementation: " + impl.getName());
		}
		return impls.get(implLower).getExpectedFileExtension();
	}
	
	public SequenceMatches measureDistance(
			final Set<NamespaceID> namespaceIDs,
			final Path sketchDB,
			int returnCount,
			boolean strict)
			throws NoSuchNamespaceException, AssemblyHomologyStorageException,
				IllegalParameterException, MinHashException, InvalidSketchException {
		checkNoNullsInCollection(namespaceIDs, "namespaceIDs");
		checkNotNull(sketchDB, "sketchDB");
		if (returnCount > MAX_RETURN || returnCount < 0) {
			returnCount = DEFAULT_RETURN;
		}
		
		final Set<Namespace> namespaces = getNamespaces(namespaceIDs);
		final Map<String, Namespace> idToNS = namespaces.stream()
				.collect(Collectors.toMap(n -> n.getId().getName(), n -> n));
		final MinHashImplementation impl = getImplementation(namespaces);
		final DistReturn distret = getDistances(
				namespaces, sketchDB, impl, returnCount, strict);
		final Map<Namespace, Map<String, SequenceMetadata>> idToSeq = 
				getSequenceMetadata(idToNS, distret.dists);
		final List<SequenceDistanceAndMetadata> distNMeta = new LinkedList<>();
		for (final MinHashDistance d: distret.dists.getDistances()) {
			final Namespace ns = idToNS.get(d.getReferenceDBName().getName());
			final SequenceMetadata seq = idToSeq.get(ns).get(d.getSequenceID());
			distNMeta.add(new SequenceDistanceAndMetadata(ns.getId(), d, seq));
		}
		
		// return query id?
		return new SequenceMatches(
				namespaces, impl.getImplementationInformation(), distNMeta, distret.warnings);
	}

	private Map<Namespace, Map<String, SequenceMetadata>> getSequenceMetadata(
			final Map<String, Namespace> idToNS,
			final MinHashDistanceSet dists)
			throws NoSuchNamespaceException, AssemblyHomologyStorageException {
		final Map<Namespace, List<String>> ids = new HashMap<>();
		for (final MinHashDistance dist: dists.getDistances()) {
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
						e.getKey().getId(), e.getKey().getLoadID(), e.getValue());
			} catch (NoSuchSequenceException err) {
				throw new IllegalStateException(String.format(
						"Database is corrupt. Unable to find sequences from sketch file for " +
						"namespace %s: %s", e.getKey().getId().getName(), err.getMessage()), err);
			}
			ret.put(e.getKey(), meta.stream().collect(Collectors.toMap(s -> s.getID(), s -> s)));
		}
		return ret;
	}

	private static class DistReturn {
		
		private final MinHashDistanceSet dists;
		private final List<String> warnings;
		
		private DistReturn(MinHashDistanceSet dists, List<String> warnings) {
			this.dists = dists;
			this.warnings = warnings;
		}
	}
	
	private DistReturn getDistances(
			final Set<Namespace> namespaces,
			final Path sketchDB,
			final MinHashImplementation impl,
			int returnCount,
			final boolean strict)
			throws IllegalParameterException, MinHashException, InvalidSketchException {
		final MinHashSketchDatabase query;
		try {
			query = impl.getDatabase(
					new MinHashSketchDBName("<query>"),
					new MinHashDBLocation(sketchDB));
		} catch (NotASketchException e) {
			if (e.getMinHashErrorOutput().isPresent()) {
				LoggerFactory.getLogger(getClass()).error(
						"minhash implementation stderr:\n {}", e.getMinHashErrorOutput().get());
			}
			throw new InvalidSketchException("The input sketch is not a valid sketch.", e);
		} catch (MinHashException e) {
			// there may be other error types that are not the user's fault here, but hard to
			// know without lots of experiments. Deal with them as they come up.
			throw new IllegalParameterException(
					"Error loading query sketch database: " + e.getMessage(), e);
		}
		if (query.getSequenceCount() != 1) {
			//TODO NOW CODE more specific exception
			throw new IllegalParameterException(
					"Query sketch database must have exactly one query");
		}
		final List<String> warnings = new LinkedList<>();
		for (final Namespace ns: namespaces) {
			try {
				warnings.addAll(ns.getSketchDatabase().checkIsQueriableBy(query, strict).stream()
						.map(s -> "Namespace " + ns.getId().getName() + ": " + s)
						.collect(Collectors.toList()));
			} catch (IncompatibleSketchesException e) {
				throw new IllegalParameterException(String.format(
						"Unable to query namespace %s with input sketch: %s",
						ns.getId().getName(), e.getMessage()), e);
			}
		}
		final List<MinHashSketchDatabase> dbs = namespaces.stream()
				.map(n -> n.getSketchDatabase()).collect(Collectors.toList());
		final MinHashDistanceSet dists;
		try {
			dists = impl.computeDistance(query, dbs, returnCount, strict);
		} catch (MinHashException e) {
			//TODO NOW CODE better exception, need to try different failure modes, maybe need a set of exceptions
			// that being said, everything should be ok from the user point of view now, so just rethrow
			throw e;
		}
		return new DistReturn(dists, warnings);
	}

	private MinHashImplementation getImplementation(final Set<Namespace> ns)
			throws MinHashInitException, IllegalParameterException {
		final Set<MinHashImplementationName> implnames = ns.stream()
				.map(n -> n.getSketchDatabase().getImplementationName())
				.collect(Collectors.toSet());
		if (implnames.size() != 1) {
			throw new IllegalParameterException(
					"The selected namespaces must share the same implementation");
		}
		final String impl = implnames.iterator().next().getName().toLowerCase();
		if (!impls.containsKey(impl)) {
			throw new IllegalStateException(String.format(
					"Application is misconfigured. Implementation %s stored in database but " +
					"not available.", impl));
		}
		//TODO NOW CODE catch and wrap min hash exception
		return impls.get(impl).getImplementation(tempFileDirectory);
	}
	
	public static void main(final String[] args) throws Exception {
		@SuppressWarnings("resource")
		final MongoClient mc = new MongoClient("localhost");
		final AssemblyHomologyStorage storage = new MongoAssemblyHomologyStorage(
				mc.getDatabase("assembly_homology_test"));
		
		final AssemblyHomology ah = new AssemblyHomology(
				storage,
				new HashSet<>(Arrays.asList(new MashFactory())),
				Paths.get("./temp_delete"));
		
		final NamespaceID nsid = new NamespaceID("mynamespace");
		System.out.println(ah.getNamespace(nsid));
		
		final SequenceMatches dists = ah.measureDistance(
				new HashSet<>(Arrays.asList(nsid)),
				Paths.get("/home/crusherofheads/kb_refseq_sourmash/" +
						"kb_refseq_ci_1000_15792_446_1.msh"),
				10,
				false);
		System.out.println(dists);
		System.out.println(dists.getNamespaces());
		System.out.println(dists.getImplementationInformation());
		System.out.println();
		for (final SequenceDistanceAndMetadata s: dists.getDistances()) {
			System.out.println(s);
		}
	}

}
