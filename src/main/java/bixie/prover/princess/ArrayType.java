package bixie.prover.princess;

import bixie.prover.ProverType;

class ArrayType implements ProverType {

	public final int arity;

	public ArrayType(int arity) {
		this.arity = arity;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + arity;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArrayType other = (ArrayType) obj;
		if (arity != other.arity)
			return false;
		return true;
	}

	public String toString() {
		return "Array(" + arity + ")";
	}

}
