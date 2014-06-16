package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.ParsedItem;

public interface PrismsItemEvaluator<T extends ParsedItem> {
	/**
	 * Validates or evaluates an expression. This method should not be called if the item's match is incomplete.
	 *
	 * @param item The item to evaluate
	 * @param evaluator The evaluator to evaluate the item's dependencies, if any
	 * @param env The evaluation environment to execute in
	 * @param asType Whether the result should be a type if possible
	 * @param withValues Whether to evaluate the value of the expression, or simply validate it and return its type
	 * @return The result of the expression
	 * @throws EvaluationException If an error occurs evaluating the expression
	 */
	EvaluationResult evaluate(T item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType, boolean withValues)
		throws EvaluationException;
}
