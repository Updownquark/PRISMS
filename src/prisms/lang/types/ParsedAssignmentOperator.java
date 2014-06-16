/*
 * AssignmentOperator.java Created Nov 15, 2011 by Andrew Butler, PSL
 */
package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents an assignment, either a straight assignment or an assignment operator, like += */
public class ParsedAssignmentOperator extends prisms.lang.ParsedItem
{
	private String theName;

	private boolean isPrefix;

	private prisms.lang.ParsedItem theVariable;

	private prisms.lang.ParsedItem theOperand;

	@Override
	public void setup(prisms.lang.PrismsParser parser, prisms.lang.ParsedItem parent, prisms.lang.ParseMatch match)
		throws prisms.lang.ParseException
	{
		super.setup(parser, parent, match);
		theName = getStored("name").text;
		theVariable = parser.parseStructures(this, getStored("variable"))[0];
		prisms.lang.ParseMatch opMatch = getStored("operand");
		if(opMatch != null)
			theOperand = parser.parseStructures(this, opMatch)[0];
		else
			for(prisms.lang.ParseMatch m : match.getParsed())
			{
				if(m.config.getName().equals("pre-op"))
					isPrefix = false;
				else if(m.config.getName().equals("op"))
					isPrefix = true;
			}
	}

	/** @return The name of this operator */
	public String getName()
	{
		return theName;
	}

	/** @return Whether, if this operator is unary, the operator occurred before the variable */
	public boolean isPrefix()
	{
		return isPrefix;
	}

	/** @return The variable whose value will be assigned with this assignment */
	public prisms.lang.ParsedItem getVariable()
	{
		return theVariable;
	}

	/** @return The operand for the assignment operation */
	public prisms.lang.ParsedItem getOperand()
	{
		return theOperand;
	}

	@Override
	public prisms.lang.ParsedItem[] getDependents()
	{
		if(theOperand == null)
			return new prisms.lang.ParsedItem [] {theVariable};
		else
			return new prisms.lang.ParsedItem [] {theVariable, theOperand};
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theVariable == dependent)
			theVariable = toReplace;
		else if(theOperand != null && theOperand == dependent)
			theOperand = toReplace;
		else
			throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString()
	{
		StringBuilder ret = new StringBuilder();
		if(theOperand == null && isPrefix)
			ret.append(theName);
		ret.append(theVariable);
		if(theOperand != null || !isPrefix)
			ret.append(theName);
		if(theOperand != null)
			ret.append(theOperand);
		return ret.toString();
	}
}
