package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;

public class WrongLogicForNullChecker extends Bug {

	public WrongLogicForNullChecker(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// Q1
		for(ConditionalExpression condExp:wholeCodeAST.getConditionalExpressions()){
			
			if(!(condExp.getExpression() instanceof InfixExpression)) continue;
			
			InfixExpression infixExp = (InfixExpression)condExp.getExpression();
			
			if(!isNullChecker(infixExp)) continue;
			
			if(!loadKnownNull(infixExp)) continue;
			
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(condExp.getStartPosition());
			
			listDetRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, condExp.toString(), false, false));	
		}
		
		return listDetRec;
	}

	private boolean isNullChecker(InfixExpression infixExp) {
		
		InfixExpression.Operator operator = infixExp.getOperator();
		
		if(!(operator.equals(InfixExpression.Operator.EQUALS)
				|| operator.equals(InfixExpression.Operator.NOT_EQUALS)))
			return false;
		
		return true;
	}
	
	private boolean loadKnownNull(InfixExpression infixExp) {
		
		InfixExpression.Operator operator = infixExp.getOperator();
		ASTNode leftOperand = infixExp.getLeftOperand();
		ASTNode rightOperand = infixExp.getRightOperand();
		
		if(!(leftOperand instanceof NullLiteral || rightOperand instanceof NullLiteral)) return false;
		
		// comes here? it's a null checker!
		ConditionalExpression condExp = (ConditionalExpression) infixExp.getParent();
		
		String strThenExp = condExp.getThenExpression().toString();
		String strElseExp = condExp.getElseExpression().toString();
		
		// Problematic cases
		// (1) A == null? A:B; null == A? A:B;
		// (2) A != null? B:A; null != A? B:A;
		
		// A
		String targetObj = rightOperand instanceof NullLiteral? leftOperand.toString():rightOperand.toString();
		
		// (1)
		if(operator.equals(InfixExpression.Operator.EQUALS)){
			
			// cases that must be ignored
			if(casesToBeIgnored(condExp.getThenExpression(),targetObj,strThenExp)) return false;
			
			// Q2
			if(intentionallyLoadKnownNull(targetObj,strThenExp,condExp.getElseExpression())) return false;
			
			if(Utils.isWordInStatement(targetObj, strThenExp)) return true;
		}
		
		// (2)
		if(operator.equals(InfixExpression.Operator.NOT_EQUALS)){
			
			// cases that must be ignored
			if(casesToBeIgnored(condExp.getElseExpression(),targetObj,strElseExp)) return false;
			
			// Q2
			if(intentionallyLoadKnownNull(targetObj,strElseExp,condExp.getThenExpression())) return false;
						
			if(Utils.isWordInStatement(targetObj,strElseExp)) return true;
		}
		
		return false;
	}

	// Q2: intentionally return null object? (e.g. v == null ? v : v.getObject() or v != null ? v.getObject() : v)
	private boolean intentionallyLoadKnownNull(String targetObj, String strExp, Expression exp) {
		
		if(!(exp instanceof MethodInvocation || exp instanceof QualifiedName )) return false;
		
		String caller = exp instanceof MethodInvocation? getCaller((MethodInvocation) exp):((QualifiedName) exp).getQualifier().toString();
		
		if(targetObj.equals(strExp)
				&& caller.equals(targetObj))
			return true;
		
		return false;
	}

	private String getCaller(ASTNode methodInv) {
		
		ASTNode currentNode = methodInv ;
		
		while(true){
			
			if(currentNode instanceof MethodInvocation)
				currentNode = ((MethodInvocation) currentNode).getExpression();
			else
				break;
		}
		
		// now current node is not a method invocation
		if(currentNode==null)
			return "";
		
		return currentNode.toString();
	}

	private boolean casesToBeIgnored(Expression targetExp, String targetObj, String strExp) {
		
		if(strExp.contains("." + targetObj)
			|| strExp.matches(".*\"[^\"]*" + targetObj+".*\".*")
			|| targetExp instanceof StringLiteral
			|| (targetExp instanceof ParenthesizedExpression
				 && ((ParenthesizedExpression)targetExp).getExpression() instanceof Assignment
				 && ((Assignment)((ParenthesizedExpression)targetExp).getExpression()).getLeftHandSide().toString().equals(targetObj)
			   )
			|| targetExp instanceof Assignment
		)
			return true;
		
		return false;
	}
}
