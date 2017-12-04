package org.mule.tools.cloudhub.patcher.patcher;

import static java.lang.System.getProperty;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.mule.tools.cloudhub.patcher.github.GitHubClient;
import org.mule.tools.cloudhub.patcher.github.GitHubClient.GitHubClientException;
import org.mule.tools.cloudhub.patcher.pom.PomUtils;
import org.mule.tools.cloudhub.patcher.pom.PomUtils.PomModifierException;

import org.apache.commons.io.FileUtils;

public class CloudHubPatcher
{

    private static final String CLOUDHUB_REPOSITORY_URL_PROPERTY = "patcher.cloudhub.repository.url";
    private static final String CLOUDHUB_REPOSITORY_BRANCH_PROPERTY = "patcher.cloudhub.repository.branch";
    private static final String CLOUDHUB_RELATIVE_POM_LOCATION_PROPERTY = "patcher.cloudhub.relative.pom.location";
    private static final String POM_NEAR_ELEMENT_PROPERTY = "patcher.cloudhub.pom.nearElement";
    private static final String POM_NUMBER_OF_TABS_PROPERTY = "patcher.cloudhub.pom.numberOfTabs";
    private static final String POM_ARTIFACT_TYPE_PROPERTY = "patcher.cloudhub.pom.artifactType";

    private static final String GITHUB_USERNAME_PROPERTY = "patcher.cloudhub.username";
    private static final String GITHUB_PASSWORD_PROPERTY = "patcher.cloudhub.password";
    private static final String GITHUB_LOCAL_REPOSITORY_PROPERTY = "patcher.cloudhub.local.repository";


    private static final String CLOUDHUB_REPOSITORY_URL = getProperty(CLOUDHUB_REPOSITORY_URL_PROPERTY, "https://github.com/ndinu/cloudhub-mule-services");
    private static final String CLOUDHUB_REPOSITORY_BRANCH = getProperty(CLOUDHUB_REPOSITORY_BRANCH_PROPERTY);
    private static final String CLOUDHUB_RELATIVE_POM_LOCATION = getProperty(CLOUDHUB_RELATIVE_POM_LOCATION_PROPERTY, "mule-extensions/mule-distribution/pom.xml");
    private static final String GITHUB_USERNAME = getProperty(GITHUB_USERNAME_PROPERTY);
    private static final String GITHUB_PASSWORD = getProperty(GITHUB_PASSWORD_PROPERTY);
    private static final String GITHUB_LOCAL_REPOSITORY = getProperty(GITHUB_LOCAL_REPOSITORY_PROPERTY);

    private static final String POM_NEAR_ELEMENT = getProperty(POM_NEAR_ELEMENT_PROPERTY, "<!-- mule patches go here -->");
    private static final String POM_ARTIFACT_TYPE = getProperty(POM_ARTIFACT_TYPE_PROPERTY, "jar");

    private static final String POM_TABS = getProperty(POM_NUMBER_OF_TABS_PROPERTY, "8");

    private final GitHubClient gitHubClient = new GitHubClient();
    private final PomUtils pomUtils = new PomUtils();


    public void patch(String githubUsername, String githubPassword,String baseBranch, String branchName, String commitMessage, String groupId, List<SupportEscalationArtifact> seArtifacts) throws GitHubClientException, PomModifierException
    {

        File repository;

        repository = gitHubClient.cloneRepository(githubUsername, githubPassword, CLOUDHUB_REPOSITORY_URL, baseBranch);


        gitHubClient.createBranch(repository, branchName);
        pomUtils.addArtifactItems(repository.getPath() + "/" + CLOUDHUB_RELATIVE_POM_LOCATION, POM_NEAR_ELEMENT, groupId, seArtifacts, POM_ARTIFACT_TYPE, Integer.parseInt(POM_TABS));
        gitHubClient.commit(repository, commitMessage);
        gitHubClient.push(repository, githubUsername, githubPassword);


        try
        {
            FileUtils.deleteDirectory(repository);
        }
        catch (IOException e)
        {
            // Ignore
        }

    }

    public String getCurrentArtifacts(String githubUsername, String githubPassword, String baseBranch) throws GitHubClientException, PomModifierException
    {
        File repository;

        repository = gitHubClient.cloneRepository(githubUsername, githubPassword, CLOUDHUB_REPOSITORY_URL, baseBranch);

        String result = pomUtils.getSEArtifactItems(repository.getPath() + "/" + CLOUDHUB_RELATIVE_POM_LOCATION);

        try
        {
            FileUtils.deleteDirectory(repository);
        }
        catch (IOException e)
        {
            // Ignore.
        }

        return result;
    }

    public static void main(String[] args) throws GitHubClientException, PomModifierException
    {
        List<SupportEscalationArtifact> supportEscalationArtifacts = new ArrayList<>();
        supportEscalationArtifacts.add(new SupportEscalationArtifact("SE-1234", "1.0.0"));
        CloudHubPatcher cloudHubPatcher = new CloudHubPatcher();
        cloudHubPatcher.patch("githubUsername","githubPassword","3.8.5","patch-test", "Patching CH", "org.mule.patches", supportEscalationArtifacts);
    }
}
