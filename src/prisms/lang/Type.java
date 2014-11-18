package prisms.lang;

/** Represents the type of an evaluated expression */
public class Type {
	private Class<?> theBaseType;

	private Type [] theParamTypes;

	private String theName;

	private boolean isBounded;

	private boolean isUpperBound;

	private Type theBoundType;

	/** The type of the "null" identifier */
	public static final Type NULL = new Type(new Object() {
	}.getClass());

	/**
	 * @param base The base type
	 * @param paramTypes The type parameters for the type
	 */
	public Type(Class<?> base, Type... paramTypes) {
		theBaseType = base;
		theParamTypes = paramTypes;
	}

	/**
	 * @param bound The bounding type
	 * @param upper Whether the type is upper- or lower- bound
	 */
	public Type(Type bound, boolean upper) {
		isBounded = true;
		theBoundType = bound;
		isUpperBound = upper;
		theParamTypes = new Type[0];
	}

	/**
	 * @param bound The bounding type
	 * @param upper Whether the type is upper- or lower- bound
	 */
	public Type(Class<?> bound, boolean upper) {
		this(new Type(bound), upper);
	}

	/**
	 * Parses a type from a java.lang.reflect.Type
	 *
	 * @param type The reflected type to represent
	 */
	public Type(java.lang.reflect.Type type) {
		init(type);
	}

	/**
	 * @param value The value to get the type of
	 * @return The type of the value
	 */
	public static Type typeOf(Object value) {
		if(value == null)
			return NULL;
		else
			return new Type(value.getClass());
	}

