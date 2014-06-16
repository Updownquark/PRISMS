/*
 * ParsedParenthetic.java Created Nov 14, 2011 by Andrew Butler, PSL
 */
package prisms.lang.eval;

import prisms.lang.*;

/** Represents a parenthetic expression */
public class ParsedParenthetic extends AssignableEvaluator
{
	private prisms.lang.ParsedItem theContent;

	@Override
	public void setup(prisms.lang.PrismsParser parser, prisms.lang.ParsedItem parent, prisms.lang.ParseMatch match)
		throws prisms.lang.ParseException
	{
		super.setup(parser, parent, match);
		theContent = parser.parseStructures(this, getStored("content"))[0];
	}

	/** @return The content of this parenthetical */
	public prisms.lang.ParsedItem getContent()
	{
		return theContent;
	}

	@Override
	public prisms.lang.ParsedItem[] getDependents()
	{
		return new prisms.lang.ParsedItem [] {theContent};
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theContent == dependent)
			theContent = toReplace;
		else
			throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString()
	{
		return "(" + theContent + ")";
	}

	@Override
	public prisms.lang.EvaluationResult evaluate(prisms.lang.EvaluationEnvironment env, boolean asType,
		boolean withValues) throws prisms.lang.EvaluationException
	{
		return theContent.evaluate(env, asType, withValues);
	}

	@Override
	public EvaluationResult getValue(EvaluationEnvironment env, AssignmentOperatorEvaluator assign)
		throws EvaluationException
	{
		if(!(theContent instanceof AssignableEvaluator))
			throw new EvaluationException("Invalid argument for assignment operator " + assign.getName(), this,
				theContent.getMatch().index);
		return ((AssignableEvaluator) theContent).getValue(env, assign);
	}

	@Override
	public void assign(EvaluationResult value, EvaluationEnvironment env, AssignmentOperatorEvaluator assign)
		throws EvaluationException
	{
		if(!(theContent instanceof AssignableEvaluator))
			throw new EvaluationException("Invalid argument for assignment operator " + assign.getName(), this,
				theContent.getMatch().index);
		((AssignableEvaluator) theContent).assign(value, env, assign);
	}
}
