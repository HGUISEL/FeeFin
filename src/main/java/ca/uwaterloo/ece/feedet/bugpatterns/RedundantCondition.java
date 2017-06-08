package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class RedundantCondition extends Bug {

	public RedundantCondition(String prjName, JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		// InFixExpression: leftOperand operator rightOperand
		// Conditional expression
		for(InfixExpression exp:wholeCodeAST.getInfixExpressions()){
			if(exp.getOperator().equals(InfixExpression.Operator.CONDITIONAL_OR)
					|| exp.getOperator().equals(InfixExpression.Operator.CONDITIONAL_AND)){
				
				if(exp.getLeftOperand().toString().equals(exp.getRightOperand().toString())){
					int lineNum = wholeCodeAST.getLineNum(exp.getStartPosition());
					detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, exp.getParent().toString(), false, false));
				}
			}
		}
		
		return detRec;
	}
}
