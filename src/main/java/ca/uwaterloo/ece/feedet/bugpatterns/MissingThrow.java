package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class MissingThrow extends Bug {

	public MissingThrow(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		for(ClassInstanceCreation cInstCre:wholeCodeAST.getClassInstanceCreations()){
			if(cInstCre.getParent() instanceof ExpressionStatement && cInstCre.getType().toString().endsWith("Exception")){
				int lineNum = wholeCodeAST.getLineNum(cInstCre.getStartPosition());
				detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, cInstCre.getParent().toString(), false, false));
			}
		}
		
		return detRec;
	}
}
