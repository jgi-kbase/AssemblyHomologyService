package us.kbase.assemblyhomology.minhash.mash;

import static com.google.common.base.Preconditions.checkNotNull;
import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import us.kbase.assemblyhomology.minhash.MinHashDBLocation;
import us.kbase.assemblyhomology.minhash.MinHashImplementationInformation;
import us.kbase.assemblyhomology.minhash.MinHashParameters;
import us.kbase.assemblyhomology.minhash.MinHashSketchDatabase;

public class MashSketchDatabase implements MinHashSketchDatabase {

	//TODO TEST
	//TODO JAVADOC

	private final MinHashImplementationInformation info;
	private final MinHashParameters parameterSet;
	private final MinHashDBLocation location;
	private final List<String> sketchIDs;
	
	public MashSketchDatabase(
			final MinHashImplementationInformation info,
			final MinHashParameters parameterSet,
			final MinHashDBLocation location,
			final List<String> sketchIDs) {
		checkNotNull(info, "info");
		checkNotNull(parameterSet, "parameterSet");
		checkNotNull(location, "location");
		checkNotNull(sketchIDs, "sketchIDs");
		for (final String id: sketchIDs) {
			exceptOnEmpty(id, "null or whitespace only id in sketchIDs");
		}
		this.info = info;
		this.parameterSet = parameterSet;
		this.location = location;
		this.sketchIDs = Collections.unmodifiableList(new ArrayList<>(sketchIDs));
	}

	@Override
	public MinHashImplementationInformation getImplementationInformation() {
		return info;
	}
	
	@Override
	public MinHashParameters getParameterSet() {
		return parameterSet;
	}

	@Override
	public MinHashDBLocation getLocation() {
		return location;
	}

	@Override
	public int getSketchCount() {
		return sketchIDs.size();
	}

	@Override
	public List<String> getSketchIDs() {
		return sketchIDs;
	}

}
