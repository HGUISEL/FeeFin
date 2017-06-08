package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class MissingTimeResolution extends Bug {

	public MissingTimeResolution(String prjName, JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();

		// (1) retrieve methods
		for(MethodInvocation mI:wholeCodeAST.getMethodInvocations()){

			//setLastModified(System.currentTimeMillis());
			if(mI.toString().equals("System.currentTimeMillis()")){
				if(mI.getParent() instanceof MethodInvocation){
					MethodInvocation parentMI = (MethodInvocation) mI.getParent();
					if(parentMI.getName().toString().equals("setLastModified")){
						int lineNum = wholeCodeAST.getLineNum(parentMI.getStartPosition());
						detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, parentMI.toString(), false, false));
					}
				}
			}
		}
		
		return detRec;
	}
}