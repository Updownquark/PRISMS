package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents a switch/case statement */
public class ParsedSwitch extends ParsedItem
{
	private ParsedItem theVariable;

	private ParsedItem [] theCases;

	private ParsedItem [][] theCaseBlocks;

	@Override
	public void setup(prisms.lang.PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match) throws prisms.lang.ParseException
	{
		super.setup(parser, parent, match);
		theVariable = parser.parseStructures(this, getStored("variable"))[0];
		java.util.ArrayList<ParsedItem> caseStmts = new java.util.ArrayList<ParsedItem>();
		boolean hasDefault = false;
		theCases = new ParsedItem [0];
		theCaseBlocks = new ParsedItem [0] [];
		for(prisms.lang.ParseMatch m : matches())
		{
			if("case".equals(m.config.get("storeAs")) || "default".equals(m.config.get("storeAs")))
			{
				if(theCases.length > 0)
					theCaseBlocks = org.qommons.ArrayUtils.add(theCaseBlocks, caseStmts.toArray(new ParsedItem [caseStmts.size()]));
				caseStmts.clear();
				if("case".equals(m.config.get("storeAs")))
					theCases = org.qommons.ArrayUtils.add(theCases, parser.parseStructures(this, m)[0]);
				else if(hasDefault)
					throw new prisms.lang.ParseException("Only one default statement allowed in a switch", getRoot().getFullCommand(),
						m.index);
				else
				{
					theCases = org.qommons.ArrayUtils.add(theCases, null);
					hasDefault = true;
				}
			}
			else if(theCases.length > 0 && m.config.getName().equals("op"))
				caseStmts.add(parser.parseStructures(this, m)[0]);
		}
		if(theCases.length > theCaseBlocks.length)
			theCaseBlocks = org.qommons.ArrayUtils.add(theCaseBlocks, caseStmts.toArray(new ParsedItem [caseStmts.size()]));
	}

	/** @return The variable that this switch switches on */
	public ParsedItem getVariable() {
		return theVariable;
	}

	/** @return The case values that may execute for this switch */
	public ParsedItem [] getCases() {
		return theCases;
	}

	/** @return The case blocks to execute for each case statement */
	public ParsedItem [][] getCaseBlocks() {
		return theCaseBlocks;
	}

	@Override
	public ParsedItem [] getDependents()
	{
		java.util.ArrayList<ParsedItem> ret = new java.util.ArrayList<ParsedItem>();
		ret.add(theVariable);
		for(int c = 0; c < theCases.length; c++)
		{
			if(theCases[c] != null)
				ret.add(theCases[c]);
			for(ParsedItem pi : theCaseBlocks[c])
				ret.add(pi);
		}
		return ret.toArray(new ParsedItem [ret.size()]);
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException
	{
		if(theVariable == dependent)
		{
			theVariable = toReplace;
			return;
		}
		for(int i = 0; i < theCases.length; i++)
		{
			if(theCases[i] == dependent)
			{
				theCases[i] = toReplace;
				return;
			}
			for(int j = 0; j < theCaseBlocks[i].length; j++)
			{
				if(theCaseBlocks[i][j] == dependent)
				{
					theCaseBlocks[i][j] = toReplace;
					return;
				}
			}
		}
		throw new IllegalArgumentException("No such dependent " + dependent);
	}
}
