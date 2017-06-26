package ca.uwaterloo.ece.feedet.bugpatterns;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Repository;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import ca.uwaterloo.ece.feedet.utils.Utils;

public class RedundantException extends Bug {

	public RedundantException(String prjName, JavaASTParser ast, String id, String path, Repository repo){
		initialize(prjName,ast,id,path,repo,this.getClass().getSimpleName());
	}
	
	@Override
	protected String getDescription() {
		return "Redundant exception that may lead to memory leak. (Close an object propoerly rather than throwing an exception.)";
	}
	
	@Override
	public ArrayList<DetectionRecord> detect() {
		
		ArrayList<DetectionRecord> detRec = new ArrayList<DetectionRecord>();

		// (1) retrieve methods
		for(MethodDeclaration mD:wholeCodeAST.getMethodDeclarations()){

			boolean finallyBlockRelatedToThrownExceptionAndLocalObjects  = false;

			// (2) a method throws an exception?
			ArrayList<String> arrStrThrownExceptions = new ArrayList<String>();
			for(Object a:mD.thrownExceptionTypes()){
				arrStrThrownExceptions.add(a.toString());
			}

			// if a method throws and exception, check if the same exception is caught.
			if(arrStrThrownExceptions.size() > 0){

				// a method has a body?
				if(mD.getBody()!=null){

					// a list for local objects initiated by a method call
					ArrayList<VariableDeclarationFragment> lstLocalObjectAssignedByMethodCallOutOfTryBlock = new ArrayList<VariableDeclarationFragment>();
					for(Object o: mD.getBody().statements()){
						// local objects that have a return object from method call out of try block?
						if(o instanceof VariableDeclarationStatement){

							// to ignore primitive types
							Boolean isVarablePrimitiveType = ((VariableDeclarationStatement) o).getType().isPrimitiveType();
							// to ignore array types
							Boolean isArrayType = ((VariableDeclarationStatement) o).getType().isArrayType();
							String typeName = ((VariableDeclarationStatement) o).getType().toString();

							// also ignore String type
							if(!isVarablePrimitiveType && !isArrayType && !typeName.matches("String|Integer|Long|Float|Boolean|Double|Byte|Short|Char|Enum")){
								for(Object frgm:((VariableDeclarationStatement) o).fragments()){
									VariableDeclarationFragment varDecFrag = ((VariableDeclarationFragment) frgm);
									if(varDecFrag.getInitializer()!=null){
										Expression exp = varDecFrag.getInitializer();	
										
										// consider only method invocation
										if(exp instanceof MethodInvocation){
											MethodInvocation metodInv = (MethodInvocation)exp;
										
											if(canMethodInvocationThrowTheSameException(id,path,arrStrThrownExceptions,metodInv,wholeCodeAST))
											{
												lstLocalObjectAssignedByMethodCallOutOfTryBlock.add(varDecFrag);
											}
										}
									}
								}
							}
						}

						// (3) a method catches the same exception?
						if(o instanceof TryStatement){
							boolean sameExceptionInCatchCaluse = false;

							for(Object catchClause:((TryStatement) o).catchClauses()){
								sameExceptionInCatchCaluse = arrStrThrownExceptions.contains(((CatchClause) catchClause).getException().getType().toString())?true:false;								
							}

							// (4) their is a finally block that is related to the exception?
							Block finallyBlock = ((TryStatement) o).getFinally();
							if(sameExceptionInCatchCaluse && finallyBlock!=null){
								int numRelatedLocalObjectsInFinallyBlock = 0;
								for(ASTNode localObjects:lstLocalObjectAssignedByMethodCallOutOfTryBlock){
									if(Utils.isWordInStatement(((VariableDeclarationFragment)localObjects).getName().toString(), finallyBlock.toString()))
										numRelatedLocalObjectsInFinallyBlock++;
								}
								
								if(numRelatedLocalObjectsInFinallyBlock>=2)
									finallyBlockRelatedToThrownExceptionAndLocalObjects = true;
							}
						}	
					}

					// (5) If the finally block exists, check if lstStrLocalObjectAssignedByMethodCallOutOfTryBlock have at least two object.
					//     If yes, it is a potential defect.
					if(lstLocalObjectAssignedByMethodCallOutOfTryBlock.size() >= 2 && finallyBlockRelatedToThrownExceptionAndLocalObjects){
						
						String buggyCode = "";
						int lineNum = -1;
						for(VariableDeclarationFragment varDec:lstLocalObjectAssignedByMethodCallOutOfTryBlock){
							buggyCode += varDec.getParent().toString();
							lineNum = wholeCodeAST.getLineNum(varDec.getParent().getStartPosition());
						}
						// mD.toString()
						detRec.add(new DetectionRecord(bugName, projectName, id, path, lineNum, buggyCode.replace("\n", ""), mD.toString(), false, false));
					}
				}
			}
		}
		
		return detRec;
	}
	
