/* ParsedConstructor.java Created Nov 15, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import prisms.lang.*;
import prisms.lang.types.ParsedConstructor;
import prisms.lang.types.ParsedDeclaration;
import prisms.lang.types.ParsedFunctionDeclaration;

/** Represents a constructor or anonymous class declaration */
public class ConstructorEvaluator implements PrismsItemEvaluator<ParsedConstructor> {
	@Override
	public EvaluationResult evaluate(ParsedConstructor item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		prisms.lang.EvaluationResult type = evaluator.evaluate(item.getType(), env, true, false);
		if(!type.isType())
			throw new EvaluationException(type.typeString() + " cannot be resolved to a type", item, item.getType().getMatch().index);
		if(!item.isAnonymous())
			return evaluateConstructor(item, evaluator, env, type.getType(), withValues);
		else
			return evaluateAnonymous(item, evaluator, env, type.getType(), withValues);
	}

	/**
	 * @param item The item to use when throwing evaluation exceptions
	 * @param type The type to get the constructor for
	 * @param argTypes The argument types to pass to the constructor
	 * @param publicOnly Whether to look at only public constructors or all of them
	 * @return The constructor instance that matches the arguments
	 * @throws EvaluationException If no such constructor matches the arguments or if the constructor is not visible (when publicOnly is
	 *             true).
	 */
	@SuppressWarnings("rawtypes")
	public Constructor<?> getTarget(ParsedItem item, Type type, Type [] argTypes, boolean publicOnly) throws EvaluationException {
		Constructor [] constructors;
		if(publicOnly)
			constructors = type.getBaseType().getConstructors();
		else
			constructors = type.getBaseType().getDeclaredConstructors();

		Constructor goodTarget = null;
		Constructor badTarget = null;
		for(Constructor c : constructors) {
			java.lang.reflect.Type [] _paramTypes = c.getGenericParameterTypes();
			Type [] paramTypes = new Type[_paramTypes.length];
			for(int p = 0; p < paramTypes.length; p++)
				paramTypes[p] = type.resolve(_paramTypes[p], c.getDeclaringClass(), null, c.getGenericParameterTypes(), argTypes);
			if(paramTypes.length > argTypes.length + 1)
				continue;
			boolean bad = false;
			int p;
			for(p = 0; !bad && p < paramTypes.length - 1; p++) {
				if(!paramTypes[p].isAssignable(argTypes[p]))
					bad = true;
			}
			if(bad) {
				if(badTarget == null)
					badTarget = c;
				continue;
			}
			Constructor target = null;
			if(paramTypes.length == argTypes.length && (paramTypes.length == 0 || paramTypes[p].isAssignable(argTypes[p])))
				target = c;
			else if(c.isVarArgs()) {
				Type varArgType = paramTypes[paramTypes.length - 1].getComponentType();
				for(; !bad && p < argTypes.length; p++)
					if(!varArgType.isAssignable(argTypes[p]))
						bad = true;
				if(!bad)
					target = c;
			}
			if(target == null) {
				if(badTarget == null)
					badTarget = c;
				continue;
			}
			if(publicOnly && (target.getModifiers() & java.lang.reflect.Modifier.PUBLIC) == 0)
				badTarget = target;
			else {
				goodTarget = target;
				break;
			}
		}
		if(goodTarget != null)
			return goodTarget;
		else if(badTarget != null) {
			StringBuilder msg = new StringBuilder();
			msg.append("new ").append(type.getBaseType().getName()).append('(');
			Class<?> [] paramTypes = badTarget.getParameterTypes();
			int p;
			for(p = 0; p < paramTypes.length - 1; p++) {
				msg.append(prisms.lang.Type.typeString(paramTypes[p]));
				msg.append(", ");
			}
			if(badTarget.isVarArgs())
				msg.append(prisms.lang.Type.typeString(paramTypes[p].getComponentType())).append("...");
			else
				msg.append(prisms.lang.Type.typeString(paramTypes[p]));
			msg.append(')');
			if(publicOnly && (badTarget.getModifiers() & java.lang.reflect.Modifier.PUBLIC) == 0)
				throw new EvaluationException("The constructor " + msg + " is not visible", item, item.getStored("type").index);
			StringBuilder types = new StringBuilder();
			for(p = 0; p < argTypes.length; p++) {
				if(p > 0)
					types.append(", ");
				types.append(argTypes[p]);
			}
			throw new EvaluationException("The constructor " + msg + " is undefined for parameter types " + types, item,
				item.getStored("type").index);
		} else {
			StringBuilder msg = new StringBuilder();
			msg.append("new ").append(type.getBaseType().getName()).append('(');
			int p;
			for(p = 0; p < argTypes.length; p++) {
				if(p > 0)
					msg.append(", ");
				msg.append(argTypes[p]);
			}
			msg.append(')');
			throw new EvaluationException("The constructor " + msg + " is undefined", item, item.getStored("type").index);
		}
	}

