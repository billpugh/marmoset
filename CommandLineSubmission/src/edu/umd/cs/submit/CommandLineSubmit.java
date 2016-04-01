/**
 * Marmoset: an automated snapshot, submission and testing system
 * Copyright (C) 2005, University of Maryland
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Email: marmoset-discuss@cs.umd.edu
 *
 * Snail mail:
 * The Marmoset Project
 * Attention: Dr. Bill Pugh
 * Dept. of Computer Science
 * Univ. of Maryland
 * College Park, MD 20742
 */

/*
 * Created on Jan 24, 2005
 *
 */
package edu.umd.cs.submit;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.protocol.Protocol;

/**
 * @author pugh
 * 
 */
public class CommandLineSubmit {

    public static final String VERSION = "0.9.2";
    public static final int HTTP_TIMEOUT = Integer.getInteger("HTTP_TIMEOUT", 30).intValue() * 1000;

    public static void main(String[] args) {
        try {
            Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
            File home = args.length > 0 ? new File(args[0]) : new File(".");

            Protocol.registerProtocol("easyhttps", easyhttps);
            File submitFile = new File(home, ".submit");
            File submitUserFile = new File(home, ".submitUser");
            File submitIgnoreFile = new File(home, ".submitIgnore");
            File cvsIgnoreFile = new File(home, ".cvsignore");

            if (!submitFile.canRead()) {
                System.out.println("Must perform submit from a directory containing a \".submit\" file");
                System.out.println("No such file found at " + submitFile.getCanonicalPath());
                System.exit(1);
            }

            Properties p = new Properties();
            p.load(new FileInputStream(submitFile));
            String submitURL = p.getProperty("submitURL");
            if (submitURL == null) {
                System.out.println(".submit file does not contain a submitURL");
                System.exit(1);
            }
            String courseName = p.getProperty("courseName");
            String courseKey = p.getProperty("courseKey");
            String semester = p.getProperty("semester");
            String projectNumber = p.getProperty("projectNumber");
            String authenticationType = p.getProperty("authentication.type");
            String baseURL = p.getProperty("baseURL");

            System.out.println("Submitting contents of " + home.getCanonicalPath());
            System.out.println(" as project " + projectNumber + " for course " + courseName);
            FilesToIgnore ignorePatterns = new FilesToIgnore();
            addIgnoredPatternsFromFile(cvsIgnoreFile, ignorePatterns);
            addIgnoredPatternsFromFile(submitIgnoreFile, ignorePatterns);

            FindAllFiles find = new FindAllFiles(home, ignorePatterns.getPattern());
            Collection<File> files = find.getAllFiles();

            boolean createdSubmitUser = false;
            Properties userProps = new Properties();
            if (submitUserFile.canRead()) {
                userProps.load(new FileInputStream(submitUserFile));
            }
            if (userProps.getProperty("cvsAccount") == null && userProps.getProperty("classAccount") == null
                    || userProps.getProperty("oneTimePassword") == null) {
                System.out.println();
                System.out
                        .println("We need to authenticate you and create a .submitUser file so you can submit your project");

                createSubmitUser(submitUserFile, courseKey, projectNumber,
                        authenticationType, baseURL);
                createdSubmitUser = true;
                userProps.load(new FileInputStream(submitUserFile));
            }

            MultipartPostMethod filePost = createFilePost(p, find, files,
                    userProps);
            HttpClient client = new HttpClient();
            client.setConnectionTimeout(HTTP_TIMEOUT);
            int status = client.executeMethod(filePost);
            System.out.println(filePost.getResponseBodyAsString());
            if (status == 500 && !createdSubmitUser) {
                System.out.println("Let's try reauthenticating you");
                System.out.println();


                createSubmitUser(submitUserFile, courseKey, projectNumber,
                        authenticationType, baseURL);
                userProps.load(new FileInputStream(submitUserFile));
                filePost = createFilePost(p, find, files,
                        userProps);
                client = new HttpClient();
                client.setConnectionTimeout(HTTP_TIMEOUT);
                status = client.executeMethod(filePost);
                System.out.println(filePost.getResponseBodyAsString());
            }
            if (status != HttpStatus.SC_OK) {
                System.out.println("Status code: " + status);
                System.exit(1);
            }
            System.out.println("Submission accepted");
        } catch (Exception e) {
            System.out.println();
            System.out.println("An Error has occured during submission!");
            System.out.println();
            System.out.println("[DETAILS]");
            System.out.println(e.getMessage());
            e.printStackTrace(System.out);
            System.out.println();
        }
    }

