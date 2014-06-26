package prisms.lang.types;

import prisms.lang.ParsedItem;

/** Represents a try{}[catch(? extends Throwable){}]*[finally{}]? statement */
public class ParsedTryCatchFinally extends ParsedItem {
	private ParsedStatementBlock theTryBlock;

	private ParsedDeclaration [] theCatchDeclarations;

	private ParsedStatementBlock [] theCatchBlocks;

	private ParsedStatementBlock theFinallyBlock;

	@Override
	public void setup(prisms.lang.PrismsParser parser, ParsedItem parent, prisms.lang.ParseMatch match) throws prisms.lang.ParseException {
		super.setup(parser, parent, match);
		theTryBlock = (ParsedStatementBlock) parser.parseStructures(this, getStored("try"))[0];
		ParsedItem [] cds = parser.parseStructures(this, getAllStored("catchDeclaration"));
		theCatchDeclarations = new ParsedDeclaration[cds.length];
		System.arraycopy(cds, 0, theCatchDeclarations, 0, cds.length);
		ParsedItem [] cbs = parser.parseStructures(this, getAllStored("catch"));
		theCatchBlocks = new ParsedStatementBlock[cbs.length];
		System.arraycopy(cbs, 0, theCatchBlocks, 0, cbs.length);
		prisms.lang.ParseMatch finallyMatch = getStored("finally");
		if(finallyMatch != null)
			theFinallyBlock = (ParsedStatementBlock) parser.parseStructures(this, finallyMatch)[0];
		if(getMatch().isComplete() && theFinallyBlock == null && theCatchDeclarations.length == 0)
			throw new prisms.lang.ParseException("try statements must use catch or finally", getRoot().getFullCommand(),
				theTryBlock.getMatch().index + theTryBlock.getMatch().text.length());
	}

	/** @return The statement block that always begins executing */
	public ParsedStatementBlock getTryBlock() {
		return theTryBlock;
	}

	/** @return The declarations of the throwables that are registered to be caught */
	public ParsedDeclaration [] getCatchDeclarations() {
		return theCatchDeclarations;
	}

	/** @return The statement blocks that will execute for each throwable type that is caught */
	public ParsedStatementBlock [] getCatchBlocks() {
		return theCatchBlocks;
	}

	/** @return The statement block that is always executed after all the try/catch blocks have finished */
	public ParsedStatementBlock getFinallyBlock() {
		return theFinallyBlock;
	}

	@Override
	public ParsedItem [] getDependents() {
		java.util.ArrayList<ParsedItem> ret = new java.util.ArrayList<>();
		ret.add(theTryBlock);
		for(int i = 0; i < theCatchDeclarations.length; i++) {
			ret.add(theCatchDeclarations[i]);
			ret.add(theCatchBlocks[i]);
		}
		if(theFinallyBlock != null)
			ret.add(theFinallyBlock);
		return ret.toArray(new ParsedItem[ret.size()]);
	}

	@Override
	public void replace(ParsedItem dependent, ParsedItem toReplace) throws IllegalArgumentException {
		if(theTryBlock == dependent) {
			if(toReplace instanceof ParsedStatementBlock) {
				theTryBlock = (ParsedStatementBlock) toReplace;
				return;
			} else
				throw new IllegalArgumentException("Cannot replace try block with " + toReplace.getClass().getSimpleName());
		}
		for(int i = 0; i < theCatchDeclarations.length; i++) {
			if(theCatchDeclarations[i] == dependent) {
				if(toReplace instanceof ParsedDeclaration) {
					theCatchDeclarations[i] = (ParsedDeclaration) toReplace;
					return;
				} else
					throw new IllegalArgumentException("Cannot replace catch declaration with " + toReplace.getClass().getSimpleName());
			}
			if(theCatchBlocks[i] == dependent) {
				if(toReplace instanceof ParsedStatementBlock) {
					theCatchBlocks[i] = (ParsedStatementBlock) toReplace;
					return;
				} else
					throw new IllegalArgumentException("Cannot replace catch block with " + toReplace.getClass().getSimpleName());
			}
		}
		if(theFinallyBlock != null && theFinallyBlock == dependent) {
			if(toReplace instanceof ParsedStatementBlock) {
				theFinallyBlock = (ParsedStatementBlock) toReplace;
				return;
			} else
				throw new IllegalArgumentException("Cannot replace finally block with " + toReplace.getClass().getSimpleName());
		}
		throw new IllegalArgumentException("No such dependent " + dependent);
	}

	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("try\n").append(theTryBlock);
		for(int i = 0; i < theCatchDeclarations.length; i++)
			ret.append("catch(").append(theCatchDeclarations[i]).append(")\n").append(theCatchBlocks[i]);
		if(theFinallyBlock != null)
			ret.append("finally\n").append(theFinallyBlock);
		return ret.toString();
	}
}
