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

@Path(ServicePaths.ROOT)
public class Root {
	
	//TODO ZLATER ROOT add configurable server name
	//TODO ZLATER ROOT add paths to endpoints
	//TODO ZLATER ROOT add configurable contact email or link
	
	//TODO TEST
	
	//TODO JAVADOC or swagger
	
	private static final String VERSION = "0.1.0";
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, Object> rootJSON() {
		return root();
	}
	
	private Map<String, Object> root() {
		return ImmutableMap.of(
				Fields.VERSION, VERSION,
				Fields.SERVER_TIME, Instant.now().toEpochMilli(),
				Fields.GIT_HASH, GitCommit.COMMIT);
	}

}