    /**
     * @param p
     * @param find
     * @param files
     * @param userProps
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static MultipartPostMethod createFilePost(Properties p,
            FindAllFiles find, Collection<File> files, Properties userProps)
            throws IOException, FileNotFoundException {
        // ========================== assemble zip file in byte array
        // ==============================
        String loginName = userProps.getProperty("loginName");
        String classAccount = userProps.getProperty("classAccount");
        String from = classAccount;
        if (loginName != null && !loginName.equals(classAccount))
            from += "/" + loginName;
        System.out.println(" submitted by " + from); 
        System.out.println();
        System.out.println("Submitting the following files");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[4096];
        ZipOutputStream zipfile = new ZipOutputStream(bytes);
        zipfile.setComment("zipfile for CommandLineTurnin, version " + VERSION);
        for (File resource : files) {
            if (resource.isDirectory())
                continue;
            String relativePath = resource.getCanonicalPath().substring(find.rootPathLength + 1);
            System.out.println(relativePath);
            ZipEntry entry = new ZipEntry(relativePath);
            entry.setTime(resource.lastModified());

            zipfile.putNextEntry(entry);
            InputStream in = new FileInputStream(resource);
            try {
                while (true) {
                    int n = in.read(buf);
                    if (n < 0)
                        break;
                    zipfile.write(buf, 0, n);
                }
            } finally {
                in.close();
            }
            zipfile.closeEntry();

        } // for each file
        zipfile.close();

        MultipartPostMethod filePost = new MultipartPostMethod(p.getProperty("submitURL"));

        p.putAll(userProps);
        // add properties
        for (Map.Entry<?, ?> e : p.entrySet()) {
            String key = (String) e.getKey();
            String value = (String) e.getValue();
            if (!key.equals("submitURL"))
                filePost.addParameter(key, value);
        }
        filePost.addParameter("submitClientTool", "CommandLineTool");
        filePost.addParameter("submitClientVersion", VERSION);
        byte[] allInput = bytes.toByteArray();
        filePost.addPart(new FilePart("submittedFiles", new ByteArrayPartSource("submit.zip", allInput)));
        return filePost;
    }

    /**
     * @param submitUserFile
     * @param courseKey
     * @param projectNumber
     * @param authenticationType
     * @param baseURL
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws URISyntaxException
     * @throws HttpException
     */
    public static void createSubmitUser(File submitUserFile, String courseKey,
            String projectNumber, String authenticationType, String baseURL)
            throws IOException, UnsupportedEncodingException,
            URISyntaxException, HttpException {
        PrintWriter newUserProjectFile;
        if (authenticationType.equals("openid")) {
            String[] result = getSubmitUserForOpenId(courseKey, projectNumber, baseURL);
            String classAccount = result[0];
            String onetimePassword = result[1];
            newUserProjectFile = new PrintWriter(new FileWriter(submitUserFile));
            newUserProjectFile.println("classAccount=" + classAccount);
            newUserProjectFile.println("oneTimePassword=" + onetimePassword);
        } else {
            String loginName, password;
            
            Console console = System.console();
            
            System.out.println("Please enter your LDAP username and password");
            System.out.print("LDAP username: ");
            loginName = console.readLine();
            System.out.println("Password: ");
            password = new String(console.readPassword());
            System.out.println("Thanks!");
            System.out.println("Preparing for submission. Please wait...");

            String url = baseURL + "/eclipse/NegotiateOneTimePassword";
            PostMethod post = new PostMethod(url);

            post.addParameter("loginName", loginName);
            post.addParameter("password", password);
            
            post.addParameter("courseKey", courseKey);
            post.addParameter("projectNumber", projectNumber);

            HttpClient client = new HttpClient();
            client.setConnectionTimeout(HTTP_TIMEOUT);

            // System.out.println("Preparing to execute method");
            int status = client.executeMethod(post);
            // System.out.println("Post finished with status: " +status);

            if (status != HttpStatus.SC_OK) {
                throw new HttpException("Unable to negotiate one-time password with the server: "
                        + post.getResponseBodyAsString());
            }

            InputStream inputStream = post.getResponseBodyAsStream();
            BufferedReader userStream = new BufferedReader(new InputStreamReader(inputStream));
            newUserProjectFile = new PrintWriter(new FileWriter(submitUserFile));
            while (true) {
                String line = userStream.readLine();
                if (line == null)
                    break;
                // System.out.println(line);
                newUserProjectFile.println(line);
            }
            userStream.close(); 
        }
        newUserProjectFile.close();
        if (!submitUserFile.canRead()) {
            System.out.println("Can't generate or access " + submitUserFile);
            System.exit(1);
        }
    }

