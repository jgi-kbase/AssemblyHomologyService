package us.kbase.test.assemblyhomology.load;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import us.kbase.assemblyhomology.load.ParseHelpers;
import us.kbase.assemblyhomology.load.exceptions.LoadInputParseException;
import us.kbase.test.assemblyhomology.MapBuilder;
import us.kbase.test.assemblyhomology.TestCommon;

public class ParseHelpersTest {

	private static final String YAML =
			"foo: bar\n" +
			"list:\n" +
			"    - item 1\n" +
			"    - item 2\n";
	
	private static final String YAML_BAD =
			"bleah\n" + 
			"foo: bar\n" +
			"list:\n" +
			"    - item 1\n" +
			"    - item 2\n";
	
	@Test
	public void parseYAMLInputStream() throws Exception {
		final Object yaml = ParseHelpers.fromYAML(
				new ByteArrayInputStream(YAML.getBytes()), "some file");
		
		final Object expected = ImmutableMap.of(
				"foo", "bar",
				"list", Arrays.asList("item 1", "item 2"));
		
		assertThat("incorrect yaml", yaml, is(expected));
	}
	
	@Test
	public void parseYAMLString() throws Exception {
		final Object yaml = ParseHelpers.fromYAML(YAML, "some file");
		
		final Object expected = ImmutableMap.of(
				"foo", "bar",
				"list", Arrays.asList("item 1", "item 2"));
		
		assertThat("incorrect yaml", yaml, is(expected));
	}
	
	@Test
	public void parseYAMLInputStreamFail() throws Exception {
		failParseYAML((InputStream) null, "s", new NullPointerException("input"));
		failParseYAML(new ByteArrayInputStream("s".getBytes()), null,
				new IllegalArgumentException("sourceInfo cannot be null or whitespace only"));
		failParseYAML(new ByteArrayInputStream("s".getBytes()), "   \t    \n   ",
				new IllegalArgumentException("sourceInfo cannot be null or whitespace only"));
		failParseYAML(new ByteArrayInputStream(YAML_BAD.getBytes()), "mysource",
				new LoadInputParseException(
						"Error parsing source mysource: class " +
						"org.yaml.snakeyaml.scanner.ScannerException mapping values are not " +
						"allowed here\n in 'reader', " +
						"line 2, column 4:\n    foo: bar\n       ^\n"));
	}
	
	@Test
	public void parseYAMLStringFail() throws Exception {
		failParseYAML((String) null, "s", new IllegalArgumentException(
				"input cannot be null or whitespace only"));
		failParseYAML("   \t   \n  ", "s", new IllegalArgumentException(
				"input cannot be null or whitespace only"));
		failParseYAML("s", null,
				new IllegalArgumentException("sourceInfo cannot be null or whitespace only"));
		failParseYAML("s", "   \t    \n   ",
				new IllegalArgumentException("sourceInfo cannot be null or whitespace only"));
		failParseYAML(YAML_BAD, "mysource",
				new LoadInputParseException(
						"Error parsing source mysource: class " +
						"org.yaml.snakeyaml.scanner.ScannerException mapping values are not " +
						"allowed here\n in 'string', " +
						"line 2, column 4:\n    foo: bar\n       ^\n"));
	}
	
	private void failParseYAML(
			final InputStream input,
			final String sourceInfo,
			final Exception expected) {
		try {
			final Object yaml = ParseHelpers.fromYAML(input, sourceInfo);
			System.out.println(yaml);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	private void failParseYAML(
			final String input,
			final String sourceInfo,
			final Exception expected) {
		try {
			final Object yaml = ParseHelpers.fromYAML(input, sourceInfo);
			System.out.println(yaml);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void getString() throws Exception {
		final String res = ParseHelpers.getString(
				ImmutableMap.of("foo", "bar", "baz", Arrays.asList("bat")),
				"foo",
				"source",
				false);
		
		assertThat("incorrect string", res, is("bar"));
	}
	
	@Test
	public void getStringOptionalNull() throws Exception {
		final String res = ParseHelpers.getString(
				MapBuilder.<String, Object>newHashMap()
						.with("foo", null)
						.with("baz", Arrays.asList("bat"))
						.build(),
				"foo",
				"source",
				true);
		
		assertThat("incorrect string", res, is((String) null));
	}
	
	@Test
	public void getStringOptionalWhitespace() throws Exception {
		final String res = ParseHelpers.getString(
				MapBuilder.<String, Object>newHashMap()
					.with(null, "  \t  \n  ")
					.with("baz", Arrays.asList("bat"))
					.build(),
				null,
				"source",
				true);
		
		assertThat("incorrect string", res, is((String) null));
	}
	
	@Test
	public void getStringFail() {
		failGetString(null, "k", "s", true, new NullPointerException("map"));
		
		failGetString(ImmutableMap.of(1, "whee", 2, "foo"), 3, "mysource", false,
				new LoadInputParseException("Missing value at 3. Source: mysource"));
		failGetString(ImmutableMap.of(1, "whee", 2, "foo"), 3, null, false,
				new LoadInputParseException("Missing value at 3."));
		failGetString(ImmutableMap.of(1, "whee", 2, "foo"), 3, "  \t  ", false,
				new LoadInputParseException("Missing value at 3."));
		
		failGetString(ImmutableMap.of(1, "whee", 2, Arrays.asList("foo")), 2, "mys", true,
				new LoadInputParseException("Expected string, got [foo] at 2. Source: mys"));
		failGetString(ImmutableMap.of(1, "whee", 2, Arrays.asList("foo")), 2, null, true,
				new LoadInputParseException("Expected string, got [foo] at 2."));
		failGetString(ImmutableMap.of(1, "whee", 2, Arrays.asList("foo")), 2, "  \t  ", true,
				new LoadInputParseException("Expected string, got [foo] at 2."));
		
		final Map<String, Object> input = MapBuilder.<String, Object>newHashMap()
				.with("foo", "  \t  \n ")
				.with("bar", "baz")
				.build();
		failGetString(input, "foo", "mys", false,
				new LoadInputParseException("Missing value at foo. Source: mys"));
		failGetString(input, "foo", null, false,
				new LoadInputParseException("Missing value at foo."));
		failGetString(input, "foo", "   \t  ", false,
				new LoadInputParseException("Missing value at foo."));
		
		
	}
	
	private void failGetString(
			final Map<?, Object> map,
			final Object key,
			final String sourceInfo,
			final boolean optional,
			final Exception expected) {
		try {
			ParseHelpers.getString(map, key, sourceInfo, optional);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
