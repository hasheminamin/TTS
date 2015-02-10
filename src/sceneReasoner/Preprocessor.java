package sceneReasoner;

import ir.ac.itrc.qqa.semantic.enums.DependencyRelationType;
import ir.ac.itrc.qqa.semantic.enums.POS;
import ir.ac.itrc.qqa.semantic.enums.SourceType;
import ir.ac.itrc.qqa.semantic.kb.KnowledgeBase;
import ir.ac.itrc.qqa.semantic.kb.Node;
import ir.ac.itrc.qqa.semantic.util.Common;
import ir.ac.itrc.qqa.semantic.util.MyError;
import ir.ac.itrc.qqa.semantic.reasoning.PlausibleAnswer;
import ir.ac.itrc.qqa.semantic.reasoning.PlausibleStatement;
import ir.ac.itrc.qqa.semantic.reasoning.SemanticReasoner;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import sceneElement.*;
import model.MainSemanticTag;
import model.ScenePart;
import model.SemanticTag;
import model.SentencePart;
import model.SceneModel;
import model.SentenceModel;
import model.StoryModel;
import model.SubSemanticTag;
import model.VerbType;

/**
 * Preprocessor preprocesses the input natural language sentences.  
 * It can convert sentence in natural language to their SentenceModel.
 * It can also convert the SentenceModel to primary sceneModel.
 *  
 * @author hashemi
 *
 */
public class Preprocessor {
	
	private KnowledgeBase _kb;
//	private SemanticReasoner _re;
	private TTSEngine _ttsEngine = null;
		
//	private ArrayList<PlausibleStatement> default_contexts = null;
	private String mainSemanticArgumet_name = "MainSemArg";
	private String zamir_enekasi = "خود#n3";
	
	
	/**
	 * We have no NLP module to process input text and convert it to related part,
	 * so temporarily we aught to read these processed information from a file named  SentenceInfosFileName. 
	 */
//	private String sentenceInfosFileName = "inputStory/sentenceInfos2_simple.txt";
//	private String sentenceInfosFileName = "inputStory/sentenceInfos_SS.txt";
	private String sentenceInfosFileName = "inputStory/SentenceInfos11.txt";

	public Preprocessor(KnowledgeBase kb, SemanticReasoner re, TTSEngine ttsEngine) {
		this._kb = kb;
//		this._re = re;
		this._ttsEngine = ttsEngine;
	}		
	
	/**
	 * This method gets the stream of SentenceInfosFileName file and reads information 
	 * related to a specific sentence from it, 
	 * then return those lines of file which have information of different parts of this sentence.
	 * 
	 * @param stream  stream of SentenceInfosFileName file.
	 * @return those lines of file which have information of different parts of this sentence.
	 */
	private ArrayList<String> readSentenceParts(BufferedReader stream){		
				
		ArrayList<String> senParts = new ArrayList<String>(); 
		String content = "";

		try {
			content = stream.readLine();
			
			if (content == null)
				return null;
			//when loop terminates, stream has reached to the information of the next sentence.
			while(!content.contains("sentence:")){
				
				if(!content.startsWith("#"))// not comment line
					
					if(!content.equals(""))
						senParts.add(content);
				
				content = stream.readLine();
				
				if(content == null)
					break;
			}
		
		} catch (IOException e) {				
			e.printStackTrace();
		}
		return senParts;
	}
		
