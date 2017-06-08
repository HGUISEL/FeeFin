package ca.uwaterloo.ece.feedet;

import java.io.File;
import java.io.FilenameFilter;

public class JITFeeDetSimulator {

	public static void main(String[] args) {
		new JITFeeDetSimulator().run(args);
	}

	private void run(String[] args) {
		String rootPath = args[0];
		String patternName = args.length==1? "":args[1];
		
		File file = new File(rootPath);
		String[] projects = file.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});

		int i=0;	
		for(String project:projects){
			System.out.println((i++) + "=================\n" + project + "\n=================");
			//String [] args ={"-d","data/exampleBIChanges.txt", "-g", System.getProperty("user.home") + "/git/BICER"};
			String [] arguments ={"-g",rootPath + File.separator + project,"-p",project,"-n",patternName};
			BuggyChangeDetectorAndValidator runner = new BuggyChangeDetectorAndValidator();
			runner.run(arguments);
		}
	}

}
