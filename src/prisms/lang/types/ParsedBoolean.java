/* ParsedBoolean.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.types;

import prisms.lang.ParseException;
import prisms.lang.Type;

/** Represents a boolean value */
public class ParsedBoolean extends ParsedLiteral {
	@Override
	public Type getType() {
		return new Type(Boolean.TYPE);
	}

	@Override
	public Object parseValue(String text) throws ParseException {
		switch (text) {
		case "true":
			return true;
		case "false":
			return false;
		}
		throw new ParseException("Unrecognized boolean value: " + text, getRoot().getFullCommand(), getStored("value").index);
	}

	@Override
	public Boolean getValue() {
		return (Boolean) super.getValue();
	}
}
