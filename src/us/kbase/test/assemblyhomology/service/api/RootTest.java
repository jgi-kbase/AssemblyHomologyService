package us.kbase.test.assemblyhomology.service.api;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.assemblyhomology.service.api.Root;
import us.kbase.test.assemblyhomology.TestCommon;

public class RootTest {
	
	public static final String SERVER_VER = "0.1.1-dev3";
	private static final String GIT_ERR = 
			"Missing git commit file gitcommit, should be in us.kbase.assemblyhomology";
	
	@Test
	public void root() {
		final Map<String, Object> r = new HashMap<>(new Root().rootJSON());
		
		final long servertime = (long) r.get("servertime");
		r.remove("servertime");
		TestCommon.assertCloseToNow(servertime);
		
		final String gitcommit = (String) r.get("gitcommithash");
		r.remove("gitcommithash");
		assertGitCommitFromRootAcceptable(gitcommit);
		
		final Map<String, Object> expected = ImmutableMap.of(
				"version", SERVER_VER,
				"servname", "Assembly Homology service");
		
		assertThat("root json incorrect", r, is(expected));
	}
	
	public static void assertGitCommitFromRootAcceptable(final String gitcommit) {
		final boolean giterr = GIT_ERR.equals(gitcommit);
		final Pattern githash = Pattern.compile("[a-f\\d]{40}");
		final Matcher gitmatch = githash.matcher(gitcommit);
		final boolean gitcommitmatch = gitmatch.matches();
		
		assertThat("gitcommithash is neither an appropriate error nor a git commit: [" +
				gitcommit + "]",
				giterr || gitcommitmatch, is(true));
	}

}
