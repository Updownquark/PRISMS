package prisms.lang;

public class MathUtils {
	public static Number unaryOp(String op, Object value) {
		if("+".equals(op))
			return posit(value);
		else if("-".equals(op))
			return negate(value);
		else if("~".equals(op))
			return complement(value);
		else
			throw new IllegalArgumentException("Unrecognized operator: " + op);
	}

	public static Number binaryOp(String op, Type t1, Object value1, Type t2, Object value2) {
		if("+".equals(op))
			return add(t1, value1, t2, value2);
		else if("-".equals(op))
			return subtract(t1, value1, t2, value2);
		else if("*".equals(op))
			return multiply(t1, value1, t2, value2);
		else if("/".equals(op))
			return divide(t1, value1, t2, value2);
		else if("%".equals(op))
			return modulo(t1, value1, t2, value2);
		else if("<<".equals(op))
			return leftShift(t1, value1, t2, value2);
		else if(">>".equals(op))
			return rightShift(t1, value1, t2, value2);
		else if(">>>".equals(op))
			return rightShiftUnsigned(t1, value1, t2, value2);
		else
			throw new IllegalArgumentException("Unrecognized operator: " + op);
	}

	public static Number posit(Object value) {
		if(value instanceof Number)
			return (Number) value;
		else if(value instanceof Character)
			return +((Character) value).charValue();
		else
			throw new IllegalStateException("Unrecognized number type");
	}

	public static Number negate(Object value) {
		if(value instanceof Double)
			return -((Number) value).doubleValue();
		else if(value instanceof Float)
			return -((Number) value).floatValue();
		else if(value instanceof Long)
			return -((Number) value).longValue();
		else if(value instanceof Integer)
			return -((Number) value).intValue();
		else if(value instanceof Short)
			return -((Number) value).shortValue();
		else if(value instanceof Byte)
			return -((Number) value).byteValue();
		else if(value instanceof Character)
			return -((Character) value).charValue();
		else
			throw new IllegalStateException("Unrecognized number type");
	}

	public static Number complement(Object value) {
		if(value instanceof Long)
			return ~((Number) value).longValue();
		else if(value instanceof Integer)
			return ~((Number) value).intValue();
		else if(value instanceof Short)
			return ~((Number) value).shortValue();
		else if(value instanceof Byte)
			return ~((Number) value).byteValue();
		else if(value instanceof Character)
			return ~((Character) value).charValue();
		else
			throw new IllegalStateException("Unrecognized number type");
	}

	public static Number add(Type t1, Object v1, Type t2, Object v2) {
		if(t1 == null)
			t1 = new Type(Type.getPrimitiveType(v1.getClass()));
		if(t2 == null)
			t2 = new Type(Type.getPrimitiveType(v2.getClass()));
		if(t1.canAssignTo(Integer.TYPE) && t2.canAssignTo(Integer.TYPE)) {
			Type intType = new Type(Integer.TYPE);
			return ((Number) intType.cast(v1)).intValue() + ((Number) intType.cast(v2)).intValue();
		} else if(t1.canAssignTo(Long.TYPE) && t2.canAssignTo(Long.TYPE)) {
			Type longType = new Type(Long.TYPE);
			return ((Number) longType.cast(v1)).longValue() + ((Number) longType.cast(v2)).longValue();
		} else if(t1.canAssignTo(Float.TYPE) && t2.canAssignTo(Float.TYPE)) {
			Type floatType = new Type(Float.TYPE);
			return ((Number) floatType.cast(v1)).floatValue() + ((Number) floatType.cast(v2)).floatValue();
		} else if(t1.canAssignTo(Double.TYPE) && t2.canAssignTo(Double.TYPE)) {
			Type doubleType = new Type(Double.TYPE);
			return ((Number) doubleType.cast(v1)).doubleValue() + ((Number) doubleType.cast(v2)).doubleValue();
		} else
			throw new IllegalArgumentException("Add cannot be applied to operand types " + t1 + " and " + t2);
	}

