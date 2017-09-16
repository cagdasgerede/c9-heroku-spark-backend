import spark.*;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import static spark.Spark.*;
import static spark.debug.DebugScreen.enableDebugScreen;

public class UploadExample {

    public static void main(String[] args) {
        port(getHerokuAssignedPort());
        enableDebugScreen();

        File uploadDir = new File("upload");
        uploadDir.mkdir(); // create the upload directory if it doesn't exist

        staticFiles.externalLocation("upload");

        get("/error", (req, res) -> {
            throw new Exception("Exceptions everywhere!");
        });

        get("/", (req, res) ->
                  "<form method='post' enctype='multipart/form-data'>" // note the enctype
                + "    <input type='file' name='uploaded_file' accept='.png'>" // make sure to call getPart using the same "name" in the post
                + "    <button>Upload picture</button>"
                + "</form>"
        );

        post("/", (req, res) -> {

            Path tempFile = Files.createTempFile(uploadDir.toPath(), "", "");

            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

            try (InputStream input = req.raw().getPart("uploaded_file").getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            logInfo(req, tempFile, "uploaded_file");
            return "<h1>You uploaded this image:</h1><img src='" + tempFile.getFileName() + "'>";

        });
        
        get("/x", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            // See https://www.learnhowtoprogram.com/java/web-applications-in-java/using-layouts-in-spark
            // and http://sparkjava.com/documentation#views-and-templates
            return render(model, "templates/layout.vtl");
        });
        //           "<form action='/x' method='post' enctype='multipart/form-data'>" // note the enctype
        //         + "    <input type='file' name='uploaded_file1' accept='.csv'>" // make sure to call getPart using the same "name" in the post
        //         + "    <input type='file' name='uploaded_file2' accept='.csv'>" // make sure to call getPart using the same "name" in the post
        //         + "    <button>Compare</button>"
        //         + "</form>"
        // );

        post("/x", (req, res) -> {
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

            Path tempFile1 = Files.createTempFile(uploadDir.toPath(), "", "");
            try (InputStream input = req.raw().getPart("uploaded_file1").getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile1, StandardCopyOption.REPLACE_EXISTING);
            }
            logInfo(req, tempFile1, "uploaded_file1");
            
            
            Path tempFile2 = Files.createTempFile(uploadDir.toPath(), "", "");
            try (InputStream input = req.raw().getPart("uploaded_file2").getInputStream()) { // getPart needs to use same "name" as input field in form
                Files.copy(input, tempFile2, StandardCopyOption.REPLACE_EXISTING);
            }
            logInfo(req, tempFile2, "uploaded_file2");
            

            String line1 = "";
            try (BufferedReader source = new BufferedReader(
                    new FileReader(tempFile1.toAbsolutePath().toString()))) {

                line1 = source.readLine();  // Skip header.

                // while ((line = source.readLine()) != null) {}
            } catch (IOException e) {
                e.printStackTrace();
            }

            String line2 = "";
            try (BufferedReader source = new BufferedReader(
                    new FileReader(tempFile2.toAbsolutePath().toString()))) {

                line2 = source.readLine();  // Skip header.

                // while ((line = source.readLine()) != null) {}
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            
            
            return "<h1>You uploaded a csv with this first row:</h1>" + line1 + "<br>" + line2;
        });

    }

    // methods used for logging
    private static void logInfo(Request req, Path tempFile, String partName) throws IOException, ServletException {
        System.out.println("Uploaded file '" + getFileName(req.raw().getPart(partName)) + "' saved as '" + tempFile.toAbsolutePath() + "'");
    }

    private static String getFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 8080; //return default port if heroku-port isn't set (i.e. on localhost)
    }

    static String render(Map<String, Object> model, String templatePath) {
        return new VelocityTemplateEngine().render(new ModelAndView(model, templatePath));
    }
}