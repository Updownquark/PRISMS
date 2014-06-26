package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.NoOpItem;

/** Represents a commented text block */
public class NoOpEvaluator implements PrismsItemEvaluator<NoOpItem> {
	@Override
	public EvaluationResult evaluate(NoOpItem item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType, boolean withValues)
		throws EvaluationException {
		return null;
	}
}
