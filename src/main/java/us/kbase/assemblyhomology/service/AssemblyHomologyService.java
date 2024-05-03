package us.kbase.assemblyhomology.service;

import java.io.IOException;
import java.nio.file.Files;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.mongodb.MongoClient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import us.kbase.assemblyhomology.build.AssemblyHomologyBuilder;
import us.kbase.assemblyhomology.config.AssemblyHomologyConfig;
import us.kbase.assemblyhomology.config.AssemblyHomologyConfigurationException;
import us.kbase.assemblyhomology.core.AssemblyHomology;
import us.kbase.assemblyhomology.service.exceptions.ExceptionHandler;
import us.kbase.assemblyhomology.storage.exceptions.StorageInitException;

public class AssemblyHomologyService extends ResourceConfig {
	
	//TODO TEST
	//TODO JAVADOC
	
	private static MongoClient mc;
	@SuppressWarnings("unused")
	private final SLF4JAutoLogger logger; //keep a reference to prevent GC
	
	public AssemblyHomologyService()
			throws StorageInitException, AssemblyHomologyConfigurationException {
		//TODO ZLATER CONFIG Get the class name from environment & load if we need alternate config mechanism
		final AssemblyHomologyConfig cfg = new AssemblyHomologyConfig();
		
		quietLogger();
		logger = cfg.getLogger();
		try {
			Files.createDirectories(cfg.getPathToTemporaryFileDirectory());
		} catch (IOException e) {
			throw new AssemblyHomologyConfigurationException(e.getMessage(), e);
		}
		try {
			buildApp(cfg);
		} catch (StorageInitException e) {
			LoggerFactory.getLogger(getClass()).error(
					"Failed to initialize storage engine: " + e.getMessage(), e);
			throw e;
		}
	}

	private void quietLogger() {
		((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
				.setLevel(Level.INFO);
	}

	private void buildApp(
			final AssemblyHomologyConfig c)
			throws StorageInitException, AssemblyHomologyConfigurationException {
		final AssemblyHomologyBuilder ab;
		synchronized(this) {
			if (mc == null) {
				ab = new AssemblyHomologyBuilder(c);
				mc = ab.getMongoClient();
			} else {
				ab = new AssemblyHomologyBuilder(c, mc);
			}
		}
		packages("us.kbase.assemblyhomology.service.api");
		register(JacksonJaxbJsonProvider.class);
		register(LoggingFilter.class);
		register(ExceptionHandler.class);
		final AssemblyHomology ah = ab.getAssemblyHomology();
		register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(c).to(AssemblyHomologyConfig.class);
				bind(ah).to(AssemblyHomology.class);
				bind(c.getLogger()).to(SLF4JAutoLogger.class);
			}
		});
	}
	
	static void shutdown() {
		mc.close();
	}
}
