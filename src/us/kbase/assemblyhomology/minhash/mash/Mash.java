package us.kbase.assemblyhomology.minhash.mash;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplemenationInformation;
import us.kbase.assemblyhomology.minhash.MinHashImplementation;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashInitException;

public class Mash implements MinHashImplementation {
	
	//TODO JAVADOC
	//TODO TEST
	
	private final static String MASH = "mash";
	
	private final MinHashImplemenationInformation info;
	
	public Mash() throws MinHashInitException {
		checkMashExists();
		info = getInfo();
	}
	
	
	private void checkMashExists() throws MinHashInitException {
		try {
			final Process which = new ProcessBuilder("which", MASH).start();
			if (!which.waitFor(10L, TimeUnit.SECONDS)) {
				throw new MinHashInitException(String.format(
						"Timed out waiting for os to confirm %s is available on the system path",
						MASH));
			}
			// on ubuntu which returns 1 if the command isn't found
			if (which.exitValue() != 0) {
				//TODO LOG log this
				try (final InputStream is = which.getErrorStream()) {
					System.out.println(IOUtils.toString(is));
				}
				throw new MinHashInitException(String.format(
						"Error attempting to confirm %s is available on the system path", MASH));
			}
			try (final InputStream is = which .getInputStream()) {
				if (IOUtils.toString(is).isEmpty()) {
					throw new MinHashInitException(String.format(
							"%s is not available on the system path", MASH));
				}
			}
		} catch (IOException | InterruptedException e) {
			throw new MinHashInitException(String.format(
					"Error attempting to confirm %s is available on the system path: ", MASH) + 
					e.getMessage(), e);
		}
		
	}
	private MinHashImplemenationInformation getInfo() throws MinHashInitException {
		final String version;
		try {
			final Process mashHelp = new ProcessBuilder(MASH, "-h").start();
			if (!mashHelp.waitFor(10L, TimeUnit.SECONDS)) {
				throw new MinHashInitException(String.format(
						"Timed out waiting for %s to run", MASH));
			}
			if (mashHelp.exitValue() != 0) {
				//TODO LOG log this
				try (final InputStream is = mashHelp.getErrorStream()) {
					System.out.println(IOUtils.toString(is));
				}
				throw new MinHashInitException(String.format(
						"Error running %s", MASH));
			}
			try (final InputStream is = mashHelp.getInputStream()) {
				final String mashHelpOut = IOUtils.toString(is);
				version = getVersion(mashHelpOut);
			}
		} catch (IOException | InterruptedException e) {
			throw new MinHashInitException(String.format(
					"Error running %s:", MASH) + e.getMessage(), e);
		}
		return new MinHashImplemenationInformation(MASH, version);
	}
	
	private String getVersion(final String mashHelpOut) {
		//TODO CODE use regex later
		final String[] lines = mashHelpOut.split("\n");
		final String[] verline = lines[1].trim().split("\\s+");
		return verline[verline.length - 1].trim();
	}


	@Override
	public MinHashImplemenationInformation getImplementationInformation() {
		return info;
	}

	@Override
	public MinHashSketchDatabase getDatabase(final MinHashDBLocation location) {
		checkNotNull(location, "location");
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(final String[] args) throws MinHashInitException {
		final MinHashImplementation mash = new Mash();
		System.out.println(mash.getImplementationInformation());
	}

}
