package us.kbase.assemblyhomology.service.api;

import java.time.Instant;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableMap;

import us.kbase.assemblyhomology.GitCommit;
import us.kbase.assemblyhomology.service.Fields;

/** The root of the server - returns basic information about the service, like
 * the server name, the version, the server local time, and the git hash from the build.
 * @author gaprice@lbl.gov
 *
 */
@Path(ServicePaths.ROOT)
public class Root {
	
	//TODO ZLATER ROOT add configurable server name
	//TODO ZLATER ROOT add paths to endpoints
	//TODO ZLATER ROOT add configurable contact email or link
	//TODO ZLATER swagger
	
	private static final String VERSION = "0.1.1-dev1";
	private static final String SERVER_NAME = "Assembly Homology service";
	
	/** Return the root information.
	 * @return the root information.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> rootJSON() {
		return ImmutableMap.of(
				Fields.SERVER_NAME, SERVER_NAME,
				Fields.VERSION, VERSION,
				Fields.SERVER_TIME, Instant.now().toEpochMilli(),
				Fields.GIT_HASH, GitCommit.COMMIT);
	}

}
