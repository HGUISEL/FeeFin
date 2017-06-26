package ca.uwaterloo.ece.feedet.bugpatterns.declined;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.bugpatterns.Bug;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class WrongReturnTypeForSetter extends Bug {

	public WrongReturnTypeForSetter(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(MethodDeclaration methodDec:wholeCodeAST.getMethodDeclarations()){
			
			if(!methodDec.getName().toString().startsWith("set")) continue;
			
			String returnType = methodDec.getReturnType2().toString().toLowerCase();
			
			if(returnType.equals("void") || returnType.equals("boolean")) continue;
			
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(methodDec.getStartPosition());
			listDetRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodDec.toString(), false, false));
		}
		
		return listDetRec;
	}
}
