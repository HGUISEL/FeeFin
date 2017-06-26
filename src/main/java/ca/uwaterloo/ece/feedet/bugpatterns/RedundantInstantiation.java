package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;

public class RedundantInstantiation extends Bug {

	public RedundantInstantiation(String prjName, JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "Redundant instantiation that may lead to performance issues.";
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		for(VariableDeclarationFragment varDec:wholeCodeAST.getVariableDeclarationFragments()){
		
			if(varDec.getParent() instanceof VariableDeclarationStatement){
				int decLine = wholeCodeAST.getCompilationUnit().getLineNumber(varDec.getParent().getStartPosition());
				
				if(!(varDec.getInitializer() instanceof MethodInvocation))
					continue;
				
				String varName = varDec.getName().toString();
				
				//wholeCodeAST.getCompilationUnit().
				// get next line and check if the assignment is from ClassInstanceCreation
				for(ClassInstanceCreation instCre:wholeCodeAST.getClassInstanceCreations()){
					if(wholeCodeAST.getCompilationUnit().getLineNumber(instCre.getStartPosition()) != decLine+1)
						continue;
					
					if(!(instCre.getParent() instanceof Assignment)) continue;
					
					String varNameForInstCre = ((Assignment)instCre.getParent()).getLeftHandSide().toString();
					
					if(varDec.getParent().getParent().equals(instCre.getParent().getParent().getParent())
							&& varName.equals(varNameForInstCre) && !Utils.isWordInStatement(varName,instCre.toString())){
						
						int lineNum = wholeCodeAST.getLineNum(varDec.getParent().getStartPosition());;
						detRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, varDec.getParent().toString() + instCre.getParent(), false, false));
					}
				}
			}
		}
		
		return detRec;
	}
}
