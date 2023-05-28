package ca.uwaterloo.ece;

import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.bugpatterns.Bug;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import org.eclipse.jgit.lib.Repository;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class TestSnapshotFeeDetPerProject {
    public static List<DetectionRecord> detect(JavaASTParser javaASTParser, String patternName) {
        List<DetectionRecord> result = new ArrayList<>();

        try {
            Class<?> bugPatternClass = Class.forName("ca.uwaterloo.ece.feedet.bugpatterns." + patternName);
            Constructor<?> constructor = bugPatternClass.getConstructor(String.class, JavaASTParser.class, String.class, String.class, Repository.class);
            result = process(((Bug) constructor.newInstance(null, javaASTParser, null, null, null)).detect());

        } catch (NoSuchMethodException | SecurityException | InstantiationException |
                 IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
            e1.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Pattern, " + patternName + ", does not exist. Please check if the pattern name is correct.");
            System.exit(0);
        }
        return result;
    }

    private static List<DetectionRecord> process(ArrayList<DetectionRecord> detectionRecords) {
        if (detectionRecords.isEmpty()) {
            System.out.println("No bugs found");
        }
        for (DetectionRecord detRec : detectionRecords) {
            System.out.println("###\t" + detRec.getPatternName() + "\t"
                    + detRec.getLineNum() + "\t"
                    + detRec.getCode()
            );
            System.out.println(detRec.getCode());
            System.out.println(detRec.getSurroundCode());
        }
        return detectionRecords;
    }
}