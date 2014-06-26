/* ParsedUnaryOp.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.Type;
import prisms.lang.types.ParsedUnaryOp;

/** An operation on a single operand */
public class UnaryOpEvaluator implements PrismsItemEvaluator<ParsedUnaryOp> {
	@Override
	public EvaluationResult evaluate(ParsedUnaryOp item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		String name = item.getName();
		EvaluationResult res = evaluator.evaluate(item.getOp(), env, false, withValues);
		if(res.isType())
			throw new EvaluationException("The operator " + name + " is not defined for type java.lang.Class", item,
				item.getStored("name").index);
		if(res.getType() == null)
			throw new EvaluationException(res.getFirstVar() + " cannot be resolved to a variable", item, item.getOp().getMatch().index);
		if("+".equals(name)) {
			if(!res.getType().isPrimitive() || Boolean.TYPE.equals(res.getType().getBaseType()))
				throw new EvaluationException("The operator " + name + " is not defined for type " + res.typeString(), item,
					item.getStored("name").index);
			return res;
		} else if("-".equals(name)) {
			if(!res.getType().isMathable())
				throw new EvaluationException("The operator " + name + " is not defined for type " + res.typeString(), item,
					item.getStored("name").index);

			if(!withValues && res.getValue() == null)
				return res;
			return new EvaluationResult(res.getType(), MathUtils.negate(res.getValue()));
		} else if("!".equals(name)) {
			if(Boolean.TYPE.equals(res.getType().getBaseType()) || Boolean.class.equals(res.getType().getBaseType()))
				return new EvaluationResult(new Type(Boolean.TYPE), withValues ? !((Boolean) res.getValue()) : null);
			else
				throw new EvaluationException("The operator " + name + " is not defined for type " + res.typeString(), item,
					item.getStored("name").index);
		} else if("~".equals(name)) {
			if(!res.getType().isIntMathable())
				throw new EvaluationException("The operator " + name + " is not defined for type " + res.typeString(), item,
					item.getStored("name").index);

			if(!withValues && res.getValue() == null)
				return res;
			return new EvaluationResult(res.getType(), MathUtils.complement(res.getValue()));
		} else
			throw new EvaluationException("Unary operator " + name + " not recognized", item, item.getStored("name").index);
	}
}
