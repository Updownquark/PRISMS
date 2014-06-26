package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents an item that does not need to be evaluated */
public abstract class NoOpItem extends ParsedItem {
	@Override
	public ParsedItem [] getDependents() {
		return new ParsedItem[0];
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
	}
}
