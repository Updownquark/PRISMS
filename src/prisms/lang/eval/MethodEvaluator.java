/* ParsedMethod.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import java.lang.reflect.Modifier;

import prisms.lang.*;
import prisms.lang.types.ParsedAssignmentOperator;
import prisms.lang.types.ParsedDeclaration;
import prisms.lang.types.ParsedFunctionDeclaration;
import prisms.lang.types.ParsedMethod;

/**
 * Represents one of:
 * <ul>
 * <li><b>A function:</b> An operation with no context, in the form of fn(arg1, arg2...)</li>
 * <li><b>A field:</b> A property out of a context, in the form of ctx.fieldName</li>
 * <li><b>A method:</b> An operation with a context, in the form of ctx.fn(arg1, arg2...)</li>
 * </ul>
 */
public class MethodEvaluator implements AssignableEvaluator<ParsedMethod> {
	@Override
	public EvaluationResult evaluate(ParsedMethod item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		DeclarationEvaluator decEval = new DeclarationEvaluator();
		FunctionDeclarationEvaluator funcEval = new FunctionDeclarationEvaluator();
		String name = item.getName();
		EvaluationResult ctxType;
		EvaluationResult [] argRes = new EvaluationResult[item.getArguments().length];
		for(int i = 0; i < argRes.length; i++) {
			argRes[i] = evaluator.evaluate(item.getArguments()[i], env, false, withValues);
			if(argRes[i].getPackageName() != null || argRes[i].isType())
				throw new EvaluationException(argRes[i].getFirstVar() + " cannot be resolved to a variable", item,
					item.getArguments()[i].getMatch().index);
		}
		if(item.getContext() == null) {
			Class<?> c = env.getImportMethodType(name);
			if(c != null)
				ctxType = new EvaluationResult(new Type(c));
			else {
				ParsedFunctionDeclaration [] funcs = env.getDeclaredFunctions();
				ParsedFunctionDeclaration goodTarget = null;
				ParsedFunctionDeclaration badTarget = null;
				for(ParsedFunctionDeclaration func : funcs) {
					if(!func.getName().equals(name))
						continue;

					Type [] paramTypes;
					int p;
					ParsedDeclaration [] _paramTypes = func.getParameters();
					paramTypes = new Type[_paramTypes.length];
					for(p = 0; p < paramTypes.length; p++)
						paramTypes[p] = decEval.evaluateType(_paramTypes[p], evaluator, env);
					if(!func.isVarArgs() && paramTypes.length != argRes.length)
						continue;
					if(paramTypes.length > argRes.length + 1)
						continue;
					boolean bad = false;
					for(p = 0; !bad && p < paramTypes.length - 1; p++) {
						if(!paramTypes[p].isAssignable(argRes[p].getType()))
							bad = true;
					}
					if(bad) {
						if(badTarget == null)
							badTarget = func;
						continue;
					}
					ParsedFunctionDeclaration target = null;
					if(paramTypes.length == argRes.length && (paramTypes.length == 0 || paramTypes[p].isAssignable(argRes[p].getType())))
						target = func;
					else if(func.isVarArgs()) {
						Type varArgType = paramTypes[p].getComponentType();
						for(; !bad && p < argRes.length; p++)
							if(!varArgType.isAssignable(argRes[p].getType()))
								bad = true;
						if(!bad) {
							target = func;
							Type arrType;
							if(argRes.length < paramTypes.length)
								arrType = varArgType;
							else {
								arrType = argRes[paramTypes.length - 1].getType();
								for(int i = paramTypes.length; i < argRes.length; i++)
									arrType = arrType.getCommonType(argRes[i].getType());
							}
							Object newArg = java.lang.reflect.Array.newInstance(arrType.toClass(), argRes.length - paramTypes.length + 1);
							for(int i = paramTypes.length - 1; i < argRes.length; i++)
								java.lang.reflect.Array.set(newArg, i - paramTypes.length + 1, argRes[i].getValue());
							EvaluationResult [] newArgRes = new prisms.lang.EvaluationResult[paramTypes.length];
							System.arraycopy(argRes, 0, newArgRes, 0, newArgRes.length - 1);
							newArgRes[newArgRes.length - 1] = new EvaluationResult(arrType.getArrayType(), newArg);
							argRes = newArgRes;
						}
					}
					if(target == null) {
						if(badTarget == null)
							badTarget = func;
						continue;
					}
					goodTarget = target;
					break;
				}
				if(goodTarget != null)
					return funcEval.execute(goodTarget, evaluator, env, argRes, withValues);
				else if(badTarget != null) {
					StringBuilder msg = new StringBuilder();
					msg.append(name).append('(');
					ParsedDeclaration [] paramTypes = badTarget.getParameters();
					int p;
					for(p = 0; p < paramTypes.length - 1; p++) {
						msg.append(evaluator.evaluate(paramTypes[p].getType(), env, true, withValues).getType());
						msg.append(", ");
					}
					if(badTarget.isVarArgs())
						msg.append(evaluator.evaluate(paramTypes[p].getType(), env, true, withValues).getType().getComponentType()).append(
							"...");
					else
						msg.append(evaluator.evaluate(paramTypes[p].getType(), env, true, withValues).getType());
					msg.append(')');
					StringBuilder types = new StringBuilder();
					for(p = 0; p < argRes.length; p++) {
						if(p > 0)
							types.append(", ");
						types.append(argRes[p].getType());
					}
					throw new EvaluationException("The function " + msg + " is undefined for parameter types " + types, item,
						item.getStored("name").index);
				} else
					throw new EvaluationException((item.isMethod() ? "Method " : "Field ") + name + " unrecognized", item,
						item.getStored("name").index);
			}
		} else
			ctxType = evaluator.evaluate(item.getContext(), env, false, withValues);
		if(ctxType == null)
			throw new EvaluationException("No value for context to " + (item.isMethod() ? "method " : "field ") + name, item, item
				.getContext().getMatch().index);
		boolean isStatic = ctxType.isType();
		if(!item.isMethod()) {
			if(ctxType.getPackageName() != null || ctxType.isType()) {
				// Could be a class name or a more specific package name
				String typeName;
				if(ctxType.getPackageName() != null)
					typeName = ctxType.getPackageName() + "." + name;
				else
					typeName = ctxType.getType().toString() + "$" + name;
				java.lang.Class<?> clazz = env.getClassGetter().getClass(typeName);
				if(clazz != null)
					return new EvaluationResult(new Type(clazz));
				if(env.getClassGetter().isPackage(typeName))
					return new EvaluationResult(typeName);
				if(!ctxType.isType())
					throw new EvaluationException(ctxType.getFirstVar() + " cannot be resolved to a variable", item, item.getContext()
						.getMatch().index);
			}
			if(!ctxType.isType() && ctxType.getType().isPrimitive())
				throw new EvaluationException("The primitive type " + ctxType.getType().getBaseType().getName() + " does not have a field "
					+ name, item, item.getContext().getMatch().index + item.getContext().getMatch().text.length());
			if(name.equals("length") && ctxType.getType().isArray())
				return new EvaluationResult(new Type(Integer.TYPE), withValues ? Integer.valueOf(java.lang.reflect.Array.getLength(ctxType
					.getValue())) : null);
			else if(name.equals("class") && ctxType.isType())
				return new EvaluationResult(new Type(Class.class, ctxType.getType()), ctxType.getType().getBaseType());
			java.lang.reflect.Field field;
			try {
				field = ctxType.getType().getBaseType().getField(name);
			} catch(Exception e) {
				throw new EvaluationException("Could not access field " + name + " on type " + ctxType.typeString(), e, item,
					item.getStored("name").index);
			}
			if(field == null)
				throw new EvaluationException(ctxType.typeString() + "." + name + " cannot be resolved or is not a field", item,
					item.getStored("name").index);
			if(env.usePublicOnly() && (field.getModifiers() & Modifier.PUBLIC) == 0)
				throw new EvaluationException(ctxType.typeString() + "." + name + " is not visible", item, item.getStored("name").index);
			if(isStatic && (field.getModifiers() & Modifier.STATIC) == 0)
				throw new EvaluationException("Cannot make a static reference to non-static field " + name + " from the type "
					+ ctxType.typeString() + "." + name + " is not static", item, item.getStored("name").index);
			if(withValues && !field.isAccessible() && !env.usePublicOnly()) {
				try {
					field.setAccessible(true);
				} catch(SecurityException e) {
					throw new EvaluationException("Field " + field.getName() + " of type " + field.getDeclaringClass().getName()
						+ " cannot be accessed from the current security context", item, item.getStored("name").index);
				}
			}
			try {
				return new EvaluationResult(ctxType.getType().resolve(field.getGenericType(), field.getDeclaringClass(), null,
					new java.lang.reflect.Type[0], new Type[0]), withValues ? field.get(ctxType.getValue()) : null);
			} catch(Exception e) {
				throw new EvaluationException("Retrieval of field " + field.getName() + " of type " + field.getDeclaringClass().getName()
					+ " failed", e, item, item.getStored("name").index);
			}
		} else {
			if(ctxType.getPackageName() != null)
				throw new EvaluationException(ctxType.getFirstVar() + " cannot be resolved to a variable", item, item.getContext()
					.getMatch().index);
			if(!ctxType.isType() && ctxType.getType().isPrimitive()) {
				StringBuilder msg = new StringBuilder();
				msg.append(name).append('(');
				int p;
				for(p = 0; p < argRes.length; p++) {
					if(p > 0)
						msg.append(", ");
					msg.append(argRes[p].typeString());
				}
				msg.append(')');
				throw new EvaluationException("Cannot invoke " + msg + " on primitive type " + ctxType.getType(), item, item.getContext()
					.getMatch().index + item.getContext().getMatch().text.length());
			}
			if("getClass".equals(name)) {
				if(isStatic)
					throw new EvaluationException("Cannot access the non-static getClass() method from a static context", item,
						item.getStored("name").index);
				try {
					return new EvaluationResult(new Type(Class.class, ctxType.getType()), withValues ? ctxType.getValue().getClass() : null);
				} catch(NullPointerException e) {
					throw new EvaluationException("Argument to getClass() is null", e, item, item.getStored("dot").index);
				}
			}
			java.lang.reflect.Method [] methods;
			if(ctxType.getType() == Type.NULL)
				methods = Object.class.getMethods();
			else
				methods = ctxType.getType().getBaseType().getMethods();
			if(!env.usePublicOnly())
				methods = org.qommons.ArrayUtils.mergeInclusive(java.lang.reflect.Method.class, methods, ctxType.getType().getBaseType()
					.getDeclaredMethods());
			java.lang.reflect.Method goodTarget = null;
			java.lang.reflect.Method badTarget = null;
			java.util.Map<String, Type> inferred = new java.util.HashMap<>();
			Type [] argTypes = new Type[argRes.length];
			for(int i = 0; i < argTypes.length; i++)
				argTypes[i] = argRes[i].getType();
			for(java.lang.reflect.Method m : methods) {
				if(!m.getName().equals(name) || m.isSynthetic())
					continue;

				java.lang.reflect.Type [] _paramTypes = m.getGenericParameterTypes();
				Type [] paramTypes = new Type[_paramTypes.length];
				for(int p = 0; p < paramTypes.length; p++)
					paramTypes[p] = ctxType.getType().resolve(_paramTypes[p], m.getDeclaringClass(), null, m.getGenericParameterTypes(),
						argTypes);
				if(paramTypes.length > argRes.length + 1)
					continue;
				inferred.clear();
				inferMethodTypes(inferred, m, argTypes);
				boolean bad = false;
				int p;
				for(p = 0; !bad && p < paramTypes.length - 1; p++) {
					if(!paramTypes[p].isAssignable(argRes[p].getType()))
						bad = true;
				}
				if(bad) {
					if(badTarget == null)
						badTarget = m;
					continue;
				}
				java.lang.reflect.Method target = null;
				if(paramTypes.length == argRes.length && (paramTypes.length == 0 || paramTypes[p].isAssignable(argRes[p].getType())))
					target = m;
				else if(m.isVarArgs()) {
					Type varArgType = paramTypes[p].getComponentType();
					for(; !bad && p < argRes.length; p++)
						if(!varArgType.isAssignable(argRes[p].getType()))
							bad = true;
					if(!bad) {
						target = m;
						Type arrType;
						if(argRes.length < paramTypes.length)
							arrType = varArgType;
						else {
							arrType = argRes[paramTypes.length - 1].getType();
							for(int i = paramTypes.length; i < argRes.length; i++)
								arrType = arrType.getCommonType(argRes[i].getType());
						}
						Object newArg = java.lang.reflect.Array.newInstance(arrType.toClass(), argRes.length - paramTypes.length + 1);
						for(int i = paramTypes.length - 1; i < argRes.length; i++)
							java.lang.reflect.Array.set(newArg, i - paramTypes.length + 1, argRes[i].getValue());
						EvaluationResult [] newArgRes = new prisms.lang.EvaluationResult[paramTypes.length];
						System.arraycopy(argRes, 0, newArgRes, 0, newArgRes.length - 1);
						newArgRes[newArgRes.length - 1] = new EvaluationResult(arrType.getArrayType(), newArg);
						argRes = newArgRes;
					}
				}
				if(target == null) {
					if(badTarget == null)
						badTarget = m;
					continue;
				}
				if(env.usePublicOnly() && (target.getModifiers() & Modifier.PUBLIC) == 0)
					badTarget = target;
				else if(isStatic && (target.getModifiers() & Modifier.STATIC) == 0)
					badTarget = target;
				else {
					goodTarget = target;
					break;
				}
			}
			if(goodTarget != null) {
				for(java.lang.reflect.Type c : goodTarget.getGenericExceptionTypes()) {
					Type ct = ctxType.getType().resolve(c, goodTarget.getDeclaringClass(), inferred, goodTarget.getGenericParameterTypes(),
						argTypes);
					if(!env.canHandle(ct))
						throw new prisms.lang.EvaluationException("Unhandled exception type " + ct, item, item.getStored("name").index);
				}
				Class<?> [] paramTypes = goodTarget.getParameterTypes();
				Object [] args = new Object[paramTypes.length];
				for(int i = 0; i < args.length - 1; i++)
					args[i] = argRes[i].getValue();
				if(!goodTarget.isVarArgs()) {
					if(args.length > 0)
						args[args.length - 1] = argRes[args.length - 1].getValue();
				} else
					args[args.length - 1] = argRes[argRes.length - 1].getValue();
				if(withValues && !isStatic && !Modifier.isStatic(goodTarget.getModifiers()) && ctxType.getValue() == null)
					throw new ExecutionException(new Type(NullPointerException.class), new NullPointerException(), item.getContext(), item
						.getContext().getMatch().index);
				if(withValues && !goodTarget.isAccessible() && !env.usePublicOnly()) {
					try {
						goodTarget.setAccessible(true);
					} catch(SecurityException e) {
						throw new EvaluationException("Method " + goodTarget.getName() + " of type "
							+ goodTarget.getDeclaringClass().getName() + " cannot be accessed from the current security context", item,
							item.getStored("name").index);
					}
				}
				try {
					Type retType = ctxType.getType().resolve(goodTarget.getGenericReturnType(), goodTarget.getDeclaringClass(), inferred,
						goodTarget.getGenericParameterTypes(), argTypes);
					return new EvaluationResult(retType, withValues ? goodTarget.invoke(ctxType.getValue(), args) : null);
				} catch(java.lang.reflect.InvocationTargetException e) {
					throw new ExecutionException(new Type(e.getCause().getClass()), e.getCause(), item, item.getStored("name").index);
				} catch(Exception e) {
					throw new EvaluationException("Could not invoke method " + name + " of class " + ctxType.typeString(), e, item,
						item.getStored("name").index);
				}
			} else if(badTarget != null) {
				StringBuilder msg = new StringBuilder();
				msg.append(name).append('(');
				java.lang.reflect.Type [] paramTypes = badTarget.getGenericParameterTypes();
				int p;
				for(p = 0; p < paramTypes.length - 1; p++) {
					msg.append(new Type(paramTypes[p]));
					msg.append(", ");
				}
				if(badTarget.isVarArgs())
					msg.append(new Type(paramTypes[p]).getComponentType()).append("...");
				else
					msg.append(new Type(paramTypes[p]));
				msg.append(')');
				if(env.usePublicOnly() && (badTarget.getModifiers() & Modifier.PUBLIC) == 0)
					throw new EvaluationException("The method " + msg + " from the type " + ctxType.typeString() + "." + name
						+ " is not visible", item, item.getStored("name").index);
				if(isStatic && (badTarget.getModifiers() & Modifier.STATIC) == 0)
					throw new EvaluationException("Cannot make a static reference to the non-static method " + msg + " from the type "
						+ ctxType.typeString(), item, item.getStored("name").index);
				else {
					StringBuilder types = new StringBuilder();
					for(p = 0; p < argRes.length; p++) {
						if(p > 0)
							types.append(", ");
						types.append(argRes[p].getType());
					}
					throw new EvaluationException("The method " + ctxType.getType() + "." + msg + " is undefined for parameter types "
						+ types, item, item.getStored("name").index);
				}
			} else {
				StringBuilder msg = new StringBuilder();
				msg.append(name).append('(');
				int a;
				for(a = 0; a < argRes.length; a++) {
					msg.append(argRes[a].typeString());
					if(a < argRes.length - 1)
						msg.append(", ");
				}
				msg.append(')');
				throw new EvaluationException("The method " + msg + " is undefined for the type " + ctxType.typeString(), item,
					item.getStored("name").index);
			}
		}
	}

