package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class MissingLongCast extends Bug {

	public MissingLongCast(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}

	@Override
	protected String getDescription() {
		return "Multiplications or summations of int values need to be casted to long. Otherwise, there might be inteager overflow.";
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();

		// An example loop to get some AST nodes to analyze
		for(InfixExpression infixExp:wholeCodeAST.getInfixExpressions()){
			
			if(!(infixExp.getOperator() == InfixExpression.Operator.TIMES || infixExp.getOperator() == InfixExpression.Operator.PLUS)) continue;
			
			if(infixExp.getParent() instanceof InfixExpression) continue;
			
			
			// TODO
			if(!(infixExp.getLeftOperand() instanceof SimpleName || infixExp.getLeftOperand() instanceof QualifiedName)) continue;
			if(!(infixExp.getRightOperand() instanceof SimpleName || infixExp.getRightOperand() instanceof QualifiedName)) continue;

			System.out.println(infixExp.toString());
			
			// get Line number
			//int lineNum = wholeCodeAST.getLineNum(infixExp.getStartPosition());


			//listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, infixExp.getParent().toString(), false, false));

		}

		return listDetRec;
	}
}