	EvaluationResult evaluateConstructor(ParsedConstructor item, PrismsEvaluator evaluator, EvaluationEnvironment env, Type type,
		boolean withValues) throws EvaluationException {
		EvaluationResult [] argRes = new EvaluationResult[item.getArguments().length];
		Type [] argTypes = new Type[item.getArguments().length];
		for(int i = 0; i < argRes.length; i++) {
			argRes[i] = evaluator.evaluate(item.getArguments()[i], env, false, withValues);
			argTypes[i] = argRes[i].getType();
		}

		Constructor<?> goodTarget = getTarget(item, type, argTypes, env.usePublicOnly());
		for(java.lang.reflect.Type c : goodTarget.getGenericExceptionTypes()) {
			Type ct = new Type(c);
			if(!env.canHandle(ct))
				throw new EvaluationException("Unhandled exception type " + ct, item, item.getMatch().index);
		}
		Class<?> [] paramTypes = goodTarget.getParameterTypes();
		Object [] args = new Object[paramTypes.length];
		for(int i = 0; i < args.length - 1; i++)
			args[i] = argRes[i].getValue();
		if(!goodTarget.isVarArgs()) {
			if(args.length > 0)
				args[args.length - 1] = argRes[args.length - 1].getValue();
		} else {
			Object varArgs = java.lang.reflect.Array.newInstance(paramTypes[args.length - 1].getComponentType(), item.getArguments().length
				- paramTypes.length + 1);
			args[args.length - 1] = varArgs;
			for(int i = paramTypes.length - 1; i < item.getArguments().length; i++)
				java.lang.reflect.Array.set(varArgs, i - paramTypes.length + 1, argRes[i].getValue());
		}
		try {
			return new prisms.lang.EvaluationResult(type, withValues ? goodTarget.newInstance(args) : null);
		} catch(java.lang.reflect.InvocationTargetException e) {
			throw new prisms.lang.ExecutionException(new Type(e.getCause().getClass()), e.getCause(), item, item.getStored("type").index);
		} catch(Exception e) {
			throw new EvaluationException("Could not invoke constructor of class " + type.getBaseType().getName(), e, item,
				item.getStored("type").index);
		}
	}

