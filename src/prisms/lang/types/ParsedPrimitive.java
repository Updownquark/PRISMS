package prisms.lang.types;

import prisms.lang.ParseException;
import prisms.lang.ParsedItem;

/** An immutable, constant value parsed from an expression */
public abstract class ParsedPrimitive extends ParsedItem {
	private Object theValue;

	@Override
	public void setup(prisms.lang.PrismsParser parser, prisms.lang.ParsedItem parent, prisms.lang.ParseMatch match)
		throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		theValue = parseValue(getStored("value").text);
	}

	/**
	 * @param text The text to parse
	 * @return The value of this primitive given the text
	 * @throws ParseException If the value cannot be parsed
	 */
	public abstract Object parseValue(String text) throws ParseException;

	/** @return The value of this primitive */
	public Object getValue() {
		return theValue;
	}

	@Override
	public prisms.lang.ParsedItem [] getDependents() {
		return new prisms.lang.ParsedItem[0];
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		return String.valueOf(theValue);
	}
}
