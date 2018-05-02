package us.kbase.assemblyhomology.storage.mongo;

/** This class defines the field names used in MongoDB documents for storing assembly homology
 * data.
 * @author gaprice@lbl.gov
 *
 */
public class Fields {
	
	//TODO JAVADOC

	/** The separator between mongo field names. */
	public static final String FIELD_SEP = ".";

	/** The key for the MongoDB ID in documents. */
	public static final String MONGO_ID = "_id";
	
	/* ***********************
	 * namespace fields
	 * ***********************
	 */
	public static final String NAMESPACE_ID = "id";
	public static final String NAMESPACE_LOAD_ID = "load";
	public static final String NAMESPACE_DATASOURCE_ID = "dsid";
	public static final String NAMESPACE_CREATION_DATE = "create";
	public static final String NAMESPACE_DATABASE_ID = "dbid";
	public static final String NAMESPACE_DESCRIPTION = "desc";
	public static final String NAMESPACE_IMPLEMENTATION = "impl";
	public static final String NAMESPACE_IMPLEMENTATION_VERSION = "impver";
	public static final String NAMESPACE_KMER_SIZE = "kmer";
	public static final String NAMESPACE_SKETCH_SIZE = "sksz";
	public static final String NAMESPACE_SCALING_FACTOR = "scle";
	public static final String NAMESPACE_SKETCH_DB_PATH = "path";
	public static final String NAMESPACE_SEQUENCE_COUNT = "seqcnt";
	
	/* ***********************
	 * sequence metadata fields
	 * ***********************
	 */
	public static final String SEQMETA_NAMESPACE_ID = "nsid";
	public static final String SEQMETA_LOAD_ID = "load";
	public static final String SEQMETA_CREATION_DATE = "create";
	public static final String SEQMETA_SEQUENCE_ID = "seqid";
	public static final String SEQMETA_SOURCE_ID = "srcid";
	public static final String SEQMETA_SCIENTIFIC_NAME = "scinm";
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
