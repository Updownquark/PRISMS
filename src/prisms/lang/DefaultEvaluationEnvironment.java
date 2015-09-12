/* DefaultEvaluationEnvironment.java Created Nov 14, 2011 by Andrew Butler, PSL */
package prisms.lang;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import org.qommons.ProgramTracker;
import org.qommons.json.JsonSerialReader.StructState;

import prisms.lang.eval.PrismsEvaluator;
import prisms.lang.types.ParsedFunctionDeclaration;

/** Default implementation of {@link EvaluationEnvironment} */
public class DefaultEvaluationEnvironment implements EvaluationEnvironment {
	private final DefaultEvaluationEnvironment theParent;

	private final boolean isTransaction;

	private boolean canOverride;

	private boolean isPublic;

	private ClassGetter theClassGetter;

	private HashMap<String, VariableImpl> theInternalVariables;

	private ArrayList<VariableSource> theExternalVariables;

	private ArrayList<VariableImpl> theHistory;

	private HashMap<String, Class<?>> theImportTypes;

	private HashMap<String, Class<?>> theImportMethods;

	private java.util.HashSet<String> theImportPackages;

	private ArrayList<prisms.lang.types.ParsedFunctionDeclaration> theFunctions;

	private Type theReturnType;

	private Type [] theHandledExceptionTypes;

	private volatile boolean isCanceled;

	private org.qommons.ProgramTracker theTracker;

	/** Creates the environment */
	public DefaultEvaluationEnvironment() {
		theParent = null;
		isTransaction = false;
		isPublic = true;
		theClassGetter = new ClassGetter();
		theInternalVariables = new java.util.LinkedHashMap<>();
		theExternalVariables = new ArrayList<>();
		theHistory = new ArrayList<>();
		theImportTypes = new HashMap<>();
		theImportMethods = new HashMap<>();
		theImportPackages = new java.util.HashSet<>();
		theFunctions = new ArrayList<>();
	}

	DefaultEvaluationEnvironment(DefaultEvaluationEnvironment parent, ClassGetter cg, boolean override, boolean transaction) {
		theParent = parent;
		isTransaction = transaction;
		isPublic = parent == null ? true : parent.isPublic;
		canOverride = override;
		theInternalVariables = new HashMap<>();
		theExternalVariables = new ArrayList<>();
		theFunctions = new ArrayList<>();
		if(theParent == null)
			theClassGetter = cg;
		if(canOverride)
			theHistory = new ArrayList<>();
		if(canOverride || transaction) {
			theImportTypes = new HashMap<>();
			theImportMethods = new HashMap<>();
			theImportPackages = new java.util.HashSet<>();
		}
	}

	/** @param src A source to inject variables in this environment */
	public void addVariableSource(VariableSource src) {
		synchronized(theExternalVariables) {
			theExternalVariables.add(src);
		}
	}

	/** @param src The source to remove from this environment */
	public void removeVariableSource(VariableSource src) {
		synchronized(theExternalVariables) {
			theExternalVariables.remove(src);
		}
	}

	/** @param publicOnly Whether evaluations in this environment should see only public methods */
	public void setPublicOnly(boolean publicOnly) {
		isPublic = publicOnly;
	}

	@Override
	public boolean usePublicOnly() {
		return isPublic;
	}

	@Override
	public ClassGetter getClassGetter() {
		if(theParent != null)
			return theParent.getClassGetter();
		else
			return theClassGetter;
	}

	@Override
	public Type getVariableType(String name) {
		Variable vbl = getVariable(name, true);
		return vbl == null ? null : vbl.getType();
	}

