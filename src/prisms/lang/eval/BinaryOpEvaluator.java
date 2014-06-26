/* ParsedBinaryOp.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.Type;
import prisms.lang.types.ParsedBinaryOp;

/** Represents an operation on two operands */
public class BinaryOpEvaluator implements PrismsItemEvaluator<ParsedBinaryOp> {
	@Override
	public EvaluationResult evaluate(ParsedBinaryOp item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		EvaluationResult res1 = evaluator.evaluate(item.getOp1(), env, false, withValues);
		EvaluationResult res2 = evaluator.evaluate(item.getOp2(), env, false, withValues);
		if(res1.isType() || res1.getPackageName() != null)
			throw new EvaluationException(res1.getFirstVar() + " cannot be resolved to a variable", item, item.getOp1().getMatch().index);
		if(res1.getControl() != null)
			throw new EvaluationException("Syntax error: misplaced construct", item, item.getStored("name").index);
		if(res2.isType() || res2.getPackageName() != null)
			throw new EvaluationException(res2.getFirstVar() + " cannot be resolved to a variable", item, item.getOp2().getMatch().index);
		if(res2.getControl() != null)
			throw new EvaluationException("Syntax error: misplaced construct", item, item.getStored("name").index);

		String name = item.getName();
		Type common = res1.getType().getCommonType(res2.getType());
		if("+".equals(name) || "-".equals(name) || "*".equals(name) || "/".equals(name) || "%".equals(name)) {
			if("+".equals(name) && (String.class.equals(res1.getType().getBaseType()) || String.class.equals(res2.getType().getBaseType()))) {
				StringBuilder value = null;
				if(withValues)
					value = new StringBuilder().append(res1.getValue()).append(res2.getValue());
				return new EvaluationResult(new prisms.lang.Type(String.class), withValues ? value.toString() : null);
			}
			if(!res1.getType().isMathable() || !res2.getType().isMathable())
				throw new EvaluationException("The operator " + name + " is not defined for types " + res1 + ", " + res2, item,
					item.getStored("name").index);
			return new EvaluationResult(common, withValues ? MathUtils.binaryMathOp(name, res1.getType(), res1.getValue(), res2.getType(),
				res2.getValue()) : null);
		} else if("<".equals(name) || "<=".equals(name) || ">".equals(name) || ">=".equals(name)) {
			if(!res1.getType().isMathable() || !res2.getType().isMathable())
				throw new EvaluationException("The operator " + name + " is not defined for types " + res1 + ", " + res2, item,
					item.getStored("name").index);
			return new EvaluationResult(new Type(Boolean.TYPE), withValues ? MathUtils.compare(name, res1.getType(), res1.getValue(),
				res2.getType(),
				res2.getValue()) : null);
		} else if("==".equals(name) || "!=".equals(name)) {
			if(common == null || res1.getType().isPrimitive() != res2.getType().isPrimitive())
				throw new EvaluationException("The operator " + name + " is not defined for types " + res1.typeString() + ", "
					+ res2.typeString(), item, item.getStored("name").index);
			boolean equal = false;
			if(withValues)
				equal = res1.getValue() == res2.getValue();
			if(withValues)
				return new EvaluationResult(new Type(Boolean.TYPE), ("==".equals(name) ? equal : !equal));
			else
				return new EvaluationResult(new Type(Boolean.TYPE), null);
		} else if("||".equals(name)) {
			if(Boolean.TYPE.equals(res1.getType().getBaseType()) && Boolean.TYPE.equals(res2.getType().getBaseType()))
				return new EvaluationResult(res1.getType(), withValues ? (Boolean) res1.getValue() || (Boolean) res2.getValue() : null);
			throw new EvaluationException("The operator " + name + " is not defined for types " + res1.typeString() + ", "
				+ res2.typeString(), item, item.getStored("name").index);
		} else if("&&".equals(name)) {
			if(Boolean.TYPE.equals(res1.getType().getBaseType()) && Boolean.TYPE.equals(res2.getType().getBaseType()))
				return new EvaluationResult(res1.getType(), withValues ? (Boolean) res1.getValue() && (Boolean) res2.getValue() : null);
			throw new EvaluationException("The operator " + name + " is not defined for types " + res1.typeString() + ", "
				+ res2.typeString(), item, item.getStored("name").index);
		} else if("|".equals(name) || "&".equals(name) || "^".equals(name)) {
			if(Boolean.TYPE.equals(res1.getType().getBaseType()) && Boolean.TYPE.equals(res2.getType().getBaseType())) {
				if(!withValues)
					return new EvaluationResult(res1.getType(), (Boolean) null);
				else if("|".equals(name))
					return new EvaluationResult(res1.getType(), (Boolean) res1.getValue() | (Boolean) res2.getValue());
				else if("&".equals(name))
					return new EvaluationResult(res1.getType(), (Boolean) res1.getValue() & (Boolean) res2.getValue());
				else
					return new EvaluationResult(res1.getType(), (Boolean) res1.getValue() ^ (Boolean) res2.getValue());
			} else if(res1.getType().isIntMathable() && res2.getType().isIntMathable()) {
				if(!withValues)
					return new EvaluationResult(common, null);
				return new EvaluationResult(common, MathUtils.binaryMathOp(name, res1.getType(), res1.getValue(), res2.getType(),
					res2.getValue()));
			} else
				throw new EvaluationException("The operator " + name + " is not defined for types " + res1.typeString() + ", "
					+ res2.typeString(), item, item.getStored("name").index);
		} else if("<<".equals(name) || ">>".equals(name) || ">>>".equals(name)) {
			if(!res1.getType().isIntMathable() || !res2.getType().isIntMathable())
				throw new EvaluationException("The operator " + name + " is not defined for types " + res1.typeString() + ", "
					+ res2.typeString(), item, item.getStored("name").index);
			if(!withValues)
				return new EvaluationResult(common, null);
			return new EvaluationResult(common, MathUtils.binaryMathOp(name, res1.getType(), res1.getValue(), res2.getType(),
				res2.getValue()));
		} else
			throw new EvaluationException("Binary operator " + name + " not recognized", item, item.getStored("name").index);
	}
}
