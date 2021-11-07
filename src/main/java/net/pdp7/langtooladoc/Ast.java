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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
			try (FileOutputStream outputStream = new FileOutputStream(asciiDocAstJarFile)) {
				outputStream.getChannel().transferFrom(Channels.newChannel(new URL(ASCIIDOC_AST_JAR_URL).openStream()),
						0, Long.MAX_VALUE);
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
		System.out.println(ast.parse(args[0]));
	}

	public Node parse(String path) throws IOException, InterruptedException {
		return mapToNodes(parseAsciiDocToMap(path));
	}

	public Node mapToNodes(Map<?, ?> map) {
		return new Node(map);
	}

	public static class Node {
		public final NodeType type;
		public final String text;
		public final int startOffset;
		public final int endOffset;
		public final List<Node> children;

		public Node(Map<?, ?> map) {
			type = NodeType.valueOf(map.get("type").toString().replace("AsciiDoc:", ""));
			text = map.containsKey("text") ? map.get("text").toString() : null;
			startOffset = Integer.parseInt(map.get("startOffset").toString().split("\\.")[0]);
			endOffset = Integer.parseInt(map.get("endOffset").toString().split("\\.")[0]);
			children = getChildren(map).stream().map(m -> new Node(m)).collect(Collectors.toList());
		}

		@SuppressWarnings("unchecked")
		private List<Map<?, ?>> getChildren(Map<?, ?> map) {
			return (List<Map<?, ?>>) map.get("children");
		}

		@Override
		public String toString() {
			return super.toString() + "(type=" + type + ",text=" + text + ",offset=" + startOffset + ".." + endOffset
					+ ",children=" + children + ")";
		}

	}

	public static enum NodeType {
		BLOCK_MACRO_ID, ASSIGNMENT, LISTING, BLOCK_MACRO, ASCIIDOC_ATTRIBUTE_DECLARATION, ATTRIBUTE_NAME_END,
		BLOCK_ATTRIBUTES, MONO_START, SEPARATOR, ATTRS_END, HEADING, ATTRIBUTE_REF_END, HEADING_TOKEN, ASCIIDOC_SECTION,
		WHITE_SPACE, LISTING_TEXT, ATTR_NAME, FILE, ATTRIBUTE_IN_BRACKETS, ATTRIBUTE_REF, TEXT, BULLET, LIST_ITEM,
		ATTRIBUTE_REF_START, GT, RPAREN, ATTR_VALUE, ATTRIBUTE_VAL, SINGLE_QUOTE, MONO, LIST, MONO_END, ATTRS_START,
		ATTRIBUTE_NAME_START, ASCIIDOC_BLOCKID, ATTRIBUTE_NAME, LT, LISTING_BLOCK_DELIMITER, END_OF_SENTENCE, LPAREN,
		ATTRIBUTE_DECLARATION_NAME;
	}

	public Map<?, ?> parseAsciiDocToMap(String path) throws IOException, InterruptedException {
		File stdout = Files.createTempFile(null, null).toFile();
		try {
			File stderr = Files.createTempFile(null, null).toFile();

			try {
				int returnCode = new ProcessBuilder("java", "--add-opens=java.base/java.io=ALL-UNNAMED",
						"--add-opens=java.base/java.lang=ALL-UNNAMED", "-jar", getAsciiDocAstJarPath().toString(), path)
								.redirectError(stderr).redirectOutput(stdout).start().waitFor();

				if (returnCode != 0) {
					throw new RuntimeException(Files.readString(stderr.toPath()));
				}

				return new Gson().fromJson(new FileReader(stdout), Map.class);
			} finally {
				stderr.delete();
			}
		} finally {
			stdout.delete();
		}
	}

}
