package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class BugPatternTemplate extends Bug {

	public BugPatternTemplate(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(ClassInstanceCreation cInstCre:wholeCodeAST.getClassInstanceCreations()){
			
			// Condition to check the detection question: Does a statement call a Exception related to method without throw keyword?
			// if cInstCre.getParent() is ExpressionStatement, it means there is no throw.
			// Plese comment when implementing each detection qeustion.
			// E.g.
			// Q1: Does a statement call a Exception related to method without throw keyword?
			if(cInstCre.getParent() instanceof ExpressionStatement && cInstCre.getType().toString().endsWith("Exception")){
				
				// get Line number
				int lineNum = wholeCodeAST.getLineNum(cInstCre.getStartPosition());
				
				// (1) DetectionRecord with eight arguments
				//		patternName
				//		projectName
				//		recentRevisionBugAlive: git commit id
				//		path
				//		lineNum = lineNum;
				//		detected representative buggy line: should be one line or several tokens
				//		isAlreadyFixed: currently not using set as false
				//		isAliveInHEAD: currently not using set as false
				
				listDetRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, cInstCre.getParent().toString(), false, false));
				
				// (2) DetectionRecord with nine arguments
				//		patternName
				//		projectName
				//		recentRevisionBugAlive: git commit id
				//		path
				//		lineNum = lineNum;
				//		detected representative buggy line: should be one line or several tokens
				//		surroundingCode to show the buggy code
				//		isAlreadyFixed: currently not using set as false
				//		isAliveInHEAD: currently not using set as false
				// detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, cInstCre.getParent().toString(), cInstCre.getParent().getParent().toString(), false, false));
				
			}
		}
		
		return listDetRec;
	}
}