    public static String[] getSubmitUserForOpenId(String courseKey, String projectNumber, String baseURL)
            throws UnsupportedEncodingException, URISyntaxException, IOException {
        Console console = System.console();
        
        boolean requested = false;
        String encodedProjectNumber = URLEncoder.encode(projectNumber, "UTF-8");
        URI u = new URI(baseURL + "/view/submitStatus.jsp?courseKey=" + courseKey + "&projectNumber="
                + encodedProjectNumber);

        if (java.awt.Desktop.isDesktopSupported()) {
            Desktop desktop = java.awt.Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                System.out
                        .println("Your browser will connect to the submit server, which may require you to authenticate yourself");
                System.out.println("Please do so, and then you will be shown a page with a textfield on it");
                System.out.println("Then copy that text and paste it into the prompt here");
                desktop.browse(u);
                requested = true;
            }
        }
        if (!requested) {
            System.out.println("Please enter the following URL into your browser");
            System.out.println("  " + u);
            System.out.println();
            System.out
                    .println("Your browser will connect to the submit server, which may require you to authenticate yourself");
            System.out.println("Please do so, and then you will be shown a page with a textfield on it");
            System.out.println("Then copy that text and paste it into the prompt here");

        }
        while (true) {
            System.out.println();
            System.out.println("Submission verification information from browser? ");
            String info = new String(console.readLine());
            if (info.length() > 2) {
                int checksum = Integer.parseInt(info.substring(info.length() - 1), 16);
                info = info.substring(0, info.length() - 1);
                int hash = info.hashCode() & 0x0f;
                if (checksum == hash) {
                    String fields[] = info.split(";");
                    if (fields.length == 2) {
                        return fields;

                    }
                }
            }
            System.out.println("That doesn't seem right");
            System.out
                    .println("The information should be your account name and a string of hexidecimal digits, separated by a semicolon");
            System.out.println("Please try again");
            System.out.println();
        }
    }

    /**
     * @param submitIgnoreFile
     * @param ignorePatterns
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void addIgnoredPatternsFromFile(File submitIgnoreFile, FilesToIgnore ignorePatterns)
            throws FileNotFoundException, IOException {
        if (submitIgnoreFile.canRead()) {
            BufferedReader ignoreContents = new BufferedReader(new FileReader(submitIgnoreFile));
            while (true) {
                String ignoreMe = ignoreContents.readLine();
                if (ignoreMe == null)
                    break;
                ignorePatterns.addPattern(ignoreMe);
            }
            ignoreContents.close();
        }
    }
}
