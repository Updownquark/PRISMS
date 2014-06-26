package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents a throw statement */
public class ParsedThrow extends prisms.lang.ParsedItem {
	private ParsedItem theValue;

	@Override
	public void setup(prisms.lang.PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match) throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		prisms.lang.ParseMatch m = getStored("value");
		if(m != null)
			theValue = parser.parseStructures(this, m)[0];
	}

	/** @return The Throwable that this throw statement will evaluate and throw */
	public ParsedItem getValue() {
		return theValue;
	}

	@Override
	public ParsedItem [] getDependents() {
		return new ParsedItem[] {theValue};
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theValue == dependent)
			theValue = toReplace;
		else
			throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		return "throw " + theValue;
	}
}
