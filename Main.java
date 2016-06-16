package foon;

import java.io.*;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import org.jblas.*;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.Lin;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import edu.mit.jwi.*;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

public class Main {

	// Paths that need to be changed depending on computer being used!
	//	- File which contains the ENTIRE, COMBINED network
	static String filePath = "C:/Users/David Paulius/Documents/USF/Research/Graphs/Parsed 2.29.2016/Text Files/F7-FOON.txt";
	//	- File which contains the sequence which serves as INPUT for Roger's simulation,
	static String sequenceOutput = "C:/Users/David Paulius/Documents/Eclipse/Eclipse Projects/FoodNetwork/src/mainGraph.txt";
	//	- File which contains another network we wish to merge with the existing network (temporary)
	static String graphToBeMerged = "C:/Users/David Paulius/Documents/USF/Research/Graphs/Parsed 2.29.2016/Text Files/x-barbeque ribsNewNewNew2New" + ".txt";

	// -- ArrayList of objects and motions from parsing program
	private static ArrayList<String> objectList;
	private static ArrayList<String> motionList;
	
	private static ArrayList<Thing> nodes, nodesReversed; // dynamic list of all objects/motions observed in file; keeps track of matrix rows and columns 
	private static int totalNodes = 0; // total number of nodes that are in the network

	private static ArrayList<Thing> oneModeObject; // one-mode projection of only object nodes, not accounting states!
	private static ArrayList<Thing> oneModeObjectAbstract; // one-mode projection of only object nodes, accounting for abstract state
	private static ArrayList<Thing> oneModeObjectIngredients; // one-mode projection of only object nodes, accounting for uniqueness of ingredients.
	private static ArrayList<Thing> functionalMotions; 
	static int[] distances;

	static boolean[] visited;

	private static int[] motionFrequency; // array to count the number of instances of each motion in a graph

	static ArrayList<String> file; 
	private static ArrayList<FunctionalUnit> FOON; // FOON - abstract list
	private static ArrayList<FunctionalUnit> FOON_containers; //  -- this will be the list of FUs taking into account ingredients for uniqueness

	// for backtracking/branch-and-bound algorithm
	private static ArrayList<FunctionalUnit> reverseFOON; // list of all Functional Units in the network but edges are in REVERSE
	private static ArrayList<FunctionalUnit> reverseFOON_containers; // list of all Functional Units in the network but edges are in REVERSE

	// adjacency matrix of all objects
	private static double[][] oneModeObjectMatrix;
	private static double[][] oneModeObjectAbstractMatrix;
	private static double[][] oneModeObjectIngredientsMatrix;

	// Testing stack for backtracking purposes
	static Stack<Thing> backtrack, tempStack;

	public static void main(String[] args) throws Exception {
		// Initialize the ArrayList objects
		nodes = new ArrayList<Thing>();  
		FOON = new ArrayList<FunctionalUnit>();  
		FOON_containers = new ArrayList<FunctionalUnit>();  
		reverseFOON = new ArrayList<FunctionalUnit>();  
		reverseFOON_containers = new ArrayList<FunctionalUnit>();  

		// initializing all ArrayList objects used for representing network (forward + backward)
		nodesReversed = new ArrayList<Thing>();  
		oneModeObject = new ArrayList<Thing>(); // trying a thing with recording only objects with NO states  
		oneModeObjectAbstract = new ArrayList<Thing>();  
		oneModeObjectIngredients = new ArrayList<Thing>();  
		functionalMotions = new ArrayList<Thing>();  

		// Opens a dialog that will be used for opening the network file:
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle("FOON_analysis - Choose the main file to open:");
		chooser.setAcceptAllFileFilterUsed(true);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			System.out.println("Main network file selected : " + chooser.getSelectedFile());
		} else {
			System.err.println("Error in getting path of file!");
			return;
		}
		
		filePath = chooser.getSelectedFile().getAbsolutePath();
		// Populate the adjacency matrices and record number of UNIQUE nodes
		totalNodes = constructFUGraph(new Scanner(new File(filePath)));
		
		// -- Adding distances between nodes?
		//distances = new int[oneModeObject.size()];
		//int index = 0;
		//for (Thing T : oneModeObjectNil){
		//	distances[index] = exploreNeighbours(index);
		//	System.out.println(distances[index++]);
		//}

		objectList = new ArrayList<String>();
		motionList = new ArrayList<String>();

		System.out.println("\n*********************************************************************************************");
		System.out.println("\n			FOON Analysis + Graph Merging Program (revised 11/5/2016)\n");
		System.out.println("*********************************************************************************************");
		
		populateLists();

		// objects used for taking input from the console:
		String response;
		Scanner keyboard = new Scanner(System.in);

