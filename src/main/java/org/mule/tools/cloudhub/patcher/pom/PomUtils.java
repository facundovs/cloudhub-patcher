package org.mule.tools.cloudhub.patcher.pom;

import static java.lang.String.format;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.writeStringToFile;

import org.mule.tools.cloudhub.patcher.patcher.SupportEscalationArtifact;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PomUtils
{
    private static final String ARTIFACT_REGEX =
            "<groupId>com.mulesoft.muleesb.patches<\\/groupId>[\\n\\r\\t ]*<artifactId>SE-[0-9-.]{1,}<\\/artifactId>[\\n\\r\\t ]*<version>[0-9-a-zA-Z-.]+<\\/version>";
    private static final String ARTIFACT_ID_REGEX =
            "<artifactId>(.+)<\\/artifactId>"
            ;
    private static final String VERSION_REGEX =
            "<version>(.+)<\\/version>";
    private static final Pattern ARTIFACT_PATTERN = Pattern.compile(ARTIFACT_REGEX);
    private static final Pattern ARTIFACT_ID_PATTERN = Pattern.compile(ARTIFACT_ID_REGEX);
    private static final Pattern VERSION_PATTERN = Pattern.compile(VERSION_REGEX);
    private static final String NEAR_ELEMENT_TEMPLATE = "%%NEAR_ELEMENT%";
    private static final String GROUP_ID_TEMPLATE = "%GROUP_ID%";
    private static final String ARTIFACT_ID_TEMPLATE = "%ARTIFACT_ID%";
    private static final String VERSION_TEMPLATE = "%VERSION%";
    private static final String TYPE_TEMPLATE = "%TYPE%";
    private static final String PARENT_INDENTATION_TEMPLATE = "%PARENT_INDENTATION%";
    private static final String CHILD_INDENTATION_TEMPLATE = "%CHILD_INDENTATION%";
    private static final String ARTIFACTS_TEMPLATE = "%ARTIFACTS_TEMPLATE";
    private static final String SED_COMMAND = "sed";
    private static final String NEW_LINE = "%%NEW_LINE%%";
    private static final String TAB = "%%TAB%%";
    private static String ADD_ARTIFACT_ELEMENT_TEMPLATE =
            "s/"+ NEAR_ELEMENT_TEMPLATE +"/" + NEAR_ELEMENT_TEMPLATE + ARTIFACTS_TEMPLATE+ "/g";
    private static String FULL_ARTIFACTS_TEMPLATE =
            NEW_LINE + PARENT_INDENTATION_TEMPLATE + "<artifactItem>" +
            NEW_LINE + CHILD_INDENTATION_TEMPLATE + "<groupId>" + GROUP_ID_TEMPLATE + "<\\/groupId>" +
            NEW_LINE + CHILD_INDENTATION_TEMPLATE + "<artifactId>" + ARTIFACT_ID_TEMPLATE + "<\\/artifactId>" +
            NEW_LINE + CHILD_INDENTATION_TEMPLATE + "<version>" + VERSION_TEMPLATE + "<\\/version>" +
            NEW_LINE + CHILD_INDENTATION_TEMPLATE + "<type>" + TYPE_TEMPLATE + "<\\/type>" +
            NEW_LINE + CHILD_INDENTATION_TEMPLATE + "<overWrite>true<\\/overWrite>" +
            NEW_LINE + CHILD_INDENTATION_TEMPLATE + "<outputDirectory>${mule.unpack.directory}\\/lib\\/user<\\/outputDirectory>" +
            NEW_LINE + PARENT_INDENTATION_TEMPLATE+ "<\\/artifactItem>";

    public void addArtifactItems (String pomPath, String nearElement, String groupId, List<SupportEscalationArtifact> seArtifacts, String artifactType, int numberOfIdentationSpaces) throws PomModifierException
    {
        String parentIndentation = format("%0" + numberOfIdentationSpaces + "d", 0).replace("0", TAB);
        String childIndentation = format("%0" + (numberOfIdentationSpaces + 4)+ "d", 0).replace("0", TAB);
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
        String result = successOutput.replace(TAB, " ")
                            .replace(NEW_LINE, "\n");
        rewritePom(pomPath, result);
    }

    public String getSEArtifactItems (String pomPath) throws PomModifierException
    {

        File pom = new File(pomPath);
        if (! pom.exists())
        {
            throw new PomModifierException(format("The pom with the path %s doesn't exist", pomPath));
        }
        String pomContent;
        try
        {
            pomContent = readFileToString(pom);
        }
        catch (Exception e)
        {
            throw new PomModifierException("There was a problem trying to read the pom");
        }

        List<String> foundArtifacts = new ArrayList<>();
        Matcher matcher = ARTIFACT_PATTERN.matcher(pomContent);
        while(matcher.find())
        {
             foundArtifacts.add(pomContent.substring(matcher.start(), matcher.end()));
        }

        StringBuilder formattedFoundArtifacts = new StringBuilder();
        for (String foundArtifact : foundArtifacts)
        {
            Matcher artifactIdMatcher = ARTIFACT_ID_PATTERN.matcher(foundArtifact);
            if(artifactIdMatcher.find())
            {
                String patchNameWithVersion = artifactIdMatcher.group(1);
                String patchName = patchNameWithVersion.substring(0, patchNameWithVersion.lastIndexOf("-"));
                formattedFoundArtifacts.append(patchName + "/");
            }

            Matcher versionMatcher = VERSION_PATTERN.matcher(foundArtifact);

            if(versionMatcher.find())
            {
                formattedFoundArtifacts.append(versionMatcher.group(1));
            }

            formattedFoundArtifacts.append(",");
        }

        return formattedFoundArtifacts.toString().substring(0, formattedFoundArtifacts.toString().length() - 1);
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
