package us.kbase.assemblyhomology.service.api;

/** Paths to service endpoints.
 * @author gaprice@lbl.gov
 *
 */
public class ServicePaths {

	/* general strings */

	/** The URL path separator. */
	public static final String SEP = "/";
	
	private static final String NAMESPACE = "namespace";
	private static final String NAMESPACE_ID = "{" + NAMESPACE + "}";
	private static final String SEARCH = "search";
	
	
	/* Root endpoint */

	/** The root endpoint location. */
	public static final String ROOT = SEP;
	
	/* Namespaces */
	
	/** The root namespace location. */
	public static final String NAMESPACE_ROOT = SEP + NAMESPACE;
	/** The location for a specific namespace. */
	public static final String NAMESPACE_SELECT = NAMESPACE_ID;
	/** The parameter name for the namespace selector portion of the url. */
	public static final String NAMESPACE_SELECT_PARAM = NAMESPACE;
	/** The location for searching a namespace with a sketch file. */
	public static final String NAMESPACE_SEARCH = NAMESPACE_SELECT + SEP + SEARCH;
	
	
}
