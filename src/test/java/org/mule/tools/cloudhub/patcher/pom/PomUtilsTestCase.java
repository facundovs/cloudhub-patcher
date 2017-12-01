package org.mule.tools.cloudhub.patcher.pom;

import static java.io.File.createTempFile;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

import org.mule.tools.cloudhub.patcher.patcher.SupportEscalationArtifact;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class PomUtilsTestCase
{
    private static final String NEAR_ELEMENT = "<!-- mule patches go here -->";
    private final PomUtils pomUtils = new PomUtils();
    private File tempPom;

    @Before
    public void setUp() throws Exception
    {
        tempPom = createTempFile("pom", ".xml");
        writeStringToFile(tempPom, NEAR_ELEMENT);
    }

    @Test
    public void testRewritePom() throws Exception
    {
        List<SupportEscalationArtifact> supportEscalationArtifacts = new ArrayList<>();
        supportEscalationArtifacts.add(new SupportEscalationArtifact("test", "1.0.0"));
        supportEscalationArtifacts.add(new SupportEscalationArtifact("test2", "2.0.0"));

        pomUtils.addArtifactItems(tempPom.getPath(), NEAR_ELEMENT, "org.test", supportEscalationArtifacts, "jar", 8);
        String result = readFileToString(tempPom);

        assertThat(result, containsString("<groupId>org.test</groupId>"));
        assertThat(result, containsString("<artifactId>test</artifactId>"));
        assertThat(result, containsString("<version>1.0.0</version>"));
        assertThat(result, containsString("<type>jar</type>"));
        assertThat(result, containsString("<artifactId>test2</artifactId>"));
        assertThat(result, containsString("<version>2.0.0</version>"));
        assertThat(result, containsString("<type>jar</type>"));
    }

    @Test
    public void testFindArtifacts() throws Exception
    {
        String result = pomUtils.getSEArtifactItems("src/test/resources/pom.xml");
        assertThat(result, is("SE-6837-3.9.0/1.0,SE-7057-3.9.0/1.0.0,SE-7087-3.9.0/1.0.0"));
    }
}