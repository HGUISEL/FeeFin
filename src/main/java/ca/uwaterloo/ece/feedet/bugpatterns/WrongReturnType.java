package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class WrongReturnType extends Bug {

	public WrongReturnType(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(MethodDeclaration methodDec:wholeCodeAST.getMethodDeclarations()){
			
			if(!isViolatedReturnPrimitiveTypeAndMethodName(methodDec))
				continue;
			
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(methodDec.getStartPosition());
			
			detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodDec.toString(), false, false));
		}
		
		return detRec;
	}

	private boolean isViolatedReturnPrimitiveTypeAndMethodName(MethodDeclaration methodDec) {
		
		if(methodDec.getReturnType2()==null) return false; // ignore if there is no return type, i.e., constructor
		
		String returnType = methodDec.getReturnType2().toString().toLowerCase();
		String methodName = methodDec.getName().toString().toLowerCase();
		
		// only consider following primitive types
		if(containsTagetPrimitive(returnType)){
			
			if(methodName.startsWith("to") || methodName.startsWith("get")){
				
				if(containsOtherPrimitiveRatherThanItSelf(returnType, methodName)) return true;
				
			}
			return false;
		}
		
		return false;
	}

	private boolean containsOtherPrimitiveRatherThanItSelf(String returnType, String methodName) {
		
		if(methodName.contains(returnType)) return false;

		if(methodName.contains("int") 
				|| methodName.contains("long") 
				|| methodName.contains("float") 
				|| methodName.contains("double")
		)
			return true;

		return false;
	}

	private boolean containsTagetPrimitive(String returnType) {
		if(returnType.equals("int") 
				|| returnType.equals("long") 
				|| returnType.equals("float") 
				|| returnType.equals("double")
		)
			return true;
		
		return false;
	}
}
