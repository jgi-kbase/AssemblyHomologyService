package us.kbase.assemblyhomology.service;

/** Field names used in incoming and outgoing data structures.
 * @author gaprice@lbl.gov
 *
 */
public class Fields {

	/* root */
	
	/** The server name. */
	public static final String SERVER_NAME = "servname";
	/** The version of the service. */
	public static final String VERSION = "version";
	/** The time, in milliseconds since the epoch, at the service. */
	public static final String SERVER_TIME = "servertime";
	/** The Git commit from which the service was built. */
	public static final String GIT_HASH = "gitcommithash";
	
	/* namespaces */
	
	/** An ID for a namespace. */
	public static final String NAMESPACE_ID = "id";
	/** A description for a namespace. */
	public static final String NAMESPACE_DESCRIPTION = "desc";
	/** The MinHash implementation used to create the sketch database associated with a namespace.
	 */
	public static final String NAMESPACE_IMPLEMENTATION = "impl";
	/** The number of sequences in a namespace sketch database. */
	public static final String NAMESPACE_SEQ_COUNT = "seqcount";
	/** The kmer size of the namespace sketch database. */
	public static final String NAMESPACE_KMER_SIZE = "kmersize";
	/** The scaling parameter for a namespace sketch database. */
	public static final String NAMESPACE_SCALING = "scaling";
	/** The size of the sketches in a namespace sketch database. */
	public static final String NAMESPACE_SKETCH_SIZE = "sketchsize";
	/** The ID of the data source where the sequences in a namespace sketch database originated. */
	public static final String NAMESPACE_DATA_SOURCE_ID = "datasource";
	/** The ID of the database within the data source where the sequences in a namespace
	 * sketch database originated.
	 */
	public static final String NAMESPACE_DB_ID = "database";
	
	/* Distances */
	/** A set of MinHash distances from a query sequence to one or more reference sequences. */
	public static final String DISTANCES = "distances";
	/** The MinHash implementation used to calculate the distances. */
	public static final String DIST_IMPLEMENTATION = "impl";
	/** The version of the MinHash implementation used to calculate the distances. */
	public static final String DIST_IMPLEMENTATION_VERSION = "implver";
	/** The Minhash distance between two sequences. */
	public static final String DIST_DISTANCE = "dist";
	/** A set of namespaces. */
	public static final String DIST_NAMESPACES = "namespaces";
	/** A namespace ID. */
	public static final String DIST_NAMESPACE_ID = "namespaceid";
	/** IDs that are not the primary ID of a sequence but are IDs related to that sequence, e.g.
	 * the NCBI ID.
	 */
	public static final String DIST_RELATED_IDS = "relatedids";
	/** The scientific name of the organism associated with the sequence. */
	public static final String DIST_SCI_NAME = "sciname";
	/** The source ID of the sequence. */
	public static final String DIST_SOURCE_ID = "sourceid";
	/** Warnings regarding the distances calculated by a MinHash implementation. */
	public static final String DIST_WARNINGS = "warnings";
	
	/* errors */
	
	/** An error. */
	public static final String ERROR = "error";
	
	
}
