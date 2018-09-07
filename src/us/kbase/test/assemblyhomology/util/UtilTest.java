package us.kbase.test.assemblyhomology.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Test;

import us.kbase.assemblyhomology.core.exceptions.IllegalParameterException;
import us.kbase.assemblyhomology.core.exceptions.MissingParameterException;
import us.kbase.assemblyhomology.util.Util;
import us.kbase.test.assemblyhomology.TestCommon;

public class UtilTest {

	@Test
	public void exceptOnEmpty() {
		Util.exceptOnEmpty("s", "name"); // expect pass 
		
		exceptOnEmpty(null, "myname",
				new IllegalArgumentException("myname cannot be null or whitespace only"));
		exceptOnEmpty("  \n   ", "myname",
				new IllegalArgumentException("myname cannot be null or whitespace only"));
	}

	private void exceptOnEmpty(final String s, final String name, final Exception expected) {
		try {
			Util.exceptOnEmpty(s, name);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void isNullOrEmpty() {
		assertThat("incorrect null or empty", Util.isNullOrEmpty("s"), is(false));
		assertThat("incorrect null or empty", Util.isNullOrEmpty(null), is(true));
		assertThat("incorrect null or empty", Util.isNullOrEmpty("   \t    \n  "), is(true));
	}
	
	@Test
	public void checkString() throws Exception {
		Util.checkString("foo", "bar");
		Util.checkString(TestCommon.LONG1001, "name", 0);
		Util.checkString("ok", "name", 2);
		Util.checkString(" \n  ok   \t", "name", 2);
	}
	
	@Test
	public void checkStringFailMissingString() throws Exception {
		failCheckString(null, "foo", new MissingParameterException("foo"));
		failCheckString("    \n \t  ", "foo", new MissingParameterException("foo"));
		failCheckString(null, "foo", 10, new MissingParameterException("foo"));
		failCheckString("    \n \t  ", "foo", 10, new MissingParameterException("foo"));
	}

	@Test
	public void checkStringLengthFail() throws Exception {
		failCheckString("abc", "foo", 2,
				new IllegalParameterException("foo size greater than limit 2"));
	}
	
	@Test
	public void checkStringUnicodeAndLength() throws Exception {
		final String s = "ab𐎂c";
		assertThat("incorrect String length", s.length(), is(5));
		Util.checkString(s, "foo", 4);
		failCheckString(s, "foo", 3,
				new IllegalParameterException("foo size greater than limit 3"));
	}
	
	private void failCheckString(
			final String s,
			final String name,
			final Exception e) {
		try {
			Util.checkString(s, name);
			fail("check string failed");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, e);
		}
	}
	
	private void failCheckString(
			final String s,
			final String name,
			final int length,
			final Exception e) {
		try {
			Util.checkString(s, name, length);
			fail("check string failed");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, e);
		}
	}
	
	@Test
	public void noNullsCollection() throws Exception {
		Util.checkNoNullsInCollection(Arrays.asList("foo", "bar"), "whee"); // should work
	}
	
	@Test
	public void noNullsCollectionFail() throws Exception {
		failNoNullsCollection(null, "whee",
				new NullPointerException("whee"));
		failNoNullsCollection(new HashSet<>(Arrays.asList("foo", null, "bar")), "whee1",
				new NullPointerException("Null item in collection whee1"));
		failNoNullsCollection(Arrays.asList("foo", null, "bar"), "whee3",
				new NullPointerException("Null item in collection whee3"));
	}
	
	private void failNoNullsCollection(
			final Collection<?> col,
			final String name,
			final Exception expected) {
		try {
			Util.checkNoNullsInCollection(col, name);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
	
	@Test
	public void noNullsOrEmptiesCollection() throws Exception {
		Util.checkNoNullsOrEmpties(Arrays.asList("foo", "bar"), "whee"); // should work
	}
	
	@Test
	public void noNullsOrEmptiesCollectionFail() throws Exception {
		failNoNullsOrEmptiesCollection(null, "whee",
				new NullPointerException("whee"));
		failNoNullsOrEmptiesCollection(new HashSet<>(Arrays.asList("foo", null, "bar")), "whee1",
				new IllegalArgumentException(
						"Null or whitespace only string in collection whee1"));
		failNoNullsOrEmptiesCollection(new HashSet<>(Arrays.asList("foo", "   \n   \t   ", "bar")),
				"whee7", new IllegalArgumentException(
						"Null or whitespace only string in collection whee7"));
		failNoNullsOrEmptiesCollection(Arrays.asList("foo", null, "bar"), "whee6",
				new IllegalArgumentException(
						"Null or whitespace only string in collection whee6"));
		failNoNullsOrEmptiesCollection(Arrays.asList("foo", "   \n   \t   ", "bar"), "whee3",
				new IllegalArgumentException(
						"Null or whitespace only string in collection whee3"));
	}
	
	private void failNoNullsOrEmptiesCollection(
			final Collection<String> col,
			final String name,
			final Exception expected) {
		try {
			Util.checkNoNullsOrEmpties(col, name);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
