package us.kbase.assemblyhomology.storage.exceptions;

/** 
 * Thrown when an exception occurs regarding the assembly homology storage system
 * @author gaprice@lbl.gov
 *
 */
public class AssemblyHomologyStorageException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public AssemblyHomologyStorageException(String message) { super(message); }
	public AssemblyHomologyStorageException(String message, Throwable cause) {
		super(message, cause);
	}
}
