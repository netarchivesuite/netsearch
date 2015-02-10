package dk.statsbiblioteket.netarchivesuite.arctika.builder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A stated filename is matched against a list of rules (pattern and List<template> pairs).
 * The first regexp match is resolved with the templates. Order is significant.
 * The templates are expected to produce pairs of {@code field:content}.
 */
public class ArcFileNameParser {
    private static final Logger log = LoggerFactory.getLogger(ArcFileNameParser.class);

    private List<Rule> rules;

    public ArcFileNameParser(List<Rule> rules) {
        this.rules = rules;
    }

    /**
     * Format: Newline separated rules.<br/>
     * Each line has the format regexp(\ttemplate)*<br/>
     * Example: {code ([0-9]+)-([0-9]+)-([0-9]+)-([0-9]+)-(sb-prod-har)\tyear:$1\tfull:$1$2$5\t}<br/>
     * @param rules a newline separated list of rules.
     */
    public ArcFileNameParser(String rules) {
        this.rules = new ArrayList<Rule>();
        for (String line: rules.split("\n")) {
            String[] tokens = line.split("\t");
            if (line.isEmpty() || line.startsWith("#") || tokens.length == 1) {
                continue;
            }
            this.rules.add(new Rule(Pattern.compile(tokens[0]), Arrays.copyOfRange(tokens, 1, tokens.length)));
        }
        log.info("Created parser with " + rules + " rules");
    }

    /**
     * Applies the previously given rules to the file name, returning the list of expanded templates
     * for the matching pattern.
     * @return a list of field:value pairs as per the template contract.
     */
    public List<String> expandFilename(String filename) {
        List<String> pairs = new ArrayList<String>();
        for (Rule rule: rules) {
            Matcher matcher = rule.pattern.matcher(filename);
            if (!matcher.matches()) {
                continue;
            }
            for (String template: rule.templates) {
                pairs.add(matcher.replaceAll(template));
            }
            break; // Only one rule!
        }
        return pairs;
    }

    private static class Rule {
        private final Pattern pattern;
        private final List<String> templates;

        public Rule(Pattern pattern, String... templates) {
            this.pattern = pattern;
            this.templates = Arrays.asList(templates);
        }
    }
}

