package us.kbase.assemblyhomology.storage.exceptions;

/** 
 * Thrown when an exception occurs regarding initialization of the assembly homology storage system
 * @author gaprice@lbl.gov
 *
 */
public class StorageInitException extends AssemblyHomologyStorageException {

	private static final long serialVersionUID = 1L;
	
	public StorageInitException(String message) { super(message); }
	public StorageInitException(String message, Throwable cause) {
		super(message, cause);
	}
}
