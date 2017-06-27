package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class MissingCurrentObjRefThis extends Bug {

	public MissingCurrentObjRefThis(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "Current object reference, this, may be missed as the loca variable with the same file name used twice.";
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// An example loop to get some AST nodes to analyze
		for(MethodDeclaration methodDec:wholeCodeAST.getMethodDeclarations()){
			TypeDeclaration classWhereMethodExists = wholeCodeAST.getTypeDeclationOf(methodDec);
			ArrayList<String> fieldNames = wholeCodeAST.getFieldNames(classWhereMethodExists);
			ArrayList<String> localVarialbleNames = wholeCodeAST.getVariableNames(methodDec);
			ArrayList<MethodInvocation> methodInvs = wholeCodeAST.getMethodInvocations(methodDec);
			
			for(MethodInvocation methodInv:methodInvs){
				
				if(!(methodInv.getParent() instanceof ExpressionStatement)) continue;
				
				if(methodInv.getExpression()==null) continue;
				
				String optionalExpName = methodInv.getExpression().toString();
				
				if(optionalExpName.isEmpty()) continue;

				for(ASTNode node:(List<ASTNode>) methodInv.arguments()){
					if(node.toString().equals(optionalExpName)){
						if(!anyFieldAndVariableWithSameName(optionalExpName,fieldNames,localVarialbleNames)) continue;
						int lineNum = wholeCodeAST.getLineNum(methodDec.getStartPosition());
						listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, methodInv.toString(),methodInv.getParent().toString(), false, false));
					}
				}
			}
		}
		
		return listDetRec;
	}
	
	private boolean anyFieldAndVariableWithSameName(String optionalExpName, ArrayList<String> fieldNames,
			ArrayList<String> localVarialbleNames) {
		
		if(!localVarialbleNames.contains(optionalExpName)) return false;
		
		if(!fieldNames.contains(optionalExpName)) return false;
		
		return true;
	}
}
