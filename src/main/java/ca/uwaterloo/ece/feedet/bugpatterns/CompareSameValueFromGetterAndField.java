package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class CompareSameValueFromGetterAndField extends Bug {

	public CompareSameValueFromGetterAndField(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "Compared the same value of the filed as the getter returns the same field.";
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();
		
		// get all getters
		HashMap<String,MethodDeclaration> mapGetters = new HashMap<String,MethodDeclaration>();
		
		ArrayList<TypeDeclaration> typeDecs = wholeCodeAST.getTypeDeclarations();
		
		for(MethodDeclaration methodDec:wholeCodeAST.getMethodDeclarations()){
			
			if(doesMethodDecInInterface(methodDec)) continue;
			
			// only consider the primary class
			if(typeDecs.size() == 0 || !typeDecs.get(0).equals(wholeCodeAST.getTypeDeclaration(methodDec))) continue;
			
			if(methodDec.getReturnType2() != null && methodDec.parameters().size() == 0) // getter
				mapGetters.put(methodDec.getName().toString(),methodDec);
		}
		
		// get all fields
		ArrayList<String> listFields = new ArrayList<String>();
		for(FieldDeclaration fieldDec:wholeCodeAST.getFieldDeclarations()){
			for(VariableDeclarationFragment frag:(List<VariableDeclarationFragment>)fieldDec.fragments()){
				listFields.add(frag.getName().toString());
			}
		}
	
		for(MethodInvocation methodInv:wholeCodeAST.getMethodInvocations()){
			
			String methodName = methodInv.getName().toString();
			
			// skip when the method call has arguments
			if(methodInv.arguments().size()!=0) continue;		
					
			if(methodInv.getExpression() != null) continue; // to only consider member methods
			
			if(!mapGetters.containsKey(methodName)) continue;
			
			boolean returnSameValue = false;
			
			if(methodInv.getParent() instanceof InfixExpression){
				
				InfixExpression inFixExp = (InfixExpression) methodInv.getParent();
				if(inFixExp.getOperator() != InfixExpression.Operator.EQUALS)
					continue;
				
				ArrayList<ReturnStatement> returnStatements = getReturnStatements(mapGetters.get(methodName));
				
				if(returnStatements.size()!= 1) continue;
				
				// Q1
				ReturnStatement returnStmt = returnStatements.get(0);
					
				String returnValue = returnStmt.getExpression().toString();
				
				if(!listFields.contains(returnValue)) continue; // only consider when return value is from fields.
				
				if(inFixExp.getLeftOperand() instanceof MethodInvocation && ((MethodInvocation) inFixExp.getLeftOperand()).getName().toString().equals(methodName)){
					String operand = inFixExp.getRightOperand().toString();
					if(returnValue.equals(operand)
							&& !isLocaVariable(mapGetters.get(methodName),returnValue)
							&& !isLocaVariable((MethodDeclaration)getMethodDeclaration(inFixExp),operand))
						returnSameValue = true;
					
				}else{
					String operand = inFixExp.getLeftOperand().toString();
					if(returnValue.equals(operand)
							&& !isLocaVariable(mapGetters.get(methodName),returnValue)
							&& !isLocaVariable((MethodDeclaration)getMethodDeclaration(inFixExp),operand))
						returnSameValue = true;
				}
			}
			
			if(returnSameValue){				
				// get Line number
				int lineNum = wholeCodeAST.getLineNum(methodInv.getStartPosition());
				listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, methodInv.getParent().toString(), methodInv.getParent().getParent().toString() + "\n" +mapGetters.get(methodName).toString(), false, false));
			}
		}
		
		return listDetRec;
	}

	private boolean doesMethodDecInInterface(MethodDeclaration methodDec) {
		
		TypeDeclaration typeDec = (TypeDeclaration) wholeCodeAST.getInterface(methodDec);
		
		if(typeDec != null){
			if(typeDec.isInterface())
					return true;
		}
		
		return false;
	}

	private ASTNode getMethodDeclaration(ASTNode inFixExp) {
		
		if(inFixExp.getParent() instanceof MethodDeclaration)
			return inFixExp.getParent();
		
		return getMethodDeclaration(inFixExp.getParent());
	}

	private boolean isLocaVariable(MethodDeclaration methodDeclaration, String simpleName) {
		
		final ArrayList<VariableDeclarationFragment> varDecFrags = new ArrayList<VariableDeclarationFragment>();
		final ArrayList<SingleVariableDeclaration> singleVarDecs = new ArrayList<SingleVariableDeclaration>();
		
		methodDeclaration.accept(new ASTVisitor() {
			public boolean visit(VariableDeclarationFragment node) {
				varDecFrags.add(node);
				return super.visit(node);
			}
			public boolean visit(SingleVariableDeclaration node) {
				singleVarDecs.add(node);
				return super.visit(node);
			}
		});	
		
		for(VariableDeclarationFragment frag:varDecFrags){
			if(frag.getName().toString().equals(simpleName)) return true;
		}
		
		for(SingleVariableDeclaration varDec:singleVarDecs){
			if(varDec.getName().toString().equals(simpleName)) return true;
		}
		
		return false;
	}

	private ArrayList<ReturnStatement> getReturnStatements(MethodDeclaration methodDeclaration) {
		
		if(methodDeclaration.getBody()==null) return new ArrayList<ReturnStatement>();
		
		final ArrayList<ReturnStatement> listReturnStmts = new ArrayList<ReturnStatement>();
		
		methodDeclaration.accept(new ASTVisitor() {
			public boolean visit(ReturnStatement node) {
				listReturnStmts.add(node);
				return super.visit(node);
			}
		});	
		
		return listReturnStmts;
	}
}