	/**
	 * Infers, where possible, the types used in an invocation for a method's type parameters
	 *
	 * @param inferred The map to fill in--entries are type variable name/type
	 * @param method The method to infer the types for
	 * @param argTypes The types of the arguments to the method for the invocation
	 */
	public static void inferMethodTypes(java.util.Map<String, Type> inferred, java.lang.reflect.Method method, Type [] argTypes) {
		java.lang.reflect.TypeVariable<?> [] typeParams = method.getTypeParameters();
		java.lang.reflect.Type [] paramTypes = method.getGenericParameterTypes();
		for(int t = 0; t < typeParams.length; t++) {
			Type type = null;
			for(int p = 0; p < paramTypes.length && p < argTypes.length; p++) {
				Type check = inferMethodType(typeParams[t].getName(), paramTypes[p], argTypes[p]);
				if(check != null) {
					if(type != null)
						type = type.getCommonType(check);
					else
						type = check;
				}
			}
			if(type != null)
				inferred.put(typeParams[t].getName(), type);
		}
	}

	private static Type inferMethodType(String tvName, java.lang.reflect.Type paramType, Type argType) {
		if(paramType instanceof java.lang.reflect.TypeVariable) {
			java.lang.reflect.TypeVariable<?> tv = (java.lang.reflect.TypeVariable<?>) paramType;
			if(tv.getName().equals(tvName))
				return argType;
		} else if(paramType instanceof java.lang.reflect.GenericArrayType) {
			Type ct = inferMethodType(tvName, ((java.lang.reflect.GenericArrayType) paramType).getGenericComponentType(), argType);
			if(ct != null)
				return ct.getComponentType();
		} else if(paramType instanceof java.lang.reflect.WildcardType) {
			java.lang.reflect.WildcardType wt = (java.lang.reflect.WildcardType) paramType;
			if(wt.getUpperBounds().length == 1) {
				Type bound = inferMethodType(tvName, wt.getUpperBounds()[0], argType);
				if(bound != null)
					return new Type(bound, true);
			}
		} else if(paramType instanceof java.lang.reflect.ParameterizedType) {
			java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) paramType;
			if(argType.getParamTypes() != null)
				for(int p = 0; p < pt.getActualTypeArguments().length && p < argType.getParamTypes().length; p++) {
					Type ret = inferMethodType(tvName, pt.getActualTypeArguments()[p], argType.getParamTypes()[p]);
					if(ret != null)
						return ret;
				}
		}
		return null;
	}

	@Override
	public EvaluationResult getValue(ParsedMethod item, PrismsEvaluator eval, EvaluationEnvironment env, ParsedAssignmentOperator assign)
		throws EvaluationException {
		String name = item.getName();
		if(item.isMethod())
			throw new EvaluationException("Invalid argument for operator " + name, item, item.getMatch().index);
		EvaluationResult ctxType;
		if(item.getContext() == null) {
			Class<?> c = env.getImportMethodType(name);
			if(c == null)
				throw new EvaluationException(name + " cannot be resolved or is not a field", item, item.getStored("name").index);
			ctxType = new EvaluationResult(new Type(env.getImportMethodType(name)));
		} else
			ctxType = eval.evaluate(item.getContext(), env, false, true);
		boolean isStatic = ctxType.isType();
		if(name.equals("length") && ctxType.getType().isArray())
			throw new EvaluationException("The final field array.length cannot be assigned", item, item.getStored("name").index);
		java.lang.reflect.Field field;
		try {
			field = ctxType.getType().getBaseType().getField(name);
		} catch(Exception e) {
			throw new EvaluationException("Could not access field " + name + " on type " + ctxType.typeString(), e, item,
				item.getStored("name").index);
		}
		if(field == null)
			throw new EvaluationException(ctxType.typeString() + "." + name + " cannot be resolved or is not a field", item,
				item.getStored("name").index);
		if(env.usePublicOnly() && (field.getModifiers() & Modifier.PUBLIC) == 0)
			throw new EvaluationException(ctxType.typeString() + "." + name + " is not visible", item, item.getStored("name").index);
		if(isStatic && (field.getModifiers() & Modifier.STATIC) == 0)
			throw new EvaluationException("Cannot make a static reference to non-static field " + name + " from the type "
				+ ctxType.typeString() + "." + name + " is not static", item, item.getStored("name").index);
		if((field.getModifiers() & Modifier.FINAL) != 0)
			throw new EvaluationException("The final field " + ctxType.typeString() + "." + name + " cannot be assigned", item,
				item.getStored("name").index);
		try {
			return new EvaluationResult(new Type(field.getGenericType()), field.get(ctxType.getValue()));
		} catch(Exception e) {
			throw new EvaluationException("Could not access field " + field.getName() + " of class " + field.getDeclaringClass().getName(),
				e, item, item.getStored("name").index);
		}
	}

	@Override
	public void assign(ParsedMethod item, EvaluationResult value, PrismsEvaluator eval, EvaluationEnvironment env,
		ParsedAssignmentOperator assign) throws EvaluationException {
		String name = item.getName();
		if(item.isMethod())
			throw new EvaluationException("Invalid argument for operator " + name, item, item.getMatch().index);
		EvaluationResult ctxType;
		if(item.getContext() == null) {
			Class<?> c = env.getImportMethodType(name);
			if(c == null)
				throw new EvaluationException(name + " cannot be resolved or is not a field", item, item.getStored("name").index);
			ctxType = new EvaluationResult(new Type(env.getImportMethodType(name)));
		} else
			ctxType = eval.evaluate(item.getContext(), env, false, true);
		boolean isStatic = ctxType.isType();
		if(name.equals("length") && ctxType.getType().isArray())
			throw new EvaluationException("The final field array.length cannot be assigned", item, item.getStored("name").index);
		java.lang.reflect.Field field;
		try {
			field = ctxType.getType().getBaseType().getField(name);
		} catch(Exception e) {
			throw new EvaluationException("Could not access field " + name + " on type " + ctxType.typeString(), e, item,
				item.getStored("name").index);
		}
		if(field == null)
			throw new EvaluationException(ctxType.typeString() + "." + name + " cannot be resolved or is not a field", item,
				item.getStored("name").index);
		if(env.usePublicOnly() && (field.getModifiers() & Modifier.PUBLIC) == 0)
			throw new EvaluationException(ctxType.typeString() + "." + name + " is not visible", item, item.getStored("name").index);
		if(isStatic && (field.getModifiers() & Modifier.STATIC) == 0)
			throw new EvaluationException("Cannot make a static reference to non-static field " + name + " from the type "
				+ ctxType.typeString() + "." + name + " is not static", item, item.getStored("name").index);
		if((field.getModifiers() & Modifier.FINAL) != 0)
			throw new EvaluationException("The final field " + ctxType.typeString() + "." + name + " cannot be assigned", item,
				item.getStored("name").index);
		try {
			field.set(ctxType.getValue(), value.getValue());
		} catch(Exception e) {
			throw new EvaluationException("Could not assign field " + field.getName() + " of class " + field.getDeclaringClass().getName(),
				e, item, item.getStored("name").index);
		}
	}
}
