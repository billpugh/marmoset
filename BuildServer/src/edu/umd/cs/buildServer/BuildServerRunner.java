package edu.umd.cs.buildServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.httpclient.HttpException;

public class BuildServerRunner extends BuildServer {

    
    public void testSubmission(File objectInputFile, File objectOutputFile) throws Exception {
        
        if (!  okToStart()) {
            System.out.println("Can't start build server runner");
            return;
        }
        try {
        initialize();
        ProjectSubmission<?> projectSubmission;
        try (ObjectInputStream oi = new ObjectInputStream(new FileInputStream(objectInputFile))) {
            projectSubmission = (ProjectSubmission) oi.readObject();
        }
        
        projectSubmission.restore(getBuildServerConfiguration(), this.getLog());
        buildAndTest(projectSubmission);
        
        try (ObjectOutputStream oo = new ObjectOutputStream(new FileOutputStream(objectOutputFile))) {
            oo.writeObject(projectSubmission);
        }
        } finally {
            clearMyPidFile();
        }
        
    }
   
    
    @Override
    public void initConfig() throws IOException {
        super.initConfig();
        
    }

    @Override
    protected void prepareToExecute() throws MissingConfigurationPropertyException {
        
        
    }

    @Override
    protected void doWelcome() throws MissingConfigurationPropertyException, IOException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    protected ProjectSubmission<?> getProjectSubmission() throws MissingConfigurationPropertyException, IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void downloadSubmissionZipFile(ProjectSubmission<?> projectSubmission) throws IOException {
       throw new UnsupportedOperationException();
        
    }

    @Override
    protected void releaseConnection(ProjectSubmission<?> projectSubmission) {
        throw new UnsupportedOperationException();
        
    }

    @Override
    protected void downloadProjectJarFile(ProjectSubmission<?> projectSubmission)
            throws MissingConfigurationPropertyException, HttpException, IOException, BuilderException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    protected void reportTestResults(ProjectSubmission<?> projectSubmission)
            throws MissingConfigurationPropertyException {
        throw new UnsupportedOperationException();
        
    }

    @Override
    protected void reportBuildServerDeath(int submissionPK, int testSetupPK, long lastModified, String kind,
            String load) {
        throw new UnsupportedOperationException();
        
    }
    
    public static void main(String args[]) throws Exception {
        File input = new File(args[0]);
        File output = new File(args[1]);
        BuildServerRunner runner = new BuildServerRunner();
        runner.initConfig();
        runner.testSubmission(input, output);
    }

}
