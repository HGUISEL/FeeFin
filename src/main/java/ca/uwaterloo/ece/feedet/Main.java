package ca.uwaterloo.ece.feedet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class Main {
	
	boolean help = false;
	String dir,pattern="";

	public static void main(String[] args) {
		new Main().run(args);

	}

	private void run(String[] args) {
		Options options = createOptions();
		
		if(parseOptions(options, args)){
			if (help){
				printHelp(options);
				return;
			}
			
			String[] params = {dir,pattern==null?"":pattern};
			SnapshotFeeDetPerProject.main(params);
			
		}
	}

	Options createOptions(){

		// create Options object
		Options options = new Options();

		// add options
		options.addOption(Option.builder("d").longOpt("dir")
				.desc("Project directory path")
				.hasArg()
				.argName("Path")
				.required()
				.build());

		options.addOption(Option.builder("p").longOpt("pattern")
				.desc("One pattern name to detect. If not set, detect all patterns")
				.hasArg()
				.argName("Pattern Name")
				.build());

		options.addOption(Option.builder("h").longOpt("help")
				.desc("Help")
				.build());

		return options;
	}
	
	boolean parseOptions(Options options,String[] args){

		CommandLineParser parser = new DefaultParser();

		try {

			CommandLine cmd = parser.parse(options, args);
			
			dir = cmd.getOptionValue("d");
			pattern = cmd.getOptionValue("p");
			
			help = cmd.hasOption("h");

		} catch (Exception e) {
			printHelp(options);
			return false;
		}

		return true;
	}

	private void printHelp(Options options) {
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();
		String header = "Execute FeeDet to detect potential bugs. On Windows, use FeeDet.bat instead of ./FeeDet";
		String footer ="";
		formatter.printHelp( "./FeeDet", header, options, footer, true);
	}
}
