package flammable.bunny.core;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class MCSRAppsInstaller {

    private static final String MCSR_APPS_DIR = System.getProperty("user.home") + "/mcsr-apps";

    private static final String MODCHECK_REPO = "tildejustin/modcheck";
    private static final String NINJABRAIN_REPO = "Ninjabrain1/Ninjabrain-Bot";
    private static final String PACEMAN_REPO = "PaceMan-MCSR/PaceMan-Tracker";
    private static final String MAPCHECK_REPO = "cylorun/Map-Check";


    public static void installModCheck() throws IOException, InterruptedException {
        System.out.println("Installing ModCheck...");
        ensureMCSRAppsDir();
        String fileName = downloadLatestJarFromRepo(MODCHECK_REPO);
        System.out.println("ModCheck installed successfully to ~/mcsr-apps/" + fileName);
    }


    public static void installNinjabrainBot() throws IOException, InterruptedException {
        System.out.println("Installing Ninjabrain Bot...");
        ensureMCSRAppsDir();
        String fileName = downloadLatestJarFromRepo(NINJABRAIN_REPO);
        System.out.println("Ninjabrain Bot installed successfully to ~/mcsr-apps/" + fileName);
    }


    public static void installPacemanTracker() throws IOException, InterruptedException {
        System.out.println("Installing Paceman Tracker...");
        ensureMCSRAppsDir();
        String fileName = downloadLatestJarFromRepo(PACEMAN_REPO);
        System.out.println("Paceman Tracker installed successfully to ~/mcsr-apps/" + fileName);
    }


    public static void installMapCheck() throws IOException, InterruptedException {
        System.out.println("Installing MapCheck...");
        ensureMCSRAppsDir();
        String fileName = downloadLatestJarFromRepo(MAPCHECK_REPO);
        System.out.println("MapCheck installed successfully to ~/mcsr-apps/" + fileName);
    }


    private static void ensureMCSRAppsDir() throws IOException {
        Path dir = Path.of(MCSR_APPS_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
            System.out.println("Created directory: " + MCSR_APPS_DIR);
        }
    }


    private static String downloadLatestJarFromRepo(String repo) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        String apiUrl = "https://api.github.com/repos/" + repo + "/releases/latest";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get release info for " + repo + ": HTTP " + response.statusCode());
        }

        // Parse JSON to find the JAR asset
        JSONObject releaseJson = new JSONObject(response.body());
        JSONArray assets = releaseJson.getJSONArray("assets");

        String downloadUrl = null;
        String jarFileName = null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getString("name");
            if (name.endsWith(".jar")) {
                downloadUrl = asset.getString("browser_download_url");
                jarFileName = name;
                break;
            }
        }

        if (downloadUrl == null || jarFileName == null) {
            throw new IOException("No JAR file found in latest release for " + repo);
        }

        // Download the JAR file
        System.out.println("Downloading from: " + downloadUrl);
        HttpRequest downloadRequest = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .GET()
                .build();

        Path outputPath = Path.of(MCSR_APPS_DIR, jarFileName);
        HttpResponse<Path> downloadResponse = client.send(downloadRequest, HttpResponse.BodyHandlers.ofFile(outputPath));

        if (downloadResponse.statusCode() != 200) {
            throw new IOException("Failed to download JAR from " + downloadUrl + ": HTTP " + downloadResponse.statusCode() + "Please Report this to the Lingle Discord Server");
        }

        return jarFileName;
    }
}
