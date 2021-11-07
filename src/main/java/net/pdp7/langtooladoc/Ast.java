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
		FILE, TEXT, END_OF_SENTENCE, BULLET, ENUMERATION, DESCRIPTION, DESCRIPTION_END, ADMONITION, CALLOUT, LINE_BREAK,
		EMPTY_LINE, HARD_BREAK, CONTINUATION, ATTRIBUTE_CONTINUATION, ATTRIBUTE_CONTINUATION_LEGACY, WHITE_SPACE,
		WHITE_SPACE_MONO, LINE_COMMENT, BLOCK_COMMENT, PASSTRHOUGH_BLOCK_DELIMITER, PASSTRHOUGH_INLINE_START,
		PASSTRHOUGH_INLINE_END, PASSTRHOUGH_CONTENT, BLOCK_DELIMITER, LITERAL_BLOCK_DELIMITER, LITERAL_BLOCK,
		LISTING_BLOCK_DELIMITER, COMMENT_BLOCK_DELIMITER, LISTING_TEXT, HEADING_TOKEN, HEADING_OLDSTYLE, TITLE_TOKEN,
		BLOCK_MACRO_ID, BLOCK_MACRO_BODY, BLOCK_MACRO_ATTRIBUTES, ATTRS_START, ATTR_LIST_SEP, ATTR_LIST_OP, ATTR_NAME,
		ATTR_VALUE, ATTRS_END, INLINE_MACRO_ID, INLINE_MACRO_BODY, INLINE_MACRO_ATTRIBUTES, INLINE_ATTRS_START,
		INLINE_ATTRS_END, BOLD_START, BOLD_END, BOLD, ITALIC_START, ITALIC_END, MONO_START, MONO_END, ITALIC,
		BOLDITALIC, MONO, LPAREN, RPAREN, LBRACKET, RBRACKET, MONOBOLD, MONOITALIC, MONOBOLDITALIC, LT, GT,
		DOUBLE_QUOTE, SINGLE_QUOTE, ASSIGNMENT, REFSTART, REF, REFTEXT, REFEND, BLOCKIDSTART, ASCIIDOC_BLOCKID, BLOCKREFTEXT,
		BLOCKIDEND, INLINEIDSTART, INLINEIDEND, SEPARATOR, TYPOGRAPHIC_DOUBLE_QUOTE_START, TYPOGRAPHIC_DOUBLE_QUOTE_END,
		TYPOGRAPHIC_SINGLE_QUOTE_START, TYPOGRAPHIC_SINGLE_QUOTE_END, LINKSTART, LINKFILE, LINKANCHOR, MACROTEXT,
		ATTRIBUTE_NAME_START, ATTRIBUTE_NAME, ATTRIBUTE_UNSET, ATTRIBUTE_NAME_END, ATTRIBUTE_VAL, ATTRIBUTE_REF_START,
		ATTRIBUTE_REF, ATTRIBUTE_REF_END, PAGEBREAK, HORIZONTALRULE, URL_START, URL_LINK, URL_EMAIL, URL_END,
		URL_PREFIX, HEADER, HTML_ENTITY, FRONTMATTER, FRONTMATTER_DELIMITER, BIBSTART, BIBEND, CELLSEPARATOR, ARROW,
		BLOCK_MACRO, INLINE_MACRO, BLOCK, DESCRIPTION_ITEM, LIST, LIST_ITEM, CELL, BLOCK_ATTRIBUTES,
		ATTRIBUTE_IN_BRACKETS, LISTING, PASSTHROUGH, LINK, INCLUDE_TAG, ATTRIBUTE_DECLARATION_NAME, URL, TITLE, HEADING,
		ASCIIDOC_ATTRIBUTE_DECLARATION, ASCIIDOC_SECTION;
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