	/**
	 * Checks for a variable within or beyond the current scope, which may still be larger than just this instance
	 *
	 * @param name The name of the variable to get the type of
	 * @param lookBack Whether to look beyond the current scope
	 * @return The type of the variable, or null if none has been declared
	 */
	protected Variable getVariable(String name, boolean lookBack) {
		Variable vbl;
		synchronized(theInternalVariables) {
			vbl = theInternalVariables.get(name);
		}
		if(vbl == null) {
			synchronized(theExternalVariables) {
				for(VariableSource src : theExternalVariables) {
					vbl = src.getDeclaredVariable(name);
					if(vbl != null)
						break;
				}
			}
		}
		if(vbl == null) {
			if(theParent != null && (lookBack || !canOverride))
				return theParent.getVariable(name, lookBack);
			else
				return null;
		}
		return vbl;
	}

	@Override
	public Object getVariable(String name, ParsedItem struct, int index) throws EvaluationException {
		Variable vbl = getVariable(name, true);
		if(vbl == null)
			throw new EvaluationException(name + " has not been declared", struct, index);
		if(!vbl.isInitialized())
			throw new EvaluationException("Variable " + name + " has not been intialized", struct, index);
		return vbl.getValue();
	}

	@Override
	public void declareVariable(String name, Type type, boolean isFinal, ParsedItem struct, int index) throws EvaluationException {
		Variable vbl = getVariable(name, canOverride);
		if(vbl != null)
			throw new EvaluationException("Duplicate local variable " + name, struct, index);
		synchronized(theInternalVariables) {
			theInternalVariables.put(name, new VariableImpl(type, name, isFinal));
		}
	}

	@Override
	public void setVariable(String name, Object value, ParsedItem struct, int index) throws EvaluationException {
		VariableImpl vbl;
		synchronized(theInternalVariables) {
			vbl = theInternalVariables.get(name);
		}
		if(vbl == null && getVariable(name, false) != null)
			throw new EvaluationException("Cannot set values on variables from external sources: " + name, struct, index);
		if(vbl == null && theParent != null) {
			vbl = (VariableImpl) theParent.getVariable(name, true);
			if(vbl != null && isTransaction)
				vbl = new VariableImpl(vbl.getType(), vbl.getName(), vbl.isFinal());
		}
		if(vbl == null)
			throw new EvaluationException(name + " cannot be resolved to a variable ", struct, index);
		if(vbl.getType().isPrimitive()) {
			if(value == null)
				throw new EvaluationException("Variable of type " + vbl.getType().toString() + " cannot be assigned null", struct, index);
			Class<?> prim = Type.getPrimitiveType(value.getClass());
			if(prim == null || !vbl.getType().isAssignableFrom(prim))
				throw new EvaluationException(Type.typeString(value.getClass()) + " cannot be cast to " + vbl.getType(), struct, index);
		} else {
			if(value != null && !vbl.getType().isAssignableFrom(value.getClass()))
				throw new EvaluationException(Type.typeString(value.getClass()) + " cannot be cast to " + vbl.getType(), struct, index);
		}
		if(vbl.isInitialized() && vbl.isFinal())
			throw new EvaluationException("Final variable " + name + " has already been assigned", struct, index);
		vbl.setValue(value);
	}

	@Override
	public void dropVariable(String name, ParsedItem struct, int index) throws EvaluationException {
		VariableImpl vbl;
		synchronized(theInternalVariables) {
			vbl = theInternalVariables.get(name);
		}
		if(vbl == null) {
			if(theParent.getVariable(name, true) != null) {
				if(isTransaction) {
					theParent.dropVariable(name, struct, index);
					return;
				} else
					throw new EvaluationException("Variable " + name + " can only be dropped from the scope in which it was declared",
						struct, index);
			} else if(getVariable(name, false) != null)
				throw new EvaluationException("Variable " + name + " can not be dropped from an external source", struct, index);
			else
				throw new EvaluationException("No such variable named " + name, struct, index);
		}
		if(vbl.isFinal())
			throw new EvaluationException("The final variable " + name + " cannot be dropped", struct, index);
		synchronized(theInternalVariables) {
			theInternalVariables.remove(name);
		}
	}

