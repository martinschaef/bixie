/**
 * 
 */
package bixie.transformation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import util.Log;
import boogie.controlflow.BasicBlock;
import boogie.controlflow.CfgProcedure;
import boogie.controlflow.CfgVariable;
import boogie.controlflow.expression.CfgExpression;
import boogie.controlflow.expression.CfgIdentifierExpression;
import boogie.controlflow.statement.CfgAssertStatement;
import boogie.controlflow.statement.CfgAssignStatement;
import boogie.controlflow.statement.CfgAssumeStatement;
import boogie.controlflow.statement.CfgCallStatement;
import boogie.controlflow.statement.CfgHavocStatement;
import boogie.controlflow.statement.CfgStatement;
import boogie.type.BoogieType;

/**
 * @author schaef
 *
 */
public class TacasCallUnwinding {

	private static final String method_names = "/null_methods.json";
	private static final Set<String> nullMethods = new HashSet<String>();

	private long callCounter = 0L;
	private Map<CfgCallStatement, CfgVariable> returnVariables = new HashMap<CfgCallStatement, CfgVariable>();
	private Map<CfgCallStatement, CfgAssignStatement> returnAssignments = new HashMap<CfgCallStatement, CfgAssignStatement>();

	public List<CfgAssignStatement> getCallAssigns() {
		return new LinkedList<CfgAssignStatement>(this.returnAssignments.values());
	}
	
	public CfgCallStatement findCallStatement(CfgVariable v) {
		for (Entry<CfgCallStatement, CfgVariable> entry : this.returnVariables.entrySet()) {
			if (v==entry.getValue()) {
				return entry.getKey();
			}
		}
		throw new RuntimeException("NOT FOUND!");
	}
	
