package org.mule.tools.cloudhub.patcher.patcher;

public class SupportEscalationArtifact
{
    private final String artifactId;
    private final String version;

    public SupportEscalationArtifact(String artifactId, String version)
    {
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }
}
