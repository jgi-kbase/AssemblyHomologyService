package us.kbase.test.assemblyhomology.load;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;

import org.junit.Test;

import com.google.common.base.Optional;

import nl.jqno.equalsverifier.EqualsVerifier;
import us.kbase.assemblyhomology.core.DataSourceID;
import us.kbase.assemblyhomology.core.LoadID;
import us.kbase.assemblyhomology.core.Namespace;
import us.kbase.assemblyhomology.core.NamespaceID;
import us.kbase.assemblyhomology.load.NamespaceLoadInfo;
import us.kbase.assemblyhomology.load.exceptions.LoadInputParseException;
import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationName;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDBName;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;
import us.kbase.test.assemblyhomology.TestCommon;

public class NamespaceLoadInfoTest {
	
	// mock the db location to avoid creating files
	
	private static final String NS_MIN =
			"id: mynamespace\n" +
			"datasource: KBase\n";
	
	private static final String NS_MAX =
			"id: mynamespace\n" +
			"datasource: KBase\n" +
			"sourcedatabase: CI Refdata\n" +
			"description: some reference data\n";
	
	private static InputStream toStr(final String input) {
		return new ByteArrayInputStream(input.getBytes());
	}
	
	@Test
	public void equals() {
		EqualsVerifier.forClass(NamespaceLoadInfo.class).usingGetClass().verify();
	}
	
	@Test
	public void constructMinimal() throws Exception {
		final NamespaceLoadInfo ns = new NamespaceLoadInfo(toStr(NS_MIN), "s");
		
		assertThat("incorrect id", ns.getId(), is(new NamespaceID("mynamespace")));
		assertThat("incorrect id", ns.getDataSourceID(), is(new DataSourceID("KBase")));
		assertThat("incorrect id", ns.getDescription(), is(Optional.absent()));
		assertThat("incorrect id", ns.getSourceDatabaseID(), is(Optional.absent()));
	}
	
	@Test
	public void constructMaximal() throws Exception {
		final NamespaceLoadInfo ns = new NamespaceLoadInfo(toStr(NS_MAX), "s");
		
		assertThat("incorrect id", ns.getId(), is(new NamespaceID("mynamespace")));
		assertThat("incorrect id", ns.getDataSourceID(), is(new DataSourceID("KBase")));
		assertThat("incorrect id", ns.getDescription(), is(Optional.of("some reference data")));
		assertThat("incorrect id", ns.getSourceDatabaseID(), is(Optional.of("CI Refdata")));
	}
	
	@Test
	public void constructFail() {
		failConstruct(null, "s", new NullPointerException("input"));
		failConstruct(toStr(NS_MIN), null, new IllegalArgumentException(
				"sourceInfo cannot be null or whitespace only"));
		failConstruct(toStr(NS_MIN), "  \t   \n", new IllegalArgumentException(
				"sourceInfo cannot be null or whitespace only"));
		
		failConstruct(toStr("[]"), "mysource",
				new LoadInputParseException("Expected mapping at / in mysource"));
		
		failConstruct(toStr("datasource: KBase"), "mysource",
				new LoadInputParseException("Missing value at id. Source: mysource"));
		
		failConstruct(toStr("id: KBase"), "mysource",
				new LoadInputParseException("Missing value at datasource. Source: mysource"));
		
		failConstruct(toStr("id: foo&\ndatasource: KBase"), "mysource",
				new LoadInputParseException("Illegal namespace ID: foo&"));
		
		final String longStr = TestCommon.LONG1001.substring(0, 257);
		
		failConstruct(
				toStr("id: foo\ndatasource: " + longStr), "mysource",
				new LoadInputParseException("Illegal data source ID: " + longStr));
	}
	
	private void failConstruct(
			final InputStream input,
			final String sourceInfo,
			final Exception expected) {
		try {
			new NamespaceLoadInfo(input, sourceInfo);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

	@Test
	public void toNamespaceMinimal() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		final NamespaceLoadInfo nsli = new NamespaceLoadInfo(toStr(NS_MIN), "s");
		
		final Namespace ns = nsli.toNamespace(
				new MinHashSketchDatabase(
						new MinHashSketchDBName("mynamespace"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(3).withScaling(4).build(),
						loc,
						42),
				new LoadID("load 1"),
				Instant.ofEpochMilli(10000));
		
		assertThat("incorrect", ns.getCreation(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect", ns.getDescription(), is(Optional.absent()));
		assertThat("incorrect", ns.getID(), is(new NamespaceID("mynamespace")));
		assertThat("incorrect", ns.getLoadID(), is(new LoadID("load 1")));
		assertThat("incorrect", ns.getSketchDatabase(),
				is(new MinHashSketchDatabase(
					new MinHashSketchDBName("mynamespace"),
					new MinHashImplementationName("mash"),
					MinHashParameters.getBuilder(3).withScaling(4).build(),
					loc,
					42)));
		assertThat("incorrect", ns.getSourceDatabaseID(), is("default"));
		assertThat("incorrect", ns.getSourceID(), is(new DataSourceID("KBase")));
	}
	
	@Test
	public void toNamespaceMaximal() throws Exception {
		final MinHashDBLocation loc = mock(MinHashDBLocation.class);
		final NamespaceLoadInfo nsli = new NamespaceLoadInfo(toStr(NS_MAX), "s");
		
		final Namespace ns = nsli.toNamespace(
				new MinHashSketchDatabase(
						new MinHashSketchDBName("mynamespace"),
						new MinHashImplementationName("mash"),
						MinHashParameters.getBuilder(3).withScaling(4).build(),
						loc,
						42),
				new LoadID("load 1"),
				Instant.ofEpochMilli(10000));
		
		assertThat("incorrect", ns.getCreation(), is(Instant.ofEpochMilli(10000)));
		assertThat("incorrect", ns.getDescription(), is(Optional.of("some reference data")));
		assertThat("incorrect", ns.getID(), is(new NamespaceID("mynamespace")));
		assertThat("incorrect", ns.getLoadID(), is(new LoadID("load 1")));
		assertThat("incorrect", ns.getSketchDatabase(),
				is(new MinHashSketchDatabase(
					new MinHashSketchDBName("mynamespace"),
					new MinHashImplementationName("mash"),
					MinHashParameters.getBuilder(3).withScaling(4).build(),
					loc,
					42)));
		assertThat("incorrect", ns.getSourceDatabaseID(), is("CI Refdata"));
		assertThat("incorrect", ns.getSourceID(), is(new DataSourceID("KBase")));
	}
	
	@Test
	public void toNamespaceFail() throws Exception {
		final MinHashSketchDatabase db = new MinHashSketchDatabase(
				new MinHashSketchDBName("mynamespace"),
				new MinHashImplementationName("mash"),
				MinHashParameters.getBuilder(3).withScaling(4).build(),
				mock(MinHashDBLocation.class),
				42);
		final LoadID l = new LoadID("l");
		final Instant i = Instant.now();
		
		failToNamespace(null, l, i, new NullPointerException("sketchDB"));
		failToNamespace(db, null, i, new NullPointerException("loadID"));
		failToNamespace(db, l, null, new NullPointerException("creation"));
	}
	
	private void failToNamespace(
			final MinHashSketchDatabase db,
			final LoadID loadID,
			final Instant creation,
			final Exception expected) {
		try {
			new NamespaceLoadInfo(toStr(NS_MIN), "s").toNamespace(db, loadID, creation);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}
}
