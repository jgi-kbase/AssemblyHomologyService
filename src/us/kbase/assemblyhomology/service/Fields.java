package us.kbase.assemblyhomology.service;

/** Field names used in incoming and outgoing data structures.
 * @author gaprice@lbl.gov
 *
 */
public class Fields {

	//TODO JAVADOC
	
	/* root */
	
	/** The version of the service. */
	public static final String VERSION = "version";
	/** The time, in milliseconds since the epoch, at the service. */
	public static final String SERVER_TIME = "servertime";
	/** The Git commit from which the service was built. */
	public static final String GIT_HASH = "gitcommithash";
	
	/* namespaces */
	
	public static final String NAMESPACE = "namespace";
	public static final String NAMESPACE_DESCRIPTION = "desc";
	public static final String NAMESPACE_ID = "id";
	public static final String NAMESPACE_IMPLEMENTATION = "impl";
	public static final String NAMESPACE_SEQ_COUNT = "seqcount";
	public static final String NAMESPACE_KMER_SIZE = "kmersize";
	public static final String NAMESPACE_SCALING = "scaling";
	public static final String NAMESPACE_SKETCH_SIZE = "sketchsize";
	public static final String NAMESPACE_DB_ID = "database";
	public static final String NAMESPACE_DATA_SOURCE_ID = "datasource";
	
	/* Distances */
	public static final String DISTANCES = "distances";
	public static final String DIST_IMPLEMENTATION = "impl";
	public static final String DIST_IMPLEMENTATION_VERSION = "implver";
	public static final String DIST_DISTANCE = "dist";
	public static final String DIST_RELATED_IDS = "relatedids";
	public static final String DIST_SCI_NAME = "sciname";
	public static final String DIST_SOURCE_ID = "sourceid";
	public static final String DIST_WARNINGS = "warnings";
	
	/* errors */
	
	/** An error. */
	public static final String ERROR = "error";
	
	
}
