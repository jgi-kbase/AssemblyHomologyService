package us.kbase.test.assemblyhomology.util;

import com.google.common.base.Optional;

import us.kbase.assemblyhomology.core.FilterID;
import us.kbase.assemblyhomology.core.MinHashDistanceFilterFactory;
import us.kbase.assemblyhomology.core.Token;
import us.kbase.assemblyhomology.minhash.MinHashDistanceCollector;
import us.kbase.assemblyhomology.minhash.MinHashDistanceFilter;
import us.kbase.assemblyhomology.minhash.exceptions.MinHashDistanceFilterException;

public class LoadClassTestClassMissingConstructor implements MinHashDistanceFilterFactory {

	@Override
	public FilterID getID() {
		return null;
	}

	@Override
	public Optional<String> getAuthSource() {
		return null;
	}

	@Override
	public MinHashDistanceFilter getFilter(MinHashDistanceCollector collector, Token token)
			throws MinHashDistanceFilterException {
		return null;
	}

	@Override
	public boolean validateID(String id) {
		return false;
	}
	
	

}
