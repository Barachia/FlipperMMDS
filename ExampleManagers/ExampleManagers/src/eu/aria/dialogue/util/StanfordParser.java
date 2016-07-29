package eu.aria.dialogue.util;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;

import java.util.ArrayList;
import java.util.List;

// By Kevin Bowden
//parse tag meanings: https://gist.github.com/nlothian/9240750

public class StanfordParser {

    static LexicalizedParser parser;

    public StanfordParser(String parseModel) {
        parser = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

        parser.setOptionFlags(new String[] { "-maxLength", "80",
                "-retainTmpSubcategories" });
    }

    public List<TypedDependency> dependencyParse(Tree parse){
        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
        //dep structure parse, type: arrayList
        return gs.typedDependenciesCCprocessed();
    }

    public Tree parseWords(ArrayList<String> wordsList){
        String[] wordsArray = wordsList.toArray(new String[wordsList.size()]);
        List<CoreLabel> rawWords = Sentence.toCoreLabelList(wordsArray);

        //pos tag tree parse, type:Tree
//        try {
        Tree parse = parser.apply(rawWords);
        return parse;
//
//        }
//        catch(NoSuchFieldError e){
//
//        }

//        System.out.println(parse);


//        loop through parse tree
//        for (Tree subtree : parse) {
//            if (subtree.label().value().equals("PRP")) {
//
//            }
//        }

        //tregex example
//        TregexPattern tgrepPattern = TregexPattern.compile("PRP");
//        TregexMatcher m = tgrepPattern.matcher(t);
//        while (m.find()) {
//            Tree subtree = m.getMatch();
//            pronouns.add(subtree);
//        }

    }

}