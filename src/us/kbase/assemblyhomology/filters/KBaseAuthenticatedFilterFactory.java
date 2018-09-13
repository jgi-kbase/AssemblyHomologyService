package us.kbase.assemblyhomology.filters;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.isNullOrEmpty;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.github.zafarkhaja.semver.Version;
import com.google.common.base.Optional;

import us.kbase.assemblyhomology.core.FilterID;
import us.kbase.assemblyhomology.core.MinHashDistanceFilterFactory;
import us.kbase.assemblyhomology.core.Token;
import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MinHashFilterFactoryInitializationException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.minhash.MinHashDistance;
import us.kbase.assemblyhomology.minhash.MinHashDistanceCollector;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashDistanceFilterAuthenticationException;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashDistanceFilterException;
import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonClientException;
import us.kbase.common.service.UnauthorizedException;
import us.kbase.workspace.ListWorkspaceIDsParams;
import us.kbase.workspace.ListWorkspaceIDsResults;
import us.kbase.workspace.WorkspaceClient;

/** A factory for an authenticated KBase filter. The filter, once built, inspects the sequence
 * IDs in the provided {@link MinHashDistance} instances
 * (see {@link KBaseAuthenticatedFilter#accept(MinHashDistance)}
 * and determines if the user identified by the supplied token is authorized to view the sequence.
 * If not, the filter does not pass the distance on to the collector.
 * 
 * At the time of filter creation, the KBase workspace is contacted to get the workspace IDs to
 * which the user has access, including public workspaces. The filter expects sequence IDs in the
 * format X_Y_Z, where X is the integer workspace ID, Y the integer object ID, and Z the integer
 * version.
 * @author gaprice@lbl.gov
 *
 */
public class KBaseAuthenticatedFilterFactory implements MinHashDistanceFilterFactory {

	//note this is tested in an integration test as it requires the KBase workspace to function.

	private static final List<String> ENVS = Collections.unmodifiableList(Arrays.asList(
			"prod", "appdev", "next", "ci"));
	
	private static final String CONFIG_ENV = "env";
	private static final String CONFIG_URL = "url";
	
	private final FilterID id;
	private final URL url;
	private final boolean insecure;
	
	/** Create the factory. The factory accepts two configuration parameters - 'url', which must
	 * be the url of a KBase workspace service, and 'env', which identifies the KBase environment
	 * (one of prod, appdev, next, or ci) the filter is associated with. This allows for
	 * a single Assembly Homology service to be used for multiple KBase environments, if
	 * desired. If omitted, the default of 'prod' is used.
	 * @param config the filter configuration.
	 * @throws MinHashFilterFactoryInitializationException if an initialization error occurs.
	 */
	public KBaseAuthenticatedFilterFactory(final Map<String, String> config)
			throws MinHashFilterFactoryInitializationException {
		checkNotNull(config, "config");
		final String env;
		if (config.containsKey(CONFIG_ENV)) {
			env = config.get(CONFIG_ENV);
			if (!ENVS.contains(env)) {
				throw new MinHashFilterFactoryInitializationException(
						"Illegal KBase filter environment value: " + env);
			}
		} else {
			env = ENVS.get(0);
		}
		try {
			id = new FilterID("kbase" + env);
		} catch (MissingParameterException | IllegalParameterException e) {
			throw new RuntimeException("This should be impossible", e);
		}
		url = getURL(config);
		if (!url.getProtocol().equals("https")) {
			insecure = true;
			// logging is checked manually. Check that logging occurs if you make changes here
			LoggerFactory.getLogger(getClass()).info(String.format(
					"Workspace url %s is insecure. It is strongly recommended to use https.",
					url));
		} else {
			// can't really test this easily
			insecure = false;
		}
	}

	private URL getURL(final Map<String, String> config)
			throws MinHashFilterFactoryInitializationException {
		final String surl = config.get(CONFIG_URL);
		if (isNullOrEmpty(surl)) {
			throw new MinHashFilterFactoryInitializationException(
					"KBase filter requires key 'url' in config");
		}
		final URL u;
		try {
			u = new URL(surl);
		} catch (MalformedURLException e) {
			throw new MinHashFilterFactoryInitializationException(
					"KBase filter url malformed: " + surl);
		}
		final String ver;
		try {
			final WorkspaceClient cli = new WorkspaceClient(u);
			cli.setIsInsecureHttpConnectionAllowed(true); // ok since no token
			ver = cli.ver();
		} catch (JsonClientException | IOException e) {
			// not sure how a jsonclient exception could actually happen here
			throw new MinHashFilterFactoryInitializationException(String.format(
					"KBase filter failed contacting workspace at url %s: %s",
					u, e.getMessage()), e);
		}
		if (Version.valueOf(ver).lessThan(Version.valueOf("0.8.0"))) {
			// this is annoying to test. Just test manually by bumping the version above
			// and looking for appropriate test failures
			throw new MinHashFilterFactoryInitializationException(
					"KBase filter requires workspace version >= 0.8.0, was " + ver);
		}
		return u;
	}
	
	@Override
	public FilterID getID() {
		return id;
	}

	@Override
	public Optional<String> getAuthSource() {
		return Optional.of(id.getName());
	}

	@Override
	public KBaseAuthenticatedFilter getFilter(
			final MinHashDistanceCollector collector,
			final Token token)
			throws MinHashDistanceFilterException {
		final WorkspaceClient cli;
		try {
			if (token == null) {
				cli = new WorkspaceClient(url);
			} else {
				cli = new WorkspaceClient(url, new AuthToken(token.getToken(), "fakename"));
			}
		} catch (IOException | UnauthorizedException e) {
			// if you look at the current ws client code these exceptions are impossible
			throw new MinHashDistanceFilterException("This should never happen", e);
		}
		cli.setIsInsecureHttpConnectionAllowed(insecure);
		final ListWorkspaceIDsResults lids;
		try {
			lids = cli.listWorkspaceIds(new ListWorkspaceIDsParams().withExcludeGlobal(0L));
		} catch (JsonClientException | IOException e) {
			if (e.getMessage().contains(
					// hacky hacky hacky
					// ws needs error codes
					// and we need to rewrite the auth client for auth2
					"Login failed! Server responded with code 401 Unauthorized")) {
				throw new MinHashDistanceFilterAuthenticationException("Invalid token");
			} else {
				// this is annoying to test, so pass
				throw new MinHashDistanceFilterException(e.getMessage(), e);
			}
		}
		final Set<Long> ids = new HashSet<>(lids.getWorkspaces());
		ids.addAll(lids.getPub());
		return new KBaseAuthenticatedFilter(ids, collector);
	}

	@Override
	public boolean validateID(final String id) {
		//TODO NOW validate id
		return false;
	}

}
