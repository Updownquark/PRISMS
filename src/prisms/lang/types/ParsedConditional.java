/* ParsedConditional.java Created Nov 16, 2011 by Andrew Butler, PSL */
package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents a conditional expression of the form condition ? affirmative : negative */
public class ParsedConditional extends ParsedItem {
	private ParsedItem theCondition;

	private ParsedItem theAffirmative;

	private ParsedItem theNegative;

	@Override
	public void setup(prisms.lang.PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match) throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		theCondition = parser.parseStructures(this, getStored("condition"))[0];
		theAffirmative = parser.parseStructures(this, getStored("affirmative"))[0];
		theNegative = parser.parseStructures(this, getStored("negative"))[0];
	}

	/** @return The condition determining which expression is evaluated */
	public ParsedItem getCondition() {
		return theCondition;
	}

	/** @return The expression that is evaluated if the condition evaluates to true */
	public ParsedItem getAffirmative() {
		return theAffirmative;
	}

	/** @return The expression that is evaluated if the condition evaluates to false */
	public ParsedItem getNegative() {
		return theNegative;
	}

	@Override
	public ParsedItem [] getDependents() {
		return new ParsedItem[] {theCondition, theAffirmative, theNegative};
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theCondition == dependent)
			theCondition = toReplace;
		else if(theAffirmative == dependent)
			theAffirmative = toReplace;
		else if(theNegative == dependent)
			theNegative = toReplace;
		else
			throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		return theCondition + " ? " + theAffirmative + " : " + theNegative;
	}
}
