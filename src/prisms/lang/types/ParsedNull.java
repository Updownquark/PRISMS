package prisms.lang.types;

/** Represents a null identifier */
public class ParsedNull extends ParsedPrimitive {
	@Override
	public Object parseValue(String text) {
		return null;
	}

	@Override
	public String toString() {
		return "null";
	}
}
