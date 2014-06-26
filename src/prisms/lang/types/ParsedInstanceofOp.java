package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents a use of the instanceof operator */
public class ParsedInstanceofOp extends prisms.lang.ParsedItem {
	private prisms.lang.ParsedItem theVariable;

	private prisms.lang.ParsedItem theType;

	@Override
	public void setup(prisms.lang.PrismsParser parser, prisms.lang.ParsedItem parent, prisms.lang.ParseMatch match)
		throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		theVariable = parser.parseStructures(this, getStored("variable"))[0];
		theType = parser.parseStructures(this, getStored("type"))[0];
	}

	/** @return The variable that is being checked against a type */
	public prisms.lang.ParsedItem getVariable() {
		return theVariable;
	}

	/** @return The type that the variable is being checked against */
	public prisms.lang.ParsedItem getType() {
		return theType;
	}

	@Override
	public prisms.lang.ParsedItem [] getDependents() {
		return new prisms.lang.ParsedItem[] {theVariable, theType};
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theVariable == dependent)
			theVariable = toReplace;
		else if(theType == dependent)
			theType = toReplace;
		else
			throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		return theVariable + " instanceof " + theType;
	}
}
