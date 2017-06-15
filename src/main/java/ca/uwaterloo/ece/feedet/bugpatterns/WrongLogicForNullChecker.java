package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
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
			
			if(!loadKnownNull(infixExp)) continue;
			
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(condExp.getStartPosition());
			
			listDetRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, condExp.toString(), false, false));	
		}
		
		return listDetRec;
	}

	private boolean loadKnownNull(InfixExpression infixExp) {
		
		InfixExpression.Operator operator = infixExp.getOperator();
		ASTNode leftOperand = infixExp.getLeftOperand();
		ASTNode rightOperand = infixExp.getRightOperand();
		
		if(!(operator.equals(InfixExpression.Operator.EQUALS)
				|| operator.equals(InfixExpression.Operator.NOT_EQUALS)))
			return false;
		
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
			
			if(Utils.isWordInStatement(targetObj, strThenExp)) return true;
		}
		
		// (2)
		if(operator.equals(InfixExpression.Operator.NOT_EQUALS)){
			
			// cases that must be ignored
			if(casesToBeIgnored(condExp.getElseExpression(),targetObj,strElseExp)) return false;
						
			if(Utils.isWordInStatement(targetObj,strElseExp)) return true;
		}
		
		return false;
	}

	private boolean casesToBeIgnored(Expression targetExp, String targetObj, String strExp) {
		
		if(strExp.contains("." + targetObj)
			|| strExp.contains("\"" + targetObj)
			|| targetExp instanceof StringLiteral
			|| (targetExp instanceof ParenthesizedExpression
				 && ((ParenthesizedExpression)targetExp).getExpression() instanceof Assignment
				 && ((Assignment)((ParenthesizedExpression)targetExp).getExpression()).getLeftHandSide().toString().equals(targetObj)
			   )
		)
			return true;
		
		return false;
	}
}
