/* ParsedIdentifier.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.types.ParsedAssignmentOperator;
import prisms.lang.types.ParsedIdentifier;

/** A simple identifier */
public class IdentifierEvaluator implements AssignableEvaluator<ParsedIdentifier> {
	@Override
	public EvaluationResult evaluate(ParsedIdentifier item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		String name = item.getName();
		if("null".equals(name))
			return new EvaluationResult(prisms.lang.Type.NULL, null);
		if(!asType) {
			prisms.lang.Type type = env.getVariableType(name);
			if(type != null)
				return new EvaluationResult(type, withValues ? env.getVariable(name, item, item.getStored("name").index) : null);
		}
		if("boolean".equals(name))
			return new EvaluationResult(new prisms.lang.Type(Boolean.TYPE));
		else if("char".equals(name))
			return new EvaluationResult(new prisms.lang.Type(Character.TYPE));
		else if("double".equals(name))
			return new EvaluationResult(new prisms.lang.Type(Double.TYPE));
		else if("float".equals(name))
			return new EvaluationResult(new prisms.lang.Type(Float.TYPE));
		else if("long".equals(name))
			return new EvaluationResult(new prisms.lang.Type(Long.TYPE));
		else if("int".equals(name))
			return new EvaluationResult(new prisms.lang.Type(Integer.TYPE));
		else if("short".equals(name))
			return new EvaluationResult(new prisms.lang.Type(Short.TYPE));
		else if("byte".equals(name))
			return new EvaluationResult(new prisms.lang.Type(Byte.TYPE));
		Class<?> clazz = env.getClassGetter().getClass(name);
		if(clazz != null)
			return new EvaluationResult(new prisms.lang.Type(clazz));
		clazz = env.getImportType(name);
		if(clazz != null)
			return new EvaluationResult(new prisms.lang.Type(clazz));
		clazz = env.getClassGetter().getClass("java.lang." + name);
		if(clazz != null)
			return new EvaluationResult(new prisms.lang.Type(clazz));
		if(env.getClassGetter().isPackage(name))
			return new EvaluationResult(name);
		Class<?> importType = env.getImportMethodType(name);
		if(importType != null) {
			for(java.lang.reflect.Field f : importType.getDeclaredFields()) {
				if(!name.equals(f.getName()))
					continue;
				if((f.getModifiers() & java.lang.reflect.Modifier.STATIC) == 0)
					continue;
				if(env.usePublicOnly() && (f.getModifiers() & java.lang.reflect.Modifier.PUBLIC) == 0)
					continue;
				try {
					return new EvaluationResult(new prisms.lang.Type(f.getGenericType()), withValues ? f.get(null) : null);
				} catch(Exception e) {
					throw new EvaluationException("Could not access field " + name + " on type " + prisms.lang.Type.typeString(importType),
						e, item, item.getStored("name").index);
				}
			}
		}
		throw new EvaluationException(name + " cannot be resolved to a " + (asType ? "type" : "variable"), item, item.getMatch().index);
	}

	@Override
	public EvaluationResult getValue(ParsedIdentifier item, PrismsEvaluator eval, EvaluationEnvironment env, ParsedAssignmentOperator assign)
		throws EvaluationException {
		return evaluate(item, eval, env, false, true);
	}

	@Override
	public void assign(ParsedIdentifier item, EvaluationResult value, PrismsEvaluator eval, EvaluationEnvironment env,
		ParsedAssignmentOperator assign) throws EvaluationException {
		env.setVariable(item.getName(), value.getValue(), assign, assign.getStored("name").index);
	}
}
