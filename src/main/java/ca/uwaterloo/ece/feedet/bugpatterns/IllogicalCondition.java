package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;

public class IllogicalCondition extends Bug {

	public IllogicalCondition(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "Condition contains a logical error.";
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();

		// InFixExpression: leftOperand operator rightOperand
		// Conditional expression

		for(InfixExpression exp:wholeCodeAST.getInfixExpressions()){

			if(exp.getOperator().equals(InfixExpression.Operator.CONDITIONAL_OR)){

				Expression leftOperand = getLeftOperand(exp.getLeftOperand());
				
				if(leftOperand instanceof InfixExpression

						&& ((InfixExpression)leftOperand).getOperator().equals(InfixExpression.Operator.NOT_EQUALS)){

					InfixExpression lOperand = (InfixExpression) leftOperand;

					// leftOperand is null checker?
					if(lOperand.getLeftOperand() instanceof SimpleName && lOperand.getRightOperand().toString().equals("null")){

						String objectNameinLOperand = lOperand.getLeftOperand().toString();

						Expression rightOperand = exp.getRightOperand();

						if(!Utils.isWordInStatement(objectNameinLOperand + "\\.", rightOperand.toString())
								&& Utils.isWordInStatement(objectNameinLOperand,rightOperand.toString())){
							// if null checker with || and the object is used as parameter, null checker should be "=="
							// - if (nodeTypeName != null || ns.getNodeTypeName().equals(nodeTypeName)) {
							// + if (nodeTypeName == null || ns.getNodeTypeName().equals(nodeTypeName)) {
							if(rightOperand instanceof MethodInvocation){
								
								// ignore when it has multiple argmenets
								if(((MethodInvocation)rightOperand).arguments().size()>1)
									continue;
								
								for(Object param:((MethodInvocation)rightOperand).arguments()){
									
									if(param.toString().equals(objectNameinLOperand)){
										int lineNum = wholeCodeAST.getLineNum(exp.getStartPosition());
										detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, exp.toString(), false, false));
										break;
									}
									
									// TODO: what's this for?
									if(param instanceof MethodInvocation){
										if(((MethodInvocation) param).getExpression().toString().equals(objectNameinLOperand)){
											int lineNum = wholeCodeAST.getLineNum(exp.getStartPosition());
											detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, exp.toString(), false, false));
										}
									}
								}
							}
						}
					}	
				}
			}
		}
		
		return detRec;
	}

	private Expression getLeftOperand(Expression leftOperand) {
		// to deal with the left operand with parentheses. e.g., if ((relatesTo != null) || "".equals(relatesTo)) // ParenthesizedExpression
		if(leftOperand instanceof ParenthesizedExpression){
			
			return getLeftOperand(((ParenthesizedExpression) leftOperand).getExpression());
		}
		return leftOperand;
	}
}
