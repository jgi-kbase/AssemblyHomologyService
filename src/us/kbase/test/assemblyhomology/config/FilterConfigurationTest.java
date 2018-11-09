package us.kbase.test.assemblyhomology.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.config.FilterConfiguration;
import us.kbase.test.assemblyhomology.MapBuilder;
import us.kbase.test.assemblyhomology.TestCommon;

public class FilterConfigurationTest {
	
	@Test
	public void equals() {
		EqualsVerifier.forClass(FilterConfiguration.class).usingGetClass().verify();
	}
	
	@Test
	public void constructMinimal() {
		final FilterConfiguration c = new FilterConfiguration("classname", Collections.emptyMap());
		
		assertThat("incorrect classname", c.getFactoryClassName(), is("classname"));
		assertThat("incorrect config", c.getConfig(), is(Collections.emptyMap()));
	}
	
	@Test
	public void constructMaximal() {
		final FilterConfiguration c = new FilterConfiguration("otherclass",
				MapBuilder.<String, String>newHashMap()
				.with("foo", "bar").with("baz", null).with("bat", "   \t   ").build());
		
		assertThat("incorrect classname", c.getFactoryClassName(), is("otherclass"));
		assertThat("incorrect config", c.getConfig(), is(MapBuilder.<String, String>newHashMap()
				.with("foo", "bar").with("baz", null).with("bat", "   \t   ").build()));
	}
	
	@Test
	public void constructFail() {
		final Map<String, String> c = Collections.emptyMap();
		
		failConstruct(null, c, new IllegalArgumentException(
				"classname cannot be null or whitespace only"));
		failConstruct("   \t    ", c, new IllegalArgumentException(
				"classname cannot be null or whitespace only"));
		
		failConstruct("c", null, new NullPointerException("config"));
		failConstruct("c", MapBuilder.<String, String>newHashMap().with(null, "val").build(),
				new IllegalArgumentException("config key cannot be null or whitespace only"));
		failConstruct("c", MapBuilder.<String, String>newHashMap().with(" \t ", "val").build(),
				new IllegalArgumentException("config key cannot be null or whitespace only"));
	}
	
	private void failConstruct(
			final String classname,
			final Map<String, String> config,
			final Exception expected) {
		try {
			new FilterConfiguration(classname, config);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

	@Test
	public void immutable() {
		final Map<String, String> c = new HashMap<>();
		c.put("foo", "bar");
		
		final FilterConfiguration cfg = new FilterConfiguration("c", c);
		
		assertThat("incorrect config", cfg.getConfig(), is(ImmutableMap.of("foo", "bar")));
		c.put("baz", "bat");
		assertThat("incorrect config", cfg.getConfig(), is(ImmutableMap.of("foo", "bar")));
		
		try {
			cfg.getConfig().put("baz", "bat");
			fail("expected exception");
		} catch (UnsupportedOperationException e) {
			// test passed
		}
	}
	
}
