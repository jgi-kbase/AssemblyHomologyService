package us.kbase.assemblyhomology.load;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.google.common.collect.ImmutableMap;
import com.mongodb.MongoClient;

import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.core.SequenceMetadata;
import us.kbase.assemblyhomology.load.exceptions.LoadInputParseException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementation;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashException;
import us.kbase.assemblyhomology.minhash.mash.Mash;
import us.kbase.assemblyhomology.storage.AssemblyHomologyStorage;
import us.kbase.assemblyhomology.storage.exceptions.AssemblyHomologyStorageException;
import us.kbase.assemblyhomology.storage.mongo.MongoAssemblyHomologyStorage;
import us.kbase.assemblyhomology.util.Restreamable;

public class Loader {
	
	//TODO TEST
	//TODO JAVADOC
	
	private final AssemblyHomologyStorage storage;
	private final Clock clock;
	
	public Loader(final AssemblyHomologyStorage storage) {
		this(storage, Clock.systemUTC());
	}
	
	// for tests
	private Loader(final AssemblyHomologyStorage storage, final Clock clock) {
		checkNotNull(storage, "storage");
		this.storage = storage;
		this.clock = clock;
	}
	
	// LoadParams builder?
	public void load(
			final LoadID loadID,
			final MinHashImplementation minhashImpl,
			final MinHashDBLocation sketchDBlocation,
			final Restreamable namespaceYAML,
			final Restreamable sequenceMetaYAML)
			throws MinHashException, IOException, LoadInputParseException,
				AssemblyHomologyStorageException {
		checkNotNull(loadID, "loadID");
		checkNotNull(minhashImpl, "minhashImpl");
		checkNotNull(sketchDBlocation, "sketchDBlocation");
		checkNotNull(namespaceYAML, "namespaceYAML");
		checkNotNull(sequenceMetaYAML, "sequenceMetaYAML");
		final MinHashSketchDatabase sketchDB = minhashImpl.getDatabase(sketchDBlocation);
		final NamespaceLoadInfo nsinfo = loadNameSpaceInfo(namespaceYAML);
		final Set<String> seqmetaIDs = loadSequenceMetaIDs(sequenceMetaYAML);
		final Set<String> skdbIDs = new HashSet<>(minhashImpl.getSketchIDs(sketchDB));
		checkEqual(seqmetaIDs, sequenceMetaYAML, skdbIDs, sketchDBlocation);
		loadSeqs(nsinfo.getId(), loadID, sequenceMetaYAML);
		storage.createOrReplaceNamespace(nsinfo.toNamespace(sketchDB, loadID, clock.instant()));
	}

	private void loadSeqs(
			final NamespaceID namespaceID,
			final LoadID loadID,
			final Restreamable sequenceMetaYAML)
			throws IOException, LoadInputParseException, AssemblyHomologyStorageException {
		final List<SequenceMetadata> metas = new ArrayList<>(100);
		try (final InputStream is = sequenceMetaYAML.getInputStream()) {
			final BufferedReader br = new BufferedReader(
					new InputStreamReader(is, StandardCharsets.UTF_8));
			int count = 1;
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				metas.add(new SeqMetaLoadInfo(
						line, sequenceMetaYAML.getSourceInfo() + " line " + count)
						.toSequenceMetadata(clock.instant()));
				if (metas.size() >= 100) {
					storage.saveSequenceMetadata(namespaceID, loadID, metas);
					metas.clear();
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
				idspace = sdkDBLoc.getPathToFile().toString();
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

	private List<String> get3(final Set<String> strings) {
		final List<String> ret = new LinkedList<>();
		for (final String s: strings) {
			if (ret.size() > 3) {
				break;
			}
			ret.add(s);
		}
		return ret;
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

	public static void main(final String[] args) throws Exception {
		final Map<String, String> nsdata = ImmutableMap.of(
				"id", "foo",
				"datasource", "KBase",
				"sourcedatabase", "Ci Refdata",
				"description", "some ref data");
		
		final DumperOptions dos = new DumperOptions();
		dos.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		
		final String nsyaml = new Yaml(dos).dump(nsdata);
		
		@SuppressWarnings("resource")
		final MongoClient mc = new MongoClient("localhost");
		final AssemblyHomologyStorage storage = new MongoAssemblyHomologyStorage(
				mc.getDatabase("assemblyhomology"));
		
		final Restreamable namespaceYAML = new Restreamable() {
			
			@Override
			public String getSourceInfo() {
				return "Namespace yaml file";
			}
			
			@Override
			public InputStream getInputStream() {
				return new ByteArrayInputStream(nsyaml.getBytes());
			}
		};
		
		final Restreamable sequenceMetaYAML = new Restreamable() {
			
			@Override
			public String getSourceInfo() {
				return "Seq meta yaml file";
			}
			
			@Override
			public InputStream getInputStream() throws IOException {
				return Files.newInputStream(Paths.get(
						"/home/crusherofheads/kb_refseq_sourmash/ref_assemblies_ci_1000_loadtest.jsonline"));
			}
		};
		
		new Loader(storage).load(
				new LoadID("my load id"),
				new Mash(Paths.get("./temp_delete")),
				new MinHashDBLocation(Paths.get(
						"/home/crusherofheads/kb_refseq_sourmash/kb_refseq_ci_1000.msh")),
				namespaceYAML,
				sequenceMetaYAML);
		
	}
	
}
