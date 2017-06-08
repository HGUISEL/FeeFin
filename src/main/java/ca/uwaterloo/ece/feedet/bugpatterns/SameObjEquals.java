package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class SameObjEquals extends Bug {
	
	public SameObjEquals(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		// example
		// if (!initialized) {
        // if (other.uri != null) {
		// -                        return uri.equals(uri);
		// +                        return other.uri.equals(uri);
        // } else {
		ArrayList<MethodInvocation> methodInvs = wholeCodeAST.getMethodInvocations();
		
		for(MethodInvocation methodInv:methodInvs){
			
			// consider method inv only with one argument
			if(methodInv.arguments().size()!=1)
				continue;
			
			if(methodInv.getName().toString().equals("equals")){
				
				// optionExpression must exist
				if(methodInv.getExpression() == null)
					continue;
				
				if(methodInv.arguments().get(0).toString().equals(methodInv.getExpression().toString())){
					
					// ignore condition in Assert or System.out.println
					if(isCondictionInAssertOrPrintStatemet(methodInv))
						continue;
					
					// ignore test method with @test annotation
					if(isTestMethod(methodInv))
						continue;
					
					// ignore equals does not lead to any behavior, i.e., equals method call itself ExpressionStatement. equals method call seems used as a null checker of a caller
					if(isExpressionStatement(methodInv))
						continue;
					
					int lineNum = wholeCodeAST.getLineNum(methodInv.getStartPosition());
					detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodInv.toString(), false, false));
				}
			}
		}
		
		return detRec;
	}

	private boolean isExpressionStatement(MethodInvocation methodInv) {
		
		if(methodInv.getParent() instanceof ExpressionStatement)
			return true;
		
		return false;
	}

	private boolean isTestMethod(ASTNode methodInv) {
		
		if(methodInv.getParent() == null) return false;
		
		if(methodInv.getParent() instanceof MethodDeclaration){
			MethodDeclaration methodDec = (MethodDeclaration) methodInv.getParent();
			@SuppressWarnings("unchecked")
			List<ASTNode> modifiers = methodDec.modifiers();
			
			for(ASTNode node:modifiers){
				if(node instanceof MarkerAnnotation){
					if(((MarkerAnnotation) node).getTypeName().toString().equals("Test"))
						return true;
				}
			}
		}
		
		return isTestMethod(methodInv.getParent());
	}

	private boolean isCondictionInAssertOrPrintStatemet(ASTNode astNode) {
		
		if(astNode.getParent() == null)
			return false;
		
		if(astNode.getParent() instanceof AssertStatement)
			return true;
		
		if(astNode.getParent().toString().startsWith("System.out.print"))
			return true;
		
		return isCondictionInAssertOrPrintStatemet(astNode.getParent());
	}
}