	private void init(java.lang.reflect.Type type) {
		if(type == null) {
			theBaseType = Void.TYPE;
			theParamTypes = new Type[0];
		} else if(type instanceof Class) {
			theBaseType = (Class<?>) type;
			theParamTypes = new Type[0];
		} else if(type instanceof java.lang.reflect.ParameterizedType) {
			java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) type;
			theBaseType = (Class<?>) pt.getRawType();
			theParamTypes = new Type[pt.getActualTypeArguments().length];
			for(int p = 0; p < theParamTypes.length; p++)
				theParamTypes[p] = new Type(pt.getActualTypeArguments()[p]);
		} else if(type instanceof java.lang.reflect.WildcardType) {
			isBounded = true;
			java.lang.reflect.WildcardType wt = (java.lang.reflect.WildcardType) type;
			isUpperBound = wt.getLowerBounds().length == 0;
			if(wt.getLowerBounds().length > 0)
				theBoundType = new Type(wt.getLowerBounds()[0]);
			else if(wt.getUpperBounds().length > 0)
				theBoundType = new Type(wt.getUpperBounds()[0]);
		} else if(type instanceof java.lang.reflect.TypeVariable) {
			java.lang.reflect.TypeVariable<?> tv = (java.lang.reflect.TypeVariable<?>) type;
			theName = tv.getName();
			isBounded = true;
			isUpperBound = true;
			if(tv.getBounds().length > 0)
				theBoundType = new Type(tv.getBounds()[0]);
		} else if(type instanceof java.lang.reflect.GenericArrayType) {
			java.lang.reflect.GenericArrayType at = (java.lang.reflect.GenericArrayType) type;
			init(at.getGenericComponentType());
			if(theBaseType != null)
				theBaseType = java.lang.reflect.Array.newInstance(theBaseType, 0).getClass();
			else if(theBoundType != null)
				theBoundType = theBoundType.getArrayType();
			else
				theBoundType = new Type(java.lang.reflect.Array.newInstance(Object.class, 0).getClass());
		} else
			throw new IllegalArgumentException("Unrecognize reflect type: " + type.getClass().getName());
	}

	/**
	 * @param c The type to check
	 * @return Whether a variable with a type of the given class can be assigned to a variable of this type
	 */
	public boolean isAssignableFrom(Class<?> c) {
		if(isBounded) {
			if(isUpperBound)
				return theBoundType.isAssignableFrom(c);
			else
				return theBoundType.canAssignTo(c);
		} else
			return isAssignable(theBaseType, c);
	}

	/**
	 * @param c The type to check
	 * @return Whether a variable of this type can be assigned to a variable of the type of the given class
	 */
	public boolean canAssignTo(Class<?> c) {
		if(isBounded) {
			if(isUpperBound)
				return c.isAssignableFrom(c);
			else
				return c == Object.class;
		} else if(this == NULL)
			return true;
		else
			return isAssignable(c, theBaseType);
	}

	/**
	 * @param t The type to check
	 * @return Whether a variable of the given type can be assigned to a variable of this type
	 */
	public boolean isAssignable(Type t) {
		if(t != null && t == NULL)
			return !isPrimitive();
		if(isBounded) {
			if(t == null)
				return theBoundType == null || theBoundType.isAssignable(null);
			else if(t.isBounded) {
				if(isUpperBound) {
					if(t.isUpperBound)
						return theBoundType.isAssignable(t.theBoundType);
					else
						return theBoundType == null || theBoundType.isAssignable(null);
				} else {
					if(t.isUpperBound)
						return false;
					else
						return t.theBoundType == null || t.theBoundType.isAssignable(theBoundType);
				}
			} else {
				if(isUpperBound)
					return theBoundType == null || theBoundType.isAssignableFrom(t.theBaseType);
				else
					return theBoundType.canAssignTo(t.theBaseType);
			}
		} else {
			if(t == null)
				return theBaseType == Object.class;
			else if(t.isBounded) {
				if(t.isUpperBound) {
					if(t.theBoundType == null)
						return theBaseType == Object.class;
					else
						return isAssignable(t.theBoundType);
				} else
					return theBaseType == Object.class;
			} else {
				if(!isAssignable(theBaseType, t.theBaseType))
					return false;
				for(int p = 0; p < theParamTypes.length; p++)
					if(!theParamTypes[p].isAssignable(t.resolve(theBaseType.getTypeParameters()[p], theBaseType, null,
						new java.lang.reflect.Type[0], new Type[0])))
						return false;
				return true;
			}
		}
	}

	/**
	 * @param o The object to cast
	 * @return The cast object--this has no effect unless this type is primitive
	 * @throws IllegalArgumentException If this type is primitive and the given instance cannot be converted to a wrapper of this type
	 */
	public Object cast(Object o) throws IllegalArgumentException {
		if(theBoundType != null)
			return theBoundType.cast(o);
		else if(theBaseType == null)
			throw new IllegalStateException("cast() can only be called on basic or bounded types");
		if(isPrimitive()) {
			if(o == null)
				throw new IllegalArgumentException("Cannot cast a null instance to a primitive type: " + this);
			if(theBaseType == Boolean.TYPE) {
				if(o instanceof Boolean)
					return o;
			} else if(theBaseType == Character.TYPE) {
				if(o instanceof Character)
					return o;
				else if(o instanceof Integer)
					return (char) ((Integer) o).intValue();
			} else if(theBaseType == Double.TYPE) {
				if(o instanceof Double)
					return o;
				else if(o instanceof Number)
					return Double.valueOf(((Number) o).doubleValue());
			} else if(theBaseType == Float.TYPE) {
				if(o instanceof Float)
					return o;
				else if(o instanceof Number)
					return Float.valueOf(((Number) o).floatValue());
			} else if(theBaseType == Long.TYPE) {
				if(o instanceof Long)
					return o;
				else if(o instanceof Number)
					return Long.valueOf(((Number) o).longValue());
			} else if(theBaseType == Integer.TYPE) {
				if(o instanceof Integer)
					return o;
				else if(o instanceof Number)
					return Integer.valueOf(((Number) o).intValue());
			} else if(theBaseType == Short.TYPE) {
				if(o instanceof Short)
					return o;
				else if(o instanceof Number)
					return Short.valueOf(((Number) o).shortValue());
			} else if(theBaseType == Byte.TYPE) {
				if(o instanceof Byte)
					return o;
				else if(o instanceof Number)
					return Byte.valueOf(((Number) o).byteValue());
			}
			throw new IllegalArgumentException("Cannot cast " + (o == null ? "null" : "an instance of " + o.getClass().getName()) + " to "
				+ this);
		}
		if(o == null || isAssignableFrom(o.getClass()))
			return o;
		throw new IllegalArgumentException("Cannot cast " + (o == null ? "null" : "an instance of " + o.getClass().getName()) + " to "
			+ this);
	}

	private static boolean isAssignable(Class<?> to, Class<?> from) {
		if(to == from)
			return true;
		if(to.isAssignableFrom(from))
			return true;
		if(to == Void.TYPE)
			return false;

		Class<?> primTo = getPrimitiveType(to);
		if(primTo == null)
			return false;
		Class<?> primFrom = getPrimitiveType(from);
		if(primFrom == null)
			return false;
		if(primTo == primFrom)
			return true;

		if(primTo == Boolean.TYPE)
			return false;
		if(primTo == Character.TYPE)
			return primFrom == Character.TYPE || primFrom == Integer.TYPE || primFrom == Short.TYPE || primFrom == Byte.TYPE;
		if(primTo == Double.TYPE)
			return primFrom != Boolean.TYPE;
		if(primTo == Float.TYPE)
			return primFrom != Boolean.TYPE && primFrom != Double.TYPE;
		if(primTo == Long.TYPE)
			return primFrom != Boolean.TYPE && primFrom != Double.TYPE && primFrom != Float.TYPE;
		if(primTo == Integer.TYPE)
			return primFrom == Character.TYPE || primFrom == Integer.TYPE || primFrom == Short.TYPE || primFrom == Byte.TYPE;
		if(primTo == Short.TYPE)
			return primFrom == Short.TYPE || primFrom == Byte.TYPE;
		if(primTo == Byte.TYPE)
			return primFrom == Byte.TYPE;

		return false;
	}

	/**
	 * Resolves a type that may be dependent upon this type (generics)
	 *
	 * @param type The type to resolve
	 * @param declaringClass The class that declared the given type
	 * @param methodTypes A map of type variable name/type of the inferred types of an invocation for a method's declared type variables.
	 *            May be null if this resolution is not for a type-parameter-declaring method.
	 * @param paramTypes The types of the method parameters
	 * @param argTypes The types of the arguments to the method
	 * @return The resolved type
	 */
	public Type resolve(java.lang.reflect.Type type, Class<?> declaringClass, java.util.Map<String, Type> methodTypes,
		java.lang.reflect.Type [] paramTypes, Type [] argTypes) {
		if(paramTypes.length > 0 && argTypes.length > 0 && methodTypes == null)
			methodTypes = new java.util.HashMap<>();
		for(int p = 0; p < paramTypes.length; p++) {
			if(paramTypes[p] instanceof java.lang.reflect.TypeVariable) {
				String name = ((java.lang.reflect.TypeVariable<?>) paramTypes[p]).getName();
				Type t = methodTypes.get(name);
				if(t != null)
					t = t.getCommonType(argTypes[p]);
				else
					t = argTypes[p];
				methodTypes.put(name, t);
			}
		}
		Type ret = _resolve(type, declaringClass, methodTypes, new java.util.HashSet<java.lang.reflect.Type>());
		if(ret == null)
			ret = new Type(Object.class);
		return ret;
	}

	private Type _resolve(java.lang.reflect.Type type, Class<?> declaringClass, java.util.Map<String, Type> methodTypes,
		java.util.Set<java.lang.reflect.Type> tried) {
		if(type == null)
			return new Type(type);
		else if(type instanceof Class)
			return new Type(type);
		else if(tried.contains(type))
			return null;
		tried.add(type);
		if(type instanceof java.lang.reflect.ParameterizedType) {
			java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) type;
			Type ret = new Type(pt.getRawType());
			ret.theParamTypes = new Type[pt.getActualTypeArguments().length];
			for(int p = 0; p < ret.theParamTypes.length; p++)
				ret.theParamTypes[p] = _resolve(pt.getActualTypeArguments()[p], declaringClass, methodTypes, tried);
			return ret;
		} else if(type instanceof java.lang.reflect.WildcardType) {
			Type ret = new Type(null);
			ret.isBounded = true;
			java.lang.reflect.WildcardType wt = (java.lang.reflect.WildcardType) type;
			ret.isUpperBound = wt.getLowerBounds().length == 0;
			if(wt.getLowerBounds().length > 0)
				ret.theBoundType = _resolve(wt.getLowerBounds()[0], declaringClass, methodTypes, tried);
			else if(wt.getUpperBounds().length > 0)
				ret.theBoundType = _resolve(wt.getUpperBounds()[0], declaringClass, methodTypes, tried);
			return ret;
		} else if(type instanceof java.lang.reflect.TypeVariable) {
			java.lang.reflect.TypeVariable<?> tv = (java.lang.reflect.TypeVariable<?>) type;
			if(tv.getName().equals(theName))
				return this;
			if(methodTypes != null && methodTypes.get(tv.getName()) != null)
				return methodTypes.get(tv.getName());

			if(theBaseType != null) {
				int decIndex = prisms.util.ArrayUtils.indexOf(declaringClass.getTypeParameters(), tv);
				if(decIndex >= 0) {
					java.lang.reflect.Type [] path = getTypePath(declaringClass, theBaseType);
					if(path == null)
						return new Type(type);
					if(path.length > 0 && path[0] instanceof java.lang.reflect.ParameterizedType) {
						java.lang.reflect.ParameterizedType superPath = (java.lang.reflect.ParameterizedType) path[0];
						java.lang.reflect.Type paramType = superPath.getActualTypeArguments()[decIndex];
						return _resolve(paramType, (Class<?>) (path[1] instanceof Class ? path[1]
							: ((java.lang.reflect.ParameterizedType) path[1]).getRawType()), methodTypes, tried);
					}
				}
				for(int p = 0; p < theBaseType.getTypeParameters().length; p++)
					if(theBaseType.getTypeParameters()[p].getName().equals(tv.getName())) {
						if(theParamTypes.length > 0)
							return theParamTypes[p];
					}
			}
			Type ret = new Type(null);
			ret.theBaseType = null;
			ret.theName = tv.getName();
			ret.isBounded = true;
			ret.isUpperBound = true;
			if(tv.getBounds().length > 0)
				ret.theBoundType = _resolve(tv.getBounds()[0], declaringClass, methodTypes, tried);
			return ret;
		} else if(type instanceof java.lang.reflect.GenericArrayType) {
			java.lang.reflect.GenericArrayType at = (java.lang.reflect.GenericArrayType) type;
			Type ret = _resolve(at.getGenericComponentType(), declaringClass, methodTypes, tried);
			if(ret.theBaseType != null) {
				if(Void.TYPE != ret.theBaseType)
					ret.theBaseType = java.lang.reflect.Array.newInstance(ret.theBaseType, 0).getClass();
			} else if(ret.theBoundType != null)
				ret.theBoundType = ret.theBoundType.getArrayType();
			else
				ret.theBoundType = new Type(java.lang.reflect.Array.newInstance(Object.class, 0).getClass());
			return ret;
		} else
			throw new IllegalArgumentException("Unrecognize reflect type: " + type.getClass().getName());
	}

	static java.lang.reflect.Type [] getTypePath(Class<?> superType, Class<?> subType) {
		if(superType == subType)
			return new java.lang.reflect.Type[] {superType};
		java.lang.reflect.Type [] ret;
		if(subType.getSuperclass() != null) {
			ret = getTypePath(superType, subType.getSuperclass());
			if(ret != null) {
				ret[ret.length - 1] = subType.getGenericSuperclass();
				ret = prisms.util.ArrayUtils.add(ret, subType);
				return ret;
			}
		}
		for(int i = 0; i < subType.getInterfaces().length; i++) {
			ret = getTypePath(superType, subType.getInterfaces()[i]);
			if(ret != null) {
				ret[ret.length - 1] = subType.getGenericInterfaces()[i];
				ret = prisms.util.ArrayUtils.add(ret, subType);
				return ret;
			}
		}
		return null;
	}

	/** @return Whether this represents a primitive type or not */
	public boolean isPrimitive() {
		return theBaseType != null && theBaseType.isPrimitive();
	}

	/** @return Whether this represents an array type or not */
	public boolean isArray() {
		if(theBaseType != null)
			return theBaseType.isArray();
		else if(theBoundType != null)
			return theBoundType.isArray();
		else
			return false;
	}

	/** @return Whether instances of this type can be used in floating-point math operations */
	public boolean isMathable() {
		Class<?> prim = getPrimitiveType(theBaseType);
		if(prim == null)
			return false;
		return prim == Double.TYPE || prim == Float.TYPE || prim == Long.TYPE || prim == Integer.TYPE || prim == Short.TYPE
			|| prim == Byte.TYPE || prim == Character.TYPE;
	}

	/** @return Whether instances of this type can be used in integer-only math operations */
	public boolean isIntMathable() {
		Class<?> prim = getPrimitiveType(theBaseType);
		if(prim == null)
			return false;
		return prim == Long.TYPE || prim == Integer.TYPE || prim == Short.TYPE || prim == Byte.TYPE || prim == Character.TYPE;
	}

	/** @return This type's base type. Will be null if this is a wildcard type. */
	public Class<?> getBaseType() {
		return theBaseType;
	}

	/**
	 * @return This type's parameters. Zero-length if this type's base type is not generic. Null if this is a wildcard type.
	 */
	public Type [] getParamTypes() {
		return theParamTypes;
	}

	/** @return The name of this type variable, or null if this type is not variable */
	public String getName() {
		return theName;
	}

	/** @return The bound of this type */
	public Type getBoundType() {
		return theBoundType;
	}

	/** @return Whether this type is upper- or lower-bound */
	public boolean isUpperBound() {
		return isUpperBound;
	}

	/** @return This array-type's component type */
	public Type getComponentType() {
		Type ret;
		if(theBaseType != null)
			ret = new Type(theBaseType.getComponentType(), theParamTypes);
		else if(theBoundType != null)
			ret = new Type(theBoundType.getComponentType(), isUpperBound);
		else
			throw new IllegalStateException("Type " + this + " is not an array type");
		return ret;
	}

	/** @return The class that most closely represents this type */
	public Class<?> toClass() {
		if(theBaseType != null)
			return theBaseType;
		else if(theBoundType != null) {
			if(isUpperBound)
				return theBoundType.toClass();
			else
				return Object.class;
		} else
			return Object.class;
	}

	/** @return The array type whose component type is this type */
	public Type getArrayType() {
		Type ret;
		if(theBaseType != null) {
			if(Void.TYPE == theBaseType)
				ret = this;
			else
				ret = new Type(java.lang.reflect.Array.newInstance(theBaseType, 0).getClass(), theParamTypes);
		} else if(theBoundType != null)
			ret = new Type(theBoundType.getArrayType(), isUpperBound);
		else
			ret = new Type(new Type(java.lang.reflect.Array.newInstance(Object.class, 0).getClass()), isUpperBound);
		return ret;
	}

	/**
	 * @param t The type to get the common type of
	 * @return The most specific type that is assignable from both this and <code>t</code>. May be null if one or both types are primitive
	 *         and incompatible
	 */
	public Type getCommonType(Type t) {
		if(this == NULL)
			return t;
		if(t == NULL)
			return this;
		if(isAssignable(t))
			return this;
		if(t.isAssignable(this))
			return t;
		if(!t.isPrimitive() && !t.isPrimitive()) {
			if(theBaseType != null) {
				if(theBaseType.getSuperclass() != null && t.canAssignTo(theBaseType.getSuperclass()))
					return new Type(theBaseType.getSuperclass(), theParamTypes);
				for(Class<?> intf : theBaseType.getInterfaces())
					if(t.canAssignTo(intf)) {
						Type [] paramTypes = new Type[intf.getTypeParameters().length];
						for(int p = 0; p < paramTypes.length; p++)
							paramTypes[p] = resolve(intf.getTypeParameters()[p], intf, null, new java.lang.reflect.Type[0], new Type[0]);
						return new Type(intf, paramTypes);
					}
				return new Type(Object.class);
			} else if(theBoundType != null) {
				Type ct = theBoundType.getCommonType(t);
				if(ct != null)
					return new Type(ct, isUpperBound);
				else
					return new Type(Object.class);
			} else
				return new Type(Object.class);
		}

		if(!t.isPrimitive() || !t.isPrimitive())
			return null;
		if(Boolean.TYPE.equals(theBaseType) || Boolean.TYPE.equals(t.theBaseType))
			return null;
		if(Double.TYPE.equals(theBaseType) || Double.TYPE.equals(t.theBaseType))
			return new Type(Double.TYPE);
		if(Float.TYPE.equals(theBaseType) || Float.TYPE.equals(t.theBaseType))
			return new Type(Float.TYPE);
		if(Long.TYPE.equals(theBaseType) || Long.TYPE.equals(t.theBaseType))
			return new Type(Long.TYPE);
		if(Integer.TYPE.equals(theBaseType) || Integer.TYPE.equals(t.theBaseType))
			return new Type(Integer.TYPE);
		if(Character.TYPE.equals(theBaseType) || Character.TYPE.equals(t.theBaseType))
			return new Type(Character.TYPE);
		if(Short.TYPE.equals(theBaseType) || Short.TYPE.equals(t.theBaseType))
			return new Type(Short.TYPE);
		return new Type(Byte.TYPE);
	}

	@Override
	public String toString() {
		return toString(null);
	}

	/**
	 * Like {@link #toString()}, but shortens this type's string representation wherever possible by cutting off package names where they
	 * can be made implicit.
	 *
	 * @param env The evaluation environment to check type imports of. May be null.
	 * @return A string representing this type
	 */
	public String toString(EvaluationEnvironment env) {
		StringBuilder ret = new StringBuilder();
		if(isBounded) {
			ret.append("? ");
			if(theBoundType != null) {
				ret.append(isUpperBound ? "extends " : "super ");
				ret.append(theBoundType.toString(env));
			}
		} else if(this == NULL)
			ret.append("null");
		else {
			int arrayDim = 0;
			Class<?> base = theBaseType;
			while(base.isArray()) {
				arrayDim++;
				base = base.getComponentType();
			}
			String imp = isImported(base, env);
			if(imp != null)
				ret.append(imp);
			else
				ret.append(base.getName());
			if(theParamTypes.length > 0) {
				ret.append('<');
				boolean first = true;
				for(Type p : theParamTypes) {
					if(first)
						first = false;
					else
						ret.append(", ");
					ret.append(p.toString(env));
				}
				ret.append('>');
			}
			for(int i = 0; i < arrayDim; i++)
				ret.append("[]");
		}
		return ret.toString();
	}

	/**
	 * Checks to see whether a class may be represented without its package name
	 *
	 * @param t The class to check
	 * @param env The evaluation environment to check the class in. May be null.
	 * @return True if the class belongs to the default package or java.lang, or if the class's name or its package have been imported into
	 *         <code>env</code>
	 */
	public static String isImported(Class<?> t, EvaluationEnvironment env) {
		String name = t.getName();
		int idx = name.indexOf('.');
		if(idx < 0)
			return name;
		String pkg = name.substring(0, idx);
		name = name.substring(idx + 1);
		if(pkg.equals("java.lang"))
			return name;
		if(env == null)
			return null;
		return env.getImportType(name) != null ? name : null;
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Type))
			return false;
		Type t = (Type) o;
		if(isBounded != t.isBounded)
			return false;
		if(theBaseType == null ? t.theBaseType != null : !theBaseType.equals(t.theBaseType))
			return false;
		if(theBoundType == null ? t.theBoundType != null : !theBoundType.equals(t.theBoundType))
			return false;
		if(isUpperBound != t.isUpperBound)
			return false;
		if(theName == null ? t.theName != null : !theName.equals(t.theName))
			return false;
		if(!prisms.util.ArrayUtils.equals(theParamTypes, t.theParamTypes))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		int ret = 0;
		if(isBounded)
			ret++;
		if(theBaseType != null)
			ret = ret * 5 * theBaseType.hashCode();
		if(theBoundType != null)
			ret = ret * 7 + theBoundType.hashCode() + (isUpperBound ? 1 : 0);
		if(theName != null)
			ret = ret * 13 + theName.hashCode();
		if(theParamTypes != null)
			ret = ret * 17 + prisms.util.ArrayUtils.hashCode(theParamTypes);
		return ret;
	}

	/**
	 * @param wrapper The wrapper for a primitive type
	 * @return The unwrapped primitive type, or null if the given type is not primitive or a primitive wrapper
	 */
	public static Class<?> getPrimitiveType(Class<?> wrapper) {
		if(wrapper.isPrimitive())
			return wrapper;
		if(wrapper.equals(Double.class))
			return Double.TYPE;
		else if(wrapper.equals(Float.class))
			return Float.TYPE;
		else if(wrapper.equals(Long.class))
			return Long.TYPE;
		else if(wrapper.equals(Integer.class))
			return Integer.TYPE;
		else if(wrapper.equals(Character.class))
			return Character.TYPE;
		else if(wrapper.equals(Short.class))
			return Short.TYPE;
		else if(wrapper.equals(Byte.class))
			return Byte.TYPE;
		else if(wrapper.equals(Boolean.class))
			return Boolean.TYPE;
		else
			return null;
	}

	/**
	 * @param primitive The primitive type
	 * @return The wrapped primitive type, or null if the given type is not primitive or a primitive wrapper
	 */
	public static Class<?> getWrapperType(Class<?> primitive) {
		if(primitive.equals(Double.TYPE))
			return Double.class;
		else if(primitive.equals(Float.TYPE))
			return Float.class;
		else if(primitive.equals(Long.TYPE))
			return Long.class;
		else if(primitive.equals(Integer.TYPE))
			return Integer.class;
		else if(primitive.equals(Character.TYPE))
			return Character.class;
		else if(primitive.equals(Short.TYPE))
			return Short.class;
		else if(primitive.equals(Byte.TYPE))
			return Byte.class;
		else if(primitive.equals(Boolean.TYPE))
			return Boolean.class;
		else if(getPrimitiveType(primitive) != null)
			return primitive;
		else
			return null;
	}

	/**
	 * @param t The type to represent
	 * @return A string representation of the type
	 */
	public static String typeString(Class<?> t) {
		if(t.isArray())
			return typeString(t.getComponentType()) + "[]";
		else
			return t.getName();
	}
}
