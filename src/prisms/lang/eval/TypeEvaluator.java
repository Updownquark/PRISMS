package prisms.lang.eval;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.Type;
import prisms.lang.types.ParsedType;

/** Represents a type name */
public class TypeEvaluator implements PrismsItemEvaluator<ParsedType> {
	@Override
	public EvaluationResult evaluate(ParsedType item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		if(item.getName() != null) {
			Type [] paramTypes = new Type[item.getParameterTypes().length];
			Class<?> ret = getClassFromName(item.getName(), env);
			if(ret != null) {
				for(int p = 0; p < paramTypes.length; p++)
					paramTypes[p] = evaluator.evaluate(item.getParameterTypes()[p], env, true, withValues).getType();
				if(paramTypes.length > 0 && ret.getTypeParameters().length == 0) {
					String args = prisms.util.ArrayUtils.toString(paramTypes);
					args = args.substring(1, args.length() - 1);
					int index = item.getStored("base").index + item.getStored("base").text.length();
					throw new prisms.lang.EvaluationException("The type " + ret.getName()
						+ " is not generic; it cannot be parameterized with arguments <" + args + ">", item, index);
				}
				if(paramTypes.length > 0 && paramTypes.length != ret.getTypeParameters().length) {
					String type = ret.getName() + "<";
					for(java.lang.reflect.Type t : ret.getTypeParameters())
						type += t + ", ";
					type = type.substring(0, type.length() - 2);
					type += ">";
					String args = prisms.util.ArrayUtils.toString(paramTypes);
					args = args.substring(1, args.length() - 1);
					int index = item.getStored("base").index + item.getStored("base").text.length();
					throw new prisms.lang.EvaluationException("Incorrect number of arguments for type " + type
						+ "; it cannot be parameterized with arguments <" + args + ">", item, index);
				}
				Type t = new Type(ret, paramTypes);
				for(int i = 0; i < item.getArrayDimension(); i++)
					t = t.getArrayType();
				return new EvaluationResult(t);
			}
			StringBuilder name = new StringBuilder(item.getName());
			int idx = name.lastIndexOf(".");
			while(idx >= 0) {
				name.setCharAt(idx, '$');
				ret = getClassFromName(name.toString(), env);
				if(ret != null) {
					for(int p = 0; p < paramTypes.length; p++)
						paramTypes[p] = evaluator.evaluate(item.getParameterTypes()[p], env, true, withValues).getType();
					Type t = new Type(ret, paramTypes);
					for(int i = 0; i < item.getArrayDimension(); i++)
						t = t.getArrayType();
					return new EvaluationResult(t);
				}
				idx = name.lastIndexOf(".");
			}
			name = new StringBuilder(item.getName());
			idx = name.indexOf(".");
			ret = env.getImportType(idx >= 0 ? name.substring(0, idx) : name.toString());
			if(ret != null) {
				if(idx >= 0) {
					name.delete(0, idx + 1);
					for(idx = name.indexOf("."); idx >= 0; idx = name.indexOf("."))
						name.setCharAt(idx, '$');
					ret = getClassFromName(ret.getName() + name, env);
				}
				if(ret != null) {
					for(int p = 0; p < paramTypes.length; p++)
						paramTypes[p] = evaluator.evaluate(item.getParameterTypes()[p], env, true, withValues).getType();
					Type t = new Type(ret, paramTypes);
					for(int i = 0; i < item.getArrayDimension(); i++)
						t = t.getArrayType();
					return new EvaluationResult(t);
				}
			}
			if(paramTypes.length == 0 && item.getArrayDimension() == 0)
				if(env.getClassGetter().isPackage(item.getName()))
					return new EvaluationResult(item.getName());
		} else
			return new EvaluationResult(new Type(evaluator.evaluate(item.getBound(), env, true, withValues).getType(), item.isUpperBound()));

		throw new EvaluationException(item.getName() + " cannot be resolved to a type", item, item.getMatch().index);
	}

	/**
	 * @param name The name of the type to get
	 * @param env The evaluation environment to get the type in
	 * @return The type of the given name, or null if the given name does not refer to a valid type
	 */
	public static Class<?> getClassFromName(String name, prisms.lang.EvaluationEnvironment env) {
		if("boolean".equals(name))
			return Boolean.TYPE;
		else if("char".equals(name))
			return Character.TYPE;
		else if("double".equals(name))
			return Double.TYPE;
		else if("float".equals(name))
			return Float.TYPE;
		else if("long".equals(name))
			return Long.TYPE;
		else if("int".equals(name))
			return Integer.TYPE;
		else if("short".equals(name))
			return Short.TYPE;
		else if("byte".equals(name))
			return Byte.TYPE;
		else if("void".equals(name))
			return Void.TYPE;
		Class<?> clazz;
		try {
			clazz = Class.forName(name);
		} catch(ClassNotFoundException e) {
			clazz = null;
		}
		if(clazz != null)
			return clazz;
		clazz = env.getImportType(name);
		if(clazz != null)
			return clazz;
		try {
			clazz = Class.forName("java.lang." + name);
		} catch(ClassNotFoundException e) {
			clazz = null;
		}
		if(clazz != null)
			return clazz;
		return null;
	}
}