	public static Number subtract(Type t1, Object v1, Type t2, Object v2) {
		if(t1 == null)
			t1 = new Type(Type.getPrimitiveType(v1.getClass()));
		if(t2 == null)
			t2 = new Type(Type.getPrimitiveType(v2.getClass()));
		if(t1.canAssignTo(Integer.TYPE) && t2.canAssignTo(Integer.TYPE)) {
			Type intType = new Type(Integer.TYPE);
			return ((Number) intType.cast(v1)).intValue() - ((Number) intType.cast(v2)).intValue();
		} else if(t1.canAssignTo(Long.TYPE) && t2.canAssignTo(Long.TYPE)) {
			Type longType = new Type(Long.TYPE);
			return ((Number) longType.cast(v1)).longValue() - ((Number) longType.cast(v2)).longValue();
		} else if(t1.canAssignTo(Float.TYPE) && t2.canAssignTo(Float.TYPE)) {
			Type floatType = new Type(Float.TYPE);
			return ((Number) floatType.cast(v1)).floatValue() - ((Number) floatType.cast(v2)).floatValue();
		} else if(t1.canAssignTo(Double.TYPE) && t2.canAssignTo(Double.TYPE)) {
			Type doubleType = new Type(Double.TYPE);
			return ((Number) doubleType.cast(v1)).doubleValue() - ((Number) doubleType.cast(v2)).doubleValue();
		} else
			throw new IllegalArgumentException("Subtract cannot be applied to operand types " + t1 + " and " + t2);
	}

	public static Number multiply(Type t1, Object v1, Type t2, Object v2) {
		if(t1 == null)
			t1 = new Type(Type.getPrimitiveType(v1.getClass()));
		if(t2 == null)
			t2 = new Type(Type.getPrimitiveType(v2.getClass()));
		if(t1.canAssignTo(Integer.TYPE) && t2.canAssignTo(Integer.TYPE)) {
			Type intType = new Type(Integer.TYPE);
			return ((Number) intType.cast(v1)).intValue() * ((Number) intType.cast(v2)).intValue();
		} else if(t1.canAssignTo(Long.TYPE) && t2.canAssignTo(Long.TYPE)) {
			Type longType = new Type(Long.TYPE);
			return ((Number) longType.cast(v1)).longValue() * ((Number) longType.cast(v2)).longValue();
		} else if(t1.canAssignTo(Float.TYPE) && t2.canAssignTo(Float.TYPE)) {
			Type floatType = new Type(Float.TYPE);
			return ((Number) floatType.cast(v1)).floatValue() * ((Number) floatType.cast(v2)).floatValue();
		} else if(t1.canAssignTo(Double.TYPE) && t2.canAssignTo(Double.TYPE)) {
			Type doubleType = new Type(Double.TYPE);
			return ((Number) doubleType.cast(v1)).doubleValue() * ((Number) doubleType.cast(v2)).doubleValue();
		} else
			throw new IllegalArgumentException("Multiply cannot be applied to operand types " + t1 + " and " + t2);
	}

	public static Number divide(Type t1, Object v1, Type t2, Object v2) {
		if(t1 == null)
			t1 = new Type(Type.getPrimitiveType(v1.getClass()));
		if(t2 == null)
			t2 = new Type(Type.getPrimitiveType(v2.getClass()));
		if(t1.canAssignTo(Integer.TYPE) && t2.canAssignTo(Integer.TYPE)) {
			Type intType = new Type(Integer.TYPE);
			return ((Number) intType.cast(v1)).intValue() / ((Number) intType.cast(v2)).intValue();
		} else if(t1.canAssignTo(Long.TYPE) && t2.canAssignTo(Long.TYPE)) {
			Type longType = new Type(Long.TYPE);
			return ((Number) longType.cast(v1)).longValue() / ((Number) longType.cast(v2)).longValue();
		} else if(t1.canAssignTo(Float.TYPE) && t2.canAssignTo(Float.TYPE)) {
			Type floatType = new Type(Float.TYPE);
			return ((Number) floatType.cast(v1)).floatValue() / ((Number) floatType.cast(v2)).floatValue();
		} else if(t1.canAssignTo(Double.TYPE) && t2.canAssignTo(Double.TYPE)) {
			Type doubleType = new Type(Double.TYPE);
			return ((Number) doubleType.cast(v1)).doubleValue() / ((Number) doubleType.cast(v2)).doubleValue();
		} else
			throw new IllegalArgumentException("Divide cannot be applied to operand types " + t1 + " and " + t2);
	}

