package us.kbase.assemblyhomology.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.checkNoNullsInCollection;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Optional;
import com.mongodb.MongoClient;

import us.kbase.assemblyhomology.core.SequenceMatches.SequenceDistanceAndMetadata;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchNamespaceException;
import us.kbase.assemblyhomology.core.exceptions.NoSuchSequenceException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashDistanceSet;
import us.kbase.assemblyhomology.minhash.MinHashImplementation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationFactory;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;
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
			final NamespaceID namespaceID,
			final Path sketchDB,
			int returnCount)
			throws NoSuchNamespaceException, AssemblyHomologyStorageException,
				IllegalParameterException, MinHashException {
		checkNotNull(namespaceID, "namespaceID");
		checkNotNull(sketchDB, "sketchDB");
		if (returnCount > MAX_RETURN || returnCount < 0) {
			returnCount = DEFAULT_RETURN;
		}
		
		final Namespace ns = getNamespace(namespaceID);
		final MinHashImplementation impl = getImplementation(
				ns.getSketchDatabase().getImplementationName());
		final MinHashDistanceSet dists = getDistances(ns, sketchDB, impl, returnCount);
		final List<String> ids = dists.getDistances().stream().map(d -> d.getSequenceID())
				.collect(Collectors.toList());
		final List<SequenceMetadata> seqmeta;
		try{
			seqmeta = storage.getSequenceMetadata(ns.getId(), ns.getLoadID(), ids);
		} catch (NoSuchSequenceException e) {
			throw new IllegalStateException(String.format(
					"Database is corrupt. Unable to find sequences from sketch file for " +
					"namespace %s: %s", ns.getId().getName(), e.getMessage()), e);
		}
		final Map<String, SequenceMetadata> idToSeq = seqmeta.stream()
				.collect(Collectors.toMap(s -> s.getID(), s -> s));
		
		final List<SequenceDistanceAndMetadata> distNMeta = dists.getDistances().stream()
				.map(d -> new SequenceDistanceAndMetadata(d, idToSeq.get(d.getSequenceID())))
				.collect(Collectors.toList());
		
		// return query id?
		return new SequenceMatches(ns, impl.getImplementationInformation(), distNMeta);
	}

	private MinHashDistanceSet getDistances(
			final Namespace ns,
			final Path sketchDB,
			final MinHashImplementation impl,
			int returnCount)
			throws IllegalParameterException, MinHashException {
		final MinHashSketchDatabase query;
		try {
			query = impl.getDatabase(new MinHashDBLocation(sketchDB));
		} catch (MinHashException e) {
			//TODO NOW CODE this needs some work - specific exception for bad db with error code
			throw new IllegalParameterException(
					"Error loading query sketch database: " + e.getMessage(), e);
		}
		if (query.getSequenceCount() != 1) {
			//TODO NOW CODe more specific exception
			throw new IllegalParameterException(
					"Query sketch database must have exactly one query");
		}
		try {
			ns.getSketchDatabase().checkCompatibility(query);
		} catch (IllegalArgumentException e) {
			//TODO NOW CODE better exception
			throw new IllegalParameterException(e.getMessage(), e);
		}
		final MinHashDistanceSet res;
		try {
			res = impl.computeDistance(query, ns.getSketchDatabase(), returnCount);
		} catch (MinHashException e) {
			//TODO NOW CODE better exception, need to try different failure modes, maybe need a set of exceptions
			// that being said, everything should be ok from the user point of view now, so just rethrow
			throw e;
		}
		return res;
	}

	private MinHashImplementation getImplementation(
			final MinHashImplementationName implementationName) throws MinHashInitException {
		final String impl = implementationName.getName().toLowerCase();
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
				nsid,
				Paths.get("/home/crusherofheads/kb_refseq_sourmash/" +
						"kb_refseq_ci_1000_15792_446_1.msh"),
				50);
		System.out.println(dists);
		System.out.println(dists.getNamespace());
		System.out.println(dists.getImplementationInformation());
		System.out.println();
		for (final SequenceDistanceAndMetadata s: dists.getDistances()) {
			System.out.println(s);
		}
	}

}