	@Override
	public Variable [] getDeclaredVariables() {
		ArrayList<Variable> ret = new ArrayList<>();
		synchronized(theInternalVariables) {
			for(Variable vbl : theInternalVariables.values()) {
				if(!ret.contains(vbl))
					ret.add(vbl);
			}
		}
		synchronized(theExternalVariables) {
			for(VariableSource src : theExternalVariables) {
				for(Variable var : src.getDeclaredVariables())
					ret.add(var);
			}
		}
		DefaultEvaluationEnvironment env = theParent;
		while(env != null) {
			synchronized(env.theInternalVariables) {
				for(Variable vbl : env.theInternalVariables.values()) {
					if(!ret.contains(vbl))
						ret.add(vbl);
				}
			}
			env = env.theParent;
		}
		return ret.toArray(new Variable[ret.size()]);
	}

	@Override
	public Variable getDeclaredVariable(String name) {
		return getVariable(name, true);
	}

	@Override
	public void declareFunction(ParsedFunctionDeclaration function) {
		synchronized(theFunctions) {
			theFunctions.add(function);
		}
	}

	@Override
	public ParsedFunctionDeclaration [] getDeclaredFunctions() {
		ParsedFunctionDeclaration [] ret;
		synchronized(theFunctions) {
			ret = theFunctions.toArray(new ParsedFunctionDeclaration[theFunctions.size()]);
		}
		if(theParent != null)
			ret = org.qommons.ArrayUtils.addAll(ret, theParent.getDeclaredFunctions());
		return ret;
	}

	@Override
	public void dropFunction(ParsedFunctionDeclaration function, ParsedItem struct, int index) throws EvaluationException {
		synchronized(theFunctions) {
			int fIdx = theFunctions.indexOf(function);
			if(fIdx < 0) {
				if(theParent != null && org.qommons.ArrayUtils.indexOf(theParent.getDeclaredFunctions(), function) >= 0) {
					if(isTransaction)
						theParent.dropFunction(function, struct, index);
					else
						throw new EvaluationException("Function " + function.getShortSig()
							+ " can only be dropped from the scope in which it was declared", struct, index);
				} else
					throw new EvaluationException("No such function " + function.getShortSig(), struct, index);
			} else
				theFunctions.remove(fIdx);
		}
	}

	@Override
	public void setReturnType(Type type) {
		theReturnType = type;
	}

	@Override
	public Type getReturnType() {
		if(theReturnType != null)
			return theReturnType;
		else if(theParent != null)
			return theParent.getReturnType();
		else
			return null;
	}

	@Override
	public void setHandledExceptionTypes(Type [] types) {
		theHandledExceptionTypes = types;
	}

	@Override
	public boolean canHandle(Type exType) {
		if(exType.canAssignTo(Error.class))
			return true;
		if(exType.canAssignTo(RuntimeException.class))
			return true;
		if(theHandledExceptionTypes != null) {
			for(Type et : theHandledExceptionTypes)
				if(et.isAssignable(exType))
					return true;
			return false;
		} else if(theParent != null)
			return theParent.canHandle(exType);
		else
			return false;
	}

	@Override
	public int getHistoryCount() {
		if(theParent != null)
			return theParent.getHistoryCount();
		synchronized(theHistory) {
			return theHistory.size();
		}
	}

	@Override
	public Type getHistoryType(int index) {
		if(theParent != null)
			return theParent.getHistoryType(index);
		VariableImpl vbl;
		synchronized(theHistory) {
			vbl = theHistory.get(theHistory.size() - index - 1);
		}
		return vbl.getType();
	}

	@Override
	public Object getHistory(int index) {
		if(theParent != null)
			return theParent.getHistory(index);
		VariableImpl vbl;
		synchronized(theHistory) {
			vbl = theHistory.get(theHistory.size() - index - 1);
		}
		return vbl.getValue();
	}

