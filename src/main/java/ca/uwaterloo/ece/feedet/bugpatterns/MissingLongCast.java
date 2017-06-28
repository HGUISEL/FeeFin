package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class MissingLongCast extends Bug {

	public MissingLongCast(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}

	@Override
	protected String getDescription() {
		return "Multiplications or summations of int values need to be casted to long. Otherwise, there might be inteager overflow.";
	}

	@Override
	public ArrayList<DetectionRecord> detect() {
		// Detection results are stored in this ArrayList
		ArrayList<DetectionRecord> listDetRec = new ArrayList<DetectionRecord>();

		// An example loop to get some AST nodes to analyze
		for(InfixExpression infixExp:wholeCodeAST.getInfixExpressions()){
			
			if(!(infixExp.getOperator() == InfixExpression.Operator.TIMES)) continue;
			
			if(infixExp.getParent() instanceof InfixExpression) continue;
			
			if(infixExp.getLeftOperand() instanceof NumberLiteral && infixExp.getRightOperand() instanceof NumberLiteral) continue;
			if(!(infixExp.getLeftOperand() instanceof SimpleName
					|| infixExp.getLeftOperand() instanceof QualifiedName
					|| infixExp.getLeftOperand() instanceof NumberLiteral)) continue;
			if(!(infixExp.getRightOperand() instanceof SimpleName
					|| infixExp.getRightOperand() instanceof QualifiedName
					|| infixExp.getRightOperand() instanceof NumberLiteral)) continue;

			//System.out.println(infixExp.toString());
			
			ArrayList<ASTNode> operands = getAllOperands(infixExp);
			
			// e.g., a * b * 1024L?
			if(containsLongNumberLiteral(operands)) continue;
			
			// Type of SimpleName or QualifiedName is int or Integer?
			if(!areAllNamesIntTypes(operands,getMethodDeclaration(infixExp))) continue;
			
			if(!assignedToLongType(infixExp)) continue;
			
			// get Line number
			int lineNum = wholeCodeAST.getLineNum(infixExp.getStartPosition());
			listDetRec.add(new DetectionRecord(bugName, getDescription(), projectName, id, path, lineNum, infixExp.getParent().toString(), false, false));

		}

		return listDetRec;
	}

	private boolean assignedToLongType(InfixExpression infixExp) {
		
		if(infixExp.getParent() instanceof VariableDeclarationFragment){
			if(((VariableDeclarationStatement)((VariableDeclarationFragment)infixExp.getParent()).getParent()).getType().toString().toLowerCase().equals("long"))
				return true;
		}
		
		if(infixExp.getParent() instanceof MethodInvocation){
			if(((MethodInvocation)infixExp.getParent()).getName().toString().equals("sleep"))
				return true;
		}
		
		return false;
	}

	private MethodDeclaration getMethodDeclaration(ASTNode node) {
		
		if(node.getParent() == null)
			return null;
		
		if(node.getParent() instanceof MethodDeclaration)
			return (MethodDeclaration) node.getParent();
		
		return getMethodDeclaration(node.getParent());
	}

	private boolean areAllNamesIntTypes(ArrayList<ASTNode> operands,MethodDeclaration methodDec) {
		
		HashMap<String,VariableDeclarationFragment> fields = wholeCodeAST.getMapForFieldDeclarations();
		HashMap<String,VariableDeclaration> varDecs = wholeCodeAST.getMapForVariableDeclaration(methodDec);
		
		for(ASTNode operand:operands){
			
			// For SimpleName cases
			if(operand instanceof SimpleName){
				
				VariableDeclaration varDec = (VariableDeclaration) varDecs.get(operand.toString());
				
				if(varDec==null) continue;
				
				if(varDec instanceof VariableDeclarationFragment){
					if(varDec.getParent() instanceof VariableDeclarationStatement){
						VariableDeclarationStatement varDecStmt = (VariableDeclarationStatement) varDec.getParent();
						if(!(varDecStmt.getType().toString().equals("int") || varDecStmt.getType().toString().equals("Integer"))){
							return false;
						}
					}
				}
				
				VariableDeclarationFragment varDecFragForField = (VariableDeclarationFragment) fields.get(operand.toString());
				
				if(varDecFragForField==null) continue;
				
				if(varDecFragForField.getParent() instanceof VariableDeclarationStatement){
					VariableDeclarationStatement varDecStmt = (VariableDeclarationStatement) varDecFragForField.getParent();
					if(!(varDecStmt.getType().toString().equals("int") || varDecStmt.getType().toString().equals("Integer"))){
						return false;
					}
				}
			}
			
			// TODO For QualifiedName cases
		}
		
		return true;
	}

	private boolean containsLongNumberLiteral(ArrayList<ASTNode> operands) {
		
		for(ASTNode operand:operands){
			if(operand instanceof NumberLiteral){
				NumberLiteral numberLiteral = (NumberLiteral) operand;
				if(numberLiteral.getToken().toLowerCase().endsWith("l"))
					return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private ArrayList<ASTNode> getAllOperands(InfixExpression infixExp) {
		ArrayList<ASTNode> operands = new ArrayList<ASTNode>();
		
		operands.add(infixExp.getLeftOperand());
		operands.add(infixExp.getRightOperand());
		operands.addAll(infixExp.extendedOperands());
		
		return operands;
	}
}
