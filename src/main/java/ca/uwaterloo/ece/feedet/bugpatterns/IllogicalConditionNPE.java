package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;

public class IllogicalConditionNPE extends Bug {

	public IllogicalConditionNPE(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "Condition contains a logicall error that could cause NPE.";
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();

		// InFixExpression: leftOperand operator rightOperand
		// Conditional expression

		for(InfixExpression exp:wholeCodeAST.getInfixExpressions()){


			if(exp.getOperator().equals(InfixExpression.Operator.CONDITIONAL_OR)){

				Expression leftOperand = exp.getLeftOperand();
				if(leftOperand instanceof InfixExpression

						&& ((InfixExpression)leftOperand).getOperator().equals(InfixExpression.Operator.NOT_EQUALS)){

					InfixExpression lOperand = (InfixExpression) leftOperand;

					// leftOperand is null checker?
					if(lOperand.getLeftOperand() instanceof SimpleName && lOperand.getRightOperand().toString().equals("null")){

						String objectNameinLOperand = lOperand.getLeftOperand().toString();

						Expression rightOperand = exp.getRightOperand();

						if(Utils.isWordInStatement(objectNameinLOperand + "\\.", rightOperand.toString())){
							
							// null checker then ||. Should be &&. Otherwise, potential NPE
							// e.g.,- if (driver != null || driver.length() > 0) {
							//      + if (driver != null && driver.length() > 0) {
							int lineNum = wholeCodeAST.getLineNum(exp.getStartPosition());
							detRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, exp.toString(), false, false));
						}
					}	
				}
			}
		}
		
		return detRec;
	}
}