	@Override
	public void addHistory(Type type, Object result) {
		if(theParent != null) {
			if(isTransaction)
				theParent.addHistory(type, result);
			else
				throw new IllegalStateException("History can only be added to a root-level evaluation environment");
		}
		VariableImpl vbl = new VariableImpl(type, "%", false);
		vbl.setValue(result);
		synchronized(theHistory) {
			theHistory.add(vbl);
		}
	}

	@Override
	public void clearHistory() {
		theHistory.clear();
	}

	@Override
	public void addImportPackage(String packageName) {
		if(theParent != null && !isTransaction)
			throw new IllegalStateException("Imports may only be used at the top level");
		synchronized(theImportPackages) {
			theImportPackages.add(packageName);
		}
	}

	@Override
	public String [] getImportPackages() {
		synchronized(theImportPackages) {
			return theImportPackages.toArray(new String[theImportPackages.size()]);
		}
	}

	@Override
	public void clearImportPackages() {
		theImportPackages.clear();
	}

	@Override
	public void addImportType(Class<?> type) {
		if(theParent != null && !isTransaction)
			throw new IllegalStateException("Imports may only be used at the top level");
		String name = type.getName();
		int dotIdx = name.lastIndexOf('.');
		if(dotIdx >= 0)
			name = name.substring(dotIdx + 1);
		synchronized(theImportTypes) {
			theImportTypes.put(name, type);
		}
	}

	@Override
	public Class<?> getImportType(String name) {
		if(theParent != null)
			return theParent.getImportType(name);
		Class<?> ret;
		synchronized(theImportTypes) {
			ret = theImportTypes.get(name);
		}
		if(ret != null)
			return ret;
		synchronized(theImportPackages) {
			for(String pkg : theImportPackages)
				try {
					ret = Class.forName(pkg + "." + name);
					return ret;
				} catch(ClassNotFoundException e) {
				}
		}
		return null;
	}

	@Override
	public Class<?> [] getImportTypes() {
		synchronized(theImportTypes) {
			return theImportTypes.values().toArray(new Class[theImportTypes.size()]);
		}
	}

	@Override
	public void clearImportTypes() {
		theImportTypes.clear();
	}

	@Override
	public void addImportMethod(Class<?> type, String method) {
		if(theParent != null && !isTransaction)
			throw new IllegalStateException("Imports may only be used at the top level");
		synchronized(theImportMethods) {
			theImportMethods.put(method, type);
		}
	}

	@Override
	public Class<?> getImportMethodType(String methodName) {
		if(theParent != null)
			return theParent.getImportMethodType(methodName);
		synchronized(theImportMethods) {
			return theImportMethods.get(methodName);
		}
	}

	@Override
	public ImportMethod [] getImportMethods() {
		synchronized(theImportMethods) {
			ImportMethod [] ret = new ImportMethod[theImportMethods.size()];
			int i = 0;
			for(java.util.Map.Entry<String, Class<?>> entry : theImportMethods.entrySet())
				ret[i++] = new ImportMethod(entry.getValue(), entry.getKey());
			return ret;
		}
	}

	@Override
	public void clearImportMethods() {
		theImportMethods.clear();
	}

