/* ParsedPreviousAnswer.java Created Nov 15, 2011 by Andrew Butler, PSL */
package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Allows the results of previous calculations to be accessed dynamically */
public class ParsedPreviousAnswer extends ParsedItem {
	private int theIndex;

	@Override
	public void setup(prisms.lang.PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match) throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		prisms.lang.ParseMatch indexMatch = getStored("index");
		if(indexMatch != null)
			theIndex = Integer.parseInt(indexMatch.text);
	}

	/** @return The index (starting at 0) of the answer this refers to */
	public int getIndex() {
		return theIndex;
	}

	@Override
	public ParsedItem [] getDependents() {
		return new ParsedItem[0];
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		if(theIndex > 0)
			return "%" + theIndex;
		else
			return "%";
	}
}
