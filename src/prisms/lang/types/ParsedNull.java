package prisms.lang.types;

import prisms.lang.Type;

/** Represents a null identifier */
public class ParsedNull extends ParsedLiteral {
	@Override
	public Type getType() {
		return null;
	}

	@Override
	public Object parseValue(String text) {
		return null;
	}

	@Override
	public String toString() {
		return "null";
	}
}