	@Override
	public EvaluationEnvironment scope(boolean dependent) {
		if(dependent) {
			DefaultEvaluationEnvironment ret = new DefaultEvaluationEnvironment(this, null, false, false);
			ret.setTracker(theTracker);
			return ret;
		}
		DefaultEvaluationEnvironment root = this;
		while(root.theParent != null)
			root = root.theParent;
		DefaultEvaluationEnvironment ret = new DefaultEvaluationEnvironment(null, getClassGetter(), true, false);
		for(String pkg : root.theImportPackages)
			ret.theImportPackages.add(pkg);
		for(java.util.Map.Entry<String, Class<?>> entry : root.theImportTypes.entrySet())
			ret.theImportTypes.put(entry.getKey(), entry.getValue());
		for(java.util.Map.Entry<String, Class<?>> entry : root.theImportMethods.entrySet())
			ret.theImportMethods.put(entry.getKey(), entry.getValue());
		for(VariableImpl var : root.theHistory)
			ret.theHistory.add(var);
		for(ParsedFunctionDeclaration func : getDeclaredFunctions())
			ret.theFunctions.add(func);
		root = this;
		while(root != null) {
			for(java.util.Map.Entry<String, VariableImpl> var : root.theInternalVariables.entrySet())
				if(var.getValue().isFinal() && var.getValue().isInitialized())
					ret.theInternalVariables.put(var.getKey(), var.getValue());
			root = root.theParent;
		}
		ret.setTracker(theTracker);
		return ret;
	}

	@Override
	public EvaluationEnvironment transact() {
		return new DefaultEvaluationEnvironment(this, null, false, true);
	}

	@Override
	public void cancel() {
		isCanceled = true;
	}

	@Override
	public boolean isCanceled() {
		return isCanceled || (theParent != null && theParent.isCanceled());
	}

	@Override
	public void uncancel() {
		isCanceled = false;
	}

	@Override
	public ProgramTracker getTracker() {
		return theTracker;
	}

	/** @param tracker The program tracker to record performance information */
	public void setTracker(ProgramTracker tracker) {
		theTracker = tracker;
	}

	@Override
	public VariableImpl [] save(OutputStream out) throws IOException {
		java.io.OutputStreamWriter charWriter = new java.io.OutputStreamWriter(out);
		org.qommons.json.JsonStreamWriter jsonWriter = new org.qommons.json.JsonStreamWriter(charWriter);
		org.qommons.HexStreamWriter hexWriter = new org.qommons.HexStreamWriter();

		jsonWriter.startObject();
		jsonWriter.startProperty("variables");
		jsonWriter.startArray();
		ArrayList<VariableImpl> fails = new ArrayList<>();
		for(VariableImpl var : theInternalVariables.values()) {
			if(var.getValue() != null && !(var.getValue() instanceof java.io.Serializable)) {
				fails.add(var);
				continue;
			}
			try {
				writeVar(var, jsonWriter, hexWriter, charWriter, false);
			} catch(java.io.ObjectStreamException e) {
				e.printStackTrace();
				fails.add(var);
			}
		}
		jsonWriter.endArray();

		jsonWriter.startProperty("functions");
		jsonWriter.startArray();
		for(ParsedFunctionDeclaration func : theFunctions)
			jsonWriter.writeString(func.getMatch().text);
		jsonWriter.endArray();

		jsonWriter.startProperty("importPackages");
		jsonWriter.startArray();
		for(String pkg : theImportPackages)
			jsonWriter.writeString(pkg);
		jsonWriter.endArray();

		jsonWriter.startProperty("importTypes");
		jsonWriter.startArray();
		for(Class<?> type : theImportTypes.values())
			jsonWriter.writeString(type.getName());
		jsonWriter.endArray();

		jsonWriter.startProperty("importMethods");
		jsonWriter.startArray();
		for(java.util.Map.Entry<String, Class<?>> entry : theImportMethods.entrySet()) {
			jsonWriter.startObject();
			jsonWriter.startProperty("methodName");
			jsonWriter.writeString(entry.getKey());
			jsonWriter.startProperty("type");
			jsonWriter.writeString(entry.getValue().getName());
			jsonWriter.endObject();
		}
		jsonWriter.endArray();

		jsonWriter.startProperty("history");
		jsonWriter.startArray();
		for(VariableImpl var : theHistory) {
			if(var.getValue() != null && !(var.getValue() instanceof java.io.Serializable)) {
				fails.add(var);
				continue;
			}
			try {
				writeVar(var, jsonWriter, hexWriter, charWriter, true);
			} catch(java.io.ObjectStreamException e) {
				e.printStackTrace();
				fails.add(var);
			}
		}
		jsonWriter.endArray();

		jsonWriter.endObject();
		charWriter.close();
		return fails.toArray(new VariableImpl[fails.size()]);
	}

