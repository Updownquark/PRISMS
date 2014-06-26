package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents an enhanced for loop */
public class ParsedEnhancedForLoop extends ParsedItem {
	private ParsedDeclaration theVariable;

	private ParsedItem theIterable;

	private ParsedStatementBlock theContents;

	@Override
	public void setup(prisms.lang.PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match) throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		ParsedItem variable = parser.parseStructures(this, getStored("variable"))[0];
		if(!(variable instanceof ParsedDeclaration))
			throw new prisms.lang.ParseException("Enhanced for loop must begin with a declaration", getRoot().getFullCommand(),
				variable.getMatch().index);
		theVariable = (ParsedDeclaration) variable;
		theIterable = parser.parseStructures(this, getStored("iterable"))[0];
		ParsedItem content = parser.parseStructures(this, getStored("content"))[0];
		if(content instanceof ParsedStatementBlock)
			theContents = (ParsedStatementBlock) content;
		else
			theContents = new ParsedStatementBlock(parser, this, content.getMatch(), content);
	}

	/** @return The variable that each instance the iterator returns will be assigned to */
	public ParsedDeclaration getVariable() {
		return theVariable;
	}

	/** @return The iterator to iterate over in the loop */
	public ParsedItem getIterable() {
		return theIterable;
	}

	/** @return The statements to be executed for each item in the iterator */
	public ParsedStatementBlock getContents() {
		return theContents;
	}

	@Override
	public ParsedItem [] getDependents() {
		return new ParsedItem[] {theVariable, theIterable, theContents};
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theVariable == dependent) {
			if(toReplace instanceof ParsedDeclaration)
				theVariable = (ParsedDeclaration) toReplace;
			else
				throw new IllegalArgumentException("Cannot replace the variable declaration of an enhanced for loop with "
					+ toReplace.getClass().getSimpleName());
		} else if(theIterable == dependent)
			theIterable = toReplace;
		else if(theContents == dependent) {
			if(toReplace instanceof ParsedStatementBlock)
				theContents = (ParsedStatementBlock) toReplace;
			else
				throw new IllegalArgumentException("Cannot replace the contents of a for loop with " + toReplace.getClass().getSimpleName());
		} else
			throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		return new StringBuilder("for(").append(theVariable).append(" : ").append(theIterable).append('\n').append(theContents).toString();
	}
}