		while (true){
			response = printMenuOptions(keyboard);	
			if (response.equals("1")){
				// -- Reference: http://stackoverflow.com/questions/13525372/mit-java-wordnet-interface-getting-wordnet-lexicographer-classes-or-super-sense 
				//construct URL to WordNet Dictionary directory on the computer
		        String path = "C:/Users/David Paulius/Documents/Eclipse/WordNet-3.0/dict";
		        URL url = new URL("file", null, path);      
	
		        //construct the Dictionary object and open it
		        IDictionary dict = new Dictionary(url);
		        dict.open();
	
		        // look up first sense of the word "dog "
		        IIndexWord idxWord = dict.getIndexWord ("dog", POS.NOUN );
		        IWordID wordID = idxWord.getWordIDs().get(0) ;
		        IWord word = dict.getWord (wordID);         
		        ISynset synset = word.getSynset();
		        String LexFileName = synset.getLexicalFile().getName();
		        System.out.println("Id = " + wordID);
		        System.out.println(" Lemma = " + word.getLemma());
		        System.out.println(" Gloss = " + word.getSynset().getGloss());         
		        System.out.println("Lexical Name : "+ LexFileName);    
		        
		        String word1 = "apple", word2 = "bad", word3 = "evil	";
		        
		        WS4JConfiguration.getInstance().setMFS(false);
		        
		        // Comparison with WordNet corpus using Wu-Palmer metric of similarity..
		        ILexicalDatabase db = new NictWordNet();
		        WS4JConfiguration.getInstance().setMFS(true);
				double s = new Lin(db).calcRelatednessOfWords(word1, word2);
				System.out.println("Distance between " + word1 + " and " + word2 + " = " + s);
				s = new Lin(db).calcRelatednessOfWords(word1, word3);
				System.out.println("Distance between " + word1 + " and " + word3 + " = " + s);
				s = new Lin(db).calcRelatednessOfWords(word2, word3);
				System.out.println("Distance between " + word2 + " and " + word3 + " = " + s);
	
				System.out.print("\t -> Input the object found in the environment : > ");
				response = keyboard.nextLine();
				
				String answer = objectList.get(0);
				double similarity = new WuPalmer(db).calcRelatednessOfWords(response, answer);
				for (int x = 1; x < objectList.size(); x++){
					if (new WuPalmer(db).calcRelatednessOfWords(response, objectList.get(x)) > similarity){
						answer = objectList.get(x);
						similarity = new WuPalmer(db).calcRelatednessOfWords(response, answer);
						System.out.println("Closest object in FOON to " + response + " is " + answer + " with similarity distance of " + similarity);					
					}
				}
				
				System.out.println("Closest object in FOON to " + response + " is " + answer + " with similarity distance of " + similarity);
			}

			else if (response.equals("2")){
				// creating adjacency matrix for the object graph (TESTING)
				oneModeObjectAbstractMatrix = new double[oneModeObjectAbstract.size()][oneModeObjectAbstract.size()];
				oneModeObjectMatrix = new double[oneModeObject.size()][oneModeObject.size()];		
				oneModeObjectIngredientsMatrix = new double[oneModeObjectIngredients.size()][oneModeObjectIngredients.size()];
				populateAdjacencyMatrix(); // populate the structures created above
				System.out.println(oneModeObjectIngredients.size() + " " + oneModeObjectAbstract.size() + " " + oneModeObject.size());
				System.out.print(" -> Analysis with/without states/with ingredients? [1/2/3] > ");
				response = keyboard.nextLine();
				if (response.equals("1")){
					// Setting matrix object used in centrality analysis
					DoubleMatrix OMOmatrix = new DoubleMatrix(oneModeObjectAbstractMatrix);
					//DoubleMatrix OMOmatrix = new DoubleMatrix(test);
					ComplexDoubleMatrix eigenvalues = Eigen.eigenvalues(OMOmatrix); // all eigenvalues
					double largest = 0;
					for (ComplexDouble eigenvalue : eigenvalues.toArray()) {
						//System.out.print(String.format("%.2f ", eigenvalue.abs()));
						if (eigenvalue.real() > largest){
							largest = eigenvalue.abs();
						}
					}
		
					// values needed for Katz centrality
					double alpha = 1 / (largest + 0.5); // recommended that it is less than 1/K^1
					// vectors and matrix needed for Katz computation
					DoubleMatrix onesVector = DoubleMatrix.ones(oneModeObjectAbstract.size(), 1), 
							I = DoubleMatrix.eye(oneModeObjectAbstract.size()); // identity matrix
					DoubleMatrix ans = Solve.pinv((I.sub((OMOmatrix.transpose().mul(alpha))))).mmul(onesVector); // as per 7.10
					System.out.println("~");
					int count = 0, maxIndex = 0;;
					for (double D : ans.toArray()) {
						if (D > ans.toArray()[maxIndex]) {
							maxIndex = count;
						}
						System.out.println("O" + oneModeObjectAbstract.get(count).getType() + "S" + ((Object)oneModeObjectAbstract.get(count++)).getObjectState() + "\t" + String.format("%.5f ", (D)));
					}
					System.out.print("\nKATZ: Node " + (maxIndex+1) + " has the largest centrality value associated with it -> \n");
					oneModeObjectAbstract.get(maxIndex).printThing();
		
		
					// Taken from site: http://www.markhneedham.com/blog/2013/08/05/javajblas-calculating-eigenvector-centrality-of-an-adjacency-matrix/
					//  - Computing eigenvalue/vector centrality of the purely object node graph
					eigenvalues = Eigen.eigenvalues(OMOmatrix);
					//System.out.println("Eigenvalues are as follows: ");
					//for (ComplexDouble eigenvalue : eigenvalues.toArray()) {
					//    System.out.print(String.format("%.5f ", eigenvalue.real()));
					//}
					System.out.println("\n~");
					
					List<Double> principalEigenvector = normalised(getPrincipalEigenvector(OMOmatrix));
		
					maxIndex = 0;
					for (int x = maxIndex + 1; x < principalEigenvector.size(); x++) {
						if (principalEigenvector.get(maxIndex) < principalEigenvector.get(x)) {
							maxIndex = x; 
						}
					}
					System.out.println("\nEIGEN: Node " + (maxIndex+1) + " has the largest eigenvalue associated with it -> ");
					oneModeObjectAbstract.get(maxIndex).printThing();
	
					System.out.println("\n~");		
	
					maxIndex = 0;
					int maxDegree = oneModeObjectAbstract.get(maxIndex).countNeighbours(); 
					for (int x = maxIndex + 1; x < oneModeObjectAbstract.size(); x++) {
						if (oneModeObjectAbstract.get(maxIndex).countNeighbours() < oneModeObjectAbstract.get(x).countNeighbours()) {
							maxIndex = x; maxDegree = oneModeObjectAbstract.get(x).countNeighbours();
						}
					}
					System.out.println("\nDEGREE: Node " + (maxIndex+1) + " has the largest number of degrees with value of " + maxDegree);
					oneModeObjectAbstract.get(maxIndex).printThing();
					System.out.println();
	
				} else if (response.equals("2")) {
					// Setting matrix object used in centrality analysis
					DoubleMatrix OMOmatrix = new DoubleMatrix(oneModeObjectMatrix);
					//DoubleMatrix OMOmatrix = new DoubleMatrix(test);
					ComplexDoubleMatrix eigenvalues = Eigen.eigenvalues(OMOmatrix); // all eigenvalues
					double largest = 0;
					for (ComplexDouble eigenvalue : eigenvalues.toArray()) {
						//System.out.print(String.format("%.2f ", eigenvalue.abs()));
						if (eigenvalue.real() > largest){
							largest = eigenvalue.abs();
						}
					}
		
					// values needed for Katz centrality
					double alpha = 1 / (largest + 0.5); // recommended that it is less than 1/K^1
					// vectors and matrix needed for Katz computation
					DoubleMatrix onesVector = DoubleMatrix.ones(oneModeObject.size(), 1), 
							I = DoubleMatrix.eye(oneModeObject.size()); // identity matrix
					DoubleMatrix ans = Solve.pinv((I.sub((OMOmatrix.transpose().mul(alpha))))).mmul(onesVector); // as per 7.10
					System.out.println("~");
					int count = 0, maxIndex = 0;;
					for (double D : ans.toArray()) {
						if (D > ans.toArray()[maxIndex]) {
							maxIndex = count;
						}
						System.out.println("O" + oneModeObject.get(count++).getType() + "\t" + String.format("%.5f ", (D)));
					}
					System.out.println("\nKATZ: Node " + (maxIndex+1) + " has the largest centrality value associated with it ->");
					oneModeObject.get(maxIndex).printThing();
					
					System.out.println("\n~");		
	
					// Taken from site: http://www.markhneedham.com/blog/2013/08/05/javajblas-calculating-eigenvector-centrality-of-an-adjacency-matrix/
					//  - Computing eigenvalue/vector centrality of the purely object node graph
					eigenvalues = Eigen.eigenvalues(OMOmatrix);
					//System.out.println("Eigenvalues are as follows: ");
					//for (ComplexDouble eigenvalue : eigenvalues.toArray()) {
					//    System.out.print(String.format("%.5f ", eigenvalue.real()));
					//}
					List<Double> principalEigenvector = normalised(getPrincipalEigenvector(OMOmatrix));
					maxIndex = 0;
					for (int x = maxIndex + 1; x < principalEigenvector.size(); x++) {
						if (principalEigenvector.get(maxIndex) < principalEigenvector.get(x)) {
							maxIndex = x; 
						}
					}
					System.out.println("\nEIGEN: Node " + (maxIndex+1) + " has the largest eigenvalue associated with it ->");
					oneModeObject.get(maxIndex).printThing();
	
					System.out.println("\n~");		
	
					maxIndex = 0;
					int maxDegree = oneModeObject.get(maxIndex).countNeighbours(); 
					for (int x = maxIndex + 1; x < oneModeObject.size(); x++) {
						if (oneModeObject.get(maxIndex).countNeighbours() < oneModeObject.get(x).countNeighbours()) {
							maxIndex = x; maxDegree = oneModeObject.get(x).countNeighbours();
						}
					}			
					System.out.println("\nDEGREE: Node " + (maxIndex+1) + " has the largest number of degrees with value of " + maxDegree);
					oneModeObject.get(maxIndex).printThing();
					System.out.println();
				} else {
					// Setting matrix object used in centrality analysis
					DoubleMatrix OMOmatrix = new DoubleMatrix(oneModeObjectIngredientsMatrix);
					//DoubleMatrix OMOmatrix = new DoubleMatrix(test);
					ComplexDoubleMatrix eigenvalues = Eigen.eigenvalues(OMOmatrix); // all eigenvalues
					double largest = 0;
					for (ComplexDouble eigenvalue : eigenvalues.toArray()) {
						//System.out.print(String.format("%.2f ", eigenvalue.abs()));
						if (eigenvalue.real() > largest){
							largest = eigenvalue.abs();
						}
					}
		
					// values needed for Katz centrality
					double alpha = 1 / (largest + 0.5); // recommended that it is less than 1/K^1
					// vectors and matrix needed for Katz computation
					DoubleMatrix onesVector = DoubleMatrix.ones(oneModeObjectIngredients.size(), 1), 
							I = DoubleMatrix.eye(oneModeObjectIngredients.size()); // identity matrix
					DoubleMatrix ans = Solve.pinv((I.sub((OMOmatrix.transpose().mul(alpha))))).mmul(onesVector); // as per 7.10
					System.out.println("~");
					int count = 0, maxIndex = 0;;
					for (double D : ans.toArray()) {
						if (D > ans.toArray()[maxIndex]) {
							maxIndex = count;
						}
						System.out.println("O" + oneModeObjectIngredients.get(count).getType() + "S" + ((Object)oneModeObjectIngredients.get(count++)).getObjectState() + "\t" + String.format("%.5f ", (D)));
					}
					System.out.println("\nKATZ: Node " + (maxIndex+1) + " has the largest centrality value associated with it ->");
					oneModeObjectIngredients.get(maxIndex).printThing();
									
					// Taken from site: http://www.markhneedham.com/blog/2013/08/05/javajblas-calculating-eigenvector-centrality-of-an-adjacency-matrix/
					//  - Computing eigenvalue/vector centrality of the purely object node graph
					eigenvalues = Eigen.eigenvalues(OMOmatrix);
					//System.out.println("Eigenvalues are as follows: ");
					//for (ComplexDouble eigenvalue : eigenvalues.toArray()) {
					//    System.out.print(String.format("%.5f ", eigenvalue.real()));
					//}
					System.out.println("\n~");
					List<Double> principalEigenvector = normalised(getPrincipalEigenvector(OMOmatrix));
					maxIndex = 0;
					for (int x = maxIndex + 1; x < principalEigenvector.size(); x++) {
						if (principalEigenvector.get(maxIndex) < principalEigenvector.get(x)) {
							maxIndex = x; 
						}
					}
					System.out.println("\nEIGEN: Node " + (maxIndex+1) + " has the largest eigenvalue associated with it ->");
					oneModeObjectIngredients.get(maxIndex).printThing();
	
					System.out.println("\n~");		
	
					maxIndex = 0;
					int maxDegree = oneModeObjectIngredients.get(maxIndex).countNeighbours(); 
					for (int x = maxIndex + 1; x < oneModeObjectIngredients.size(); x++) {
						if (oneModeObjectIngredients.get(maxIndex).countNeighbours() < oneModeObjectIngredients.get(x).countNeighbours()) {
							maxIndex = x; maxDegree = oneModeObjectIngredients.get(x).countNeighbours();
						}
					}			
					System.out.println("\nDEGREE: Node " + (maxIndex+1) + " has the largest number of degrees with value of " + maxDegree);
					oneModeObjectIngredients.get(maxIndex).printThing();
					System.out.println();
				}
			}
			
			else if (response.equals("3")){
				// 	-- Count all nodes in the graph?
				int count = 0;
				for (Thing T : nodes) {
					if (T instanceof Object){
						count++;
					}
				}
				System.out.println(count + " object nodes found in graph!");
				count = 0;
				for (Thing T : nodes) {
					if (T instanceof Motion){
						count++;
					}
				}
				System.out.println(count + " motion nodes found in graph!");
			}
	
			else if (response.equals("4")){
				//	-- Produce functional object-motion files??
				getObjectMotions(); 
				motionFrequency = new int[motionList.size()]; // -- we should have around 5X motions; use the motion index!
				populateFrequencyList();
			}
			
			else if (response.equals("5")){
				System.err.println("Please select the FOLDER/DIRECTORY with all files you wish to merge with current file!");
				Thread.sleep(3000);
				
				// Create file explorer dialog to select the directory
				chooser = new JFileChooser();
				chooser.setCurrentDirectory(new java.io.File("."));
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setDialogTitle("FOON_analysis - Choose directory with all files to merge:");
				chooser.setAcceptAllFileFilterUsed(true);
				
				if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					System.out.println("Directory selected is : " + chooser.getSelectedFile());
				} else {
					System.err.println("Error in getting path of directory!");
					keyboard.close();
					return;
				}
				
				File directory = chooser.getSelectedFile();
				File[] listOfFiles = directory.listFiles();
				for (File F : listOfFiles){
					System.out.println(F.getName());
					if (!F.getName().equals(filePath)) {
						totalNodes = constructFUGraph(new Scanner(F));
					}
				}
				//totalNodes = constructFUGraph(new Scanner(new File(graphToBeMerged)));
				//printAllNodes();
				outputMergedGraph(filePath);
				outputGraphDegree(filePath);
			}
			