	private void writeVar(VariableImpl var, org.qommons.json.JsonStreamWriter jsonWriter, org.qommons.HexStreamWriter hexWriter,
		java.io.Writer charWriter, boolean isShort) throws IOException {
		jsonWriter.startObject();
		jsonWriter.startProperty("name");
		jsonWriter.writeString(var.getName());
		jsonWriter.startProperty("type");
		jsonWriter.writeString(var.getType().toString());
		if(!isShort) {
			jsonWriter.startProperty("final");
			jsonWriter.writeBoolean(var.isFinal());
			jsonWriter.startProperty("initialized");
			jsonWriter.writeBoolean(var.isInitialized());
		}
		jsonWriter.startProperty("value");
		hexWriter.setWrapped(jsonWriter.writeStringAsWriter());
		ObjectOutputStream oos = new ObjectOutputStream(hexWriter);
		oos.writeObject(var.getValue());
		oos.close();
		jsonWriter.endObject();
	}

	@Override
	public void load(InputStream in, PrismsParser parser, PrismsEvaluator eval) throws IOException {
		java.io.InputStreamReader charReader = new java.io.InputStreamReader(in);
		org.qommons.json.JsonSerialReader jsonReader = new org.qommons.json.JsonSerialReader(charReader);

		try (org.qommons.HexStreamReader hexReader = new org.qommons.HexStreamReader()) {
			StructState rootState = jsonReader.startObject();
			if(!"variables".equals(jsonReader.getNextProperty()))
				throw new IOException("Unrecognizable JSON stream--no variables");
			StructState arrayState = jsonReader.startArray();
			while(jsonReader.getNextItem(true, false) instanceof org.qommons.json.JsonSerialReader.ObjectItem) {
				StructState varState = jsonReader.save();
				try {
					VariableImpl var = parseNextVariable(jsonReader, hexReader, parser, eval);
					theInternalVariables.put(var.getName(), var);
				} catch(IOException e) {
					e.printStackTrace();
				} finally {
					jsonReader.endObject(varState);
				}
			}
			jsonReader.endArray(arrayState);

			if(!"functions".equals(jsonReader.getNextProperty()))
				throw new IOException("Unrecognizable JSON stream--no functions");
			arrayState = jsonReader.startArray();
			String text = jsonReader.parseString();
			while(text != null) {
				ParseStructRoot root = new ParseStructRoot(text);
				ParsedFunctionDeclaration func;
				try {
					func = (ParsedFunctionDeclaration) parser.parseStructures(root, parser.parseMatches(text))[0];
					theFunctions.add(func);
				} catch(ParseException e) {
					e.printStackTrace();
				}
				text = jsonReader.parseString();
			}
			jsonReader.endArray(arrayState);

			if(!"importPackages".equals(jsonReader.getNextProperty()))
				throw new IOException("Unrecognizable JSON stream--no importPackages");
			arrayState = jsonReader.startArray();
			String next = jsonReader.parseString();
			while(next != null) {
				theImportPackages.add(next);
				next = jsonReader.parseString();
			}
			jsonReader.endArray(arrayState);

			if(!"importTypes".equals(jsonReader.getNextProperty()))
				throw new IOException("Unrecognizable JSON stream--no importTypes");
			arrayState = jsonReader.startArray();
			text = jsonReader.parseString();
			while(text != null) {
				String name = text;
				int dotIdx = name.lastIndexOf('.');
				if(dotIdx >= 0)
					name = name.substring(dotIdx + 1);
				try {
					theImportTypes.put(name, Class.forName(text));
				} catch(ClassNotFoundException e) {
					e.printStackTrace();
				}
				text = jsonReader.parseString();
			}
			jsonReader.endArray(arrayState);

			if(!"importMethods".equals(jsonReader.getNextProperty()))
				throw new IOException("Unrecognizable JSON stream--no importMethods");
			arrayState = jsonReader.startArray();
			while(jsonReader.getNextItem(true, false) instanceof org.qommons.json.JsonSerialReader.ObjectItem) {
				StructState objState = jsonReader.save();
				if(!"methodName".equals(jsonReader.getNextProperty()))
					throw new IOException("Unrecognizable JSON stream--no methodName on importMethod");
				String name = jsonReader.parseString();
				if(!"type".equals(jsonReader.getNextProperty()))
					throw new IOException("Unrecognizable JSON stream--no type on importMethod");
				String className = jsonReader.parseString();
				try {
					theImportMethods.put(name, Class.forName(className));
				} catch(ClassNotFoundException e) {
					e.printStackTrace();
				}
				jsonReader.endObject(objState);
			}
			jsonReader.endArray(arrayState);

			if(!"history".equals(jsonReader.getNextProperty()))
				throw new IOException("Unrecognizable JSON stream--no history");
			arrayState = jsonReader.startArray();
			while(jsonReader.getNextItem(true, false) instanceof org.qommons.json.JsonSerialReader.ObjectItem) {
				StructState histState = jsonReader.save();
				try {
					VariableImpl var = parseNextVariable(jsonReader, hexReader, parser, eval);
					theHistory.add(var);
				} catch(IOException e) {
					e.printStackTrace();
				} finally {
					jsonReader.endObject(histState);
				}
			}
			jsonReader.endArray(arrayState);
			jsonReader.endObject(rootState);
		} catch(ClassCastException e) {
			throw new IOException("Could not parse environment", e);
		} catch(org.qommons.json.SAJParser.ParseException e) {
			throw new IOException("Could not parse environment", e);
		}
	}

