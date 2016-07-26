/*
by Kevin Bowden
 */
package eu.aria.dialogue.managers;

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
    private SentencesToKeywords sk;
    private ArrayList<Rules> rules;
    private RulesReader rulesReader;

    private String intentionPath = "$userstates.intention";
    private String userUtterancePath = "$userstates.utterance";
    private String userposPath = "$userstates.utterance.pos";

    private String unknownState = "unknown";
    private String longPauseState = "longPause";

    private long timeout = 15000;


    private StanfordTagger stanfordTagger;

    public POSManager(DefaultRecord is) {
        super(is);
        System.out.println("IntentGenerator is WIP. It should be using a Flipper template later. Right now it uses: ");
        System.out.println("$userstates.utterance(.consumed <boolean> |.timestamp = 't:<long ms since 1970>' |.text = <string> ) -> $userstates.intention = <string>");
        interval = 50; //fast default interval
    }

    @Override
    public void setParams(Map<String, String> params, Map<String, String[]> paramList){
        String rulesLocation = params.get("rules_path");
        if(rulesLocation == null){
            System.err.println("Parameter 'rules_path' not set for manager "+ getName() + "(" + getID() + "). Without it this manager will do nothing.");
            rules = new ArrayList<>();
        }else{
            rulesReader = new RulesReader(rulesLocation);
            rulesReader.readData();
            rules = rulesReader.getRules();
        }
        String ohTimeout = params.get("oh_timeout");
        try{
            this.timeout = Long.parseLong(ohTimeout);
        }catch(NumberFormatException ex){
            System.out.println("Parameter 'oh_timeout' is not an integer in manager " + getName() + "(" + getID() + "). Using default of 15000 ms" );
        }
        try{
            String stopwords = params.get("stopwords_path");
            String synonyms = params.get("synonyms_path");
            String posModel = params.get("pos_model_path");
            if(stopwords == null || synonyms == null || posModel == null){
                System.err.println("Parameter 'stopwords_path', 'synonyms_path' and 'pos_model_path' must all be set as filepaths in manager " + getName() + "(" + getID() + ")." );
            }
            sk = new SentencesToKeywords(stopwords, synonyms, posModel);
       }catch(IOException ex){
            System.out.println("Error in manager "+ getName() + "(" + getID() + "). Could not create SentencesToKeywords: "+ex.getMessage());
        }
        String path = params.get("user_utterance_is_path");
        if(path != null){
            userUtterancePath = path;
        }

        path = params.get("intention_is_path");
        if(path != null){
            intentionPath = path;
        }

        String stateName = params.get("long_pause_state_name");
        if(path != null){
            longPauseState = stateName;
        }

        stateName = params.get("long_pause_state_name");
        if(path != null){
            unknownState = stateName;
        }

        String posModel = params.get("pos_model_path");

        stanfordTagger = new StanfordTagger(posModel);
    }

    long lastLongPause = 0;
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
            System.out.println("pizza, idk");
            posUtterance = new DefaultRecord();
            getIS().set(userposPath, posUtterance);
        }

        if (!utterance.getString("consumed").equals("true")) {
            System.out.println("Inside the not consumed if" );
            String basicAdj = null;
            ArrayList<String> nounBuilder = new ArrayList<>();

            if (posUtterance.getString("init") == null) {
                System.out.println("Inside the init if");
                if (posUtterance.getString("nouns") == null) {
                    posUtterance.set("nouns", new DefaultList());
                }
                if (posUtterance.getString("adjectives") == null) {
                    posUtterance.set("adjectives", new DefaultList());
                }
                if (posUtterance.getString("lastStated") == null) {
                    posUtterance.set("lastStated", new DefaultList());
                }
                if (posUtterance.getInteger("frequency") == null) {
                    posUtterance.set("frequency", new DefaultList());
                }
                if (posUtterance.getInteger("preference") == null) {
                    posUtterance.set("preference", new DefaultList());
                }
                posUtterance.set("init", "true");
            }

                System.out.println("Inside the not consumed else");
                String userSay = utterance.getString("text");
                if (userSay != null && !utterance.getString("consumed").equals("true")) {
                    System.out.println("inside the userSay != null if");
                    ArrayList<String> taggedText = stanfordTagger.tagFile(new ArrayList<>(Arrays.asList(userSay.split(" "))));
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
                            System.out.println("Inside the if that eventually writes");
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
                                System.out.println("Inside the if that handles adjectives");
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

    private void processInternal(String userSay) {

        boolean keywordFound = false;

        // userSay = userSay.replaceAll("\\?", " ?");
        ArrayList<String> userSayAL = sk.removeStopWords(userSay.replaceAll("\\?", " ?"));
        try {
            userSayAL = sk.pickUp(userSayAL/*, bwDet*/);
        } catch (IOException ex) {
            Logger.getLogger(POSManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (Rules rule : rules) {
            int count = 0;
            for (String word : rule.getWords()) {
                for (String userUtt : userSayAL) {
                    if (userUtt.toLowerCase().contains(word)) count++;
                }
            }
            if (count == rule.getWords().size()) {
                for (State state : rule.getStates()) {
                    is.set(state.getName(), state.getValue());
                }
                keywordFound = true;
                break;
            }
        }
        if (!keywordFound) {
            is.set(intentionPath, unknownState);
        }
    }
 }
