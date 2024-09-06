package us.kbase.assemblyhomology.load;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.checkNoNullsInCollection;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.core.FilterID;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.MinHashDistanceFilterFactory;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.load.exceptions.LoadInputParseException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementation;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.storage.AssemblyHomologyStorage;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;
import us.kbase.assemblyhomology.util.Restreamable;

/** A data loader for the Assembly Homology software package.
 * @author gaprice@lbl.gov
 *
 */
public class Loader {
	
	private final AssemblyHomologyStorage storage;
	private final Clock clock;
	
	/** Create a new loader.
	 * @param storage the storage system where the data will be loaded.
	 */
	public Loader(final AssemblyHomologyStorage storage) {
		this(storage, Clock.systemUTC());
	}
	
	// for tests
	private Loader(final AssemblyHomologyStorage storage, final Clock clock) {
		checkNotNull(storage, "storage");
		this.storage = storage;
		this.clock = clock;
	}
	
	/** Load a data set into the Assembly Homology storage system.
	 * @param loadID the ID of the load.
	 * @param minhashImpl the MinHash implementation to use to read the sketch database to be
	 * loaded.
	 * @param sketchDBlocation the location of the MinHash sketch database.
	 * @param filters any configured filters. If a namespace to be loaded is configured with
	 * a filter ID, this filter must be provided in the set, and it will be used to validate
	 * the sketch sequence IDs.
	 * @param namespaceYAML the YAML input for the namespace as described in
	 * {@link NamespaceLoadInfo}.
	 * @param sequenceMetaJSONLines the sequence metadata information. Each line is a JSON string
	 * as described in {@link SeqMetaLoadInfo}.
	 * @throws MinHashException if the sketch database cannot be read.
	 * @throws IOException if an IO error occurs reading the input data.
	 * @throws LoadInputParseException if the load input data could not be parsed.
	 * @throws AssemblyHomologyStorageException if an error occurs communicating with the storage
	 * system.
	 */
	public void load(
			final LoadID loadID,
			final MinHashImplementation minhashImpl,
			final MinHashDBLocation sketchDBlocation,
			final Set<MinHashDistanceFilterFactory> filters,
			final Restreamable namespaceYAML,
			final Restreamable sequenceMetaJSONLines)
			throws MinHashException, IOException, LoadInputParseException,
				AssemblyHomologyStorageException {
		// reaaaaally need to think about a builder here
		checkNotNull(loadID, "loadID");
		checkNotNull(minhashImpl, "minhashImpl");
		checkNotNull(sketchDBlocation, "sketchDBlocation");
		checkNoNullsInCollection(filters, "filters");
		checkNotNull(namespaceYAML, "namespaceYAML");
		checkNotNull(sequenceMetaJSONLines, "sequenceMetaJSONLines");
		final NamespaceLoadInfo nsinfo = loadNameSpaceInfo(namespaceYAML);
		final MinHashSketchDatabase sketchDB = minhashImpl.getDatabase(
				new MinHashSketchDBName(nsinfo.getId().getName()), sketchDBlocation);
		final Set<String> seqmetaIDs = loadSequenceMetaIDs(sequenceMetaJSONLines);
		final Set<String> skdbIDs = new HashSet<>(minhashImpl.getSketchIDs(sketchDB));
		validateSequenceIDs(filters, nsinfo, skdbIDs);
		checkEqual(seqmetaIDs, sequenceMetaJSONLines, skdbIDs, sketchDBlocation);
		loadSeqs(nsinfo.getId(), loadID, sequenceMetaJSONLines);
		//TODO CODE set same load time for all seqs as option
		storage.createOrReplaceNamespace(nsinfo.toNamespace(sketchDB, loadID, clock.instant()));
	}

