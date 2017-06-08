package ca.uwaterloo.ece.feedet.bugpatterns;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;

public class InconsistentNullChecker extends Bug {

	public InconsistentNullChecker(String prjName,JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}

	@Override
	public ArrayList<DetectionRecord> detect() {

		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();

		// example
		//   final BalancerDatanode d = datanodeMap.get(datanodeUuid);
		// - if (datanode != null) { // not an unknown datanode
		// + if (d != null) { // not an unknown datanode
		//	   block.addLocation(d);
		ArrayList<IfStatement> ifStatements = wholeCodeAST.getIfStatements();

		for(IfStatement ifStatement:ifStatements){

			ifStatement.getExpression();
			
			if(!(ifStatement.getExpression() instanceof InfixExpression))
				continue;
			
			InfixExpression infixExp = (InfixExpression)ifStatement.getExpression();
			
			if(infixExp.getOperator() == Operator.NOT_EQUALS){
				
				String objName = "";
				if(infixExp.getRightOperand().toString().equals("null")){
					objName = infixExp.getLeftOperand().toString();
				}
				
				if(infixExp.getLeftOperand().toString().equals("null")){
					objName = infixExp.getRightOperand().toString();
				}
				
				// if it is not a object null checker, skip
				if(objName.isEmpty()) continue;
				
				// if object type is not sure, ignore
				// get object type
				//String objType = getObjType(objName);
				//if(objType.isEmpty()) continue;
				
				// find the similar name, if no similar name, continue
				if(!existSimilarObjNameInThenBlock(ifStatement.getThenStatement(),objName)) continue;
				
				// find the same type but different object name is in ifStatement
				//if(!isThereAnObjectWithTheSameTypeButNotNameIsNotSubString(ifStatement.getThenStatement(),objType,objName)) continue;
				
				// revision 1: check of objName is defined as a member. Otherwise, skip
				/*boolean IsObjAMember = false;
				ArrayList<FieldDeclaration> fieldDecs = wholeCodeAST.getFieldDeclarations();
				for(FieldDeclaration fieldDec:fieldDecs){
					for(Object fragment:fieldDec.fragments()){
						if(fragment instanceof VariableDeclarationFragment){
							VariableDeclarationFragment frag = (VariableDeclarationFragment) fragment;
							if(frag.getName().toString().equals(objName))
								IsObjAMember = true;
						}
					}
				}
				if(!IsObjAMember) continue;*/
				
				// revision 2: if no any object method call, skip since null checker might directly be related to objectName.methodcall()
				// this ignores something like break; return true;
				/*if(!containsObjMethodCall(ifStatement.getThenStatement(),objName))
					continue;*/
				
				// TODO: contains should be exact. Utils.isWordInStatement has issue when the objName is something like a().b()
				// Need to create a method exactly compare any objName to ifStatement.toString()
				if(!ifStatement.getThenStatement().toString().contains(objName)){
					
					int lineNum = wholeCodeAST.getLineNum(ifStatement.getStartPosition());
					detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, ifStatement.toString(), false, false));
				}
			}
		}

		return detRec;
	}

	private boolean existSimilarObjNameInThenBlock(ASTNode astNode, String objName) {
		final ArrayList<SimpleName> lstSimpleName = new ArrayList<SimpleName>();
		if(astNode instanceof SimpleName) lstSimpleName.add((SimpleName)astNode);
		
		astNode.accept(new ASTVisitor() {
					@Override
					public boolean visit(SimpleName node) {
						lstSimpleName.add(node);
						return super.visit(node);
					}
		}
		);
		
		for(SimpleName simpleName:lstSimpleName){
			// TODO: need to update for the same variable names that can be defined several times in a class
			if((simpleName.toString().contains(objName) || objName.contains(simpleName.toString())))
				return true;
		}
		
		return false;
	}
}
