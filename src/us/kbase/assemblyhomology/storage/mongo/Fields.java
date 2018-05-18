package us.kbase.assemblyhomology.storage.mongo;

/** This class defines the field names used in MongoDB documents for storing assembly homology
 * data.
 * @author gaprice@lbl.gov
 *
 */
public class Fields {
	
	/** The separator between mongo field names. */
	public static final String FIELD_SEP = ".";

	/** The key for the MongoDB ID in documents. */
	public static final String MONGO_ID = "_id";
	
	/* ***********************
	 * namespace fields
	 * ***********************
	 */

	/** The ID for the namespace. */
	public static final String NAMESPACE_ID = "id";
	/** The load ID for the currently active load in the namespace. */
	public static final String NAMESPACE_LOAD_ID = "load";
	/** The ID of the source of the data in the namespace. */
	public static final String NAMESPACE_DATASOURCE_ID = "dsid";
	/** The date of creation of the namespace record in mongo. */
	public static final String NAMESPACE_CREATION_DATE = "create";
	/** The ID of the database at the data source from which the data in the namespace came. */
	public static final String NAMESPACE_DATABASE_ID = "dbid";
	/** A free text description of the data in the namespace. */
	public static final String NAMESPACE_DESCRIPTION = "desc";
	/** The name of the MinHash implementation used to create the sketches in the namespace. */
	public static final String NAMESPACE_IMPLEMENTATION = "impl";
	/** The version of the MinHash implementation used to create the sketches in the namespace. */
	public static final String NAMESPACE_IMPLEMENTATION_VERSION = "impver";
	/** The kmer size(s) of the sketches in the sketch database associated with the namespace. */
	public static final String NAMESPACE_KMER_SIZE = "kmer";
	/** The size of the sketches in the sketch database associated with the namespace. */
	public static final String NAMESPACE_SKETCH_SIZE = "sksz";
	/** The scaling factor of the sketches in the sketch database associated with the namespace.
	 */
	public static final String NAMESPACE_SCALING_FACTOR = "scle";
	/** The path to the sketch database associated with the namespace. */
	public static final String NAMESPACE_SKETCH_DB_PATH = "path";
	/** The number of sequences in the sketch database associated with the namespace. */
	public static final String NAMESPACE_SEQUENCE_COUNT = "seqcnt";
	
	/* ***********************
	 * sequence metadata fields
	 * ***********************
	 */
	/** The namespace to which the sequence metadata belongs. */
	public static final String SEQMETA_NAMESPACE_ID = "nsid";
	/** The load ID to which the sequence metadata belongs. */
	public static final String SEQMETA_LOAD_ID = "load";
	/** The creation date of the sequence metadata record in mongo. */
	public static final String SEQMETA_CREATION_DATE = "create";
	/** The id of the sequence in the sketch database. */
	public static final String SEQMETA_SEQUENCE_ID = "seqid";
	/** The id of the sequence at the data source. */
	public static final String SEQMETA_SOURCE_ID = "srcid";
	/** The scientific name of the species / strain to which the sequence belongs. */
	public static final String SEQMETA_SCIENTIFIC_NAME = "scinm";
	/** Any related IDs associated with the sequence. */
	public static final String SEQMETA_RELATED_IDS = "relids";
	
	
	/* ***********************
	 * database schema fields
	 * ***********************
	 */
	
	/** The key for the schema field. The key and value are used to ensure there is
	 * never more than one schema record.
	 */
	public static final String DB_SCHEMA_KEY = "schema";
	/** The value for the schema field. The key and value are used to ensure there is
	 * never more than one schema record.
	 */
	public static final String DB_SCHEMA_VALUE = "schema";
	/** Whether the database schema is in the process of being updated. */
	public static final String DB_SCHEMA_UPDATE = "inupdate";
	/** The version of the database schema. */
	public static final String DB_SCHEMA_VERSION = "schemaver";

}