			else if (response.equals("6")){
				System.out.print("\tType the Object NUMBER to find: > ");
				int objectN = keyboard.nextInt();
				System.out.print("\tType the Object STATE to find: > ");
				int objectS = keyboard.nextInt();
				Object searchObject = new Object(objectN, objectS);
				System.out.println();
				searchForRecipe2(searchObject);	
			}
	

			else if (response.equals("7")){
				printAllNodes();
			}
			
			else if (response.equals("8")){
				printAllNodesReversed();
			}
			
			else if (response.equals("9")){
				printAllOneModeNodes();
				System.out.println("\n~\n");
				printAllOneModeNodesNoState();
				System.out.println("\n~\n");
				outputGraphDegree(filePath);
			}
	
			else if (response.equals("10")){
				for (FunctionalUnit T : FOON){
					T.printFunctionalUnitNoIngredients();
				}
			}
			
			
			else if (response.equals("11")){		
				for (FunctionalUnit T : FOON_containers){
					T.printFunctionalUnit();
				}
			}
			
			else break;
			
			System.out.println();
		}
		
		keyboard.close(); // closing the Scanner input
	
		System.out.print("\nTerminating program.");
		Thread.sleep(500);
		System.out.print(".");
		Thread.sleep(500);
		System.out.print(".");
		Thread.sleep(500);
		System.out.print(".");
		Thread.sleep(500);
		System.out.print(".");
		
	}
	
	public static String printMenuOptions(Scanner keyboard){
		// Printing all options for program functions:
		System.out.println("*********************************************************************************************");
		System.out.println("*************				MENU OPTIONS				*************");
		System.out.println("*********************************************************************************************");
		System.out.println("\t1.	Similarity test with WordNet?");
		System.out.println("\t2.	Perform centrality analysis?");
		System.out.println("\t3.	Count all nodes in the graph?");
		System.out.println("\t4.	Produce functional object-motion files?");
		System.out.println("\t5.	Perform merging of graphs?");
		System.out.println("\t6.	Search for recipe?");
		System.out.println("\t7.	Print all nodes?");
		System.out.println("\t8.	Print all nodes in REVERSE order?");
		System.out.println("\t9.	Print objects as one-mode projected graph?");
		System.out.println("\t10.	Print all functional units (not considering ingredients)?");
		System.out.println("\t11.	Print all functional units (considering ingredients)?");
		System.out.println("(Press any other key and ENTER to exit)");
		System.out.print("\nPlease enter your response here: > ");

		String response = keyboard.nextLine();
		return response;
	}

	private static int exploreNeighbours(int N){
		Thing temp = oneModeObject.get(N);
		int count = 0;
		if (temp.countNeighbours() == 0){
			return 0;
		}
		for (Thing T : temp.getNeigbourList()){
			count = (count++) + exploreNeighbours(oneModeObject.indexOf(T));
		}
		return count;
	}
	
	private static void populateLists() throws Exception {
		// Opens a dialog that will be used for opening the network file:
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle("FOON_analysis - Choose the OBJECT index file:");
		chooser.setAcceptAllFileFilterUsed(true);
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			System.out.println("Object Index found at location : " + chooser.getSelectedFile());
		} else {
			System.err.println("Error in getting path of file!");
			return;
		}
						
		@SuppressWarnings("resource")
		Scanner file = new Scanner(new File(chooser.getSelectedFile().getAbsolutePath()));
		
		while(file.hasNext()) {
			String line = file.nextLine();
			String[] parts = line.split("\t");
			objectList.add(parts[1]);
		}
		
		chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle("FOON_analysis - Choose the MOTION index file:");
		chooser.setAcceptAllFileFilterUsed(true);
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			System.out.println("Motion Index found at location : " + chooser.getSelectedFile());
		} else {
			System.err.println("Error in getting path of file!");
			return;
		}
						
		file = new Scanner(new File(chooser.getSelectedFile().getAbsolutePath()));
		
		while(file.hasNext()) {
			String line = file.nextLine();
			String[] parts = line.split("\t");
			motionList.add(parts[1]);
		}
		System.out.println();
	}

	private static void outputMergedGraph(String FP) throws Exception{
		// Preparing for output
		File outputFile = new File(FP);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));

		// Save the entire network to the  file
		System.out.println("Saving network to file..");
		String entireUnit = "";		
		// output everything as "containers"
		for (FunctionalUnit FU : FOON_containers) {
			entireUnit = entireUnit + (FU.getInputsForFile() + FU.getMotionForFile() + FU.getOutputsForFile() + "//\n");

			output.write(entireUnit);
			entireUnit = "";
		}
		System.out.println("File saved at " + FP);	
		output.close();
	}

	private static void populateFrequencyList() throws Exception {
		int total = 0, motions = 0, objects = 0, edges = 0;
		for (Thing T : nodes) {
			if (T instanceof Motion) {
				motionFrequency[T.getType()]++;
				motions++;
			}
			else if (T instanceof Object){
				objects++;
			}
			edges += T.countNeighbours();
			total++;
		}
		int maxIndex = 0;
		for (int x = 1; x < motionFrequency.length; x++){
			if (motionFrequency[maxIndex] < motionFrequency[x]) {
				maxIndex = x;
			}
		}
		System.out.println("There is a total of " + total + " nodes in FOON presently, with " + edges + " edges!" );
		System.out.println("	-> " + objects + " object nodes in FOON presently!" );
		System.out.println("	-> " + motions + " motion nodes in FOON presently!" );
		System.out.println("Most frequent motion found in FOON was M_" + maxIndex + ", with frequency of " + (double)motionFrequency[maxIndex]/motions * 1.0);

		String fileName = filePath.substring(0, filePath.length() - 4) + "_motion_frequency.txt";
		File outputFile = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));
		for (int x = 0; x < motionFrequency.length; x++) {
			output.write("M_" + x + " :\t " + motionFrequency[x] + " instances\n");
		}
		output.write("Total instances: " + motions);
		output.close();
	}

	private static void outputGraphDegree(String FP) throws Exception{
		// -- Considering objects but not looking at ingredients!
		String fileName = FP.substring(0, FP.length() - 4) + "_node_degrees_abstract.txt";
		File outputFile = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));

		// Save the entire network to the  file
		System.out.println("Saving node degrees to file..");
		String entireUnit = "";
		for (Thing FU : oneModeObjectAbstract) {
			entireUnit = (((Object)FU).getObject()).replace("\n", ", ") + " : " + FU.countNeighbours() + " degrees\n";
			output.write(entireUnit);
			entireUnit = "";
		}
		System.out.println("File saved at "+fileName);	
		output.close();

		// saving the connections of each object to its neighbouring objects
		fileName = FP.substring(0, FP.length() - 4) + "_edges_abstract.txt";
		outputFile = new File(fileName);
		output = new BufferedWriter(new FileWriter(outputFile));
		entireUnit = "";
		for (Thing FU : oneModeObjectAbstract) {
			entireUnit = "O" + FU.getType() + "S" + ((Object)FU).getObjectState();
			for (Thing N : FU.getNeigbourList()) {
				entireUnit += "\tO" + N.getType() + "S" + ((Object)N).getObjectState(); 
			}
			entireUnit += "\n";
			output.write(entireUnit);
			entireUnit = "";
		}
		output.close();
		
		// -- Outputting results while considering NO STATES!
		fileName = FP.substring(0, FP.length() - 4) + "_node_degrees_nil.txt";
		outputFile = new File(fileName);
		output = new BufferedWriter(new FileWriter(outputFile));

		// Save the entire network to the  file
		System.out.println("Saving node degrees to file..");
		entireUnit = "";
		for (Thing FU : oneModeObject) {
			entireUnit = FU.getType() + " : " + FU.countNeighbours() + " degrees\n";
			output.write(entireUnit);
			entireUnit = "";
		}
		System.out.println("File saved at "+fileName);	
		output.close();

		// saving the connections of each object to its neighbouring objects
		fileName = FP.substring(0, FP.length() - 4) + "_edges_nil.txt";
		outputFile = new File(fileName);
		output = new BufferedWriter(new FileWriter(outputFile));
		entireUnit = "";
		for (Thing FU : oneModeObject) {
			entireUnit = "O" + FU.getType();
			for (Thing N : FU.getNeigbourList()) {
				entireUnit += "\tO" + N.getType(); 
			}
			entireUnit += "\n";
			output.write(entireUnit);
			entireUnit = "";
		}
	
		output.close();
		
		// -- Outputting results while considering INGREDIENTS!
		fileName = FP.substring(0, FP.length() - 4) + "_node_degrees_ingredients.txt";
		outputFile = new File(fileName);
		output = new BufferedWriter(new FileWriter(outputFile));

		// Save the entire network to the  file
		System.out.println("Saving node degrees to file..");
		entireUnit = "";
		for (Thing FU : oneModeObjectIngredients) {
			entireUnit = (((Object)FU).getObject()).replace("\n", ", ") + " : " + FU.countNeighbours() + " degrees\n";
			output.write(entireUnit);
			entireUnit = "";
		}
		System.out.println("File saved at "+fileName);	
		output.close();

		// saving the connections of each object to its neighbouring objects
		fileName = FP.substring(0, FP.length() - 4) + "_edges_ingredients.txt";
		outputFile = new File(fileName);
		output = new BufferedWriter(new FileWriter(outputFile));
		entireUnit = "";
		for (Thing FU : oneModeObjectIngredients) {
			entireUnit = "O" + FU.getType() + "S" + ((Object)FU).getObjectState();
			for (Thing N : FU.getNeigbourList()) {
				entireUnit += "\tO" + N.getType() + "S" + ((Object)N).getObjectState(); 
			}
			entireUnit += "\n";
			output.write(entireUnit);
			entireUnit = "";
		}
		output.close();
	}

	private static void printAllNodes() throws Exception{
		System.out.println(totalNodes + " nodes found in graph!");
		String fileName = filePath.substring(0, filePath.length() - 4) + "_all_nodes.txt";
		File outputFile = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));
		String line = "";
		int count = 0;
		for (Thing n : nodes) {
			System.out.print("node "+ (++count) +" : ");
			line += "node "+ (count) +" : ";
			if (n instanceof Motion) {
				((Motion)n).printMotion();
				line += ((Motion)n).getMotion();
			}
			else {
				((Object)n).printObject();
				line += ((Object)n).getObject();
				System.out.println("Number of contained ingredients " +((Object)n).getIngredientsList().size());
			}
			System.out.println("Number of degrees: " + n.countNeighbours());
			line += "Number of degrees : " + n.countNeighbours();
			line += n.getNeighbours();
			output.write(line);
			line = "\n";
		}
		System.out.println("File saved at "+fileName);	
		output.close();
	}

	private static void printAllNodesReversed(){
		System.out.println(totalNodes + " nodes found in graph!");
		System.out.println(nodesReversed.size() + " nodes found in graph!");
		int count = 0;
		for (Thing n : nodesReversed) {
			System.out.print("node "+ (++count) +" : ");
			if (n instanceof Motion) {
				((Motion)n).printMotion();
			}
			else {
				((Object)n).printObject();
				System.out.println("Number of contained ingredients " +((Object)n).getIngredientsList().size());
			}
			n.printNeighbours();
		}
	}

	private static void printAllOneModeNodes(){
		System.out.println(oneModeObjectAbstract.size() + " nodes found in graph!");
		int count = 0;
		for (Thing n : oneModeObjectAbstract) {
			System.out.print("node "+ (++count) +" : ");
			// all of these nodes will be purely objects! No need to test.
			((Object)n).printObject(); 
			// Display the number of degrees for each node, which are the number of neighbours
			System.out.println("Number of degrees: " + n.countNeighbours());
			n.printNeighbours();
		}
	}

	private static void printAllOneModeNodesNoState(){
		System.out.println(oneModeObject.size() + " nodes found in graph!");
		int count = 0;
		for (Thing n : oneModeObject) {
			System.out.print("node "+ (++count) +" : ");
			// all of these nodes will be purely objects! No need to test.
			n.printThing(); 
			// Display the number of degrees for each node, which are the number of neighbours
			System.out.println("Number of degrees: " + n.countNeighbours());
			n.printNeighbours();
		}
	}

	private static void populateAdjacencyMatrix() {
		for (int x = 0; x < oneModeObjectAbstract.size(); x++) {
			oneModeObjectAbstractMatrix[x][x] = 1;
			for (Thing T : oneModeObjectAbstract.get(x).getNeigbourList()){
				int toEdge = oneModeObjectAbstract.indexOf(T);
				oneModeObjectAbstractMatrix[x][toEdge] = 1;
			}
		}

		for (int x = 0; x < oneModeObject.size(); x++) {
			oneModeObjectMatrix[x][x] = 1;
			for (Thing T : oneModeObject.get(x).getNeigbourList()){
				int toEdge = oneModeObject.indexOf(T);
				oneModeObjectMatrix[x][toEdge] = 1;
			}
		}

		for (int x = 0; x < oneModeObjectIngredients.size(); x++) {
			oneModeObjectIngredientsMatrix[x][x] = 1;
			for (Thing T : oneModeObjectIngredients.get(x).getNeigbourList()){
				int toEdge = oneModeObjectIngredients.indexOf(T);
				oneModeObjectIngredientsMatrix[x][toEdge] = 1;
			}
		}
	}

	private static void getObjectMotions() throws Exception{
		for (Thing T : nodes) {
			if (T instanceof Object) {
				Thing tempObject; int found = -1; 
				for (Thing N : functionalMotions) {
					if (N.equals(T)){
						found = functionalMotions.indexOf(N);
					}
				}
				if (found == -1){
					tempObject = new Thing(T.getType());
					functionalMotions.add(tempObject);
				}
				else {
					tempObject = functionalMotions.get(found);
				}
				for (Thing t : T.getNeigbourList()){
					found = -1;	
					for (Thing n : tempObject.getNeigbourList()) {
						if (n.equals(t)){
							found++;
						}
					}
					if (found == -1){
						tempObject.addConnection(new Thing(t.getType()));
					}
				}
			}
		}

		String fileName = filePath.substring(0, filePath.length() - 4) + "_functional_analysis.txt";
		File outputFile = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));

		String entireUnit = "";
		for (Thing O : functionalMotions) {
			entireUnit += "O" + O.getType() + "\t:";
			for (Thing M : O.getNeigbourList()){
				entireUnit += "\tM" + M.getType();
			}
			entireUnit += "\n";
			output.write(entireUnit);
			entireUnit = "";
		}
		System.out.println("File saved at "+fileName);	
		output.close();
	}

	// Taken from website : http://www.markhneedham.com/blog/2013/08/05/javajblas-calculating-eigenvector-centrality-of-an-adjacency-matrix/
	private static List<Double> normalised(List<Double> principalEigenvector) {
		double total = sum(principalEigenvector);
		List<Double> normalisedValues = new ArrayList<Double>();
		for (Double aDouble : principalEigenvector) {
			normalisedValues.add(aDouble / total);
		}
		return normalisedValues;
	}

	private static double sum(List<Double> principalEigenvector) {
		double total = 0;
		for (Double aDouble : principalEigenvector) {
			total += aDouble;
		}
		return total;
	}

	private static List<Double> getPrincipalEigenvector(DoubleMatrix matrix){
		int maxIndex = getMaxIndex(matrix);
		ComplexDoubleMatrix eigenvectors = Eigen.eigenvectors(matrix)[0];
		return getEigenVector(eigenvectors, maxIndex);
	}

	private static int getMaxIndex(DoubleMatrix matrix) {
		ComplexDouble[] doubleMatrix = Eigen.eigenvalues(matrix).toArray();
		int maxIndex = 0;
		for (int i = 0; i < doubleMatrix.length; i++){
			double newnumber = doubleMatrix[i].abs();
			if ((newnumber > doubleMatrix[maxIndex].abs())){
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	private static List<Double> getEigenVector(ComplexDoubleMatrix eigenvector, int columnId) {
		ComplexDoubleMatrix column = eigenvector.getColumn(columnId);

		List<Double> values = new ArrayList<Double>();
		for (ComplexDouble value : column.toArray()) {
			values.add(value.abs()  );
		}
		return values;
	}

	// Recursive DepthFirstSearch starting at vertex on graph.
	//   https://github.com/kfr2/java-algorithms/blob/master/algorithms_2/DepthFirstSearchRecursionMatrix.java
	public static void depthFirstSearch(int[][] graph, int vertex)
	{
		// Mark the vertex as visited.
		visited[vertex] = true;
		//System.out.println(vertex + " has been visited.");
		Thing temp = nodes.get(vertex);
		if (temp instanceof Object){
			((Object) temp).printObject();
		}
		else {
			((Motion) temp).printMotion();
		}
		// Push each node onto a stack for easy backtracking
		tempStack.push(temp);

		// Examine the graph table to determine which node to examine next.
		for(int i = 0; i < totalNodes; i++)
		{
			// If the node is adjacent to the current (and has not been visited), run DFS on it.
			if((graph[vertex][i] == 1) && (!visited[i]))
			{
				depthFirstSearch(graph, i);
			}
		}
	} // end depthFirstSearch

	// Breadth First Search starting at vertex on graph.
	public static void breadthFirstSearch(int[][] graph, int vertex)
	{
		// Store the node in a queue.
		Queue<Integer> queue = new LinkedList<Integer>();
		queue.add(vertex);

		// Mark the node as visited.
		//if (!(nodes.get(vertex) instanceof Motion)){
		visited[vertex] = true;
		//}
		// Examine all nodes in the queue.
		while(!queue.isEmpty())
		{
			vertex = queue.remove();
			//System.out.println(vertex + " has been visited.");

			Thing temp = nodes.get(vertex);
			if (temp instanceof Object){
				((Object) temp).printObject();
			}
			else {
				((Motion) temp).printMotion();
			}

			// Examine the graph table to determine which node to examine next.
			for(int i = 0; i < graph[vertex].length; i++)
			{
				// If the node is adjacent to the current (and has not been visited), add it to the queue.
				if((graph[vertex][i] == 1) && (!visited[i]))
				{
					visited[i] = true;
					queue.add(i);
				}
			}
		}
	}

	private static boolean FUExists(FunctionalUnit U, int A){
		// Four (4) cases:
		//	-- the first two check if a functional unit is equal based on the ABSTRACT method (not accounting all ingredients)
		if (A == 1){
			if (FOON.isEmpty()){
				return false;
			}
			for(FunctionalUnit F : FOON){
				if (F.equals(U)){
					System.out.println("Functional unit (no ingredients) already exists in FOON!");
					U.printFunctionalUnit();
					return true;
				}
			}
			return false;			
		}
		else if (A == 2){
			if (reverseFOON.isEmpty()){
				return false;
			}
			for(FunctionalUnit F : reverseFOON){
				if (F.equals(U)){
					System.out.println("Functional unit already exists in reversed FOON!");
					U.printFunctionalUnit();
					return true;
				}
			}
			return false;	
		}
		//		-- the latter two check if functional units are equal down to the ingredient-level
		else if (A == -1){
			if (FOON_containers.isEmpty()){
				return false;
			}
			for(FunctionalUnit F : FOON_containers){
				if (F.equalsWithIngredients(U)){
					System.out.println("Functional unit (with containers) already exists in FOON!");
					U.printFunctionalUnit();
					return true;
				}
			}
			return false;			
		}
		else {
			if (reverseFOON_containers.isEmpty()){
				return false;
			}
			for(FunctionalUnit F : reverseFOON_containers){
				if (F.equalsWithIngredients(U)){
					System.out.println("Functional unit (with containers) already exists in FOON!");
					U.printFunctionalUnit();
					return true;
				}
			}
			return false;	
		}

	}

	private static int constructFUGraph(Scanner readFile) throws Exception {
		int count = totalNodes; // we have an idea of how many objects may be in the graph by the number of lines
		String[] stateParts, objectParts, motionParts; // objects used to contain the split strings

		// Temporary objects to hold a new object/motion
		Object newObject; Motion newMotion; 
		int objectIndex = -1; // variables to hold position of object/motion within list of Things				
		boolean isInput = true;

		FunctionalUnit newFU = new FunctionalUnit(); // this is to hold reverse edges

		while (readFile.hasNext()) {
			String line = readFile.nextLine();
			int objectExisting = -1;
			if (line.startsWith("//")) {
				// we are adding a new FU, so start from scratch
				if (!FUExists(newFU,1)){
					FOON.add(newFU); // only add the Functional Unit if it is not in the list
					// if this functional unit does not exist, then the reverse should not exist either!
					makeReverseFU(newFU);
					addOneModeProjection(newFU);
					addOneModeAbstract(newFU);
					addOneModeIngredients(newFU);
				}				
				if (!FUExists(newFU,-1)){
					FOON_containers.add(newFU); // only add the Functional Unit if it is not in the list
					// if this functional unit does not exist, then the reverse should not exist either!
					makeReverseFU_Container(newFU);
				}
				newFU = new FunctionalUnit(); // create an entirely new FU
				isInput = true; // this is the end of a FU so we will now be adding input nodes
			} else if (line.startsWith("O")) {
				// this is an Object node, so we probably should read the next line one time
				objectParts = line.split("O", 2); // get the Object identifier by splitting first instance of O
				objectParts = objectParts[1].split("\t");

				// read the next line containing the Object state information
				line = readFile.nextLine();
				stateParts = line.split("S", 2); // get the Object's state identifier by splitting first instance of S
				stateParts = stateParts[1].split("\t");

				// create new Object node
				newObject = new Object(Integer.parseInt(objectParts[0]), Integer.parseInt(stateParts[0]), objectParts[1], stateParts[1]);

				// checking if this object is a container:
				if (stateParts.length > 2){
					String [] ingredients = { stateParts[2] }, temp;
					ingredients = ingredients[0].split("\\{");
					ingredients = ingredients[1].split("\\}");
					ingredients = ingredients[0].split(",");
					for (String I : ingredients){
						temp = I.split("O");
						newObject.setIngredient(Integer.parseInt(temp[1]));
					}
				}
				// checking if Object node exists in the list of objects
				for (Thing n : nodes) {
					if (n instanceof Object && ((Object)n).equalsWithIngredients(newObject)){
						objectExisting = nodes.indexOf(n);
					}
				}

				// Check if object already exists within the list so as to avoid duplicates
				if (objectExisting != -1){
					objectIndex = objectExisting;
				}
				else {
					// just add new object to the list of all nodes
					nodes.add(newObject);
					objectIndex = count++;
				}

				if (isInput){
					// this Object will be an input node to the FU
					newFU.addObjectNode(nodes.get(objectIndex), FunctionalUnit.nodeType.Input, Integer.parseInt(objectParts[2]));
				} else {
					// add the Objects as output nodes to the Functional Unit
					newFU.addObjectNode(nodes.get(objectIndex), FunctionalUnit.nodeType.Output, Integer.parseInt(objectParts[2]));
					newFU.getMotion().addConnection(newObject); // make the connection from Motion to Object
				}

			} else {
				// We are adding a Motion node, so very easy to deal with
				isInput = false;
				motionParts = line.split("M", 2); // get the Motion number
				motionParts = motionParts[1].split("\t"); // get the Motion label

				// create new Motion based on what was read
				newMotion = new Motion(Integer.parseInt(motionParts[0]), motionParts[1]+"\t"+motionParts[2]+"\t"+motionParts[3]);
				nodes.add(newMotion);
				count++; // increment number of nodes by one since we are adding a new Motion node

				newFU.setMotion(newMotion);
				for (Thing T : newFU.getInputList()){
					T.addConnection(newMotion); // make the connection from Object(s) to Motion
				}
			}
		}
		readFile.close();
		return count;
	}
	
	private static void makeReverseFU(FunctionalUnit newFU){
		FunctionalUnit reverseFU = new FunctionalUnit();
		Motion tempMotion = new Motion(((Motion)newFU.getMotion()).getMotionType());
		tempMotion.setLabel(((Motion)newFU.getMotion()).getLabel());
		// the typical FOON graph goes from the start to the goal;
		//	for the searching, we need to go from the goal to the starting nodes.
		int track = 0;
		for (Thing T : newFU.getOutputList()) {
			Object tempObject; int found = -1;
			for (Thing N : nodesReversed){
				if (N instanceof Object && ((Object)N).equals((Object)T)){
					found = nodesReversed.indexOf(N);
				}
			}
			if (found == -1 && T instanceof Object){
				tempObject = new Object(((Object)T).getObjectType(), ((Object)T).getObjectState());
				tempObject.setLabel(T.getLabel());
				tempObject.setStateLabel(((Object)T).getStateLabel());
				tempObject.setIngredientsList(((Object)T).getIngredientsList());
				nodesReversed.add(tempObject);
			} else {
				tempObject = (Object) nodesReversed.get(found);
			}
			tempObject.addConnection(tempMotion);
			reverseFU.addObjectNode(tempObject, FunctionalUnit.nodeType.Input, newFU.getOutputDescriptor().get(track++));
		}
		reverseFU.setMotion(tempMotion);
		nodesReversed.add(tempMotion);
		track = 0;
		// the input nodes in the regular Functional Unit will be OUTPUT in reverse
		for (Thing T : newFU.getInputList()) {
			Object tempObject; int found = -1;
			for (Thing N : nodesReversed){
				if (N instanceof Object && (N).equals((Object)T)){
					found = nodesReversed.indexOf(N);
				}
			}
			if (found == -1){
				tempObject = new Object(((Object)T).getObjectType(), ((Object)T).getObjectState());
				tempObject.setLabel(T.getLabel());
				tempObject.setStateLabel(((Object)T).getStateLabel());
				tempObject.setIngredientsList(((Object)T).getIngredientsList());
				nodesReversed.add(tempObject);
			} else {
				tempObject = (Object) nodesReversed.get(found);
			}
			tempMotion.addConnection(tempObject);
			reverseFU.addObjectNode(tempObject, FunctionalUnit.nodeType.Output, newFU.getInputDescriptor().get(track++));
		}

		reverseFOON.add(reverseFU);
	}

	private static void makeReverseFU_Container(FunctionalUnit newFU){
		FunctionalUnit reverseFU = new FunctionalUnit();
		// if this functional unit does not exist, then the reverse should not exist either!
		Motion tempMotion = new Motion(((Motion)newFU.getMotion()).getMotionType());
		tempMotion.setLabel(((Motion)newFU.getMotion()).getLabel());
		// the typical FOON graph goes from the start to the goal;
		//	for the searching, we need to go from the goal to the starting nodes.
		int track = 0;
		for (Thing T : newFU.getOutputList()) {
			Object tempObject; int found = -1;
			for (Thing N : nodesReversed){
				if (N instanceof Object && ((Object)N).equalsWithIngredients((Object)T)){
					found = nodesReversed.indexOf(N);
				}
			}
			if (found == -1 && T instanceof Object){
				tempObject = new Object(((Object)T).getObjectType(), ((Object)T).getObjectState());
				tempObject.setLabel(T.getLabel());
				tempObject.setStateLabel(((Object)T).getStateLabel());				
				tempObject.setIngredientsList(((Object)T).getIngredientsList());
				nodesReversed.add(tempObject);
			} else {
				tempObject = (Object) nodesReversed.get(found);
			}
			tempObject.addConnection(tempMotion);
			reverseFU.addObjectNode(tempObject, FunctionalUnit.nodeType.Input, newFU.getOutputDescriptor().get(track++));
		}
		reverseFU.setMotion(tempMotion);
		nodesReversed.add(tempMotion);
		track = 0;
		// the input nodes in the regular Functional Unit will be OUTPUT in reverse
		for (Thing T : newFU.getInputList()) {
			Object tempObject; int found = -1;
			for (Thing N : nodesReversed){
				if (N instanceof Object && ((Object)N).equalsWithIngredients((Object)T)){
					found = nodesReversed.indexOf(N);
				}
			}
			if (found == -1){
				tempObject = new Object(((Object)T).getObjectType(), ((Object)T).getObjectState());
				tempObject.setLabel(T.getLabel());
				tempObject.setStateLabel(((Object)T).getStateLabel());
				tempObject.setIngredientsList(((Object)T).getIngredientsList());
				nodesReversed.add(tempObject);
			} else {
				tempObject = (Object) nodesReversed.get(found);
			}
			tempMotion.addConnection(tempObject);
			reverseFU.addObjectNode(tempObject, FunctionalUnit.nodeType.Output, newFU.getInputDescriptor().get(track++));
		}

		reverseFOON_containers.add(reverseFU);
	}


	private static void addOneModeProjection(FunctionalUnit newFU){
		ArrayList<Thing> tempList = new ArrayList<Thing>();

		// creating one-mode projection: take the input first and then the output nodes.
		for (Thing T : newFU.getInputList()) {
			Thing tempObject; int found = -1;
			for (Thing N : oneModeObject){
				if (N.equals(T)){
					found = oneModeObject.indexOf(N);
				}
			}
			if (found == -1){
				tempObject = new Thing(((Object)T).getObjectType());
				tempObject.setLabel(T.getLabel());
				oneModeObject.add(tempObject);
			} else {
				tempObject = oneModeObject.get(found);
			}
			tempList.add(tempObject);
		}

		for (Thing T : newFU.getOutputList()) {
			Thing tempObject; int found = -1;
			for (Thing N : oneModeObject){
				if (N.equals(T)){
					found = oneModeObject.indexOf(N);
				}
			}
			if (found == -1){
				tempObject = new Thing(((Object)T).getObjectType());
				tempObject.setLabel(T.getLabel());
				oneModeObject.add(tempObject);
			} else {
				tempObject = oneModeObject.get(found);
			}

			for (Thing N : tempList) {
				found = -1;
				for (Thing M : N.getNeigbourList()) {
					if (tempObject.equals(M)){
						found = N.getNeigbourList().indexOf(M);
					}	
				}
				if (found == -1) {
					N.addConnection(tempObject);
				}
			}
		}
	}	
	

	private static void addOneModeAbstract(FunctionalUnit newFU){
		ArrayList<Thing> tempList = new ArrayList<Thing>();
		// creating one-mode projection: take the input first and then the output nodes.
		for (Thing T : newFU.getInputList()) {
			Object tempObject; int found = -1;
			for (Thing N : oneModeObjectAbstract){
				if (N instanceof Object && ((Object)T).equals((Object)N)){
					found = oneModeObjectAbstract.indexOf(N);
				}
			}
			if (found == -1){
				tempObject = new Object(((Object)T).getObjectType(), ((Object)T).getObjectState());
				tempObject.setLabel(T.getLabel());
				tempObject.setStateLabel(((Object)T).getStateLabel());
				oneModeObjectAbstract.add(tempObject);
			} else {
				tempObject = (Object) oneModeObjectAbstract.get(found);
			}
			tempList.add(tempObject);
		}

		for (Thing T : newFU.getOutputList()) {
			Object tempObject; int found = -1;
			for (Thing N : oneModeObjectAbstract){
				if (N instanceof Object && ((Object)T).equals((Object)N)){
					found = oneModeObjectAbstract.indexOf(N);
				}
			}
			if (found == -1){
				tempObject = new Object(((Object) T).getObjectType(), ((Object)T).getObjectState());
				tempObject.setLabel(T.getLabel());
				tempObject.setStateLabel(((Object)T).getStateLabel());
				oneModeObjectAbstract.add(tempObject);
			} else {
				tempObject = (Object) oneModeObjectAbstract.get(found);
			}

			for (Thing N : tempList) {
				N.addConnection(tempObject);
			}
		}	
	}
	

	private static void addOneModeIngredients(FunctionalUnit newFU){
		ArrayList<Thing> tempList = new ArrayList<Thing>();
		// creating one-mode projection: take the input first and then the output nodes.
		for (Thing T : newFU.getInputList()) {
			Object tempObject; int found = -1;
			for (Thing N : oneModeObjectIngredients){
				if (N instanceof Object && ((Object)T).equalsWithIngredients((Object)N)){
					found = oneModeObjectIngredients.indexOf(N);
				}
			}
			if (found == -1){
				tempObject = new Object(((Object)T).getObjectType(), ((Object)T).getObjectState());
				tempObject.setLabel(T.getLabel());
				tempObject.setStateLabel(((Object)T).getStateLabel());
				tempObject.setIngredientsList(((Object)T).getIngredientsList());
				oneModeObjectIngredients.add(tempObject);
			} else {
				tempObject = (Object) oneModeObjectIngredients.get(found);
			}
			tempList.add(tempObject);
		}

		for (Thing T : newFU.getOutputList()) {
			Object tempObject; int found = -1;
			for (Thing N : oneModeObjectIngredients){
				if (N instanceof Object && ((Object)T).equalsWithIngredients((Object)N)){
					found = oneModeObjectIngredients.indexOf(N);
				}
			}
			if (found == -1){
				tempObject = new Object(((Object) T).getObjectType(), ((Object)T).getObjectState());
				tempObject.setLabel(T.getLabel());
				tempObject.setStateLabel(((Object)T).getStateLabel());
				tempObject.setIngredientsList(((Object)T).getIngredientsList());
				oneModeObjectIngredients.add(tempObject);
			} else {
				tempObject = (Object) oneModeObjectIngredients.get(found);
			}

			for (Thing N : tempList) {
				N.addConnection(tempObject);
			}
		}	

	}
	
	private static void searchForRecipe(Object O) {
		Queue<Thing> itemsToSearch = new LinkedList<Thing>(); // Queue structure needed for BFS
		int index = -1; 
		// searching for the object in the FOON
		for (Thing T : nodesReversed) {
			if (T instanceof Object && O.equals((Object)T)){
				index = nodesReversed.indexOf(T);
			}
		}

		// checking to see if the item has been found in FOON
		if (index == -1) {
			System.out.println("Item O" + O.getObjectType() + "_S" + O.getObjectState() + " has not been found in network!");
			return;
		}

		int start = index; //start at the goal node's index
		boolean[] isVisited = new boolean[nodesReversed.size()]; // this is a structure to keep track of all visited nodes;
		//  the values of the boolean array will be FALSE by default.
		Scanner keyboard = new Scanner(System.in); // checking for user's input
		String response = "";

		itemsToSearch.add(nodesReversed.get(start));

		while(!itemsToSearch.isEmpty()) {
			Thing tempObject = itemsToSearch.remove(); // remove the item we are trying to make from the list        	
			// Just a test for printing an appropriate message for each item!
			if (tempObject.getNeigbourList().size() > 0) {
				System.out.println("To get item O" + ((Object)tempObject).getObjectType() + "_S" + ((Object)tempObject).getObjectState() + ", you will need: ");
			} else {
				System.out.println("Item O" + ((Object)tempObject).getObjectType() + "_S" + ((Object)tempObject).getObjectState() + " cannot be reduced any further!");
				System.out.println("You will need to acquire it somehow!");
			}

			int count = 0; // counter to list the ingredients needed to make a certain object

			for (Thing M : tempObject.getNeigbourList()) {
				for (Thing Ob : M.getNeigbourList()) {
					for (Thing T : nodesReversed) {
						if (T instanceof Object && ((Object)T).equals(Ob)){
							index = nodesReversed.indexOf(T);
						}
					}
					if (isVisited[index] == false) {
						System.out.println("\t" + (++count) + ".	O" + ((Object)Ob).getObjectType() + "_S" + ((Object)Ob).getObjectState());
						System.out.print("\t - Do you have object O" + ((Object)Ob).getObjectType() + "_S" + ((Object)Ob).getObjectState() + "[" + ((Object)Ob).getLabel() + " (" + ((Object)Ob).getStateLabel() + ")]? (Y/N) > ");
						response = keyboard.nextLine();
						if (response.equals("N")){
							System.out.print("\t - now searching for how to make item ");
							((Object)Ob).printObject();
							itemsToSearch.add(Ob); // if we do not know how to make the item, then we need to backtrack further!
						}
						isVisited[index] = true;
						System.out.println();
					}
				}
			}
		}

		keyboard.close();
		return;
	}

	private static void searchForRecipe2(Object O) {
		Queue<Thing> itemsToSearch = new LinkedList<Thing>(); // Queue structure needed for BFS
		int index = -1; 
		// searching for the object in the FOON
		for (Thing T : nodes) {
			if (T instanceof Object && O.equals((Object)T)){
				index = nodes.indexOf(T);
			}
		}

		// checking to see if the item has been found in FOON
		if (index == -1) {
			System.out.println("Item O" + O.getObjectType() + "_S" + O.getObjectState() + " has not been found in network!");
			return;
		}

		int start = index; //start at the goal node's index
		boolean[] isVisited = new boolean[FOON.size()]; // this is a structure to keep track of all visited nodes;
		//  the values of the boolean array will be FALSE by default.
		boolean[] itemsV = new boolean[nodes.size()];
		Scanner keyboard = new Scanner(System.in); // checking for user's input
		String response = "";

		itemsToSearch.add(nodes.get(start));
		Stack<FunctionalUnit> FUtoSearch = new Stack<FunctionalUnit>(); // Queue structure needed for BFS
		ArrayList<FunctionalUnit> tree = new ArrayList<FunctionalUnit>();        

		while(!itemsToSearch.isEmpty()) {
			Thing tempObject = itemsToSearch.remove(); // remove the item we are trying to make from the list        	
			// Just a test for printing an appropriate message for each item!

			boolean flag = false;
			for (FunctionalUnit FU : FOON){
				if (FU.getOutputList().contains(tempObject)){
					FUtoSearch.push(FU);
					flag = true;
				}
			}

			if (flag) {
				System.out.println("To get item O" + ((Object)tempObject).getObjectType() + "_S" + ((Object)tempObject).getObjectState() + ", you will need: ");
			} else {
				System.out.println("Item O" + ((Object)tempObject).getObjectType() + "_S" + ((Object)tempObject).getObjectState() + " cannot be reduced any further!");
				System.out.println("You will need to acquire it somehow!");
			}

			int count = 0; // counter to list the ingredients needed to make a certain object  		

			while (!FUtoSearch.isEmpty()){
				FunctionalUnit tempFU = FUtoSearch.pop();
				int itemsNeeded = 0;
				if (isVisited[FOON.indexOf(tempFU)] == false){
					for (Thing I : tempFU.getInputList()){
						System.out.println("\t" + (++count) + ".	O" + ((Object)I).getObjectType() + "_S" + ((Object)I).getObjectState());
						System.out.print("\t - Do you have object O" + ((Object)I).getObjectType() + "_S" + ((Object)I).getObjectState() + "[" 
								+ ((Object)I).getLabel() + " (" + ((Object)I).getStateLabel() + ")]? (Y/N) > ");
						response = keyboard.nextLine();
						if (response.equals("N")){
							System.out.print("\t - now searching for how to make item ");
							((Object)I).printObject();
							if (itemsV[nodes.indexOf(I)] == false){
								itemsToSearch.add(I); // if we do not know how to make the item, then we need to backtrack further!
								itemsV[nodes.indexOf(I)] = true;
							}
						}
						else {
							itemsNeeded++;
						}
						System.out.println();
					}
					isVisited[FOON.indexOf(tempFU)] = !(isVisited[FOON.indexOf(tempFU)]);
				}
				if (itemsNeeded == tempFU.getNumberOfInputs()){
					System.out.println("All items found in this unit!");
				}
				if (tree.size() == 0){
					tree.add(tempFU);	        		
				}
				else {
					int found = -1;
					for (FunctionalUnit F : tree){
						if (F.equals(tempFU) ){
							found++;
						}
					}
					if (found == -1){
						tree.add(tempFU);
					}
				}
			}
		}        
		for (FunctionalUnit FU : tree){
			FU.printFunctionalUnit();
			System.out.println("\n//");
		}

		keyboard.close();
		return;
	}
}