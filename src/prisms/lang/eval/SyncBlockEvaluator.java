package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.ParsedSyncBlock;

/** Represents a synchronized block */
public class SyncBlockEvaluator implements PrismsItemEvaluator<ParsedSyncBlock> {
	@Override
	public EvaluationResult evaluate(ParsedSyncBlock item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		EvaluationResult syncItemRes = evaluator.evaluate(item.getSyncItem(), env, false, withValues);
		if(!syncItemRes.isValue())
			throw new prisms.lang.EvaluationException(syncItemRes.typeString() + " cannot be resolved to a variable", item, item
				.getSyncItem().getMatch().index);
		EvaluationResult res;
		if(withValues)
			synchronized(syncItemRes.getValue()) {
				res = evaluator.evaluate(item.getContents(), env, false, withValues);
			}
		else
			res = evaluator.evaluate(item.getContents(), env, false, withValues);
		return res;
	}
}
