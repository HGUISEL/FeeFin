package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class IntOverflowOfMathMin extends Bug {

	public IntOverflowOfMathMin(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		/*
			-      int realLen = Math.min(len, (int) (blockEnd - pos + 1));
			+      int realLen = (int) Math.min((long) len, (blockEnd - pos + 1L));
       			   int result = blockStream.read(buf, off, realLen);"
		 */
		
		// An example loop to get some AST nodes to analyze
		for(MethodInvocation methodInv:wholeCodeAST.getMethodInvocations()){
			
			// Q1: Does int Math.min use long values as parameter?
			
			// 1) method call is Math.min?
			if(methodInv.getExpression()==null || !(methodInv.getExpression().toString() + "." + methodInv.getName().toString()).equals("Math.min")) continue;
			
			// 2) check if int Math.Min by checking a long type of arguments
			boolean isIntMin = false;
			Expression expCasted = null;
			for(ASTNode node:(List<ASTNode>) methodInv.arguments()){
				
				CastExpression castExp = getCastExpressionForInt(node);
				if(castExp==null) continue;
				
				expCasted = castExp.getExpression();
				if(!containsLongVariable(expCasted)) continue;
				
				isIntMin = true;
				
			}
			if(!isIntMin) continue;
			
			// Q2: Does a long type argument have length check such as longVariable <= Integer.MAX_VALUE. If yes, ignore
			if(haveIntegerMaxLengthCheck(methodInv,expCasted)) continue;
			
			// Detected this pattern!!
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(methodInv.getStartPosition());
			detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodInv.getParent().toString(), false, false));
		}
		
		return detRec;
	}

	private CastExpression getCastExpressionForInt(ASTNode node) {
		
		if(node instanceof CastExpression && ((CastExpression)node).getType().toString().equals("int")) return (CastExpression)node;
		
		final ArrayList<CastExpression> castExps = new ArrayList<CastExpression>();
		// find in CastExpression
		node.accept(new ASTVisitor() {
				public boolean visit(CastExpression castExp) {
					if(castExp.getType().toString().equals("int"))
						castExps.add(castExp);
					return super.visit(castExp);
				}
			});	
		
		return castExps.isEmpty()?null:castExps.get(0);
	}

	private boolean haveIntegerMaxLengthCheck(MethodInvocation methodInv, Expression expCasted) {
		
		ArrayList<String> simpleNames = getSimpleNames(expCasted);
		
		ASTNode node = methodInv.getParent();
		while(node!=null){
			if(!(node instanceof IfStatement)){
				node = node.getParent();
				continue;
			}
			
			IfStatement ifStmt = (IfStatement) node;
			 
			final ArrayList<InfixExpression> infixExps = new ArrayList<InfixExpression>();
			
			ifStmt.accept(new ASTVisitor() {
				public boolean visit(InfixExpression node) {
					infixExps.add(node);
					return super.visit(node);
				}
			});	
			
			for(InfixExpression infixExp:infixExps){
				if((infixExp.getRightOperand().toString().contains("Integer.MAX_VALUE")
						&& simpleNames.contains(infixExp.getLeftOperand().toString()))
						||
					(infixExp.getLeftOperand().toString().contains("Integer.MAX_VALUE")
						&& simpleNames.contains(infixExp.getRightOperand().toString())))
					return true;
			}
			node = node.getParent();
		}

		return false;
	}

	private boolean containsLongVariable(Expression expression) {
		
		if(expression instanceof MethodInvocation) return false; // only consider expression contains only long variables, otherwise ignore.
		
		ArrayList<String> simpleNames = getSimpleNames(expression);
		
		for(String simpleName:simpleNames){
			if(wholeCodeAST.getTypeOfSimpleName(expression, simpleName).equals("long"))
				return true;
		}
		
		return false;
	}

	private ArrayList<String> getSimpleNames(Expression expression) {
		final ArrayList<String> simpleNames = new ArrayList<String>();
		if(expression instanceof SimpleName){
			simpleNames.add(expression.toString());
			return simpleNames;
		}
		
		expression.accept(new ASTVisitor() {
			@Override
			public boolean visit(SimpleName node) {
				simpleNames.add(node.toString());
				return super.visit(node);
			}
		}
		);
		
		return simpleNames;
	}
}
