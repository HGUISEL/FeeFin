package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class SleepWithNegativeValue extends Bug {

	public SleepWithNegativeValue(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "Thread may sleep with a negative sleep time value.";
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(MethodInvocation methodInv:wholeCodeAST.getMethodInvocations()){
			
			// Q1: Is an argument of Thread.sleep nextInt()?
			if(methodInv.getName().toString().equals("sleep")
					&& methodInv.arguments().size() > 0
					&& methodInv.arguments().get(0) instanceof MethodInvocation
					&& (
							((MethodInvocation)methodInv.arguments().get(0)).getName().toString().equals("nextInt")
							|| ((MethodInvocation)methodInv.arguments().get(0)).getName().toString().equals("nextLong")
						)
				){
				
				// Q2: Does nextInt() have arguments? If yes, nextInt will not give negative values so ignore.
				MethodInvocation nextIntMethod = (MethodInvocation)methodInv.arguments().get(0);
				if(!nextIntMethod.arguments().isEmpty()) continue;
				
				// get Line number
				int lineNum = wholeCodeAST.getLineNum(methodInv.getStartPosition());
		
				detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodInv.getParent().toString(), false, false));
			}
		}
		
		return detRec;
	}
}