	private VariableImpl parseNextVariable(org.qommons.json.JsonSerialReader jsonReader, org.qommons.HexStreamReader hexReader,
		PrismsParser parser, PrismsEvaluator eval) throws IOException, org.qommons.json.SAJParser.ParseException {
		if(!"name".equals(jsonReader.getNextProperty()))
			throw new IOException("Unrecognizable JSON stream--variable has no name");
		String name = jsonReader.parseString();
		if(!"type".equals(jsonReader.getNextProperty()))
			throw new IOException("Unrecognizable JSON stream--variable has no type");
		String typeName = jsonReader.parseString();
		ParseStructRoot root = new ParseStructRoot(typeName);
		Type type;
		try {
			type = eval.evaluate(parser.parseStructures(root, parser.parseMatches(typeName))[0], this, true, false).getType();
		} catch(ParseException e) {
			throw new IOException("Could not parse type \"" + typeName + "\" of variable " + name, e);
		} catch(EvaluationException e) {
			throw new IOException("Could not parse type \"" + typeName + "\" of variable " + name, e);
		}
		String prop = jsonReader.getNextProperty();
		boolean isFinal = false;
		boolean initialized = true;
		if(prop.equals("final")) {
			isFinal = jsonReader.parseBoolean();
			prop = jsonReader.getNextProperty();
		}
		if(prop.equals("initialized")) {
			initialized = jsonReader.parseBoolean();
			prop = jsonReader.getNextProperty();
		}
		if(!prop.equals("value"))
			throw new IOException("Unrecognizable JSON stream--variable has no value");
		Object value;
		try {
			hexReader.setWrapped(jsonReader.parseStringAsReader());
			value = new ObjectInputStream(hexReader).readObject();
		} catch(Exception e) {
			throw new IOException("Could not parse variable " + name, e);
		}
		VariableImpl ret = new VariableImpl(type, name, isFinal);
		ret.setValue(value);
		ret.setInitialized(initialized);
		return ret;
	}
}
