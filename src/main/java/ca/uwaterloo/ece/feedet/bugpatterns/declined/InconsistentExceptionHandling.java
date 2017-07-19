package ca.uwaterloo.ece.feedet.bugpatterns.declined;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.bugpatterns.Bug;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class InconsistentExceptionHandling extends Bug {

	public InconsistentExceptionHandling(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "An exception that is not defined in a method definition was thrown in a method body. Please, check if thrown exceptions are correctly used.";
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(MethodDeclaration methodDec:wholeCodeAST.getMethodDeclarations()){
			
			// get thrown exception types
			@SuppressWarnings("unchecked")
			List<SimpleType> thrownExceptionTypes = (List<SimpleType>) methodDec.thrownExceptionTypes();
			
			if(thrownExceptionTypes.size() == 0) continue;
			
			// get throw statements in a method
			ArrayList<ThrowStatement> throwStatements = wholeCodeAST.getThrowStatements(methodDec);
			
			if(throwStatements.size() == 0) continue;
			
			if(definedThrowStatement(throwStatements,thrownExceptionTypes)) continue;
			
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(methodDec.getStartPosition());
			
			
			listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, methodDec.toString(), methodDec.toString(), false, false));			
		}
		
		return listDetRec;
	}

	private boolean definedThrowStatement(ArrayList<ThrowStatement> throwStatements,
			List<SimpleType> thrownExceptionTypes) {
		
		for(ThrowStatement throwStmt:throwStatements){
			if(!(throwStmt.getExpression() instanceof ClassInstanceCreation)) continue;
			
			ClassInstanceCreation instCre = (ClassInstanceCreation) throwStmt.getExpression();
			SimpleType simpleType = (SimpleType) instCre.getType();
			
			for(SimpleType thrownExpType:thrownExceptionTypes) {
				
				if(simpleType.toString().equals(thrownExpType.toString())) return true;
				
			}	
		}
		
		return false;
	}
}
