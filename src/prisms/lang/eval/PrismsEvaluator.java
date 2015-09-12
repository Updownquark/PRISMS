package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.ParsedItem;

/** A collection of {@link PrismsItemEvaluator}s for evaluating any of a large set of types of expressions */
public class PrismsEvaluator implements org.qommons.Sealable {
	private org.qommons.SubClassMap<ParsedItem, PrismsItemEvaluator<?>> theEvaluators;

	private boolean isSealed;

	/** Creates the evaluator */
	public PrismsEvaluator() {
		theEvaluators = new org.qommons.SubClassMap<>();
	}

	/**
	 * @param <T> The type of the item to add evaluation support for
	 * @param type The type of item to support evaluation for
	 * @param evaluator The evaluator for the given type
	 * @throws SealedException If this evaluator has been {@link #seal() sealed}
	 */
	public <T extends ParsedItem> void addEvaluator(Class<T> type, PrismsItemEvaluator<? super T> evaluator) throws SealedException {
		if(isSealed)
			throw new SealedException(this);
		theEvaluators.put(type, evaluator);
	}

	/**
	 * @param <T> The type of the item to get evaluation support for
	 * @param type The type to get evaluation support for
	 * @return The evaluator for the given type
	 */
	public <T extends ParsedItem> PrismsItemEvaluator<? super T> getEvaluatorFor(Class<T> type) {
		return (PrismsItemEvaluator<? super T>) theEvaluators.get(type);
	}

	/**
	 * Evaluates an item
	 *
	 * @param item The item to evaluate
	 * @param env The evaluation environment to use during evaluation
	 * @param asType Whether the result is expected to be a type as opposed to a value
	 * @param withValues Whether to evaluate the values or just types
	 * @return The result of the evaluation
	 * @throws EvaluationException If the given item is not supported for evaluation or some other non-syntax error is encountered
	 */
	public EvaluationResult evaluate(ParsedItem item, EvaluationEnvironment env, boolean asType, boolean withValues)
		throws EvaluationException {
		PrismsItemEvaluator<?> eval = getEvaluatorFor(item.getClass());
		if(eval == null)
			throw new EvaluationException("No evaluator configured for item type " + item.getClass().getName(), item, 0);
		return ((PrismsItemEvaluator<ParsedItem>) eval).evaluate(item, this, env, asType, withValues);
	}

	@Override
	public boolean isSealed() {
		return isSealed;
	}

	@Override
	public void seal() {
		isSealed = true;
	}
}
