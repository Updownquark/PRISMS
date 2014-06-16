/* AssignmentOperator.java Created Nov 15, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.MathUtils;
import prisms.lang.types.Assignable;
import prisms.lang.types.ParsedAssignmentOperator;

/** Represents an assignment, either a straight assignment or an assignment operator, like += */
public class AssignmentOperatorEvaluator implements PrismsItemEvaluator<ParsedAssignmentOperator> {
	@Override
	public EvaluationResult evaluate(ParsedAssignmentOperator item, PrismsEvaluator eval, prisms.lang.EvaluationEnvironment env,
		boolean asType, boolean withValues) throws EvaluationException {
		if(!(item.getVariable() instanceof AssignableEvaluator))
			throw new EvaluationException("The left-hand side of an assignment must be a variable", item,
				item.getVariable().getMatch().index);
		Assignable a = (Assignable) item.getVariable();

		EvaluationResult opType = item.getOperand() == null ? null : eval.evaluate(item.getOperand(), env, false, withValues);
		if(opType != null) {
			if(opType.getPackageName() != null)
				throw new EvaluationException(opType.getFirstVar() + " cannot be resolved to a variable", item, item.getVariable()
					.getMatch().index);
			if(opType.isType())
				throw new EvaluationException(opType.getType() + " cannot be resolved to a variable", item,
					item.getVariable().getMatch().index);
			if(opType.getControl() != null)
				throw new EvaluationException("Syntax error: misplaced construct", item, item.getStored("name").index);
		}

		String name = item.getName();
		EvaluationResult preRes;
		if(!withValues || name.equals("="))
			preRes = eval.evaluate(a, env, false, false);
		else {
			if(!(eval.getEvaluatorFor(a.getClass()) instanceof AssignableEvaluator))
				throw new EvaluationException("No assignable evaluator configured for " + a.getClass().getName(), item, a.getMatch().index);
			AssignableEvaluator<Assignable> evaluator = (AssignableEvaluator<Assignable>) eval.getEvaluatorFor(a.getClass());
			preRes = evaluator.getValue(a, eval, env, item);
		}

		Object ret;
		Object toSet;
		if("=".equals(name)) {
			if(preRes != null && !preRes.getType().isAssignable(opType.getType()))
				throw new EvaluationException("Type mismatch: Cannot convert from " + opType.getType() + " to " + preRes.typeString(),
					item, item.getOperand().getMatch().index);
			ret = toSet = withValues ? opType.getValue() : null;
		} else if("++".equals(name) || "--".equals(name)) {
			ret = preRes.getValue();
			int adjust = 1;
			if("--".equals(name))
				adjust = -adjust;
			if(Character.TYPE.equals(preRes))
				toSet = withValues ? Character.valueOf((char) (((Character) ret).charValue() + adjust)) : null;
				else if(preRes.getType().isPrimitive() && !Boolean.TYPE.equals(preRes.getType().getBaseType())) {
					Number num = (Number) ret;
					if(!withValues)
						toSet = null;
					else if(ret instanceof Double)
						toSet = Double.valueOf(num.doubleValue() + adjust);
					else if(ret instanceof Float)
						toSet = Float.valueOf(num.floatValue() + adjust);
					else if(ret instanceof Long)
						toSet = Long.valueOf(num.longValue() + adjust);
					else if(ret instanceof Integer)
						toSet = Integer.valueOf(num.intValue() + adjust);
					else if(ret instanceof Short)
						toSet = Short.valueOf((short) (num.shortValue() + adjust));
					else if(ret instanceof Byte)
						toSet = withValues ? Byte.valueOf((byte) (num.byteValue() + adjust)) : null;
						else
							throw new EvaluationException("The operator " + name + " is not defined for type " + preRes, item,
								item.getStored("name").index);
				} else
					throw new EvaluationException("The operator " + name + " is not defined for type " + preRes.typeString(), item,
						item.getStored("name").index);
			if(item.isPrefix())
				ret = toSet;
		} else if("+=".equals(name) || "-=".equals(name) || "*=".equals(name) || "/=".equals(name) || "%=".equals(name)) {
			if(preRes.getType().isMathable() && opType.getType().isMathable()) {
				if(withValues)
					try {
						ret = MathUtils.binaryOp(name.substring(1), preRes.getType(), preRes.getValue(), opType.getType(),
							opType.getValue());
					} catch(RuntimeException e) {
						throw new EvaluationException(e.getMessage(), e, item, item.getStored("name").index);
					}
				else
					ret = null;
				toSet = ret;
			} else
				throw new EvaluationException("The operator " + name + " is not defined for type " + preRes.typeString(), item,
					item.getStored("name").index);
		} else if("|=".equals(name) || "&=".equals(name) || "^=".equals(name)) {
			if(preRes.getType().isIntMathable() && opType.getType().isIntMathable()) {
				if(withValues) {
					Number op1 = Character.TYPE.equals(preRes.getType().getBaseType()) ? Integer.valueOf(((Character) preRes.getValue())
						.charValue()) : (Number) preRes.getValue();
					Number op2 = Character.TYPE.equals(opType.getType().getBaseType()) ? Integer.valueOf(((Character) opType.getValue())
						.charValue()) : (Number) opType.getValue();
					long val;
					if("|=".equals(name))
						val = op1.longValue() | op2.longValue();
					else if("&=".equals(name))
						val = op1.longValue() & op2.longValue();
					else
						val = op1.longValue() ^ op2.longValue();
					if(Long.TYPE.equals(preRes.getType().getBaseType()))
						ret = toSet = Long.valueOf(val);
					else if(Integer.TYPE.equals(preRes.getType().getBaseType()))
						ret = toSet = Integer.valueOf((int) val);
					else if(Short.TYPE.equals(preRes.getType().getBaseType()))
						ret = toSet = Short.valueOf((short) val);
					else if(Byte.TYPE.equals(preRes.getType().getBaseType()))
						ret = toSet = Byte.valueOf((byte) val);
					else if(Character.TYPE.equals(preRes.getType().getBaseType()))
						ret = toSet = Integer.valueOf((int) val);
					else
						throw new EvaluationException("The operator " + name + " is not defined for types " + preRes + ", " + opType, item,
							item.getStored("name").index);
				} else
					ret = toSet = null;
			} else if(Boolean.TYPE.equals(preRes.getType().getBaseType()) && Boolean.TYPE.equals(opType.getType().getBaseType())) {
				boolean val;
				if("|=".equals(name))
					val = ((Boolean) preRes.getValue()).booleanValue() | ((Boolean) opType.getValue()).booleanValue();
				else if("&=".equals(name))
					val = ((Boolean) preRes.getValue()).booleanValue() & ((Boolean) opType.getValue()).booleanValue();
				else
					val = ((Boolean) preRes.getValue()).booleanValue() ^ ((Boolean) opType.getValue()).booleanValue();
				ret = toSet = Boolean.valueOf(val);
			} else
				throw new EvaluationException("The operator " + name + " is not defined for type " + preRes, item,
					item.getStored("name").index);
		} else if("<<=".equals(name) || ">>=".equals(name) || ">>>=".equals(name)) {
			if(preRes.getType().isIntMathable() && opType.getType().isIntMathable()) {
				if(withValues) {
					try {
						ret = toSet = MathUtils.binaryOp(name.substring(1), preRes.getType(), preRes.getValue(), opType.getType(),
							opType.getValue());
					} catch(RuntimeException e) {
						throw new EvaluationException(e.getMessage(), e, item, item.getStored("name").index);
					}
				} else
					ret = toSet = null;
			} else
				throw new EvaluationException("The operator " + name + " is not defined for type " + preRes, item,
					item.getStored("name").index);
		} else
			throw new EvaluationException("Assignment operator " + name + " not recognized", item, item.getStored("name").index);

		if(!withValues)
			return preRes;
		if(preRes == null)
			preRes = opType;
		if(!(eval.getEvaluatorFor(a.getClass()) instanceof AssignableEvaluator))
			throw new EvaluationException("No assignable evaluator configured for " + a.getClass().getName(), item, a.getMatch().index);
		AssignableEvaluator<Assignable> evaluator = (AssignableEvaluator<Assignable>) eval.getEvaluatorFor(a.getClass());
		evaluator.assign(a, new EvaluationResult(preRes.getType(), toSet), eval, env, item);
		return new EvaluationResult(preRes.getType(), ret);
	}
}
