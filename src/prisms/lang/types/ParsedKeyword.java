package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents a keyword */
public class ParsedKeyword extends prisms.lang.ParsedItem {
	private String theName;

	@Override
	public void setup(prisms.lang.PrismsParser parser, prisms.lang.ParsedItem parent, prisms.lang.ParseMatch match)
		throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		theName = getStored("name").text;
	}

	/** @return This keyword's name */
	public String getName() {
		return theName;
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
		return theName;
	}
}