	//TODO don't iterate over the keyset
	@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "WMI_WRONG_MAP_ITERATOR")
	private static void loadNullMethods() {
		try (InputStream stream = TacasCallUnwinding.class
				.getResourceAsStream(method_names);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						stream, "utf-8"));) {
			JSONParser parser = new JSONParser();
			JSONObject jo = (JSONObject) parser.parse(in);

			for (Object obj : jo.keySet()) {
				JSONObject entry = (JSONObject) jo.get(obj);
				String clazz = (String) entry.get("class");
				String fullClassName = clazz;
				String pkg = (String) entry.get("package");
				if (pkg != null && !pkg.isEmpty()) {
					fullClassName = pkg + "." + fullClassName;
				}

				JSONArray methods = (JSONArray) entry.get("methods");
				for (Object m : methods.toArray()) {
					String method = (String) m;
					nullMethods.add(pkg + "." + clazz + "$" + method + "$");
				}

			}
		} catch (IOException e) {
			Log.error("Cannot load mehtod name for tcas experiments!\n"
					+ e.toString());
		} catch (ParseException e) {
			Log.error("Cannot load mehtod name for tcas experiments!\n"
					+ e.toString());
		}
	}

	public TacasCallUnwinding() {
		// TranslationHelpers.getQualifiedName(this.sootMethod)

	}

	private boolean mayReturnNull(CfgCallStatement call) {
		if (nullMethods.isEmpty()) {
			loadNullMethods();
		}

		for (String s : nullMethods) {
			if (call.getCallee().getProcedureName().contains(s)) {
				return true;
			}
		}

		return false;
	}

	public void unwindCalls(CfgProcedure p) {
//		Log.info("eliminateCalls for: " + p.getProcedureName());
		BasicBlock root = p.getRootNode();

		LinkedList<BasicBlock> todo = new LinkedList<BasicBlock>();
		LinkedList<BasicBlock> done = new LinkedList<BasicBlock>();
		todo.add(root);
		while (!todo.isEmpty()) {
			BasicBlock current = todo.pop();
			done.add(current);
			/*
			 * For each BasicBlock, iterate over the statements. If a statement
			 * is a call, collect all variables in the modifies clause and in
			 * the LHS of the call statement and replace the call by a Havoc for
			 * all these variables.
			 */
			LinkedList<CfgStatement> statements = current.getStatements();
			// shallow copy for iteration ... needed because we're modifying
			// "statements"
			LinkedList<CfgStatement> iterlist = new LinkedList<CfgStatement>(
					statements);

			for (CfgStatement stmt : iterlist) {
				if (stmt instanceof CfgCallStatement) {
					CfgCallStatement call = (CfgCallStatement) stmt;
					int offset = 0;

					LinkedList<CfgStatement> callcontract = new LinkedList<CfgStatement>();
					// insert the assert statements that are enforced by the
					// "requires" clauses
					// of the callee.
					HashMap<CfgVariable, CfgExpression> substitutes = new HashMap<CfgVariable, CfgExpression>();
					for (int j = offset; j < call.getCallee().getInParams().length; j++) {
						substitutes.put(call.getCallee().getInParams()[j],
								call.getArguments()[j]);
					}

					for (CfgExpression xp : call.getCallee().getRequires()) {
						callcontract.add(new CfgAssertStatement(call
								.getLocation(), xp.substitute(substitutes)));
					}

					// create the havoc statement for the modifies clause.
					HashSet<CfgVariable> modifies = new HashSet<CfgVariable>();
					modifies.addAll(call.getCallee().getModifies());

					if (!mayReturnNull(call)) {
						for (CfgIdentifierExpression lhs : call
								.getLeftHandSide()) {
							modifies.add(lhs.getVariable());
						}
					} else {
						// havoc everything but the first lhs (the others
						// are
						// exception parameters, etc.
						boolean first = true;
						for (CfgIdentifierExpression lhs : call
								.getLeftHandSide()) {
							if (first && !lhs.getVariable().getVarname().equals("$exception")) {
								first = false;
								continue;
							}
							modifies.add(lhs.getVariable());
						}
					}

					CfgHavocStatement havoc = new CfgHavocStatement(
							call.getLocation(),
							modifies.toArray(new CfgVariable[modifies.size()]));
					// Log.error(" call: "+ call.toString());
					// Log.error(" becomes: "+ havoc.toString());
					havoc.setReplacedStatement(call);
					callcontract.add(havoc);

					// insert the assume statements that are guaranteed by the
					// "ensures" clauses
					// of the callee.
					substitutes = new HashMap<CfgVariable, CfgExpression>();
					for (int j = offset; j < call.getCallee().getOutParams().length; j++) {
						substitutes.put(call.getCallee().getOutParams()[j],
								call.getLeftHandSide()[j]);
					}
					for (CfgExpression xp : call.getCallee().getEnsures()) {
						if (statements.indexOf(stmt) < statements.size() - 1) {
							statements
									.add(new CfgAssumeStatement(call
											.getLocation(), xp
											.substitute(substitutes)));
						} else {
							callcontract
									.add(new CfgAssumeStatement(call
											.getLocation(), xp
											.substitute(substitutes)));
						}
					}

					if (mayReturnNull(call)
							&& call.getLeftHandSide().length > 0) {
						CfgIdentifierExpression lhs = call.getLeftHandSide()[0];
						// check if lhs is actually a local/global variable and
						// not an 'exception' variable.
						if (!lhs.getVariable().getVarname()
								.equals("$exception")) {
							// If this is one of our special may-be-null
							// functions
							// add a fresh variable for the return value that
							// can be tracked
							List<CfgVariable> localVars = new LinkedList<CfgVariable>();
							for (CfgVariable v : p.getLocalVars()) {
								localVars.add(v);
							}

							String specialReturnVarName = call.getCallee()
									.getProcedureName()
									+ "__"
									+ (callCounter++); 
							CfgVariable specialReturnVar = new CfgVariable(
									specialReturnVarName, BoogieType.intType,
									false, false, true, true);
							localVars.add(specialReturnVar);
							p.setLocalVars(localVars
									.toArray(new CfgVariable[localVars.size()]));

							// add the assignment lhs := specialReturnVar
							CfgAssignStatement asgn = new CfgAssignStatement(call
									.getLocation(), lhs,
									new CfgIdentifierExpression(null,
											specialReturnVar, 0));
							callcontract.add(asgn);
							returnVariables.put(call, specialReturnVar);
							returnAssignments.put(call, asgn);
							Log.info("added special return flag "
									+ specialReturnVarName + " for "
									+ call.toString());
						}
					}

					// now merge callcontract back into statements at and
					// replace the original call stmt.
					statements.addAll(statements.indexOf(stmt), callcontract);
					// and now remove the old call statement.
					statements.remove(stmt);
				} else {
					continue;
				}
			}
			current.setStatements(statements);

			for (BasicBlock next : current.getSuccessors()) {
				if (!todo.contains(next) && !done.contains(next)) {
					todo.add(next);
				}
			}
		}
	}
}
