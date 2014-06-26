/* ParsedImport.java Created Nov 16, 2011 by Andrew Butler, PSL */
package prisms.lang.eval;

import java.lang.reflect.Modifier;

import prisms.lang.EvaluationEnvironment;
import prisms.lang.EvaluationException;
import prisms.lang.EvaluationResult;
import prisms.lang.ParsedItem;
import prisms.lang.types.ParsedImport;

/** Represents an import command */
public class ImportEvaluator implements PrismsItemEvaluator<ParsedImport> {
	@Override
	public EvaluationResult evaluate(ParsedImport item, PrismsEvaluator evaluator, EvaluationEnvironment env, boolean asType,
		boolean withValues) throws EvaluationException {
		ParsedItem type = item.getType();
		EvaluationResult typeEval = evaluator.evaluate(type, env, true, false);
		String methodName = item.getMethodName();
		if(item.isStatic()) {
			if(!typeEval.isType())
				throw new EvaluationException("The static import " + type.getMatch().text + "." + methodName + " cannot be resolved", item,
					type.getMatch().index);
			if(item.isWildcard()) {
				if(!typeEval.isType())
					throw new EvaluationException("The static import " + type.getMatch().text + ".* cannot be resolved", item,
						type.getMatch().index);
				for(java.lang.reflect.Method m : typeEval.getType().getBaseType().getDeclaredMethods()) {
					if((m.getModifiers() & Modifier.STATIC) == 0)
						continue;
					env.addImportMethod(typeEval.getType().getBaseType(), m.getName());
				}
				for(java.lang.reflect.Field f : typeEval.getType().getBaseType().getDeclaredFields()) {
					if((f.getModifiers() & Modifier.STATIC) == 0)
						continue;
					env.addImportMethod(typeEval.getType().getBaseType(), f.getName());
				}
				return null;
			} else {
				if(!typeEval.isType())
					throw new EvaluationException("The static import " + type.getMatch().text + "." + methodName + " cannot be resolved",
						item, type.getMatch().index);
				boolean found = false;
				for(java.lang.reflect.Method m : typeEval.getType().getBaseType().getDeclaredMethods()) {
					if((m.getModifiers() & Modifier.STATIC) == 0)
						continue;
					if(!m.getName().equals(methodName))
						continue;
					found = true;
					break;
				}
				if(!found) {
					for(java.lang.reflect.Field f : typeEval.getType().getBaseType().getDeclaredFields()) {
						if((f.getModifiers() & Modifier.STATIC) == 0)
							continue;
						if(!f.getName().equals(methodName))
							continue;
						found = true;
						break;
					}
				}
				if(!found)
					throw new EvaluationException("The static import " + type.getMatch().text + "." + methodName + " cannot be resolved",
						item, type.getMatch().index);
				env.addImportMethod(typeEval.getType().getBaseType(), methodName);
				return null;
			}
		} else if(item.isWildcard()) {
			if(typeEval.getPackageName() == null)
				throw new EvaluationException("The type import " + type.getMatch().text + ".* cannot be resolved", item,
					type.getMatch().index);
			env.addImportPackage(typeEval.getPackageName());
			return null;
		} else {
			if(!typeEval.isType())
				throw new EvaluationException("The type import " + type.getMatch().text + " cannot be resolved", item,
					type.getMatch().index);
			env.addImportType(typeEval.getType().getBaseType());
			return null;
		}
	}
}
