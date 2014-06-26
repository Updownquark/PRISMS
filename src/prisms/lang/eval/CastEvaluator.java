/* ParsedCast.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.ExecutionException;
import prisms.lang.Type;
import prisms.lang.types.ParsedCast;

/** Represents a cast from one type to another */
public class CastEvaluator implements PrismsItemEvaluator<ParsedCast> {
	@Override
	public EvaluationResult evaluate(ParsedCast item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		EvaluationResult typeEval = evaluator.evaluate(item.getType(), env, true, false);
		if(!typeEval.isType())
			throw new EvaluationException("Unrecognized type " + item.getType().getMatch().text, item, item.getType().getMatch().index);
		prisms.lang.EvaluationResult toCast = evaluator.evaluate(item.getValue(), env, false, withValues);
		if(toCast.getPackageName() != null || toCast.isType())
			throw new EvaluationException(toCast.getFirstVar() + " cannot be resolved to a variable", item.getValue(), item.getValue()
				.getMatch().index);
		if(typeEval.getType().getCommonType(toCast.getType()) == null)
			throw new EvaluationException("Cannot cast from " + toCast.getType() + " to " + typeEval.getType(), item, item.getType()
				.getMatch().index);
		if(withValues)
			try {
				return new EvaluationResult(typeEval.getType(), typeEval.getType().cast(toCast.getValue()));
			} catch(IllegalArgumentException e) {
				throw new ExecutionException(new Type(ClassCastException.class), new ClassCastException((toCast.getValue() == null ? "null"
					: prisms.lang.Type.typeString(toCast.getValue().getClass())) + " cannot be cast to " + typeEval.getType()), item,
					item.getStored("type").index);
			}
		else
			return new EvaluationResult(typeEval.getType(), null);
	}
}