	private boolean canMethodInvocationThrowTheSameException(String shaId,String path, ArrayList<String> arrStrThrownExceptions,
			MethodInvocation methodInv, JavaASTParser preFixWholeCodeAST) {
		String receiverName = "";
		if(methodInv.getExpression()!=null)
			receiverName = methodInv.getExpression().toString();

		String invokedMethodFullyQualifiedName = methodInv.getName().getFullyQualifiedName();
		int numArguments = methodInv.arguments().size();

		if(receiverName.equals("")){	// check method in the same file throws the same exception

			for(MethodDeclaration methodDec:preFixWholeCodeAST.getMethodDeclarations()){

				// get thrown exception from methodDec
				for(Object a:methodDec.thrownExceptionTypes()){
					if(invokedMethodFullyQualifiedName.equals(methodDec.getName().getFullyQualifiedName()) 
							&& numArguments == methodDec.parameters().size()
							&& arrStrThrownExceptions.contains(a.toString())){
						//System.out.println(invokedMethodFullyQualifiedName);
						return true;
					}
				}

				// TODO: any throw statements in the method?

			}

			//System.out.println(methodInv.getExpression().toString());
			//System.out.println(methodInv.getName());
		}
		else{
			ArrayList<String> pathsForJavaSrcCodeOfReceiver = getPathForJavaSrcCodeFromReceiverName(path, preFixWholeCodeAST,receiverName);

			for(String pathForJavaSrcCodeOfReceiver:pathsForJavaSrcCodeOfReceiver){
				// 
				if(doesMethodThrowsException(shaId, pathForJavaSrcCodeOfReceiver,arrStrThrownExceptions, invokedMethodFullyQualifiedName,numArguments)){
					return true;
				}
			}
			return false;
		}

		return false;
	}


	private boolean doesMethodThrowsException(String shaId,String pathForJavaSrcCodeOfReceiver,
			ArrayList<String> arrStrThrownExceptions, String invokedMethodFullyQualifiedName, int numArguments) {

		try {

			for(MethodDeclaration methodDec:wholeCodeAST.getMethodDeclarations()){
				// get thrown exception from methodDec
				for(Object a:methodDec.thrownExceptionTypes()){

					if(invokedMethodFullyQualifiedName.equals(methodDec.getName().getFullyQualifiedName()) 
							&& numArguments == methodDec.parameters().size()
							&& arrStrThrownExceptions.contains(a.toString())){
						//System.out.println(invokedMethodFullyQualifiedName);
						return true;
					}
				}
			}
		} catch (RevisionSyntaxException e) {
			e.printStackTrace();
		}
		return false;
	}

	private ArrayList<String> getPathForJavaSrcCodeFromReceiverName(String path, JavaASTParser preFixWholeCodeAST, String receiverName) {

		ArrayList<String> fullyQualitiedTypeNames = getPotentialFullyQualitifedTypeNames(preFixWholeCodeAST,receiverName);

		String className = "";

		for(TypeDeclaration tD:preFixWholeCodeAST.getTypeDeclarations()){
			//if(tD.modifiers().toString().contains("public"))
			// TODO: consider the first class as the main class
			className = tD.getName().toString();
			break;
		}

		// get package name and Class name
		String fullyQualifiedClassName = "";
		if(preFixWholeCodeAST.getPackageDeclaration() == null){
			fullyQualifiedClassName = className;
		}else{
			fullyQualifiedClassName = preFixWholeCodeAST.getPackageDeclaration().getName().toString() + "." + className;
		}
		// add this to fullyQualitiedTypeNames as well
		fullyQualitiedTypeNames.add(fullyQualifiedClassName);

		String pathPrefix = path.replace("/" + fullyQualifiedClassName.replace(".", File.separator) + ".java", "");

		ArrayList<String> potentialPaths = new ArrayList<String>();
		for(String name:fullyQualitiedTypeNames)
			potentialPaths.add(pathPrefix + File.separator + name.replace(".", "/") + ".java");

		return potentialPaths;
	}

	@SuppressWarnings("unchecked")
	private ArrayList<String> getPotentialFullyQualitifedTypeNames(JavaASTParser preFixWholeCodeAST, String receiverName) {
		// get a fully qualified Type name
		ArrayList<String> lstPackages = new ArrayList<String>();
		ArrayList<String> lstTypes = new ArrayList<String>();

		// get imported packages or types
		for(ImportDeclaration iD:(List<ImportDeclaration>)preFixWholeCodeAST.getImportDeclarations()){
			if(iD.toString().contains("*"))
				lstPackages.add(iD.getName().getFullyQualifiedName());
			else
				lstTypes.add(iD.getName().getFullyQualifiedName());
		}

		HashMap<String,String> mapVarableNameAndTypeName = new HashMap<String,String>(); // key: var name, value: type name;

		// get Type names associate with fields
		for(FieldDeclaration fDc:preFixWholeCodeAST.getFieldDeclarations()){
			String typeName = fDc.getType().toString();
			for(VariableDeclarationFragment vDcF:(List<VariableDeclarationFragment>)fDc.fragments()){
				mapVarableNameAndTypeName.put(vDcF.getName().toString(), typeName);
			}
		}

		// get Type names associate with single variables
		for(SingleVariableDeclaration sVDc:preFixWholeCodeAST.getSingleVariableDeclarations()){
			String typeName = sVDc.getType().toString();
			mapVarableNameAndTypeName.put(sVDc.getName().toString(), typeName);
		}

		String typeName = "";
		if(!mapVarableNameAndTypeName.containsKey(receiverName)) // no type? Then it is static method call. So the receiver name is a type name
			typeName =  receiverName; // receiverName is a Type name
		else // get a Type name from a variable or filed name;
			typeName = mapVarableNameAndTypeName.get(receiverName);

		// generate a list of potential fully qualified type names
		ArrayList<String> potentialFullyQualifiedName = new ArrayList<String>();
		for(String name:lstTypes){
			if(name.endsWith("." + typeName)){
				potentialFullyQualifiedName.add(name);
				return potentialFullyQualifiedName;
			}
		}

		for(String name:lstPackages){
			potentialFullyQualifiedName.add(name + "." + typeName);
		}

		return potentialFullyQualifiedName;
	}
}
