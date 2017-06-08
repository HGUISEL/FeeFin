package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class WrongIncrementer extends Bug {
	
	ArrayList<ForStatement> forStmtHavingChildForStmt = new ArrayList<ForStatement>();

	public WrongIncrementer(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		// example
		// for (int j = 0; j < constraints.length; j++) {
        // try {
		// -     qconstraints[j] = ValueConstraint.create(type, constraints[i], session);
		// +     qconstraints[j] = ValueConstraint.create(type, constraints[j], session);
		
		ArrayList<ForStatement> forStmts = wholeCodeAST.getForStatements();
		for(int i=forStmts.size()-1;i >=0;i--){ // in the reversed order this retrieves the child forStmt first.
												// By doing this, we can save any outer forstmt in forStmtHavingChildForStmt.
												// So, we can ignore any outer for loops.
												// TODO: better logic to check if forStmt does not have inner forStmt without using forStmtHavingChildForStmt?
			
			ForStatement forStmt = forStmts.get(i);
			
			// only consider the last inner forStmt
			ForStatement outerForStmt = getOuterForStmt(forStmt);
			if(outerForStmt == null || forStmtHavingChildForStmt.contains(forStmt)){
				continue;
			}
			
			// only consider for loop with one initializer
			if(forStmt.initializers().size() > 1){
				continue;
			}
			
			String innerIncrementer = "";
			// (1) Get incrementer
			VariableDeclarationFragment varDecFrag = null;
			for(Object initStmt:forStmt.initializers()){
				if(initStmt instanceof VariableDeclarationExpression){
					VariableDeclarationExpression varDec = (VariableDeclarationExpression) initStmt;
					
					// only consider the initializer with one fragment
					if(varDec.fragments().size() > 1)
						break;
					
					varDecFrag = (VariableDeclarationFragment) varDec.fragments().get(0);
					innerIncrementer = varDecFrag.getName().toString();
				}
			}
			// check 
			if(varDecFrag == null || !(varDecFrag.getInitializer() instanceof NumberLiteral)) continue;
			
			// (2) updater contains the incrementer?
			if(forStmt.updaters().size()!=1)
				continue;
			if(forStmt.updaters().get(0) instanceof PostfixExpression){
				PostfixExpression postfixExp = (PostfixExpression) forStmt.updaters().get(0);
				if(!postfixExp.getOperand().toString().equals(innerIncrementer)){
					break;
				}
			}
			
			// (3) incrementers check with Array.length?
			if(!(forStmt.getExpression() instanceof InfixExpression)){
				continue;
			}
			
			// get arrayName in case arrayName.length
			String innerArrayName = getTargetArrayName(forStmt,innerIncrementer);

			// no arrayName then ignore
			if(innerArrayName.equals(""))
				continue;
			
			// get incrementer in outer forstmt
			@SuppressWarnings("unchecked")
			String outerIncrementer = getIncrementer(outerForStmt.initializers());
			
			// (1) ignore when there is no outer incrementer.
			// (2) When source code is broken (ASTParser did not work correctly), there might be outerIncrementer same as innerIncrementer. Ignore.
			if(outerIncrementer.equals("") || outerIncrementer.equals(innerIncrementer)) continue;
			
			String outerArrayName = getTargetArrayName(outerForStmt,outerIncrementer);
			
			// ignore when outerArrayName and innerArrayName are same.
			if(innerArrayName.equals(outerArrayName))
				continue;
			
			// find arrayName[any but not nameOfIncrementor]
			String[] lines =  forStmt.getBody().toString().split("\n");
			
			if(isExceptionalCase(forStmt.getBody().toString(),innerIncrementer,outerIncrementer))
				continue;
			
			for(String line:lines){

				// check if arrayName[outerIncrementer]
				CheckWrongIncrementer(detRec, forStmt, innerArrayName, outerIncrementer, line);
				// check if outerArrayName[outerIncrementer]
				if(!outerArrayName.isEmpty()) // true means both array names are not empty.
					CheckWrongIncrementer(detRec, forStmt, outerArrayName, innerIncrementer, line);
			}
		}
		
		return detRec;
	}

	private boolean isExceptionalCase(String body,String innerIncrementer, String outerIncrementer) {
		// heuristic 1: ignore the case when there are  outerIncrementer == (or other comparison operators) innerIncrementer or innerIncrementer == (or other comparison operators) outerIncrementer
		Pattern regExp = Pattern.compile("((^|[^\\.\\w])" + innerIncrementer + "\\s*(==|>|<|>=|<=)\\s*" + outerIncrementer + ")|((^|[^\\.\\w])" + outerIncrementer + "\\s*(==|>|<|>=|<=)\\s*" + innerIncrementer + ")");
		Matcher m = regExp.matcher(body);

		if(m.find()){
			return true;
		}
		return false;
	}

	private void CheckWrongIncrementer(ArrayList<DetectionRecord> detRec, ForStatement forStmt, String innerArrayName,
			String outerIncrementer, String line) {
		Pattern regExp = Pattern.compile("((^|[^\\.\\w])" + innerArrayName + "\\[\\s*" + outerIncrementer + "\\s*" + "\\])|((^|[^\\.\\w])" + innerArrayName + ".get\\(\\s*" + outerIncrementer + "\\s*" + "\\))");
		Matcher m = regExp.matcher(line);

		while(m.find()){
			String forLine = "for(" + forStmt.initializers().get(0) + ";" + forStmt.getExpression() + ";" + forStmt.updaters().get(0) + ") >> ";
			int lineNum = wholeCodeAST.getLineNum(forStmt.getStartPosition());
			detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, forLine + line.trim(), false, false));
		}
	}
	
	private String getTargetArrayName(ForStatement forStmt,String nameOfIncrementor){
		if(!(forStmt.getExpression() instanceof InfixExpression)){
			return ""; // if expression is not InfixExpression, return empty string.
		}
		InfixExpression infixExp = (InfixExpression) forStmt.getExpression();
		String arrayName = getArrayName(infixExp.getLeftOperand(), infixExp.getRightOperand(),nameOfIncrementor);
		if(arrayName.equals("")){
			arrayName = getArrayName(infixExp.getRightOperand(),infixExp.getLeftOperand(),nameOfIncrementor);
		}
		return arrayName;
	}

	private String getIncrementer(List<Object> initializers) {
		VariableDeclarationFragment varDecFrag = null;
		String nameOfIncrementor = "";
		for(Object initStmt:initializers){
			if(initStmt instanceof VariableDeclarationExpression){
				VariableDeclarationExpression varDec = (VariableDeclarationExpression) initStmt;
				
				// only consider the initializer with one fragment
				if(varDec.fragments().size() > 1)
					break;
				
				varDecFrag = (VariableDeclarationFragment) varDec.fragments().get(0);
				nameOfIncrementor = varDecFrag.getName().toString();
			}
		}
		return nameOfIncrementor;
	}

	private ForStatement getOuterForStmt(ASTNode node) {
		
		if(node.getParent() == null || node.getParent() instanceof MethodDeclaration
				|| node.getParent() instanceof TypeDeclaration)
			return null;
		
		if(node.getParent() instanceof ForStatement){
			forStmtHavingChildForStmt.add((ForStatement) node.getParent());
			return (ForStatement) node.getParent();
		}
		
		return getOuterForStmt(node.getParent());
	}

	String getArrayName(Object operand1, Object operand2,String nameOfIncrementor){
		
		String arrayName = "";
		
		if(!operand1.toString().equals(nameOfIncrementor) && !operand2.toString().equals(nameOfIncrementor)){
			return "";
		}
		
		if(operand1.toString().equals(nameOfIncrementor)){
			if(!(operand2 instanceof QualifiedName) && !(operand2 instanceof MethodInvocation)){
				return "";
			}
			
			if(operand2 instanceof QualifiedName){
				QualifiedName qualifiedName = (QualifiedName) operand2;
				if(!qualifiedName.getName().toString().equals("length"))
					return "";
				
				arrayName = qualifiedName.getQualifier().toString();
			}
			else{ // operand2 instanceof MethodInvocation
				MethodInvocation methodInv = (MethodInvocation) operand2;
				if(!methodInv.getName().toString().equals("size"))
					return "";
				arrayName = methodInv.getExpression()==null? "":methodInv.getExpression().toString();
			}
		}
		
		return arrayName;
	}
}
