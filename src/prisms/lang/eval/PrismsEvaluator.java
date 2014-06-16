package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.ParsedItem;

public class PrismsEvaluator {
	private prisms.util.SubClassMap<ParsedItem, PrismsItemEvaluator<?>> theEvaluators;

	public PrismsEvaluator() {
		theEvaluators = new prisms.util.SubClassMap<>();
	}

	public <T extends ParsedItem> void addEvaluator(Class<T> type, PrismsItemEvaluator<? super T> evaluator) {
		theEvaluators.put(type, evaluator);
	}

	public <T extends ParsedItem> PrismsItemEvaluator<? super T> getEvaluatorFor(Class<T> type) {
		return (PrismsItemEvaluator<? super T>) theEvaluators.get(type);
	}

	public EvaluationResult evaluate(ParsedItem item, EvaluationEnvironment env, boolean asType, boolean withValues)
		throws EvaluationException {
		PrismsItemEvaluator<?> eval = getEvaluatorFor(item.getClass());
		if(eval == null)
			throw new EvaluationException("No evaluator configured for item type " + item.getClass().getName(), item, 0);
		return ((PrismsItemEvaluator<ParsedItem>) eval).evaluate(item, this, env, asType, withValues);
	}
}
