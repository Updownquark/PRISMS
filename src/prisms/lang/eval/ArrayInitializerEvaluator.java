package prisms.lang.eval;

import java.lang.reflect.Array;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.ParsedArrayInitializer;

/** Evaluates array initializers */
public class ArrayInitializerEvaluator implements PrismsItemEvaluator<ParsedArrayInitializer> {
	@Override
	public EvaluationResult evaluate(final ParsedArrayInitializer item, PrismsEvaluator eval, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		prisms.lang.EvaluationResult typeEval = item.getType().evaluate(env, true, false);
		if(!typeEval.isType())
			throw new EvaluationException("Unrecognized type " + item.getType().getMatch().text, item, item.getType().getMatch().index);
		prisms.lang.Type type = typeEval.getType();
		if(type.getBaseType() == null) {
		}
		if(!withValues)
			return new prisms.lang.EvaluationResult(type, null);
		prisms.lang.Type retType = type;
		Object ret = null;
		if(item.getSizes().length > 0) {
			final int [] sizes = new int[item.getSizes().length];
			for(int i = 0; i < sizes.length; i++) {
				prisms.lang.EvaluationResult sizeEval = eval.evaluate(item.getSizes()[i], env, false, withValues);
				if(sizeEval.isType() || sizeEval.getPackageName() != null)
					throw new EvaluationException(sizeEval.typeString() + " cannot be resolved to a variable", item,
						item.getSizes()[i].getMatch().index);
				if(!sizeEval.isIntType() || Long.TYPE.equals(sizeEval.getType().getBaseType()))
					throw new EvaluationException("Type mismatch: " + sizeEval.typeString() + " cannot be cast to int", item,
						item.getSizes()[i].getMatch().index);
				sizes[i] = ((Number) sizeEval.getValue()).intValue();
			}
			type = type.getComponentType();
			ret = Array.newInstance(type.getBaseType(), sizes[0]);
			class ArrayFiller {
				public void fillArray(Object array, Class<?> componentType, int dimIdx) {
					if(dimIdx == item.getSizes().length)
						return;
					int len = Array.getLength(array);
					for(int i = 0; i < len; i++) {
						Object element = Array.newInstance(componentType, sizes[dimIdx]);
						Array.set(array, i, element);
						fillArray(element, componentType.getComponentType(), dimIdx);
					}
				}
			}
			new ArrayFiller().fillArray(ret, type.getComponentType().getBaseType(), 1);
		} else {
			type = type.getComponentType();
			Object [] elements = new Object[item.getElements().length];
			for(int i = 0; i < elements.length; i++) {
				prisms.lang.EvaluationResult elEval = eval.evaluate(item.getElements()[i], env, false, withValues);
				if(elEval.isType() || elEval.getPackageName() != null)
					throw new EvaluationException(elEval.typeString() + " cannot be resolved to a variable", item,
						item.getSizes()[i].getMatch().index);
				if(!type.isAssignable(elEval.getType()))
					throw new EvaluationException("Type mismatch: cannot convert from " + elEval.typeString() + " to " + type, item,
						item.getElements()[i].getMatch().index);
				elements[i] = elEval.getValue();
			}
			ret = Array.newInstance(type.getBaseType(), elements.length);
			for(int i = 0; i < elements.length; i++)
				Array.set(ret, i, elements[i]);
		}

		return new prisms.lang.EvaluationResult(retType, ret);
	}
}
