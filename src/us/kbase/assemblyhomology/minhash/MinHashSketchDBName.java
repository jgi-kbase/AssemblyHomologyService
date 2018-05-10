package us.kbase.assemblyhomology.minhash;

import static us.kbase.assemblyhomology.util.Util.exceptOnEmpty;

public class MinHashSketchDBName implements Comparable<MinHashSketchDBName>{

	private final String name;
	
	public MinHashSketchDBName(final String name) {
		exceptOnEmpty(name, "name");
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int compareTo(final MinHashSketchDBName o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MinHashSketchDBName [name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MinHashSketchDBName other = (MinHashSketchDBName) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
}
