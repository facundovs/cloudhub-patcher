package org.mule.tools.cloudhub.patcher.pom;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.writeStringToFile;

import org.mule.tools.cloudhub.patcher.patcher.SupportEscalationArtifact;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.util.List;

public class PomModifier
{
    private static final String NEAR_ELEMENT_TEMPLATE = "%%NEAR_ELEMENT%";
    private static final String GROUP_ID_TEMPLATE = "%GROUP_ID%";
    private static final String ARTIFACT_ID_TEMPLATE = "%ARTIFACT_ID%";
    private static final String VERSION_TEMPLATE = "%VERSION%";
    private static final String TYPE_TEMPLATE = "%TYPE%";
    private static final String PARENT_INDENTATION_TEMPLATE = "%PARENT_INDENTATION%";
    private static final String CHILD_INDENTATION_TEMPLATE = "%CHILD_INDENTATION%";
    private static final String ARTIFACTS_TEMPLATE = "%ARTIFACTS_TEMPLATE";
    private static final String SED_COMMAND = "sed";
    private static String ADD_ARTIFACT_ELEMENT_TEMPLATE =
            "s/"+ NEAR_ELEMENT_TEMPLATE +"/" + NEAR_ELEMENT_TEMPLATE + ARTIFACTS_TEMPLATE+ "/g";
    private static String FULL_ARTIFACTS_TEMPLATE =
            "\\r\\n" + PARENT_INDENTATION_TEMPLATE + "<artifactItem>" +
            "\\r\\n" + CHILD_INDENTATION_TEMPLATE + "<groupId>" + GROUP_ID_TEMPLATE + "<\\/groupId>" +
            "\\r\\n" + CHILD_INDENTATION_TEMPLATE + "<artifactId>" + ARTIFACT_ID_TEMPLATE + "<\\/artifactId>" +
            "\\r\\n" + CHILD_INDENTATION_TEMPLATE + "<version>" + VERSION_TEMPLATE + "<\\/version>" +
            "\\r\\n" + CHILD_INDENTATION_TEMPLATE + "<type>" + TYPE_TEMPLATE + "<\\/type>" +
            "\\r\\n" + CHILD_INDENTATION_TEMPLATE + "<overWrite>true<\\/overWrite>" +
            "\\r\\n" + CHILD_INDENTATION_TEMPLATE + "<outputDirectory>${mule.unpack.directory}\\/lib\\/user<\\/outputDirectory>" +
            "\\r\\n" + PARENT_INDENTATION_TEMPLATE+ "<\\/artifactItem>";

    public void addArtifactItems (String pomPath, String nearElement, String groupId, List<SupportEscalationArtifact> seArtifacts, String artifactType, int numberOfTabs) throws PomModifierException
    {
        String parentIndentation = format("%0" + numberOfTabs + "d", 0).replace("0", "\\t");
        String childIndentation = format("%0" + (numberOfTabs + 1)+ "d", 0).replace("0", "\\t");
        StringBuilder artifacts = new StringBuilder();
        for (SupportEscalationArtifact seArtifact : seArtifacts)
        {
            artifacts.append(FULL_ARTIFACTS_TEMPLATE .replace(GROUP_ID_TEMPLATE, groupId)
                                     .replace(ARTIFACT_ID_TEMPLATE, seArtifact.getArtifactId())
                                     .replace(VERSION_TEMPLATE, seArtifact.getVersion())
                                     .replace(TYPE_TEMPLATE, artifactType)
                                     .replace(PARENT_INDENTATION_TEMPLATE, parentIndentation)
                                     .replace(CHILD_INDENTATION_TEMPLATE, childIndentation));
        }

        String command = ADD_ARTIFACT_ELEMENT_TEMPLATE.replace(NEAR_ELEMENT_TEMPLATE, nearElement)
                                    .replace(ARTIFACTS_TEMPLATE, artifacts.toString());

        String successOutput;
        String errorOutput;
        try
        {
            String [] commands = {SED_COMMAND, command, pomPath};
            Process process = Runtime.getRuntime().exec(commands);
            successOutput = getOutput(new BufferedReader(new InputStreamReader(process.getInputStream())));
            errorOutput = getOutput(new BufferedReader(new InputStreamReader(process.getErrorStream())));
        }
        catch (IOException e)
        {
            throw new PomModifierException("There was an error trying to add the artifactItem", e);
        }

        if(errorOutput != null && !errorOutput.equals(""))
        {
            throw new PomModifierException("There was an error trying to add the artifactItem: " + errorOutput );
        }

        rewritePom(pomPath, successOutput);
    }

    private void rewritePom (String pomPath, String newContent) throws PomModifierException
    {
        File pom = new File(pomPath);
        if(!pom.delete())
        {
            throw new PomModifierException("There was a problem trying to modify the pom. It couldn't be deleted");
        }
        try
        {
            writeStringToFile(pom, newContent);
        }
        catch (IOException e)
        {
            throw new PomModifierException("There was a problem trying to rewrite the pom.");
        }

    }

    private String getOutput (BufferedReader reader) throws IOException
    {
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
        {
            output.append(line + "\n");
        }
        return output.toString();
    }

    public class PomModifierException extends Exception
    {

        public PomModifierException(String message)
        {
            super(message);
        }

        public PomModifierException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