	public static Number modulo(Type t1, Object v1, Type t2, Object v2) {
		if(t1 == null)
			t1 = new Type(Type.getPrimitiveType(v1.getClass()));
		if(t2 == null)
			t2 = new Type(Type.getPrimitiveType(v2.getClass()));
		if(t1.canAssignTo(Integer.TYPE) && t2.canAssignTo(Integer.TYPE)) {
			Type intType = new Type(Integer.TYPE);
			return ((Number) intType.cast(v1)).intValue() % ((Number) intType.cast(v2)).intValue();
		} else if(t1.canAssignTo(Long.TYPE) && t2.canAssignTo(Long.TYPE)) {
			Type longType = new Type(Long.TYPE);
			return ((Number) longType.cast(v1)).longValue() % ((Number) longType.cast(v2)).longValue();
		} else if(t1.canAssignTo(Float.TYPE) && t2.canAssignTo(Float.TYPE)) {
			Type floatType = new Type(Float.TYPE);
			return ((Number) floatType.cast(v1)).floatValue() % ((Number) floatType.cast(v2)).floatValue();
		} else if(t1.canAssignTo(Double.TYPE) && t2.canAssignTo(Double.TYPE)) {
			Type doubleType = new Type(Double.TYPE);
			return ((Number) doubleType.cast(v1)).doubleValue() % ((Number) doubleType.cast(v2)).doubleValue();
		} else
			throw new IllegalArgumentException("Modulo cannot be applied to operand types " + t1 + " and " + t2);
	}

	public static Number leftShift(Type t1, Object v1, Type t2, Object v2) throws IllegalArgumentException {
		if(t1 == null)
			t1 = new Type(Type.getPrimitiveType(v1.getClass()));
		if(t2 == null)
			t2 = new Type(Type.getPrimitiveType(v2.getClass()));
		if(t1.canAssignTo(Integer.TYPE) && t2.canAssignTo(Integer.TYPE)) {
			Type intType = new Type(Integer.TYPE);
			return ((Number) intType.cast(v1)).intValue() << ((Number) intType.cast(v2)).intValue();
		} else if(t1.canAssignTo(Long.TYPE) && t2.canAssignTo(Long.TYPE)) {
			Type longType = new Type(Long.TYPE);
			return ((Number) longType.cast(t1)).longValue() << ((Number) longType.cast(v2)).longValue();
		} else
			throw new IllegalArgumentException("Left shift cannot be applied to operand types " + t1 + " and " + t2);
	}

	public static Number rightShift(Type t1, Object v1, Type t2, Object v2) {
		if(t1 == null)
			t1 = new Type(Type.getPrimitiveType(v1.getClass()));
		if(t2 == null)
			t2 = new Type(Type.getPrimitiveType(v2.getClass()));
		if(t1.canAssignTo(Integer.TYPE) && t2.canAssignTo(Integer.TYPE)) {
			Type intType = new Type(Integer.TYPE);
			return ((Number) intType.cast(v1)).intValue() << ((Number) intType.cast(v2)).intValue();
		} else if(t1.canAssignTo(Long.TYPE) && t2.canAssignTo(Long.TYPE)) {
			Type longType = new Type(Long.TYPE);
			return ((Number) longType.cast(t1)).longValue() << ((Number) longType.cast(v2)).longValue();
		} else
			throw new IllegalArgumentException("Right shift cannot be applied to operand types " + t1 + " and " + t2);
	}

	public static Number rightShiftUnsigned(Type t1, Object v1, Type t2, Object v2) {
		if(t1 == null)
			t1 = new Type(Type.getPrimitiveType(v1.getClass()));
		if(t2 == null)
			t2 = new Type(Type.getPrimitiveType(v2.getClass()));
		if(t1.canAssignTo(Integer.TYPE) && t2.canAssignTo(Integer.TYPE)) {
			Type intType = new Type(Integer.TYPE);
			return ((Number) intType.cast(v1)).intValue() << ((Number) intType.cast(v2)).intValue();
		} else if(t1.canAssignTo(Long.TYPE) && t2.canAssignTo(Long.TYPE)) {
			Type longType = new Type(Long.TYPE);
			return ((Number) longType.cast(t1)).longValue() << ((Number) longType.cast(v2)).longValue();
		} else
			throw new IllegalArgumentException("Unsigned right shift cannot be applied to operand types " + t1 + " and " + t2);
	}
}
