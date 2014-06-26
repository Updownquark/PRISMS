package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.Type;
import prisms.lang.types.ParsedEnhancedForLoop;

/** Represents an enhanced for loop */
public class EnhancedForLoopEvaluator implements PrismsItemEvaluator<ParsedEnhancedForLoop> {
	@Override
	public EvaluationResult evaluate(ParsedEnhancedForLoop item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		DeclarationEvaluator decEval = new DeclarationEvaluator();
		EvaluationEnvironment scoped = env.scope(true);
		evaluator.evaluate(item.getVariable(), scoped, false, withValues);
		final EvaluationResult iterRes = evaluator.evaluate(item.getIterable(), scoped, false, withValues);
		if(withValues) {
			if(!iterRes.isValue())
				throw new EvaluationException(iterRes.typeString() + " cannot be resolved to a variable", item, item.getIterable()
					.getMatch().index);
			Type instanceType;
			if(Iterable.class.isAssignableFrom(iterRes.getType().getBaseType()))
				instanceType = iterRes.getType().resolve(Iterable.class.getTypeParameters()[0], Iterable.class, null,
					new java.lang.reflect.Type[0], new Type[0]);
			else if(iterRes.getType().isArray())
				instanceType = iterRes.getType().getComponentType();
			else
				throw new EvaluationException("Can only iterate over an array or an instanceof java.lang.Iterable", item, item
					.getIterable().getMatch().index);

			Type type = decEval.evaluateType(item.getVariable(), evaluator, env);
			if(!type.isAssignable(instanceType))
				throw new EvaluationException("Type mismatch: cannot convert from " + instanceType + " to " + type, item.getIterable(),
					item.getIterable().getMatch().index);

			java.util.Iterator<?> iterator;
			if(iterRes.getValue() instanceof Iterable)
				iterator = ((Iterable<?>) iterRes.getValue()).iterator();
			else {
				class ArrayIterator implements java.util.Iterator<Object> {
					private int theIndex;

					private int theLength = java.lang.reflect.Array.getLength(iterRes.getValue());

					@Override
					public boolean hasNext() {
						return theIndex < theLength;
					}

					@Override
					public Object next() {
						return java.lang.reflect.Array.get(iterRes.getValue(), theIndex++);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				}
				iterator = new ArrayIterator();
			}
			iterLoop: while(iterator.hasNext()) {
				if(env.isCanceled())
					throw new EvaluationException("User canceled execution", item, item.getMatch().index);
				Object value = iterator.next();
				scoped.setVariable(item.getVariable().getName(), value, item.getVariable(), item.getVariable().getStored("name").index);
				prisms.lang.EvaluationResult res = evaluator.evaluate(item.getContents(), scoped, false, withValues);
				if(res != null && res.getControl() != null) {
					switch (res.getControl()) {
					case RETURN:
						return res;
					case CONTINUE:
						continue;
					case BREAK:
						break iterLoop;
					}
				}
			}
		} else
			evaluator.evaluate(item.getContents(), scoped, asType, withValues);
		return null;
	}
}
