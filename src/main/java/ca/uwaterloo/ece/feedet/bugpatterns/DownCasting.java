package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class DownCasting extends Bug {

	public DownCasting(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "Down cast such as long to int and doulbe to float may assigne wrong values.";
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(VariableDeclarationFragment varDec:wholeCodeAST.getVariableDeclarationFragments()){	
			
			if(!(varDec.getParent() instanceof VariableDeclarationStatement)) continue;
			
			VariableDeclarationStatement varDecStmt = (VariableDeclarationStatement) varDec.getParent();
			
			if(!(varDecStmt.getType().toString().toLowerCase().equals("int")
					|| varDecStmt.getType().toString().toLowerCase().equals("float")))
				continue;
			
			if(!(varDec.getInitializer() instanceof CastExpression)) continue;
			
			CastExpression castExp = (CastExpression) varDec.getInitializer();
			
			if(varDecStmt.getType().toString().toLowerCase().equals("int")){
				
				if(!castExp.getType().toString().toLowerCase().equals("int")) continue;
				if(castExp.getExpression() instanceof MethodInvocation && dealWithFloatOrDoubleForIntCasting(castExp.getExpression())) continue;
				//Q2
				if(containsMinusOrShiftOperation(castExp.getExpression())) continue;
			} else {
				
				if(!castExp.getType().toString().toLowerCase().equals("float")) continue;
			}
			
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(varDec.getStartPosition());	
			listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, varDec.getParent().toString(), false, false));	
		}
		
		return listDetRec;
	}

	private boolean containsMinusOrShiftOperation(Expression expression) {
		
		ArrayList<InfixExpression> infixExpressions = wholeCodeAST.getInfixExpressions(expression);
		
		for(InfixExpression infixExp:infixExpressions){
			if(infixExp.getOperator() == Operator.MINUS)
				return true;
			if(infixExp.getOperator() == Operator.LEFT_SHIFT
					|| infixExp.getOperator() == Operator.RIGHT_SHIFT_SIGNED
					|| infixExp.getOperator() == Operator.RIGHT_SHIFT_UNSIGNED)
				return true;
		}
		
		return false;
	}

	private boolean dealWithFloatOrDoubleForIntCasting(Expression expression) {
		return expression.toString().toLowerCase().contains("float") 
				|| expression.toString().toLowerCase().contains("double")
				|| expression.toString().toLowerCase().contains("floor")
				|| expression.toString().toLowerCase().contains("ceil")
				|| expression.toString().toLowerCase().contains("round");
	}
}
