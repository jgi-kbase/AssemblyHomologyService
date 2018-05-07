package us.kbase.assemblyhomology.service.api;

public class ServicePaths {

	//TODO JAVADOC
	
	/* general strings */

	/** The URL path separator. */
	public static final String SEP = "/";
	
	private static final String NAMESPACE = "namespace";
	private static final String NAMESPACE_ID = "{" + NAMESPACE + "}";
	
	
	/* Root endpoint */

	/** The root endpoint location. */
	public static final String ROOT = SEP;
	
	/* Namespaces */
	
	/** The root namespace location. */
	public static final String NAMESPACE_ROOT = SEP + NAMESPACE;
	/** The location for a specific namespace */
	public static final String NAMESPACE_SELECT = NAMESPACE_ID;
	public static final String NAMESPACE_SELECT_PARAM = NAMESPACE;
	
}
