package ca.uwaterloo.ece.feefin;

import ca.uwaterloo.ece.TestSnapshotFeeDetPerProject;
import ca.uwaterloo.ece.feedet.DetectionRecord;
import ca.uwaterloo.ece.feedet.utils.JavaASTParser;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class IncorrectStringCompareTests {

    @Test
    public void TestBugPatternIncorrectStringCompareTest_75() {
        // given
        String src = "public class MyClass {\n" +
                "    private int s = 1;\n" +
                "    private int t = 2;\n" +
                "    \n" +
                "    public void assignValue(String s) {\n" +
                "        s = \"str\";\n" +
                "    }\n" +
                "    \n" +
                "    public boolean compareFields() {\n" +
                "        return s == t;\n" +
                "    }\n" +
                "}\n";

        // when
        JavaASTParser javaASTParser = new JavaASTParser(src);
        List<DetectionRecord> results =
                TestSnapshotFeeDetPerProject.detect(javaASTParser, "IncorrectStringCompare");

        // then
        assertEquals(Collections.emptyList(), results);
    }

    @Test
    public void TestBugPatternIncorrectStringCompareTest_102() {
        // given
        String src = "public class StringComparator {\n" +
                "    private String str1;\n" +
                "    private String str2;\n" +
                "    \n" +
                "    public StringComparator(String str1, String str2) {\n" +
                "        this.str1 = str1;\n" +
                "        this.str2 = str2;\n" +
                "    }\n" +
                "    \n" +
                "    public boolean compareStrings() {\n" +
                "        return str1 == str2;\n" +
                "    }\n" +
                "}\n";

        // when
        JavaASTParser javaASTParser = new JavaASTParser(src);
        List<DetectionRecord> results =
                TestSnapshotFeeDetPerProject.detect(javaASTParser, "IncorrectStringCompare");
        DetectionRecord record = results.get(0);

        // then
        assertEquals("IncorrectStringCompare", record.getPatternName());
    }
}
