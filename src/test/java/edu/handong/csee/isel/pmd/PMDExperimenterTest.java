package edu.handong.csee.isel.pmd;

import static org.junit.Assert.*;

import org.junit.Test;

public class PMDExperimenterTest {

	@Test
	public void test() {
		String[] args = {"-d", "C:\\Users\\jaech\\git\\FeeFin"};
		PMDExperimenter.main(args);
	}
	
	@Test
	public void testApexCore() {
		String[] args = {"-d", "C:\\Users\\jaech\\git\\apex-core", "-s", "2016-07-15 17:26:35", "-e","2016-07-19 05:48:13","-v"};
		PMDExperimenter.main(args);
	}
	
	@Test
	public void testApexCoreAIOOBE() {
		String[] args = {"-d", "C:\\Users\\jaech\\git\\apex-core", "-s", "2012-12-02 22:02:55", "-e","2014-12-02 23:38:48","-v"};
		PMDExperimenter.main(args);
	}
	

}