	EvaluationResult evaluateAnonymous(ParsedConstructor item, PrismsEvaluator evaluator, EvaluationEnvironment env, Type type,
		boolean withValues) throws EvaluationException {
		if(java.lang.reflect.Modifier.isFinal(type.getBaseType().getModifiers()))
			throw new EvaluationException("An anonymous class cannot subclass the final class " + type.getBaseType().getName(), item, item
				.getType().getMatch().index);
		if(!type.getBaseType().isInterface())
			throw new EvaluationException("Only interfaces can be implemented by anonymous" + " classes in this interpreter", item, item
				.getType().getMatch().index);
		prisms.lang.EvaluationEnvironment instanceScope = env.scope(false);
		for(ParsedItem field : item.getFields().getContents()) {
			evaluator.evaluate(field, instanceScope, false, withValues);
			if(field instanceof ParsedDeclaration) {
				String name = ((ParsedDeclaration) field).getName();
				instanceScope.setVariable(name, getDefaultValue(instanceScope.getVariableType(name).getBaseType()), item,
					field.getMatch().index);
			}
		}
		for(java.lang.reflect.Method m : type.getBaseType().getMethods()) {
			boolean found = false;
			for(ParsedFunctionDeclaration f : item.getMethods()) {
				if(!f.getName().equals(m.getName()) || f.getParameters().length != m.getParameterTypes().length)
					continue;
				int p;
				for(p = 0; p < f.getParameters().length; p++) {
					EvaluationResult paramRes = evaluator.evaluate(f.getParameters()[p].getType(), env, true, false);
					if(!paramRes.getType().isAssignable(
						type.resolve(m.getGenericParameterTypes()[p], m.getDeclaringClass(), null, new java.lang.reflect.Type[0],
							new Type[0])))
						break;
				}
				if(p == f.getParameters().length)
					found = true;
				else
					continue;
				Type rt = type.resolve(m.getReturnType(), m.getDeclaringClass(), null, new java.lang.reflect.Type[0], new Type[0]);
				if(!rt.isAssignable(evaluator.evaluate(f.getReturnType(), env, true, false).getType())) {
					StringBuilder msg = new StringBuilder();
					msg.append("Return type").append(f.getReturnType()).append(" not compatible with return type ").append(rt)
					.append(" of method ");
					msg.append(m.getDeclaringClass().getName()).append(m.getName()).append('(');
					for(int p2 = 0; p2 < m.getParameterTypes().length; p2++) {
						if(p2 > 0)
							msg.append(", ");
						msg.append(type.resolve(m.getGenericParameterTypes()[p2], m.getDeclaringClass(), null,
							new java.lang.reflect.Type[0], new Type[0]));
					}
					msg.append(')');
					throw new EvaluationException(msg.toString(), item, f.getMatch().index);
				}
				for(prisms.lang.types.ParsedType exType : f.getExceptionTypes()) {
					Type t = evaluator.evaluate(exType, env, true, false).getType();
					if(t.canAssignTo(RuntimeException.class) || t.canAssignTo(Error.class))
						continue;
					boolean exOk = false;
					for(java.lang.reflect.Type met : m.getGenericExceptionTypes())
						if(type.resolve(met, m.getDeclaringClass(), null, new java.lang.reflect.Type[0], new Type[0]).isAssignable(t)) {
							exOk = true;
							break;
						}
					if(!exOk) {
						StringBuilder msg = new StringBuilder();
						msg.append("Declared throwable type ").append(t).append(" not declared to be thrown by method ");
						msg.append(m.getDeclaringClass().getName()).append(m.getName()).append('(');
						for(int p2 = 0; p2 < m.getParameterTypes().length; p2++) {
							if(p2 > 0)
								msg.append(", ");
							msg.append(type.resolve(m.getGenericParameterTypes()[p2], m.getDeclaringClass(), null,
								new java.lang.reflect.Type[0], new Type[0]));
						}
						msg.append(')');
						throw new EvaluationException(msg.toString(), item, f.getMatch().index);
					}
				}
			}
			if(!found) {
				StringBuilder msg = new StringBuilder();
				msg.append("Anonymous class implementing ").append(item.getType()).append(" must implement the abstract method ");
				msg.append(m.getName()).append('(');
				for(int p = 0; p < m.getParameterTypes().length; p++) {
					if(p > 0)
						msg.append(", ");
					msg.append(type.resolve(m.getGenericParameterTypes()[p], m.getDeclaringClass(), null, new java.lang.reflect.Type[0],
						new Type[0]));
				}
				msg.append(')');
				throw new EvaluationException(msg.toString(), item, item.getType().getMatch().index);
			}
		}
		if(item.getInstanceInitializer() != null)
			evaluator.evaluate(item.getInstanceInitializer(), instanceScope, false, withValues);

		for(ParsedFunctionDeclaration method : item.getMethods())
			evaluator.evaluate(method, instanceScope, false, withValues);

		Object proxy = null;
		if(withValues)
			proxy = java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {type.getBaseType()},
				new AnonymousClassHandler(item, evaluator, type, instanceScope));
		return new prisms.lang.EvaluationResult(type, proxy);
	}

	/**
	 * @param type The type to get the default value for
	 * @return The value that is initialized in a field by default if its value is not initialized by the constructor
	 */
	public static Object getDefaultValue(Class<?> type) {
		if(Boolean.TYPE.equals(type))
			return Boolean.FALSE;
		else if(Character.TYPE.equals(type))
			return Character.valueOf((char) 0);
		else if(Float.TYPE.equals(type))
			return Float.valueOf(0);
		else if(Double.TYPE.equals(type))
			return Double.valueOf(0);
		else if(Long.TYPE.equals(type))
			return Long.valueOf(0);
		else if(Long.TYPE.equals(type))
			return Long.valueOf(0);
		else if(Long.TYPE.equals(type))
			return Long.valueOf(0);
		else if(Long.TYPE.equals(type))
			return Long.valueOf(0);
		else
			return null;
	}

	/** A proxy handler for implementing interfaces with anonymous classes */
	public static class AnonymousClassHandler implements java.lang.reflect.InvocationHandler {
		private final ParsedConstructor theConstructor;
		private final PrismsEvaluator theEvaluator;
		private final Type theImplType;

		private final prisms.lang.EvaluationEnvironment theEnv;

		/**
		 * @param constructor The constructor for the anonymous class
		 * @param evaluator The evaluator for evaluating field types
		 * @param implType The interface type being implemented
		 * @param env The evaluation environment representing this instance's state
		 */
		public AnonymousClassHandler(ParsedConstructor constructor, PrismsEvaluator evaluator, Type implType,
			prisms.lang.EvaluationEnvironment env) {
			theConstructor = constructor;
			theEvaluator = evaluator;
			theImplType = implType;
			theEnv = env;
		}

		/** @return The evaluation environment representing this instance's state */
		public prisms.lang.EvaluationEnvironment getEnv() {
			return theEnv;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object [] args) throws Throwable {
			DeclarationEvaluator decEval = new DeclarationEvaluator();
			FunctionDeclarationEvaluator funcEval = new FunctionDeclarationEvaluator();
			for(ParsedFunctionDeclaration m : theConstructor.getMethods()) {
				if(!m.getName().equals(method.getName()) || m.getParameters().length != method.getParameterTypes().length)
					continue;
				prisms.lang.EvaluationResult [] argRes = new prisms.lang.EvaluationResult[m.getParameters().length];
				int p;
				for(p = 0; p < m.getParameters().length; p++) {
					Type argType = theImplType.resolve(method.getGenericParameterTypes()[p], method.getDeclaringClass(), null,
						new java.lang.reflect.Type[0], new Type[0]);
					if(!decEval.evaluateType(m.getParameters()[p], theEvaluator, theEnv).isAssignable(argType))
						break;
					argRes[p] = new prisms.lang.EvaluationResult(argType, args[p]);
				}
				if(p < m.getParameters().length)
					continue;
				return funcEval.execute(m, theEvaluator, theEnv, argRes, true).getValue();
			}
			if(method.getDeclaringClass().equals(Object.class)) {
				if("toString".equals(method.getName()))
					return theImplType.getName() + "$JITRAnon@" + Integer.toHexString(System.identityHashCode(proxy));
				else if("equals".equals(method.getName()))
					return Boolean.valueOf(proxy == args[0]);
				else if("hashCode".equals(method.getName()))
					return Integer.valueOf(System.identityHashCode(proxy));
			}
			throw new IllegalStateException("Unsupported method! " + method);
		}
	}
}
