package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class InconsistentIncrementerInWhile extends Bug {

	public InconsistentIncrementerInWhile(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(WhileStatement whileStmt:wholeCodeAST.getWhileStatements()){
			
			ArrayList<InfixExpression> infixExps = wholeCodeAST.getInfixExpressions(whileStmt.getExpression());
			
			if(infixExps.size()==0) continue;

			//TODO			
			
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(whileStmt.getStartPosition());
			
			listDetRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, whileStmt.toString(), false, false));
			
		}
		
		return listDetRec;
	}
}
