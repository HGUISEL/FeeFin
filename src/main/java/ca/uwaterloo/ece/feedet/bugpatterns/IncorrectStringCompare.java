package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class IncorrectStringCompare extends Bug {

	public IncorrectStringCompare(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "String variables incorrectly conampared. (Consider to use equals rather than ==.)";
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		for(InfixExpression infixExp:wholeCodeAST.getInfixExpressions()){
			
			
			if((infixExp.getOperator() == Operator.NOT_EQUALS || infixExp.getOperator() == Operator.EQUALS)) {
				if ((wholeCodeAST.getTypeOfSimpleName(infixExp, infixExp.getLeftOperand().toString()).equals("String")&&!isNull(infixExp.getRightOperand()))
						||(wholeCodeAST.getTypeOfSimpleName(infixExp, infixExp.getRightOperand().toString()).equals("String")&&!isNull(infixExp.getLeftOperand()))) {
						
					int lineNum = wholeCodeAST.getLineNum(infixExp.getStartPosition());
				
					detRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, infixExp.toString(), false, false));
			
				}
			}
		}
		
		return detRec;
	}

	private boolean isNull(Expression operand) {
		
		if(operand instanceof NullLiteral)
			return true;
		
		if(operand instanceof ParenthesizedExpression)
			return isNull(((ParenthesizedExpression)operand).getExpression());
		
		return false;
	}
}
