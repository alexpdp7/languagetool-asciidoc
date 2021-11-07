package net.pdp7.langtooladoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.google.gson.Gson;

import dev.dirs.ProjectDirectories;

public class Ast {

	public static final String ASCIIDOC_AST_JAR_URL = "https://github.com/alexpdp7/asciidoc-ast/releases/download/v20211106.1/asciidoc-ast-20211106.1.jar";

	public void setupAsciiDocAst() {
		Path asciiDocAstJarPath = getAsciiDocAstJarPath();

		if (!asciiDocAstJarPath.toFile().exists()) {
			downloadAsciiDocAst();
		}
	}

	public void downloadAsciiDocAst() {
		try {
			System.err.println("downloading " + ASCIIDOC_AST_JAR_URL + " to " + getAsciiDocAstJarPath());
			File asciiDocAstJarFile = getAsciiDocAstJarPath().toFile();
			asciiDocAstJarFile.getParentFile().mkdirs();
			try(FileOutputStream outputStream = new FileOutputStream(asciiDocAstJarFile)) {
				outputStream.getChannel().transferFrom(Channels.newChannel(new URL(ASCIIDOC_AST_JAR_URL).openStream()), 0,
						Long.MAX_VALUE);
			}
			System.err.println("downloaded " + ASCIIDOC_AST_JAR_URL + " to " + getAsciiDocAstJarPath());
		} catch (IOException e) {
			throw new RuntimeException("Can't download " + ASCIIDOC_AST_JAR_URL + " to " + getAsciiDocAstJarPath(), e);
		}
	}

	public Path getAsciiDocAstJarPath() {
		String cacheDir = ProjectDirectories.from("net.pdp7", "pdp7.net", "languagetool-asciidoc").cacheDir;
		String[] urlParts = ASCIIDOC_AST_JAR_URL.split("/");
		String fileName = urlParts[urlParts.length - 1];
		Path asciiDocAstJarPath = Paths.get(cacheDir, fileName);
		return asciiDocAstJarPath;
	}

	public static void main(String[] args) throws Exception {
		Ast ast = new Ast();
		ast.setupAsciiDocAst();
		System.out.println(ast.parseAsciiDoc(args[0]));
	}

	public Map<?, ?> parseAsciiDoc(String path) throws IOException, InterruptedException {
		File stdout = Files.createTempFile(null, null).toFile();
		try {
			File stderr = Files.createTempFile(null, null).toFile();
	
			try {
				int returnCode = new ProcessBuilder("java", "--add-opens=java.base/java.io=ALL-UNNAMED",
						"--add-opens=java.base/java.lang=ALL-UNNAMED", "-jar", getAsciiDocAstJarPath().toString(), path)
								.redirectError(stderr).redirectOutput(stdout).start().waitFor();
		
				if(returnCode != 0) {
					throw new RuntimeException(Files.readString(stderr.toPath()));
				}
				
				return new Gson().fromJson(new FileReader(stdout), Map.class);
			}
			finally {
				stderr.delete();
			}
		}
		finally {
			stdout.delete();
		}
	}

}
