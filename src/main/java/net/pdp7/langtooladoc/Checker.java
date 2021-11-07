package net.pdp7.langtooladoc;

import java.io.IOException;
import java.util.List;

import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.markup.AnnotatedTextBuilder;
import org.languagetool.rules.MultipleWhitespaceRule;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;

import net.pdp7.langtooladoc.Ast.Node;
import net.pdp7.langtooladoc.Ast.NodeType;

public class Checker {
	public static void main(String[] args) throws Exception {
		new Checker().check(args[0]);
	}

	public void check(String path) throws IOException {
		Ast ast = new Ast();
		ast.setupAsciiDocAst();
		Node parsed;
		try {
			parsed = ast.parse(path);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException("Error parsing AsciiDoc", e);
		}
		AnnotatedTextBuilder annotatedTextBuilder = new AnnotatedTextBuilder();
		annotate(annotatedTextBuilder, parsed);
		JLanguageTool languageTool = new JLanguageTool(new AmericanEnglish());
		for (Rule rule : languageTool.getAllActiveRules()) {
			if (rule instanceof MultipleWhitespaceRule) {
				rule.setDefaultOff();
			}
		}
		List<RuleMatch> matches = languageTool.check(annotatedTextBuilder.build());
		for (RuleMatch match : matches) {
			System.out.println("Potential error at characters " + match.getFromPos() + "-" + match.getToPos() + ": "
					+ match.getMessage());
			System.out.println("Suggested correction(s): " + match.getSuggestedReplacements());
		}
	}

	public void annotate(AnnotatedTextBuilder annotatedTextBuilder, Node ast) {
		if (ast.text == null) {
			for (Node child : ast.children) {
				annotate(annotatedTextBuilder, child);
			}
		} else {
			if (ast.type == NodeType.TEXT || ast.type == NodeType.WHITE_SPACE) {
				annotatedTextBuilder.addText(ast.text);
			} else {
				annotatedTextBuilder.addMarkup(ast.text);
			}
		}
	}
}
