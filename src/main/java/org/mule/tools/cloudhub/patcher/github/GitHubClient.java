package org.mule.tools.cloudhub.patcher.github;

import static java.nio.file.Files.createTempDirectory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;

public class GitHubClient
{

    public void createBranch(File repository, String resultBranchName) throws GitHubClientException
    {
        try
        {
            Git.open(repository).checkout().setName(resultBranchName).setCreateBranch(true).call();
        }
        catch (Exception e)
        {
            throw new GitHubClientException("There was a problem trying to create the branch", e);
        }
    }

    public void commit (File repository, String commitMessage) throws GitHubClientException
    {
        try
        {
            Git.open(repository).commit().setAll(true).setMessage(commitMessage).call();
        }
        catch (Exception e)
        {
            throw new GitHubClientException("There was a problem trying to commit the pom change", e);
        }
    }

    public void push (File repository, String username, String password) throws GitHubClientException
    {
        try
        {
            Git.open(repository).push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).call();
        }
        catch (Exception e)
        {
            throw new GitHubClientException("There was a problem trying to push the pom change", e);
        }
    }

    public File cloneRepository (String username, String password, String repositoryPath, String originalBranch) throws GitHubClientException
    {
        File tempDirectory;
        try
        {
            tempDirectory = createTempDirectory("tempRepository").toFile();
            CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
            Git.cloneRepository().setCredentialsProvider(credentialsProvider).setURI(repositoryPath).setBranch(originalBranch).setDirectory(tempDirectory).call();
            return tempDirectory;
        }
        catch (Exception e)
        {
            throw new GitHubClientException("There was a problem trying to clone the repository", e);
        }
    }

    public class GitHubClientException extends Exception
    {
        public GitHubClientException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

}
