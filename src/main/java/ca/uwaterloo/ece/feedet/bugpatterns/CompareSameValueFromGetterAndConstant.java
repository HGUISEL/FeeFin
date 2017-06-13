package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class CompareSameValueFromGetterAndConstant extends Bug {

	public CompareSameValueFromGetterAndConstant(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();
		
		// get all getters
		HashMap<String,MethodDeclaration> getters = new HashMap<String,MethodDeclaration>();
		
		for(MethodDeclaration methodDec:wholeCodeAST.getMethodDeclarations()){
			if(methodDec.getReturnType2() != null && methodDec.parameters().size() ==0) // getter
				getters.put(methodDec.getName().toString(),methodDec);
		}
		
	
		for(MethodInvocation methodInv:wholeCodeAST.getMethodInvocations()){
			
			String methodName = methodInv.getName().toString();
			
			if(!getters.containsKey(methodName)) continue;
			
			boolean returnSameValue = false;
			
			if(methodInv.getParent() instanceof InfixExpression){
				
				InfixExpression inFixExp = (InfixExpression) methodInv.getParent();
				if(inFixExp.getOperator() != InfixExpression.Operator.EQUALS)
					continue;
				
				ArrayList<ReturnStatement> returnStatements = getReturnStatements(getters.get(methodName));
				
				// Q1
				for(ReturnStatement returnStmt:returnStatements){
					
					String returnValue = returnStmt.getExpression().toString();
					
					if(returnValue.equals("null")) continue;
					
					if(inFixExp.getLeftOperand() instanceof MethodInvocation && ((MethodInvocation) inFixExp.getLeftOperand()).getName().toString().equals(methodName)){
						String operand = inFixExp.getRightOperand().toString();
						if(returnValue.equals(operand))
							returnSameValue = true;
						
					}else{
						String operand = inFixExp.getLeftOperand().toString();
						if(returnValue.equals(operand))
							returnSameValue = true;
					}
				}
				
			}
			
			if(returnSameValue){				
				// get Line number
				int lineNum = wholeCodeAST.getLineNum(methodInv.getStartPosition());
				detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodInv.getParent().toString(), getters.get(methodName).toString(), false, false));
			}
		}
		
		return detRec;
	}

	private ArrayList<ReturnStatement> getReturnStatements(MethodDeclaration methodDeclaration) {
		
		@SuppressWarnings("unchecked")
		List<Statement> statements = methodDeclaration.getBody().statements();
		
		final ArrayList<ReturnStatement> returnStmts = new ArrayList<ReturnStatement>();
		
		methodDeclaration.accept(new ASTVisitor() {
			public boolean visit(ReturnStatement node) {
				returnStmts.add(node);
				return super.visit(node);
			}
		});	
		
		for(Statement stmt:statements){
			if(stmt instanceof ReturnStatement)
				returnStmts.add((ReturnStatement)stmt);
		}
		
		return returnStmts;
	}
}