	private void validateSequenceIDs(
			final Set<MinHashDistanceFilterFactory> filters,
			final NamespaceLoadInfo nsinfo,
			final Set<String> sequenceIDs)
			throws LoadInputParseException {
		if (!nsinfo.getFilterID().isPresent()) {
			return;
		}
		final FilterID filterID = nsinfo.getFilterID().get();
		MinHashDistanceFilterFactory filter = null;
		for (final MinHashDistanceFilterFactory f: filters) {
			if (f.getID().equals(filterID)) {
				filter = f;
			}
		}
		if (filter == null) {
			throw new LoadInputParseException(String.format(
					"Filter ID %s is specified, but no filter with that ID is configured",
					filterID.getName()));
		}
		for (final String sid: sequenceIDs) {
			if (!filter.validateID(sid)) {
				throw new LoadInputParseException(String.format(
						"Filter %s reports that sequence ID %s is not valid",
						filterID.getName(), sid));
			}
		}
	}

	private void loadSeqs(
			final NamespaceID namespaceID,
			final LoadID loadID,
			final Restreamable sequenceMetaJSONLines)
			throws IOException, LoadInputParseException, AssemblyHomologyStorageException {
		try (final InputStream is = sequenceMetaJSONLines.getInputStream()) {
			final BufferedReader br = new BufferedReader(
					new InputStreamReader(is, StandardCharsets.UTF_8));
			List<SequenceMetadata> metas = new ArrayList<>(100);
			int count = 1;
			Instant time = clock.instant();
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				metas.add(new SeqMetaLoadInfo(
						line, sequenceMetaJSONLines.getSourceInfo() + " line " + count)
						.toSequenceMetadata(time));
				if (metas.size() >= 100) {
					storage.saveSequenceMetadata(namespaceID, loadID, metas);
					metas = new ArrayList<>(100);
					time = clock.instant();
				}
				count++;
			}
			if (!metas.isEmpty()) {
				storage.saveSequenceMetadata(namespaceID, loadID, metas);
			}
		}
	}

	private void checkEqual(
			final Set<String> seqmetaIDs,
			final Restreamable seqmetaStream,
			final Set<String> skdbIDs,
			final MinHashDBLocation sdkDBLoc)
			throws LoadInputParseException {
		if (!seqmetaIDs.equals(skdbIDs)) {
			final String idspace;
			List<String> extra = get3(subtract(seqmetaIDs, skdbIDs));
			if (extra.isEmpty()) {
				extra = get3(subtract(skdbIDs, seqmetaIDs));
				final Optional<Path> p = sdkDBLoc.getPathToFile();
				idspace = p.isPresent() ? p.get().toString() : "the sketch database";
			} else {
				idspace = seqmetaStream.getSourceInfo();
			}
			throw new LoadInputParseException(String.format(
					"IDs in the sketch database and sequence metadata file don't match. " +
					"For example, %s has extra IDs %s", idspace, extra));
		}
	}

	private Set<String> subtract(final Set<String> term1, final Set<String> term2) {
		final Set<String> metaExtra = new HashSet<>(term1);
		metaExtra.removeAll(term2);
		return metaExtra;
	}

	private <T> List<T> get3(final Collection<T> strings) {
		return new TreeSet<>(strings).stream().limit(3).collect(Collectors.toList());
	}

	private Set<String> loadSequenceMetaIDs(final Restreamable sequenceMetaYAML)
			throws IOException, LoadInputParseException {
		final Set<String> ids = new HashSet<>();
		try (final InputStream is = sequenceMetaYAML.getInputStream()) {
			final BufferedReader br = new BufferedReader(
					new InputStreamReader(is, StandardCharsets.UTF_8));
			int count = 1;
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				ids.add(new SeqMetaLoadInfo(
						line, sequenceMetaYAML.getSourceInfo() + " line " + count).getId());
				count++;
			}
		}
		return ids;
	}

	private NamespaceLoadInfo loadNameSpaceInfo(final Restreamable namespaceYAML)
			throws IOException, LoadInputParseException {
		try (final InputStream is = namespaceYAML.getInputStream()) {
			final BufferedInputStream bis = new BufferedInputStream(is);
			return new NamespaceLoadInfo(bis, namespaceYAML.getSourceInfo());
		}
	}

}
