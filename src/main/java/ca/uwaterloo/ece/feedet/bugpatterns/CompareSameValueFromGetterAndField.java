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

public class CompareSameValueFromGetterAndField extends Bug {

	public CompareSameValueFromGetterAndField(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// get all getters
		HashMap<String,MethodDeclaration> mapGetters = new HashMap<String,MethodDeclaration>();
		
		for(MethodDeclaration methodDec:wholeCodeAST.getMethodDeclarations()){
			if(methodDec.getReturnType2() != null && methodDec.parameters().size() ==0) // getter
				mapGetters.put(methodDec.getName().toString(),methodDec);
		}
	
		for(MethodInvocation methodInv:wholeCodeAST.getMethodInvocations()){
			
			String methodName = methodInv.getName().toString();
			
			if(methodInv.getExpression() != null) continue; // to only consider member methods
			
			if(!mapGetters.containsKey(methodName)) continue;
			
			boolean returnSameValue = false;
			
			if(methodInv.getParent() instanceof InfixExpression){
				
				InfixExpression inFixExp = (InfixExpression) methodInv.getParent();
				if(inFixExp.getOperator() != InfixExpression.Operator.EQUALS)
					continue;
				
				ArrayList<ReturnStatement> returnStatements = getReturnStatements(mapGetters.get(methodName));
				
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
				listDetRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, methodInv.getParent().toString(), methodInv.getParent().getParent().toString() + "\n" +mapGetters.get(methodName).toString(), false, false));
			}
		}
		
		return listDetRec;
	}

	private ArrayList<ReturnStatement> getReturnStatements(MethodDeclaration methodDeclaration) {
		
		if(methodDeclaration.getBody()==null) return new ArrayList<ReturnStatement>();
		
		@SuppressWarnings("unchecked")		
		List<Statement> listStatements = methodDeclaration.getBody().statements();
		
		final ArrayList<ReturnStatement> listReturnStmts = new ArrayList<ReturnStatement>();
		
		methodDeclaration.accept(new ASTVisitor() {
			public boolean visit(ReturnStatement node) {
				listReturnStmts.add(node);
				return super.visit(node);
			}
		});	
		
		for(Statement stmt:listStatements){
			if(stmt instanceof ReturnStatement)
				listReturnStmts.add((ReturnStatement)stmt);
		}
		
		return listReturnStmts;
	}
}
