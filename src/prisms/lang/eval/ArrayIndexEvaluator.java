package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.ParsedArrayIndex;

/** Parses an array index operation */
public class ArrayIndexEvaluator implements AssignableEvaluator<ParsedArrayIndex> {
	@Override
	public EvaluationResult evaluate(ParsedArrayIndex item, PrismsEvaluator eval, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		EvaluationResult array = eval.evaluate(item.getArray(), env, false, withValues);
		if(!array.isValue())
			throw new EvaluationException(array.typeString() + " cannot be resolved to a variable", item, item.getArray().getMatch().index);
		if(!array.getType().isArray())
			throw new EvaluationException("The type of the expression must resolve to an array but it resolved to " + array.typeString(),
				item, item.getArray().getMatch().index);
		EvaluationResult index = eval.evaluate(item.getIndex(), env, false, withValues);
		if(!index.isValue())
			throw new EvaluationException(index.typeString() + " cannot be resolved to a variable", item, item.getIndex().getMatch().index);
		if(!index.isIntType() || Long.TYPE.equals(index.getType().getBaseType()))
			throw new EvaluationException("Type mismatch: cannot convert from " + index + " to int", item, item.getIndex().getMatch().index);
		return new EvaluationResult(array.getType().getComponentType(), withValues ? java.lang.reflect.Array.get(array.getValue(),
			((Number) index.getValue()).intValue()) : null);
	}

	@Override
	public EvaluationResult getValue(ParsedArrayIndex item, PrismsEvaluator eval, EvaluationEnvironment env, AssignmentOperatorEvaluator assign)
		throws EvaluationException {
		return evaluate(item, eval, env, false, true);
	}

	@Override
	public void assign(ParsedArrayIndex item, EvaluationResult value, PrismsEvaluator eval, EvaluationEnvironment env,
		AssignmentOperatorEvaluator assign) throws EvaluationException {
		int index = ((Number) eval.evaluate(item.getIndex(), env, false, true).getValue()).intValue();
		EvaluationResult res = eval.evaluate(item.getArray(), env, false, true);
		java.lang.reflect.Array.set(res.getValue(), index, value.getValue());
	}
}
