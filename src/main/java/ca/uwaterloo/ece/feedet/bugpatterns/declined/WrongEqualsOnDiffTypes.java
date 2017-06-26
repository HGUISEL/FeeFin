package ca.uwaterloo.ece.feedet.bugpatterns.declined;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.bugpatterns.Bug;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class WrongEqualsOnDiffTypes extends Bug{
	/**
	 * an IDE may find this kind of bugs if comparing two different types with equals;
	 * @param prjName
	 * @param ast
	 * @param id
	 * @param path
	 * @param repo
	 */
	public WrongEqualsOnDiffTypes(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "Equals compares different types.";
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		/* examples:
		 -          if (t.getService().toString().equals(hdfsServiceName)) {
		 +          if (t.getService().equals(hdfsServiceName)) {
		 */
		
		for(MethodInvocation methodInvo : wholeCodeAST.getMethodInvocations()){
			if(methodInvo.getName().toString().trim().equals("equals")){
				Expression exp = methodInvo.getExpression();
				Expression argument = (Expression) methodInvo.arguments().get(0);
				
				if(exp == null || argument == null)
					continue;
				
				int lineNum = wholeCodeAST.getLineNum(methodInvo.getStartPosition());
			/*
			 * Q1: a.equals(b) ;a.equals(b()) ;
			 * check the type of variable "a" and the type of variable "b"	
			 * check the type of variable "a" and the type of method "b"
			 */
				if((exp instanceof Name) && (argument instanceof Name)){
					Name a = (Name) exp;
					Name b = (Name) argument;
					if(rtVariable(a)!=null && rtVariable(b)!=null &&!(rtVariable(a).equals(rtVariable(b)))){
					 detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodInvo.getParent().toString(), false, false));	
					}
				}
				if((exp instanceof Name) && (argument instanceof MethodInvocation)){
					Name a = (Name) exp;
					MethodInvocation b = (MethodInvocation) argument;
					if(rtVariable(a)!=null && rtMethod(b)!=null &&!(rtVariable(a).equals(rtMethod(b)))){
						 detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodInvo.getParent().toString(), false, false));	
						}
					}
			/*
			 * Q2: a.c().equals(d)
			 * check the return type of method "c" and the return type of variable "d"	
			 */
				if((exp instanceof MethodInvocation) && (argument instanceof Name)){
					MethodInvocation a = (MethodInvocation) exp;
					Name b = (Name) argument;
					if(rtMethod(a)!=null && rtVariable(b)!=null &&!(rtMethod(a).equals(rtVariable(b)))){
						 detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodInvo.getParent().toString(), false, false));	
						}
					}
			/*
			 * Q3: a.c().equals(d.e())
			 * check the return type of method "c" and the return type of method "e"
			 */
				if((exp instanceof MethodInvocation) && (argument instanceof MethodInvocation)){
					MethodInvocation a = (MethodInvocation) exp;
					MethodInvocation b = (MethodInvocation) argument;
					if(rtMethod(a)!=null && rtMethod(b)!=null &&!(rtMethod(a).equals(rtMethod(b)))){
						 detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodInvo.getParent().toString(), false, false));	
						}
					}
			}
		}
		
		return detRec;
	}
	
	public Type rtVariable(Name n){
		Map<String,Type> variable = new HashMap<String, Type>();//variable and return type
		Map<String,Type> field = new HashMap<String, Type>();//field and return type
		Type rt = null;
		for(VariableDeclarationFragment varDec: wholeCodeAST.getVariableDeclarationFragments()){
			 if(varDec instanceof VariableDeclaration){
				
				 if(varDec.getParent() instanceof VariableDeclarationStatement){
					 VariableDeclarationStatement vds = (VariableDeclarationStatement)varDec.getParent();
					 variable.put(varDec.getName().toString(), vds.getType());
				 }
				 if(varDec.getParent() instanceof FieldDeclaration){
					 FieldDeclaration vds = (FieldDeclaration)varDec.getParent();
					 field.put(varDec.getName().toString(), vds.getType());
				 }
			 }
		}
		//if matched with variable
		if(variable.containsKey(n.toString()) &&!(field.containsKey(n.toString())))
			rt = variable.get(n.toString());
		
		//if matched with field
		if(field.containsKey(n.toString()) && !(variable.containsKey(n.toString())))
			rt = field.get(n.toString());
		
			return rt;
	}
	
	public Type rtMethod(MethodInvocation methodInvo){
		String methodname = methodInvo.getName().getFullyQualifiedName();
		Map<String,Type> methods = new HashMap<String, Type>();//method and return type
		Type rt = null;
		
		for(MethodDeclaration methInv : wholeCodeAST.getMethodDeclarations()){
			methods.put(methInv.getName().getFullyQualifiedName(), methInv.getReturnType2());
		}
		
		if(methods.containsKey(methodname))
			rt = methods.get(methodname);
		
		return rt;
	}
}