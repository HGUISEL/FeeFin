package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class MissingArrayLengthCheck extends Bug {

	public MissingArrayLengthCheck(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		for(InfixExpression exp:wholeCodeAST.getInfixExpressions()){
			
			// Q1: Does array.length - n is used in if conditions? but did not check the length that may cause AOOIE?
			IfStatement ifStmt = getIfStatement(exp);
			if(ifStmt==null) continue;
			if(!(exp.getOperator().toString().equals("-")
					&& exp.getLeftOperand().toString().endsWith(".length")
					&& exp.getParent() instanceof ArrayAccess)) continue;
			
			String strLengthExpession = exp.getLeftOperand().toString();
			
			if(doesLengthCheckerExist(strLengthExpession,ifStmt)) continue;
			
			int lineNum = wholeCodeAST.getLineNum(exp.getStartPosition());
			detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, exp.toString(), ifStmt.toString(), false, false));
		}
		
		return detRec;
	}

	private boolean doesLengthCheckerExist(String strLengthExpession, IfStatement ifStmt) {
		
		final ArrayList<InfixExpression> lstInFixExp = new ArrayList<InfixExpression>();
		
		ifStmt.accept(new ASTVisitor() {
			@Override
			public boolean visit(InfixExpression node) {
				lstInFixExp.add(node);
				return super.visit(node);
			}
		});

		for(InfixExpression infixExp:lstInFixExp){
			if((infixExp.getLeftOperand().toString().equals(strLengthExpession) 
					|| infixExp.getRightOperand().toString().equals(strLengthExpession))
					&&
				(infixExp.getOperator().equals(InfixExpression.Operator.GREATER)
						|| infixExp.getOperator().equals(InfixExpression.Operator.GREATER_EQUALS)
						|| infixExp.getOperator().equals(InfixExpression.Operator.LESS)
						|| infixExp.getOperator().equals(InfixExpression.Operator.LESS_EQUALS)
						|| infixExp.getOperator().equals(InfixExpression.Operator.EQUALS)
			)){
				return true;
			}
		}
		
		return false;
	}

	private IfStatement getIfStatement(ASTNode exp) {
		if(exp.getParent() == null)
			return null;
		
		if(exp.getParent() instanceof ExpressionStatement || exp.getParent() instanceof Block)
			return null;
		
		if(exp.getParent() instanceof IfStatement)
			return (IfStatement) exp.getParent();
		
		return getIfStatement(exp.getParent());
	}
}
