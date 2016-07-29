/*
by Kevin Bowden
 */
package eu.aria.dialogue.managers;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TypedDependency;
import eu.aria.dialogue.util.*;
import eu.ariaagent.managers.DefaultManager;
import hmi.flipper.defaultInformationstate.DefaultList;
import hmi.flipper.defaultInformationstate.DefaultRecord;
import hmi.flipper.informationstate.Item;
import hmi.flipper.informationstate.List;
import hmi.flipper.informationstate.Record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Siewart
 */
 public class POSManager extends DefaultManager {
    private String userUtterancePath = "$userstates.utterance";
    private String userposPath = "$userstates.utterance.pos";

    private StanfordTagger stanfordTagger;
    private StanfordParser stanfordParser;

    public POSManager(DefaultRecord is) {
        super(is);
        System.out.println("IntentGenerator is WIP. It should be using a Flipper template later. Right now it uses: ");
        System.out.println("$userstates.utterance(.consumed <boolean> |.timestamp = 't:<long ms since 1970>' |.text = <string> ) -> $userstates.intention = <string>");
        interval = 50; //fast default interval
    }

    @Override
    public void setParams(Map<String, String> params, Map<String, String[]> paramList){
        String path = params.get("user_utterance_is_path");
        if(path != null){
            userUtterancePath = path;
        }

        String posModel = params.get("pos_model_path");
        String parseModel = params.get("parse_model");

        stanfordTagger = new StanfordTagger(posModel);
        stanfordParser = new StanfordParser(parseModel);

    }

    @Override
    public void process() {
        super.process();

        //if(is.getString(intentionPath) != null && !is.getString(intentionPath).equals("")){
        //    System.out.println(is.getString(intentionPath));
        //}

        Record utterance = getIS().getRecord(userUtterancePath);
        if (utterance == null) {
            utterance = new DefaultRecord();
            getIS().set(userUtterancePath, utterance);
        }

        Record posUtterance = getIS().getRecord(userposPath);
        if (posUtterance == null) {
            posUtterance = new DefaultRecord();
            getIS().set(userposPath, posUtterance);
        }

        if (!utterance.getString("consumed").equals("true")) {
            String basicAdj = null;
            ArrayList<String> nounBuilder = new ArrayList<>();

                if (posUtterance.getList("nouns") == null) {
                    posUtterance.set("nouns", new DefaultList());
                }
                if (posUtterance.getList("adjectives") == null) {
                    posUtterance.set("adjectives", new DefaultList());
                }
                if (posUtterance.getList("lastStated") == null) {
                    posUtterance.set("lastStated", new DefaultList());
                }
                if (posUtterance.getList("frequency") == null) {
                    posUtterance.set("frequency", new DefaultList());
                }
                if (posUtterance.getList("preference") == null) {
                    posUtterance.set("preference", new DefaultList());
                }
                if (posUtterance.getList("preference") == null) {
                    posUtterance.set("preference", new DefaultList());
                }

                String userSay = utterance.getString("text");
                if (userSay != null && !utterance.getString("consumed").equals("true")) {
                    ArrayList<String> wordset = new ArrayList<>(Arrays.asList(userSay.split(" ")));
                    ArrayList<String> taggedText = stanfordTagger.tagFile(wordset);
                    //TODO implement parsing relationships, mostly modifiers
                    Tree parse = stanfordParser.parseWords(wordset);
                    java.util.List<TypedDependency> depParse = stanfordParser.dependencyParse(parse);

//                    for(dep : depParse){
//                        System.out.println(dep);
//                    }

                    for (int i = 0; i < taggedText.size(); i++) {
                        String word = taggedText.get(i);
                        int index = word.lastIndexOf("_");

                        String pos = word.substring(index, word.length());
                        pos = pos.substring(1, pos.length());
                        word = word.substring(0, index);


                        List nounsList = posUtterance.getList("nouns");
                        List frequency = posUtterance.getList("frequency");
                        List preference = posUtterance.getList("preference");
                        List lastStated = posUtterance.getList("lastStated");
                        List adjectives = posUtterance.getList("adjectives");

//                    Random randomGenerator = new Random();
//                    int k = randomGenerator.nextInt(nouns.size());
//                    String randomItem = nouns.getString(k);
//                    nounsList.addItemEnd(randomItem);

//                    if(nounsList.size() > 2) {
//                        nounsList.getItem(0).setStringValue(nounsList.getItem(0).getString() + "::string");
//                    }

                        if (pos.startsWith("JJ") && i + 1 < taggedText.size()) {
                            String nextWord = taggedText.get(i + 1);
                            int adjDex = nextWord.lastIndexOf("_");
                            String nextPos = nextWord.substring(adjDex, nextWord.length());
                            nextPos = nextPos.substring(1, nextPos.length());
                            if (nextPos.startsWith("NN")) {
                                basicAdj = word;
                            }
                        }
                        if (pos.startsWith("NN")) {
                            nounBuilder.add(word);
                        }
                        double currTime = (double) System.currentTimeMillis();
                        if ((!pos.startsWith("NN") || i == taggedText.size() - 1) && nounBuilder.size() > 0) {
                            String noun = "";
                            for (String n : nounBuilder) {
                                noun += n + " ";
                                noun = noun.trim();
                            }
                            nounBuilder.clear();
                            int nid = -1;
                            if (nounsList.size() > 0 && nounsList.contains(noun)) {
                                //update freq by 1 and timestamp
                                for (int j = 0; j < nounsList.size(); j++) {
                                    if (noun.equals(nounsList.getString(j))) {
                                        nid = j;
                                        break;
                                    }
                                }

                                frequency.getItem(nid).setIntegerValue(frequency.getInteger(nid) + 1);
                                lastStated.getItem(nid).setDoubleValue(currTime);

                                posUtterance.set("frequency", frequency);
                                posUtterance.set("lastStated", lastStated);

                            } else {
                                //add word, freq of 1, timestamp, and preference
                                nounsList.addItemEnd(noun);
                                frequency.addItemEnd(1);
                                preference.addItemEnd(.5);
                                lastStated.addItemEnd(currTime);
                                adjectives.addItemEnd(new DefaultList());
                                nid = nounsList.size() - 1;

                                posUtterance.set("nouns", nounsList);
                                posUtterance.set("frequency", frequency);
                                posUtterance.set("preference", preference);
                                posUtterance.set("lastStated", lastStated);
                            }
                            if (basicAdj != null) {
                                DefaultList currAdjList = (DefaultList) adjectives.getItem(nid).getList();
                                currAdjList.addItemEnd(basicAdj);
                                basicAdj = null;
                                posUtterance.set("adjectives", adjectives);
                            }
                        }
                    }
                }
        }
    }
 }
