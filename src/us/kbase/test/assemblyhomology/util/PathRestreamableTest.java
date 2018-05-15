package us.kbase.test.assemblyhomology.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import us.kbase.assemblyhomology.util.FileOpener;
import us.kbase.assemblyhomology.util.PathRestreamable;
import us.kbase.test.assemblyhomology.TestCommon;

public class PathRestreamableTest {
	
	@Test
	public void getSourceInfo() {
		final FileOpener fo = mock(FileOpener.class);
		
		final PathRestreamable pr = new PathRestreamable(Paths.get("some_file"), fo);
		
		assertThat("incorrect source info", pr.getSourceInfo(), is("some_file"));
	}
	
	@Test
	public void stream() throws Exception {
		final FileOpener fo = mock(FileOpener.class);
		
		when(fo.open(Paths.get("some_file"))).thenReturn(
				new ByteArrayInputStream("foo1".getBytes()),
				new ByteArrayInputStream("foo2".getBytes()));
		
		final PathRestreamable pr = new PathRestreamable(Paths.get("some_file"), fo);
		
		final String res = IOUtils.toString(pr.getInputStream());
		assertThat("incorrect stream contents", res, is("foo1"));
		
		final String res2 = IOUtils.toString(pr.getInputStream());
		assertThat("incorrect stream contents", res2, is("foo2"));
	}
	
	@Test
	public void constructFail() {
		final FileOpener fo = mock(FileOpener.class);
		failConstruct(null, fo, new NullPointerException("input"));
		failConstruct(Paths.get("foo"), null, new NullPointerException("fileOpener"));
	}

	private void failConstruct(final Path path, final FileOpener fo, final Exception expected) {
		try {
			new PathRestreamable(path, fo);
			fail("expected exception");
		} catch (Exception got) {
			TestCommon.assertExceptionCorrect(got, expected);
		}
	}

}
