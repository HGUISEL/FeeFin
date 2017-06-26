package ca.uwaterloo.ece.feedet;

public class DetectionRecord {

	String patternName = "";
	String description = "";
	String projectName = "";
	String recentRevisionBugAlive = "";
	String path = "";
	String code = "";
	int lineNum = -1;
	String surroundingCode = "";
	boolean isAlreadyFixed = false;
	boolean isAliveInHEAD = false;
	
	public DetectionRecord(String pName,String dscritption, String prjName,String rcntRevBugAlive,String path,int lineNum, String code,String surroundingCode,boolean alreadyFixed,boolean aliveinHead){
		patternName = pName;
		description = dscritption;
		projectName = prjName;
		recentRevisionBugAlive = rcntRevBugAlive;
		this.path = path;
		this.lineNum = lineNum;
		this.code = code;
		this.surroundingCode = surroundingCode;
		isAlreadyFixed = alreadyFixed;
		isAliveInHEAD = aliveinHead;
	}
	
	public DetectionRecord(String pName, String descirption, String prjName,String rcntRevBugAlive,String path,int lineNum,String code,boolean alreadyFixed,boolean aliveinHead){
		patternName = pName;
		projectName = prjName;
		recentRevisionBugAlive = rcntRevBugAlive;
		this.path = path;
		this.lineNum = lineNum;
		this.code = code;
		isAlreadyFixed = alreadyFixed;
		isAliveInHEAD = aliveinHead;
	}
	
	@Override
	public boolean equals(Object obj) {
		DetectionRecord decRec = (DetectionRecord) obj;
		if(this.path.equals(decRec.path) && this.code.equals(decRec.code)
				&& this.projectName.equals(decRec.projectName)
				&& this.patternName.equals(decRec.patternName)
		){
			
			if(this.recentRevisionBugAlive.equals(decRec.recentRevisionBugAlive)){
				if(this.getLineNum() != decRec.getLineNum())
					return false;
			}
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		String key = projectName + patternName + path + code;
		return key.hashCode();
	}
	
	public boolean naiveEquals(DetectionRecord decRec){
		if(this.patternName.equals(decRec.patternName) && this.path.equals(decRec.path))
			return true;
		
		return false;
	}

	public String getPath() {
		return path;
	}

	public String getRevID() {
		return recentRevisionBugAlive;
	}

	public String getCode() {
		return code;
	}
	
	public int getLineNum() {
		return lineNum;
	}
	
	public String getSurroundCode() {
		return surroundingCode;
	}

	public String getPatternName() {
		return patternName;
	}
	public String getDescription() {
		return description;
	}
}
