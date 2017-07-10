package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class WrongClassLogName extends Bug {

	public WrongClassLogName(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "LogFactory loads a wrong class.";
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(MethodInvocation methodInv:wholeCodeAST.getMethodInvocations()){
			
			if(!methodInv.toString().contains("class.getName()")) continue;
			
			if(!(methodInv.getParent() instanceof MethodInvocation
					&& methodInv.getParent().toString().toLowerCase().contains("getlog"))
				|| methodInv.getParent().toString().toLowerCase().contains("getlogi") // exceptional case
			)
				continue;
			
			
			if(!doesClassNameConsistent(methodInv)){
				// get Line number
				int lineNum = wholeCodeAST.getLineNum(methodInv.getStartPosition());
				
				listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, methodInv.getParent().toString(), false, false));	
			}
		}
		
		return listDetRec;
	}

	private boolean doesClassNameConsistent(MethodInvocation methodInv) {
		
		TypeDeclaration typeDec = wholeCodeAST.getTypeDeclaration(methodInv);
		if(typeDec==null) return false;
		
		String typeName = typeDec.getName().toString();
		String classNameInLogging = methodInv.toString().replace(".class.getName()", "");
		
		return typeName.equals(classNameInLogging) || (typeDec.getSuperclassType()!= null && typeDec.getSuperclassType().toString().equals(classNameInLogging));
	}
}