	/**
	 * This method gets NLsentence as input and finds its processed information in SentenceInfosFileName file.
	 * 
	 * @param NLsentence
	 * @return informations of parts of this sentence.
	 */
	private ArrayList<String> findSentenceInfos(String NLsentence){		
		ArrayList<String> senPartStrs = null;
		BufferedReader stream = null;		
		try
		{
			stream = new BufferedReader(new InputStreamReader(new FileInputStream(sentenceInfosFileName), "utf-8"));			
		}
		catch(Exception e)
		{
			System.out.println("Error opening `" + sentenceInfosFileName + "` for reading input natural language texts!");
			e.printStackTrace();		
		}
			
		try {
			String line = "";
			while (line != null)
			{				
				line = stream.readLine();
				
				if(line == null)
					break;
				
				if(line.equals(""))
					continue;	
												
				if (line.startsWith("#")) // comment line
					continue;
				
				//it means the next sentence in file has reached!
				if (line.equals("sentence:" + NLsentence)){

					//this array has information of all parts of this sentence. 
					senPartStrs = readSentenceParts(stream);					
					break;
				}
			}
			stream.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return senPartStrs;		
	}
	
	
	/**
	 * This method gets partStr and return the its equivalent Part object.
	 * partStr has all information about current Part. 
	 * this information has a format like this:
	 * 
	 * name:کبوتر را	POS:NOUN	SYN:OBJ		SRC:	SEM:ARG1_THEME		WSD:کبوتر#n1	sub_part:2,3	num:
	 * name:کبوتر	POS:NOUN	SYN:PREDEP	SRC:3	SEM:ARG1_THEME_P	WSD:کبوتر#n1	sub_part:-		num:2
	 * name:را	POS:UNKNOWN	SYN:OBJ		SRC:4	SEM:ARG1_THEME_P	WSD:-		sub_part:-		num:3
	 * 
	 * @param partStr partStr has all information about current Part.
	 * @return equivalent Part Object.
	 */
	private SentencePart createPart(String partStr, SentenceModel senteceModel){
	
		String[] parts = partStr.split("(\t)+");
		
		if(parts.length != 8){			
			MyError.error("Bad sentence information format " + partStr + " parts-num " + parts.length);
			return null;
		}
					
		for(int i = 0; i < parts.length; i++)
			parts[i] = parts[i].substring(parts[i].indexOf(":")+1);				
		
		SentencePart newPart = new SentencePart(parts[0], parts[7], senteceModel);			
		
		if(parts[1] != null && !parts[1].equals("-"))
			newPart.set_pos(parts[1]);
			
		newPart.set_syntaxTag(parts[2]);
		
		newPart.set_sourceOfSynNum(parts[3]);
		
		if(parts[4] != null && !parts[4].equals("-"))
			newPart.set_semanticTag(parts[4]);
		
		newPart.set_wsd_name(parts[5]);
		
		if(parts[6] != null && !parts[6].equals("-")){				
			String[] subs = parts[6].split(",");
			
			ArrayList<SentencePart> subParts = new ArrayList<SentencePart>();
			for(String s:subs)			
				subParts.add(new SentencePart(s, senteceModel));
			
			newPart.setSub_parts(subParts);
		}		
				
		//print(newPart.getStr());
		return newPart;		
	}

	private void print(String s){
		System.out.println(s);
	}
		
	/**
	 * preprocessSentence first finds preprocessed information of this sentence from its related file.
	 * then convert this information to SentenceModel object 
	 * @param NLsentence sentence in natural language.
	 * @param senPartStrs contains informations of parts of this sentence.
	 * @return
	 */
	public SentenceModel preprocessSentence(String NLsentence) {
		
		SentenceModel sentence = new SentenceModel(NLsentence);
		
		//this array has information of all parts of this sentence.
		ArrayList<String> senPartStrs = findSentenceInfos(NLsentence);
		
		if(senPartStrs == null)
			return null;
		
		ArrayList<SentencePart> senParts = new ArrayList<SentencePart> ();
		
		for(int i = 0; i < senPartStrs.size(); i++){
			
			String currentPartStr = senPartStrs.get(i);
			
			SentencePart currentPart = createPart(currentPartStr, sentence);		
			
			//it means next line are informations of sub_parts of this current_part.
			// we have assumed that sub_parts has depth of 1. It means each sub_part has no sub_part in itself.
			if(currentPart != null && currentPart.hasSub_parts()){
				ArrayList<SentencePart> subParts = new ArrayList<SentencePart> (currentPart.getSub_parts().size());
				
				for(int j = 0; j < currentPart.getSub_parts().size() && (i+1)<senPartStrs.size(); j++){
					i++;
					String subPartStr = senPartStrs.get(i);
					SentencePart sPart = createPart(subPartStr, sentence);					
					if(sPart != null)
						subParts.add(sPart);							
				}
				currentPart.setSub_parts(subParts);							
			}						
			senParts.add(currentPart);

			//setting _wsd of currentPart to the proper Node of KB.			
			if(currentPart.isVerb())
				//isnewNode parameter is true, because every verb is new a one!
				allocate_wsd(sentence ,currentPart, true);
			else

				allocate_wsd(sentence, currentPart, false);
			
			if(currentPart._wsd == null)
				MyError.error(currentPart._wsd_name + " couldn't get allocated!");				
			
		}

		//now senParts has all parts' objects of this sentence, so we specify which is subject, object, adverb, ...						
		sentence.arrageSentenceParts(NLsentence, senParts);
		
		//adding verb relation to KB.
		//delayed to preprocessScene after preparing nullSemanticTags
		/*ArrayList<PlausibleStatement> verbRelations = */ //defineVerbRelation(sentence);
		
		//loading verb SemanticArguments.
		ArrayList<Node> semArgs = loadVerbSemanticArguments(sentence);
		
		SentencePart verb = sentence.getVerb();
		
		if(verb != null)
			verb.setCapacities(semArgs);
		else
			MyError.error("this sentnce has no verb! " + sentence);
		
		return sentence;
		
	}

	/**
	 * preprocessScene preprocesses input sentenceModel and converts it to the primarySceneModel.    
	 * 
	 * @param sentenceModel the SenetenceModel to be converted. guaranteed not to be null.
	 * @param primarySceneModel the SceneModel which this sentenceModel is to be added to. guaranteed not to be null.
	 * @param storyModel the StoryModel which the returned sceneModel is to be added to. guaranteed not to be null.
	 * @return SceneModel containing input sentenceModel 
	 */	 
	public SceneModel preprocessScene(SentenceModel sentenceModel, SceneModel primarySceneModel, StoryModel storyModel){		
		
		if(sentenceModel == null || primarySceneModel == null || storyModel == null){
			MyError.error("None of senetecenModel, sceneModel, and storyModel should be null! " + sentenceModel);
			return null;
		}		
		
		prepareNullSemanticTags(sentenceModel, primarySceneModel, storyModel);
		
		checkAllSemanticTagsWithUser(sentenceModel, primarySceneModel);
		
		if(sentenceModel.getArg0() != null)
			preprocessSemanticArg(sentenceModel.getArg0().convertToSemanticTag(), sentenceModel, primarySceneModel);
		
		if(sentenceModel.getArg1() != null)
			preprocessSemanticArg(sentenceModel.getArg1().convertToSemanticTag(), sentenceModel, primarySceneModel);
	
		if(sentenceModel.getArg2() != null)
			preprocessSemanticArg(sentenceModel.getArg2().convertToSemanticTag(), sentenceModel, primarySceneModel);

		preprocessVerbArg(sentenceModel, primarySceneModel);
		
		SceneElement arg3SceneElem = null;
		
		SceneElement arg4SceneElem = null;
		
		if(sentenceModel.getArg3() != null)
			arg3SceneElem = preprocessSemanticArg(sentenceModel.getArg3().convertToSemanticTag(), sentenceModel, primarySceneModel);
				
		if(sentenceModel.getArg4() != null)
			arg4SceneElem = preprocessSemanticArg(sentenceModel.getArg4().convertToSemanticTag(), sentenceModel, primarySceneModel);
		
		processLocationOfScene(sentenceModel, arg3SceneElem, arg4SceneElem, primarySceneModel);
		
		if(sentenceModel.getArg5() != null)
			preprocessSemanticArg(sentenceModel.getArg5().convertToSemanticTag(), sentenceModel, primarySceneModel);
	
		preprocessSecondaryArgs(sentenceModel, primarySceneModel);
		
		preprocessVisualTagVerb(sentenceModel, primarySceneModel);
		
		preprocessAllVisualTag(sentenceModel, primarySceneModel);
		
		print("\nprimarySceneModel\n" + primarySceneModel);
		return primarySceneModel;
	}
	
	/**
	 * this method prepares the null semanticTags (only ARG0, ARG1) of the verb of sentenceModel as possible with the help of 
	 * other sentences of primarySceneModel and other scenes of stroyModel.
	 * 
	 * @param sentenceModel the sentenceModel which the semanticArgs of its verb is to be prepared. guaranteed not to be null.
	 * @param primarySceneModel the sceneModel which sentenceModel belongs to. guaranteed not to be null.
	 * @param storyModel the storyModel which primarySceneModel belongs to. guaranteed not to be null.
	 */
	private void prepareNullSemanticTags(SentenceModel sentenceModel, SceneModel primarySceneModel, StoryModel storyModel) {
		
		print("\n=============== in   prepareNullSemanticTags =======================");
		
		ArrayList<MainSemanticTag> existingSemTags = sentenceModel.getExistingMainSematicArgs();
		ArrayList<MainSemanticTag> necessarySemTags = sentenceModel.getNecessarySematicArgs();
						
//		print("HHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH");
//		print("existing " + existingSemTags);
//		print("necessar " + necessarySemTags);
		
		ArrayList<MainSemanticTag> missingMainArgs = new ArrayList<MainSemanticTag>();
		
		for(MainSemanticTag necess:necessarySemTags)
			if(!existingSemTags.contains(necess))
				missingMainArgs.add(necess);
//		print("missings " + missingMainArgs);
		
		prepareNullSemanticTagsForAScene(sentenceModel, missingMainArgs, primarySceneModel);
		
		while(!Common.isEmpty(missingMainArgs)){
//			print("new mis " + missingMainArgs + "scene ");
			ArrayList<SceneModel> allScene = storyModel.getScenes();
			
			if(!Common.isEmpty(allScene))
				for(SceneModel oldScene:allScene)
					if(!oldScene.equals(primarySceneModel))
						prepareNullSemanticTagsForAScene(sentenceModel, missingMainArgs, oldScene);
		}
		
		print("=============== end of prepareNullSemanticTags =====================");
	}	
	
	
	private void checkAllSemanticTagsWithUser(SentenceModel sentenceModel, SceneModel primarySceneModel){
		//TODO check with user!		
	}
	
	/**
	 * 
	 * @param semanticTag may be null.
	 * @param sentenceModel guaranteed not to be null.
	 * @param primarySceneModel guaranteed not to be null.
	 * @return the SceneElement created based on semanticTag
	 */
	private SceneElement preprocessSemanticArg(SemanticTag semanticTag, SentenceModel sentenceModel, SceneModel primarySceneModel) {
		
		if(semanticTag == null)
			return null;
		
		if(sentenceModel.hasSemanticArg(semanticTag)){
						
			print("\n=============== in     preprocess " + semanticTag + " =======================");
			
			
			SentencePart semArgPart = sentenceModel.getSentencePart(semanticTag);
			
			if(semArgPart == null){				
				MyError.error("the sentenceModel has " + semanticTag + " but it didn't find!" + sentenceModel);
				return null;
			}
			
			//It means that it is verb (infinitive) so the processing must perform on it and its dependents too!
			if(semArgPart.isInfinitive()){
				//TODO: complete this part later!
				print("It is an infinitive so, the process should got performed for it again!");
				
				return null;
			}
			else{
						
				//reasoning ScenePart from KB, ROLE,DYNAMIC_OBJECT, STATIC_OBJECT, ....
				ScenePart scenePart = _ttsEngine.whichScenePart(semArgPart);
				
				if(scenePart == null || scenePart == ScenePart.UNKNOWN){
					MyError.error("the " + semanticTag +": " + semArgPart + " ScenePart was not found!");
					return null;
				}
				
				SceneElement inputSceneElement = createSceneElement(semArgPart, scenePart);
				
				if(inputSceneElement == null){
					MyError.error("the " + semArgPart + " could not convert to a SceneElement!");
					return null;
				}
				
				boolean isRedundantPart = primarySceneModel.hasSceneElement(inputSceneElement);
				
				SceneElement sceneElem = null;
				
				//It means that the primarySceneModel has had this ScenePart before, so we will merge the information of this part with that one
				if(isRedundantPart){
					
					print(inputSceneElement._name + " is redundant!");
					
					sceneElem = primarySceneModel.getSceneElement(inputSceneElement);
					
					if(sceneElem == null){
						MyError.error("primarySceneModel has " + semArgPart + " but it could not be found!");
						return null;
					}
					sceneElem.mergeWith(inputSceneElement);	
				}
				//It means that this semArgPart is a newly seen ScenePart which is to be added to primarySceneModel.
				else{
					//creates a new ScenePart based on semArgPart and adds it to the primarySceneModel or return null if it was redundant!
					primarySceneModel.addToPrimarySceneModel(inputSceneElement);
					
					sceneElem = inputSceneElement;				
				}
				
				//------------------- pre-processing dependents of semArgPart --------------------
				
				processDependentsOfSemanticArg(semArgPart, sceneElem);
								
				print("=============== end of preprocess " + semanticTag + " =======================");
				
				return sceneElem;
			}
		}
		return null;
	}
		
	/**
	 * @param sentenceModel guaranteed not to be null.
	 * @param primarySceneModel guaranteed not to be null.
	 */
	private void preprocessVerbArg(SentenceModel sentenceModel, SceneModel primarySceneModel) {
				
		print("\n--------------- in   verb preprocess -------------------------------");
		
		//TODO: check the correct place of this statement!
		defineVerbRelation(sentenceModel);
		
		SentencePart verb = sentenceModel.getVerb();
		
		if(verb == null){
			MyError.error("The SentenceModel has no verb as a mistake!");
			return;
		}
			
		VerbType verbType = verb.getVerbType();
		
		print(verb + " vebType is: " + verbType);
		
		switch(verbType.name()){
			case("MORAKAB"):
				actionVerbProcessing(sentenceModel, primarySceneModel);
				break;
			case("BASIT"):
				actionVerbProcessing(sentenceModel, primarySceneModel);			
				break;			
			case("BASIT_RABTI"):
				//TODO: check this part!
				rabtiVerbProcessing(verb, sentenceModel, primarySceneModel);
				break;				
			case("BASIT_NAMAFOLI"):
				//TODO: check this part!
				namafoliVerbProcessing(verb, sentenceModel, primarySceneModel);
				break;			
			default:
				print("Unknown verb type!");
			
		}		
		print("\n--------------- end of verb preprocess -----------------------------");
	}
	
	/**
	 * 
	 * @param sentenceModel guaranteed not to be null.
	 * @param arg3SceneElement
	 * @param arg4SceneElement
	 * @param primarySceneModel guaranteed not to be null.
	 */
	private void processLocationOfScene(SentenceModel sentenceModel, SceneElement arg3SceneElement, SceneElement arg4SceneElement, SceneModel primarySceneModel) {
		
		print("\n=============== in preprocessLocation ==============================");
		
		SentencePart arg3Part = sentenceModel.getArg3SentencePart();
		
		SentencePart arg4Part = sentenceModel.getArg4SentencePart();
		
		if(arg3Part != null && arg3Part._semanticTag != null && arg3Part._semanticTag.isMainSemanticTag()){
		
			MainSemanticTag arg3SemArg = arg3Part._semanticTag.convertToMainSemanticTag();
			
			//Sentence has ARG3_SOURCE_STARTPOINT
			if(arg3SemArg == MainSemanticTag.ARG3_SOURCE_STARTPOINT && arg3SceneElement != null && arg3SceneElement.scenePart == ScenePart.LOCATION){
			
				primarySceneModel.setLocation((Location)arg3SceneElement);
				
				//Sentence has ARG4_ENDPOINT
				if(arg4Part != null && arg4SceneElement != null)			 
						primarySceneModel.addAlternativeLocation((Location)arg4SceneElement);				
				
				//Sentence hasn't ARG4_ENDPOINT
				else{
					
					SentencePart arg_dirPart = sentenceModel.getSentencePart(SubSemanticTag.DIR);
					
					//Sentence has ARG_DIR
					if(arg_dirPart != null){
						
						SceneElement arg_dirSceneElem = preprocessSemanticArg(SubSemanticTag.DIR.convertToSemanticTag(), sentenceModel, primarySceneModel) ;							
						
						if(arg_dirSceneElem == null){
							MyError.error("the " + arg_dirPart + " could not convert to a SceneElement!");
							return;
						}							

						primarySceneModel.addAlternativeLocation((Location)arg_dirSceneElem);
					}										
				}				
			}
			return;		 			
		}
		//Sentence hasn't ARG3_SOURCE_STARTPOINT but maybe have arg4Part or arg_dirPart
		
		//Sentence has ARG4_ENDPOINT
		if(arg4Part != null)			
				primarySceneModel.addAlternativeLocation((Location)arg4SceneElement);				
		
		//Sentence hasn't ARG4_ENDPOINT
		else{
			
			SentencePart arg_dirPart = sentenceModel.getSentencePart(SubSemanticTag.DIR);
			
			//Sentence has ARG_DIR
			if(arg_dirPart != null){
				
				SceneElement arg_dirSceneElem = preprocessSemanticArg(SubSemanticTag.DIR.convertToSemanticTag(), sentenceModel, primarySceneModel);
				
				if(arg_dirSceneElem == null){
					MyError.error("the " + arg_dirPart + " could not convert to a SceneElement!");
					return;
				}
				
				primarySceneModel.addAlternativeLocation((Location)arg_dirSceneElem);
			}								
		}
		
		print("\n=============== end of preprocessLocation ==========================");
	}

	private void preprocessSecondaryArgs(SentenceModel sentenceModel, SceneModel primarySceneModel) {
		// TODO Auto-generated method stub
		
	}

	private void preprocessVisualTagVerb(SentenceModel sentenceModel, SceneModel primarySceneModel) {
		// TODO Auto-generated method stub
		
	}

	private void preprocessAllVisualTag(SentenceModel sentenceModel, SceneModel primarySceneModel) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * 
	 * @param sentenceModel guaranteed not to be null.
	 * @param missingMainArgs guaranteed not to be null.
	 * @param sceneModel guaranteed not to be null.
	 */
	private void prepareNullSemanticTagsForAScene(SentenceModel sentenceModel, ArrayList<MainSemanticTag> missingMainArgs, SceneModel sceneModel){		
		
		if(!Common.isEmpty(missingMainArgs)){
			
			ArrayList<MainSemanticTag> preparedMainArgs = new ArrayList<MainSemanticTag>();
			
			ArrayList<SentenceModel> allSentences = sceneModel.getSentences();
			
			if(!Common.isEmpty(allSentences)){		
				
				for(MainSemanticTag miss:missingMainArgs){
					
//					print("for missed " + miss);
					
					for(SentenceModel sent:allSentences){
						if(sent.equals(sentenceModel))
							continue;
						
//						print("In sentence: " + sent.getOriginalSentence());
						
						if(miss != null && miss.isArg0() && sent.hasArg0()){							
							SentencePart arg0SentencePart = sent.getArg0SentencePart();
							
							arg0SentencePart.set_semanticTag(miss.name());
							preparedMainArgs.add(arg0SentencePart._semanticTag.convertToMainSemanticTag());							
							sentenceModel.setPrerparedSentencePart(arg0SentencePart);						
							
							print("prepared " + arg0SentencePart._semanticTag);
							break;
						}
						else if(miss != null && miss.isArg1() && sent.hasArg1()){
							SentencePart arg1SentencePart = sent.getArg1SentencePart();
							
							arg1SentencePart.set_semanticTag(miss.name());
							preparedMainArgs.add(arg1SentencePart._semanticTag.convertToMainSemanticTag());
							sentenceModel.setPrerparedSentencePart(arg1SentencePart);
							
							print("prepared " + arg1SentencePart._semanticTag);
							break;
						}
					}					
				}
				for(MainSemanticTag prepared:preparedMainArgs)
					missingMainArgs.remove(prepared);
			}			
		}			
	}
	
	/**
	 * this method process the dependents (adjectives and mozaf_elaihs) of semArgPart (if any).
	 * it converts each adjective and mozaf_elaih of semArgPart to :
	 * <ul> if scenePart of sceneElement is ROLE:
	 * 		<li> it adds a RoleMood to sceneElement. </li>		  			
	 * <ul> if scenePart of sceneElement is DYNAMIC_OBJECT or STATIC_OBJECT:
	 *  	<li> it adds an ObjectState to sceneElement. </li> 		
	 * </ul> 
	 * <ul> if scenePart of sceneElement is LOCATION, TIME, SCENE_EMOTION, SCENE_GOAL:
	 * 		<li> id dose nothing yet! </li>
	 * <ul> 
	 * 
	 * @param semArgPart
	 * @param sceneElement
	 */
	private void processDependentsOfSemanticArg(SentencePart semArgPart, SceneElement sceneElement){
		
		//It means that this sentencePart has some adjectives
		if(semArgPart.hasAnyAdjectives()){
			
			print(semArgPart + " " + semArgPart._semanticTag + " has some adjectives:" + semArgPart.getAdjectives());
			
			ArrayList<SentencePart> adjectives = semArgPart.getAdjectives();
			
			for(SentencePart adj:adjectives)				
				
				if(sceneElement.scenePart == ScenePart.ROLE)
					 sceneElement.addRoleMoodToRole(adj._name, adj._wsd);
					
				else if(sceneElement.scenePart == ScenePart.DYNAMIC_OBJECT || sceneElement.scenePart == ScenePart.STATIC_OBJECT)
					sceneElement.addStateToSceneObject(adj._name, adj._wsd);
		
				else if(sceneElement.scenePart == ScenePart.LOCATION)
					print("what to do for adj of LOCATION ?!");//TODO

				else if(sceneElement.scenePart == ScenePart.SCENE_GOAL)
					print("what to do for adj of SCENE_GOAL ?!");//TODO
			
				else if(sceneElement.scenePart == ScenePart.TIME)
					print("what to do for adj of TIME ?!");//TODO
			
				else if(sceneElement.scenePart == ScenePart.SCENE_EMOTION)
					print("what to do for adj of SCENE_GOAL ?!");//TODO
		}
		
		//It means that this sentencePart has some mozad_elaih
		if(semArgPart.hasAnyMozaf_elaihs()){				
			
			print(semArgPart  + " " + semArgPart._semanticTag + " has mozaf_elaih: " + semArgPart.getMozaf_elaih());
			
			ArrayList<SentencePart> mozafs = semArgPart.getMozaf_elaih();
			
			for(SentencePart moz:mozafs)
				
				if(!moz._wsd.getName().contains(zamir_enekasi))					
		
					if(sceneElement.scenePart == ScenePart.ROLE)
						 sceneElement.addRoleMoodToRole(moz._name, moz._wsd);
						
					else if(sceneElement.scenePart == ScenePart.DYNAMIC_OBJECT || sceneElement.scenePart == ScenePart.STATIC_OBJECT)
						sceneElement.addStateToSceneObject(moz._name, moz._wsd);
		
					else if(sceneElement.scenePart == ScenePart.LOCATION)
						print("what to do for moz of LOCATION ?!");//TODO
						
					else if(sceneElement.scenePart == ScenePart.SCENE_GOAL)
						print("what to do for moz of SCENE_GOAL ?!");//TODO
			
					else if(sceneElement.scenePart == ScenePart.TIME)
						print("what to do for moz of TIME ?!");//TODO
	
					else if(sceneElement.scenePart == ScenePart.SCENE_EMOTION)
						print("what to do for moz of SCENE_GOAL ?!");//TODO
		}			
	}
	
	/**
	 * 
	 * @param verb guaranteed not to be null.
	 * @param sentenceModel guaranteed not to be null.
	 * @param primarySceneModel guaranteed not to be null.
	 */
	private void namafoliVerbProcessing(SentencePart verb, SentenceModel sentenceModel, SceneModel primarySceneModel) {
		
		SentencePart arg1Part = sentenceModel.getArg1SentencePart();
		
		if(arg1Part == null){
			print(verb + " is namafoli but ARG1 not found as a mistake!");
			return;
		}		
		
		SceneElement arg1Elem = primarySceneModel.getSceneElement(arg1Part._wsd);
		
		if(arg1Elem == null){
			print(arg1Part + " can not be found in primarySceneModel!");
			return;
		}		
		
		if(arg1Elem.scenePart == ScenePart.ROLE)
			arg1Elem.addRoleMoodToRole(verb._name, verb._wsd);
		
		else if(arg1Elem.scenePart == ScenePart.DYNAMIC_OBJECT || arg1Elem.scenePart == ScenePart.STATIC_OBJECT)
			arg1Elem.addStateToSceneObject(verb._name, verb._wsd);		
		
		else{
			print("scenePart of " + arg1Elem + " was none of Role, DynamicObject, and StaticObject!");
			return;
		}
		
	}

	/**
	 * 
	 * @param verb guaranteed not to be null.
	 * @param sentenceModel guaranteed not to be null.
	 * @param primarySceneModel guaranteed not to be null.
	 */
	private void rabtiVerbProcessing(SentencePart verb, SentenceModel sentenceModel, SceneModel primarySceneModel) {
		
		SentencePart arg1Part = sentenceModel.getArg1SentencePart();
		
		SentencePart arg2Part = sentenceModel.getArg2SentencePart();
		
		if(arg1Part == null || arg2Part == null){
			print(verb + " is rabti but ARG1 or ARG2 not found as a mistake!");
			return;
		}		
		
		if(arg2Part.isAdjective()){
		
			SceneElement arg1Elem = primarySceneModel.getSceneElement(arg1Part._wsd);
			
			if(arg1Elem == null){
				print(arg1Part + " can not be found in primarySceneModel!");
				return;
			}		
			
			if(arg1Elem.scenePart == ScenePart.ROLE)
				arg1Elem.addRoleMoodToRole(arg2Part._name, arg2Part._wsd);
			
			else if(arg1Elem.scenePart == ScenePart.DYNAMIC_OBJECT || arg1Elem.scenePart == ScenePart.STATIC_OBJECT)
				arg1Elem.addStateToSceneObject(arg2Part._name, arg2Part._wsd);
			
			else{
				print("scenePart of " + arg1Elem + " was none of Role, DynamicObject, and StaticObject!");
				return;
			}
		}
		else
			print(verb + " is rabti but Arg2Part " + arg2Part + " is not adjective!");
	}

	private void actionVerbProcessing(SentenceModel sentenceModel, SceneModel primarySceneModel) {
		
		SentencePart verb = sentenceModel.getVerb();
		
		if(verb == null){
			MyError.error("The SentenceModel has no verb as a mistake!");
			return;
		}
		
		SentencePart arg0Part = sentenceModel.getArg0SentencePart();
		
		if(arg0Part == null){
			MyError.error("The " + verb + " is BASIT, but has no Arg0Part as a mistake!");
			return;
		}
		
		SceneElement arg0Elem = primarySceneModel.getSceneElement(arg0Part._wsd);
		
		if(arg0Elem == null){
			print(arg0Part + " can not be found in primarySceneModel!");
			return;
		}
		
		if(arg0Elem.scenePart == ScenePart.ROLE)
			arg0Elem.addRoleActionToRole(verb._name, verb._wsd);
		
		else if(arg0Elem.scenePart == ScenePart.DYNAMIC_OBJECT)
			arg0Elem.addObjectActionToDynmicAction(verb._name, verb._wsd);	
		
	}

	/**
	 * This method maps part's wsd parameter to a concept in _kb based on part's wsd_name parameter.
	 * if part's wsd_name is "-" no mapping occurs.
	 * if part's wsd_name has just one part, it is the main concept name, so it must directly maps to a node in _kb.
	 * if part's wsd_name has more than one part which includes one MAIN and probably a PRE or POST, so it must be mapped to a plausible statement in a _kb.
	 * 
	 * @param part the part its wsd parameter to be set.
	 * @param isNewNode is this part a new instance or is is the same as seen before.
	 * @param synTag the SyntaxTag of this node in the sentence.
	 */
	private void allocate_wsd(SentenceModel sentence, SentencePart part, boolean isNewNode){
		if(part == null)
			return;
			
		String wsd_name = part._wsd_name;
		
		if(wsd_name == null || wsd_name.equals("-"))
			return;
		
		//TODO: I have an assumption that sub_parts has simple (just concept name) _wsd_name.
		if(part.hasSub_parts())
			for(SentencePart p:part.getSub_parts())
				if(p._wsd == null && p._wsd_name != null && !p._wsd_name.equals("-")){																				
					Node wsd = _ttsEngine.findorCreateInstance(p._wsd_name, isNewNode);
					if(wsd != null)
						p.set_wsd(wsd);
				} 
		
		//it means that it is not just node but plausible statement for example MAIN_وضعیت سلامتی_POST
		if(wsd_name.indexOf("_") != -1){
			
			String[] sub_parts = wsd_name.split("_"); //1_وضعیت سنی#a_خردسال#a1 --> [part_num:1 --> وضعیت سلامتی --> خردسال#a1]
			
			if(sub_parts.length != 3){
				MyError.error("wrong wsd_name" + wsd_name);
				return;
			}
			
			Node argument = null;
			Node referent = null;
			Node descriptor = null;
			
			PlausibleStatement wsd = null;
			
			String relation_name = null;
			
			for(int i = 0; i < 3; i++){
				
				Node cur_part_wsd = null;
				
				try {
					int part_num = -1;
					
					//if it dosen't throw exception, it means that cur_part is a number
			        part_num = Integer.parseInt(sub_parts[i]);
			        
			        SentencePart cur_part = null;
			        
			        //it is a valid part_num. 
			        if(part_num != -1){
			        	cur_part = part.getSub_part(part_num);
			        	cur_part_wsd = cur_part._wsd;			        	
			        }
			        
			        if(i == 0 || i == 2)
			    		if(cur_part_wsd != null)
							if(argument == null)
								argument = cur_part_wsd;
							else if(referent == null)
								referent = cur_part_wsd;					
			    	
			        
			    } catch (NumberFormatException e) {
			    	//if it has thrown an exception it means that sub_part[i] is the name of a concept.
			    	//it is argument or referent
			    	if(i == 0 || i == 2){
			    		cur_part_wsd = _ttsEngine.findorCreateInstance(sub_parts[i], isNewNode);
			    		
			    		if(cur_part_wsd != null){
							
			    			if(argument == null)
								argument = cur_part_wsd;
							else if(referent == null)
								referent = cur_part_wsd;
						}
			    	}
			    	//it is descriptor
			    	else{
			    		relation_name = sub_parts[1];
						wsd = _ttsEngine.findRelationInstance(relation_name);
						if(wsd == null){
							//it must got directly fetched from kb, and then addRelation will clone it.
							descriptor = _kb.addConcept(relation_name, false);
						}			    		
			    	}
			    }	
			}
			
			
			if(descriptor != null){//it means that findRelation has not found it and it is newly fetched from kb.			
				
				wsd = _kb.addRelation(argument, referent, descriptor, SourceType.TTS);
				
				print("wsdRel added ---- : " + wsd.argument.getName() + " --> " + wsd.getName() + " --> " + wsd.referent.getName() + "\n");
				
				_ttsEngine.addRelationInstance(descriptor.getName(), wsd);
			}
			else{//it means that findRelation has found this relation.
				
				//it means that this relation must be different with the seen one! I have checked this logic it seems to be correct! 
				if(wsd.argument != argument || wsd.referent != referent){
					
					ArrayList<PlausibleStatement> allInst = _ttsEngine.getRelationAllInstances(relation_name);
					if(allInst != null)
						for(PlausibleStatement relInst:allInst)						
							if(relInst.argument == argument && relInst.referent == referent){
								descriptor = relInst;
								break;
							}
						
					if(descriptor == null){
						descriptor = _kb.addConcept(relation_name, false);						
						
						wsd = _kb.addRelation(argument, referent, descriptor, SourceType.TTS);
						
						print("wsdRel added ---- : " + wsd.argument.getName() + " -- " + wsd.getName() + " -- " + wsd.referent.getName() + "\n");
						
						_ttsEngine.addRelationInstance(relation_name, wsd);
					}
				}
			}
			part.set_wsd(argument);
			
			add_adjective_mozaf(sentence, part, descriptor, referent);
			//return;
		}
		else{
			//it means this part wsd_name is just one concept name, so we find or add it in sceneModel.			
			Node wsd = _ttsEngine.findorCreateInstance(wsd_name, isNewNode);
			part.set_wsd(wsd);
		}		
	}
	
	private SceneElement createSceneElement(SentencePart part, ScenePart scenePart){
		
		if(part == null || scenePart == null || scenePart == ScenePart.UNKNOWN){
			MyError.error("null input parameter for createSceneElement !");
			return null;
		}								
		
		if(scenePart == ScenePart.ROLE)			
				return new Role(part._name, part._wsd);
		else if(scenePart == ScenePart.ROLE_ACTION)
			return new RoleAction(part._name, part._wsd);
		else if(scenePart == ScenePart.DYNAMIC_OBJECT)
			return new DynamicObject(part._name, part._wsd);
		else if(scenePart == ScenePart.OBJECT_ACTION)
			return new ObjectAction(part._name, part._wsd);
		else if(scenePart == ScenePart.STATIC_OBJECT)
			return new StaticObject(part._name, part._wsd);				
		else if(scenePart == ScenePart.LOCATION)
			return new Location(part._name, part._wsd);			
		else if(scenePart == ScenePart.TIME)
			return new Time(part._name, part._wsd);
		else if(scenePart == ScenePart.SCENE_EMOTION)
			return new SceneEmotion(part._name, part._wsd);
		else if(scenePart == ScenePart.SCENE_GOAL)
			return new SceneGoal(part._name, part._wsd);
		return null;
	}

	
	@SuppressWarnings("unused")
	private void add_adjective_mozaf(SentenceModel sentence, SentencePart mainPart, Node descriptor, Node referent) {
		
		if(sentence == null || mainPart == null || descriptor == null || referent == null){
			MyError.error("null input parameter for add_adjective_mozaf!");
			if(mainPart == null)
				MyError.error("no mainPart could be found!");
			return;
		}
		
		if(descriptor.getPos() == POS.ADJECTIVE){ //it means that descriptor is describing an adjective.
		
			SentencePart adjPart = new SentencePart(referent.getName(), POS.ADJECTIVE, DependencyRelationType.NPOSTMOD, null, referent, null, sentence);
			
			//1:added, 0:merged, -1:Nop
			int added = mainPart.addAdjective(adjPart);
//			if(added == 1)
//				print("----------------" + adjPart + " adjective added to " + mainPart + "\n");
//			else if(added == 0)
//				print("----------------" +  mainPart + " own adjective merged with " + adjPart + "\n");
//			else
//				print("----------------"+  mainPart + " has " + adjPart + " before\n");
		}
		else if(descriptor.getPos() == POS.NOUN){ //it means that descriptor is describing a mozaf_alaih.
			
			SentencePart mozPart = new SentencePart(referent.getName(), POS.NOUN, DependencyRelationType.MOZ, null, referent, null, sentence);
			
			//1:added, 0:merged, -1:Nop
			int added = mainPart.addMozaf_elaih(mozPart); 
//			if(added == 1)
//				print("----------------" + mozPart + " mozaf_elaih added to " + mainPart + "\n");
//			else if(added == 0)
//				print("----------------" +  mainPart + " own mozaf_elaih merged with " + mozPart + "\n");
//			else
//				print("----------------"+  mainPart + " has " + mozPart + " before\n");
		}
	}

	
	
//	/**
//	 * this method based on the ScenePart of the subject(s) of the sentenceModel adds RoleAction(s) or ObjectAction(s) to primarySceneModel. 
//	 * It is important to note that when this method is called _wsd parameter of  
//	 * all subject(s) of this sentenceModel has been allocated ! 
//	 * 
//	 * @param verbRelation the relation indicating the action of this verb. 
//	 * @param primarySceneModel guaranteed not to be null.
//	 */
//	private void addVerbToPrimarySceneModel(PlausibleStatement verbRelation, SceneModel primarySceneModel){
//		if(verbRelation == null)
//			MyError.error("verb parameter of this method should not be null!");
//		
//		Node sbj = verbRelation.argument;				
//										
//		ScenePart sbjSp = _ttsEngine.whichScenePart(sbj, DependencyRelationType.SBJ);
//		
//		if(sbjSp == null || sbjSp == ScenePart.UNKNOWN){
//			MyError.error("this subject \"" + sbj + "\" has no ScenePart");
//			return;
//		}
//		
//		if(sbjSp == ScenePart.ROLE){			
//			
//			Role role = primarySceneModel.getRole(sbj);
//			if(role == null){
//				MyError.error(primarySceneModel + " SceneModel has not such a " + sbj + " Role.");
//				return;
//			}					
//			
//			RoleAction role_action = new RoleAction(verbRelation.getName(), verbRelation);				
//			role.addRole_action(role_action);
//		}
//		else if(sbjSp == ScenePart.DYNAMIC_OBJECT){
//			
//			DynamicObject dyn_obj = primarySceneModel.getDynamic_object(sbj);
//			if(dyn_obj == null){
//				MyError.error(primarySceneModel + " SceneModel has not such a " + sbj + " DynamicObject.");
//				return;
//			}
//			
//			ObjectAction obj_act = new ObjectAction(verbRelation.getName(), verbRelation);				
//			dyn_obj.addObejct_action(obj_act);
//		}			
//		
//	}
	
//	private void preprocessSubject(SentenceModel sentenceModel, SceneModel primarySceneModel){
//		
//		ArrayList<SentencePart> subjects = sentenceModel.getSubjects();
//			
//		print("\npreprocess subject: " + subjects);
//		
//		for(SentencePart sbj:subjects){
//		
//			if(sbj == null || !sbj.isSubject()){
//				MyError.error("bad subjct part " + sbj);
//				return;
//			}
//			
//			//_wsd of sbj is set to proper Node of KB.
//			allocate_wsd(sbj, false);	
//			
//			if(sbj._wsd != null)
//				addToPrimarySceneModel(sbj, primarySceneModel);			
//			else
//				MyError.error(sbj._wsd_name + " couldn't get allocated!");
//		}
//	}
//	
//	private void preprocessObject(SentenceModel sentenceModel, SceneModel primarySceneModel){
//		
//		ArrayList<SentencePart> objects = sentenceModel.getObjects();
//		
//		print("\npreprocess object: " + objects);
//		
//		for(SentencePart obj:objects){
//			
//			if(obj != null && !obj.isObject()){
//				MyError.error("bad obejct part " + obj);
//				return;
//			}	
//			else if(obj != null && obj.isObject()){
//			
//				//_wsd of obj is set to proper Node of KB.
//				allocate_wsd(obj, false);
//				
//				if(obj._wsd != null)			
//					addToPrimarySceneModel(obj, primarySceneModel);
//				else
//					MyError.error(obj._wsd_name + " couldn't get allocated!");
//			}
//		}
//	}		
//	
//	
//	private void preprocessAdverb(SentenceModel sentenceModel, SceneModel primarySceneModel) {
//		
//		ArrayList<SentencePart> adverbs = sentenceModel.getAdverbs();
//		
//		print("\npreprocess adverb: " + adverbs);
//		
//		for(SentencePart adv:adverbs){
//			if(adv != null && !adv.isAdverb()){
//				MyError.error("bad adverb part " + adv);
//				return;
//			}
//			else if(adv != null && adv.isAdverb()){
//				
//				//_wsd of adv is set to proper Node of KB.
//				allocate_wsd(adv, false);
//				
//				if(adv._wsd != null)			
//					addToPrimarySceneModel(adv, primarySceneModel);
//				else
//					MyError.error(adv._wsd_name + " couldn't get allocated!");
//			}
//		}		
//	}
//	/**
//	 * TODO:
//	 * 1- allocate_wsd verb 									   				--> done
//	 * 2- adding proper RoleActoin or ObjectAction to sceneModel.  				--> done
//	 * 3- create relation of verb!		   						   				--> done
//	 * 
//	 * 4- defining verb capacities in INJUREDPIGEON kb.							--> done as test
//	 * 5- loading these capacities from kb for SynSetof verbs.					--> done
//	 * 6- preparing values for these capacities --> in SceneReasoner,next phase.
//	 *   															  
//	 * @param sentenceModel guaranteed not to be null.
//	 * @param primarySceneModel guaranteed not to be null.
//	 */
//	private void preprocessVerb(SentenceModel sentenceModel, SceneModel primarySceneModel) {
//		
//		SentencePart verb = sentenceModel.getVerb();
//						
//		if(verb == null || !verb.isVerb()){
//			MyError.error("bad verb part " + verb);
//			return;
//		}		
//				
//		Node pure_verb = _ttsEngine.getPureNode(verb._wsd);
//		
//		if(pure_verb == null){
//			MyError.error("the pure version of " + verb._wsd + " could not be found");
//			return;
//		}
//		
//		//load verb capacities from kb.
//		ArrayList<PlausibleStatement> verb_cxs = loadVerbCapacities(pure_verb);
//		
//		print("loaded contexts " + verb_cxs + "\n");
//
//		here _wsd of subject(s) and object(s) of this sentence has been allocated!
//		ArrayList<PlausibleStatement> verbRelations = defineVerbRelation(sentenceModel, primarySceneModel);
//		
//		for(PlausibleStatement verbRel: verbRelations){
//			
////			addVerbToPrimarySceneModel(verbRel, primarySceneModel);	
//		
//			setLocationContext(verbRel, primarySceneModel.getLocation(), verb_cxs);
//			
//			setTimeContext(verbRel, primarySceneModel.getTime(), verb_cxs);				
//		}
//	}	

	/**
	 * this method defines the proper relation resulted from the verb of this sentence.
	 * It is important to note that when this method is called _wsd parameter of  
	 * all subject(s) and object(s) of this sentence has been allocated! 
	 * 
	 * @param verb the verb its relation are to be defined. guaranteed not to be null.
	 * @param sentenceModel guaranteed not to be null.
	 * @param primarySceneModel guaranteed not to be null.
	 */
	private ArrayList<PlausibleStatement> defineVerbRelation(SentenceModel sentenceModel){
		
		ArrayList<PlausibleStatement> verbRelations = new ArrayList<PlausibleStatement>();
		
		SentencePart verb = sentenceModel.getVerb();
		
		//here _wsd of subject(s), object(s), and adverb(s) of this sentence has been allocated!								
		ArrayList<SentencePart> subjects = sentenceModel.getSubjects();
		
		if(verb == null || subjects == null || subjects.size() == 0){
			MyError.error("sentence with verb " + verb + " has no subject part! " + sentenceModel);
			return verbRelations;
		}
		
		for(SentencePart sbj:subjects){				
			
			ArrayList<SentencePart> objects = sentenceModel.getObjects();
			
			boolean transitive_verb = false;
			
			if(objects != null && objects.size() > 0)
				
				for(SentencePart obj:objects)
					
					if(obj != null){						
						//it is a transitive verb.
						transitive_verb = true;
						
						//adding the relation of this sentence to kb.
						PlausibleStatement rel = _kb.addRelation(sbj._wsd, obj._wsd, verb._wsd, SourceType.TTS);
						verbRelations.add(rel);
						print("verbRel added ---- : " + rel.argument.getName() + " --> " + rel.getName() + " --> " + rel.referent.getName() + "\n");														
					}				
			
			if(!transitive_verb){
				//TODO check if using "KnowledgeBase.HPR_ANY" as referent is correct?!
				//adding the relation of this sentence to kb. 
				PlausibleStatement rel = _kb.addRelation(sbj._wsd, KnowledgeBase.HPR_ANY, verb._wsd, SourceType.TTS);
				verbRelations.add(rel);
				print("verbRel added ---- : " + rel.argument.getName() + " --> " + rel.getName() + " --> " + rel.referent.getName() + "\n");				
			}
		}
		return verbRelations;
	}

	private  ArrayList<Node>  loadVerbSemanticArguments(SentenceModel sentence) {
		SentencePart verbPart = sentence.getVerb();
		
		Node pure_verb = _ttsEngine.getPureNode(verbPart._wsd);
		
		if(verbPart == null || verbPart._wsd == null || pure_verb == null){
			MyError.error("verb or its wsd or its pure version should not be null!" + verbPart);
			return null;
		}
		
		Node verb_synSet = pure_verb.getSynSet();
						
		if(verb_synSet == null){
			MyError.error("this verb '" + pure_verb + "' has no SynSet!");
			return null;
		}
		
		Node mainSemanticArg = _kb.addConcept(mainSemanticArgumet_name, false);
		
		ArrayList<PlausibleAnswer> semArgsAnswers = _ttsEngine.writeAnswersTo(mainSemanticArg, verb_synSet, null);	
		
		ArrayList<Node> semArgs = new ArrayList<Node>();			
		
		for(PlausibleAnswer arg:semArgsAnswers)
			if(arg != null && arg.answer != null)
				semArgs.add(arg.answer);
		
		return semArgs;		
	}

	
//	/**
//	 * load verb capacities from kb.
//	 * 
//	 * @param pure_verb guaranteed not to be null.
//	 */
//	private ArrayList<PlausibleStatement> loadVerbCapacities(Node pure_verb) {	
//		
//		ArrayList<PlausibleStatement> cxs = new ArrayList<PlausibleStatement>();
//		
//		Node synSet = pure_verb.getSynSet();
//		
//		print("\nSynSet of " + pure_verb + " is " + synSet);
//		
//		if(synSet == null)
//			MyError.error("the verb " + pure_verb + " has no Synset!");
//		else
//			cxs = synSet.loadCXs();
//		
//		if(default_contexts == null){			
//			Node verb_root = _ttsEngine.verb_root;
//			default_contexts = verb_root.loadCXs();
//		}
//		
//		cxs.addAll(default_contexts);	
//		
//		return cxs;
//	}
//	
//	private void setLocationContext(PlausibleStatement verbRelation, Location location, ArrayList<PlausibleStatement> CXs){
//		if(verbRelation == null || location == null || CXs == null)
//			return;
//		
//		for(PlausibleStatement cx : CXs){		
//			
//			if(cx.relationType == null){
//				MyError.error("this " + cx + " has no relationType!");
//				return;
//			}			
//			
//			String cxName = cx.relationType.getContextName();			
//			
//			if(CONTEXT.fromString(cxName) == CONTEXT.LOCATION){				
//				PlausibleStatement locCx = _kb.addRelation(verbRelation, location._node, cx.relationType);
//				print("" + locCx + " (" + verbRelation.getName() + ")= " + location._node + "\n");			
//			}
//		}
//	}
//	
//	
//	private void setTimeContext(PlausibleStatement verbRelation, Time time, ArrayList<PlausibleStatement> CXs) {
//		if(verbRelation == null || time == null || CXs == null)
//			return;
//		
//		for(PlausibleStatement cx : CXs){		
//			
//			if(cx.relationType == null){
//				MyError.error("this " + cx + " has no relationType!");
//				return;
//			}			
//			
//			String cxName = cx.relationType.getContextName();			
//			
//			if(CONTEXT.fromString(cxName) == CONTEXT.TIME){				
//				PlausibleStatement timeCx = _kb.addRelation(verbRelation, time._node, cx.relationType);
//				print("" + timeCx + " (" + verbRelation.getName() + ")= " + time._node + "\n");			
//			}
//		}
//		
//	}
}


	