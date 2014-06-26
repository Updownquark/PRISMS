/* ParsedPreviousAnswer.java Created Nov 15, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.ParsedPreviousAnswer;

/** Allows the results of previous calculations to be accessed dynamically */
public class PreviousAnswerEvaluator implements PrismsItemEvaluator<ParsedPreviousAnswer> {
	@Override
	public EvaluationResult evaluate(ParsedPreviousAnswer item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		if(env.getHistoryCount() == 0)
			throw new prisms.lang.EvaluationException("No previous results available", item, item.getMatch().index);
		else if(env.getHistoryCount() <= item.getIndex())
			throw new prisms.lang.EvaluationException("Only " + env.getHistoryCount() + " previous result(s) are available", item,
				item.getMatch().index);
		else {
			int index = item.getIndex();
			return new prisms.lang.EvaluationResult(env.getHistoryType(index), withValues ? env.getHistory(index) : null);
		}
	}
}
