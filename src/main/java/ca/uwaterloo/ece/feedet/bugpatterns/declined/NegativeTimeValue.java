package ca.uwaterloo.ece.feedet.bugpatterns.declined;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.bugpatterns.Bug;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class NegativeTimeValue extends Bug {

	public NegativeTimeValue(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "Time value may be negative.";
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(MethodInvocation methodInv:wholeCodeAST.getMethodInvocations()){
			
			// long skew = System.currentTimeMillis() - serverCurrentTime;
			
			if(!methodInv.getName().toString().equals("currentTimeMillis")) continue;
			
			
			if(!(methodInv.getParent() instanceof InfixExpression
					&& ((InfixExpression)methodInv.getParent()).getOperator() == InfixExpression.Operator.MINUS
				)
			  )
				continue;
			
			if(!(methodInv.getParent().getParent() instanceof VariableDeclarationFragment))
				continue;			
			
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(methodInv.getStartPosition());
			
			listDetRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodInv.getParent().getParent().toString(), false, false));
		}
		
		return listDetRec;
	}
}
