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
	private static ArrayList<String> stateList;
	private static int[] objectsSeen;
	
	private static ArrayList<Thing> nodes_Ingredients, nodesReversed; // dynamic list of all objects/motions observed in file; keeps track of matrix rows and columns 
	private static ArrayList<Thing> nodes_abstract, nodes;
	private static int totalNodes = 0; // total number of nodes that are in the network

	private static ArrayList<Thing> oneModeObject; // one-mode projection of only object nodes, not accounting states!
	private static ArrayList<Thing> oneModeObjectAbstract; // one-mode projection of only object nodes, accounting for abstract state
	private static ArrayList<Thing> oneModeObjectIngredients; // one-mode projection of only object nodes, accounting for uniqueness of ingredients.
	private static ArrayList<Thing> functionalMotions; 
	static int[] distances;

	static boolean[] visited;

	private static int[] motionFrequency; // array to count the number of instances of each motion in a graph

	static ArrayList<String> file; 
	private static ArrayList<FunctionalUnit> FOON;
	private static ArrayList<FunctionalUnit> FOON_abstract; // FOON - list for object-state 
	private static ArrayList<FunctionalUnit> FOON_Ingredients; //  -- this will be the list of FUs taking into account ingredients for uniqueness
	
	// for backtracking/branch-and-bound algorithm
	private static ArrayList<FunctionalUnit> reverseFOON; // list of all Functional Units in the network but edges are in REVERSE
	private static ArrayList<FunctionalUnit> reverseFOON_containers; // list of all Functional Units in the network but edges are in REVERSE

	// adjacency matrix of all objects
	private static double[][] oneModeObjectMatrix;
	private static double[][] oneModeObjectAbstractMatrix;
	private static double[][] oneModeObjectIngredientsMatrix;

	// Testing stack for backtracking purposes
	static Stack<Thing> backtrack, tempStack;

	static Scanner keyboard;	
	
	public static void main(String[] args) throws Exception {
		// Initialize the ArrayList objects
		nodes = new ArrayList<Thing>();  
		nodes_abstract = new ArrayList<Thing>();  
		nodes_Ingredients = new ArrayList<Thing>();  

		FOON = new ArrayList<FunctionalUnit>();  
		FOON_abstract = new ArrayList<FunctionalUnit>();  
		FOON_Ingredients = new ArrayList<FunctionalUnit>();  

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
		chooser.setFileHidingEnabled(true);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			System.out.println("Main network file selected : " + chooser.getSelectedFile());
		} else {
			System.err.println("Error in getting path of file!");
			return;
		}
		
		filePath = chooser.getSelectedFile().getAbsolutePath();
		// Populate the adjacency matrices and record number of UNIQUE nodes
		totalNodes = constructFUGraph(new Scanner(new File(filePath)));
		
		objectList = new ArrayList<String>();
		stateList = new ArrayList<String>();
		motionList = new ArrayList<String>();

		System.out.println("\n*********************************************************************************************");
		System.out.println("\n			FOON Analysis + Graph Merging Program (revised 11/5/2016)\n");
		System.out.println("*********************************************************************************************");
		
		populateLists();
		averageMotionTimes();

		// -- list used to print the objects actually existent in FOON 
		objectsSeen = new int[objectList.size()];
		checkObjectsExist();
		
		// objects used for taking input from the console:
		String response;
		keyboard = new Scanner(System.in);

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
		
					double alpha = 1 / (largest + 0.25); // recommended that it is less than 1/K^1
					// vectors and matrix needed for Katz computation
					DoubleMatrix B = DoubleMatrix.ones(oneModeObjectAbstract.size(), 1), // vector with elements of 1, 
							I = DoubleMatrix.eye(oneModeObjectAbstract.size()); // identity matrix
					DoubleMatrix A = (I.sub((OMOmatrix.transpose().mul(alpha)))); 
					DoubleMatrix ans = Solve.solve(A, B); // as per 7.10 - Ax = B

					System.out.println("~");
					int count = 0, maxIndex = 0;;
					for (double D : ans.toArray()) {
						if (D > ans.toArray()[maxIndex]) {
							maxIndex = count;
						}
						count++;
						//System.out.println("O" + oneModeObjectAbstract.get(count).getType() + "S" + ((Object)oneModeObjectAbstract.get(count++)).getObjectState() + "\t" + String.format("%.5f ", (D)));
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
					double alpha = 1 / (largest + 0.25); // recommended that it is less than 1/K^1
					// vectors and matrix needed for Katz computation
					DoubleMatrix B = DoubleMatrix.ones(oneModeObject.size(), 1), // vector with elements of 1, 
							I = DoubleMatrix.eye(oneModeObject.size()); // identity matrix
					DoubleMatrix A = (I.sub((OMOmatrix.transpose().mul(alpha)))); 
					DoubleMatrix ans = Solve.solve(A, B); // as per 7.10 - Ax = B
	
					// old way with pseudo-inverse: DoubleMatrix ans = Solve.pinv((I.sub((OMOmatrix.transpose().mul(alpha))))).mmul(onesVector); // as per 7.10
					System.out.println("~");
					int count = 0, maxIndex = 0;;
					for (double D : ans.toArray()) {
						if (D > ans.toArray()[maxIndex]) {
							maxIndex = count;
						}
						System.out.println("O" + oneModeObject.get(count).getType() + "\t" + String.format("%.5f ", (D)));
						count++;
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
		
					double alpha = 1 / (largest + 0.25); // recommended that it is less than 1/K^1
					// vectors and matrix needed for Katz computation
					DoubleMatrix B = DoubleMatrix.ones(oneModeObjectIngredients.size(), 1), // vector with elements of 1, 
							I = DoubleMatrix.eye(oneModeObjectIngredients.size()); // identity matrix
					DoubleMatrix A = (I.sub((OMOmatrix.transpose().mul(alpha)))); 
					DoubleMatrix ans = Solve.solve(A, B); // as per 7.10 - Ax = B
					
					System.out.println("~");
					int count = 0, maxIndex = 0;;
					for (double D : ans.toArray()) {
						if (D > ans.toArray()[maxIndex]) {
							maxIndex = count;
						}
						count++;
						//System.out.println("O" + oneModeObjectIngredients.get(count).getType() + "S" + ((Object)oneModeObjectIngredients.get(count++)).getObjectState() + "\t" + String.format("%.5f ", (D)));
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
				int objectCount = 0, motionCount = 0, edgeCount = 0;
				System.out.println("\nBIPARTITE FOON (No State):");
				System.out.println(" -> Number of object nodes: "  + oneModeObject.size());
				System.out.println(" -> Number of motion nodes: "  + FOON.size());

				System.out.println("\n~\n");
				
				System.out.println("\nBIPARTITE FOON (No Ingredients):");
				System.out.println(" -> Number of object nodes: "  + oneModeObjectAbstract.size());
				System.out.println(" -> Number of motion nodes: "  + FOON_abstract.size());

				System.out.println("\n~\n");
				
				System.out.println("\nBIPARTITE FOON (Containers):");
				System.out.println(totalNodes + " nodes found in entire graph!");
				// 	-- Count all nodes in the graph?
				for (Thing T : nodes_Ingredients) {
					if (T instanceof Object){
						objectCount++;
					} else {
						motionCount++;
					}
					// we are iterating through all nodes, so why not just save time?
					//	-- counting the number of outgoing edges for each node.
					edgeCount += T.countNeighbours();
				}
				System.out.println(" -> Number of object nodes: " + objectCount);
				System.out.println(" -> Number of motion nodes: "  + FOON_Ingredients.size());
				System.out.println(" -> Number of motion nodes: " + motionCount);
				System.out.println(" -> Total number of nodes: " + nodes_Ingredients.size());
				System.out.println(" -> Total number of edges in network: " + edgeCount);
				
				System.out.println("\n~\n");

				System.out.println("\nONE-MODE PROJECTED FOON:");
				System.out.println(" -> Size of object-motion-content list: " + oneModeObjectIngredients.size() 
						+ "\n -> Size of no-state list: " + oneModeObjectAbstract.size() 
						+ "\n -> Size of object-state list: " + oneModeObject.size());

				
				for (Thing T : nodes_Ingredients) {
					if (T instanceof Object){
						objectCount++;
					} else {
						motionCount++;
					}
					// we are iterating through all nodes, so why not just save time?
					//	-- counting the number of outgoing edges for each node.
					edgeCount += T.countNeighbours();
				}

				objectCount = 0; motionCount = 0; edgeCount = 0;
				for (Thing T : nodes_abstract) {
					if (T instanceof Object){
						objectCount++;
					} else {
						motionCount++;
					}
					// we are iterating through all nodes, so why not just save time?
					//	-- counting the number of outgoing edges for each node.
					edgeCount += T.countNeighbours();
				}

				System.out.println(" -> Number of object nodes: " + objectCount);
				System.out.println(" -> Number of motion nodes: " + motionCount);
				System.out.println(" -> Number of motion nodes: "  + FOON_abstract.size());
				System.out.println(" -> Total number of nodes: " + nodes_abstract.size());
				System.out.println(" -> Total number of edges in network: " + edgeCount);

				objectCount = 0; motionCount = 0; edgeCount = 0;
				for (Thing T : nodes) {
					if (!(T instanceof Motion)){
						objectCount++;
					} else {
						motionCount++;
					}
					// we are iterating through all nodes, so why not just save time?
					//	-- counting the number of outgoing edges for each node.
					edgeCount += T.countNeighbours();
				}

				System.out.println(" -> Number of object nodes: " + objectCount);
				System.out.println(" -> Number of motion nodes: " + motionCount);
				System.out.println(" -> Number of motion nodes: "  + FOON.size());
				System.out.println(" -> Total number of nodes: " + nodes.size());
				System.out.println(" -> Total number of edges in network: " + edgeCount);

				
			}
	
			else if (response.equals("4")){
				//	-- Produce functional object-motion files??
				getObjectMotions(); 
				motionFrequency = new int[motionList.size()]; // -- we should have around 5X motions; use the motion index!
				//populateFrequencyList();
				motionFrequency();
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
				chooser.setFileHidingEnabled(true);
				
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
				// -- First, user gives the program object NUMBER and STATE to find:
				System.out.print("\t-> Type the Object NUMBER to find: > ");
				int objectN = keyboard.nextInt();
				System.out.print("\t-> Type the Object STATE to find: > ");
				int objectS = keyboard.nextInt();
				Object searchObject = new Object(objectN, objectS);
				System.out.println("Please select file with all OBJECTS IN ENVIRONMENT.\n");
				keyboard.nextLine();
				
				// -- Secondly, user will provide a list of all items 
				//		currently in the environment (for now...)
				chooser = new JFileChooser();
				chooser.setCurrentDirectory(new java.io.File("."));
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setDialogTitle("FOON_analysis - Choose file with items in Environment:");
				chooser.setAcceptAllFileFilterUsed(true);
				
				if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					System.out.println("Directory selected is : " + chooser.getSelectedFile());
				} else {
					System.err.println("Error in getting path of directory!");
					keyboard.close();
					return;
				}
				
				// -- all ingredients/utensils available in scene will be stored in a 
				//		set data structure, taking into account only unique objects.
				HashSet<Object> kitchenItems = new HashSet<Object>();
				
				// -- reading in all available items from file:
				Scanner file = new Scanner(new File(chooser.getSelectedFile().getAbsolutePath()));				
				while(file.hasNext()) {
					String line = file.nextLine();
					if (line.startsWith("O")){
						String[] objectParts = line.split("O", 2); // get the Object identifier by splitting first instance of O
						objectParts = objectParts[1].split("\t");

						// read the next line containing the Object state information
						line = file.nextLine();
						String[] stateParts = line.split("S", 2); // get the Object's state identifier by splitting first instance of S
						stateParts = stateParts[1].split("\t");

						// create new Object node
						Object newObject = new Object(Integer.parseInt(objectParts[0]), Integer.parseInt(stateParts[0]), objectParts[1], stateParts[1]);

						// checking if this object is a container:
						if (stateParts.length > 2){
							String [] ingredients = { stateParts[2] };
							ingredients = ingredients[0].split("\\{");
							ingredients = ingredients[1].split("\\}");
							ingredients = ingredients[0].split(",");
							// setting all the ingredients
							for (String I : ingredients){
								newObject.setIngredient(I);
							}
						}
						kitchenItems.add(newObject);
					}
				}
				file.close();
		
				// -- discerning which hierarchy level we should perform the search at.
				System.out.print("\tAt what hierarchy level should the search be done? [1/2/3] > ");
				response = keyboard.nextLine();
				System.out.println("HEHE"+response);
				if (response.equals("1")){
					getTaskTreeLevel1(searchObject, kitchenItems);
				} else if (response.equals("2")){
					getTaskTreeLevel2(searchObject, kitchenItems);
				} else if (response.equals("3")){
					getTaskTreeLevel3(searchObject, kitchenItems);
				}
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
				printAllOneModeNodesIngredients();
				System.out.println("\n~\n");
				outputGraphDegree(filePath);
			}
	
			else if (response.equals("10")){
				for (FunctionalUnit T : FOON_abstract){
					T.printFunctionalUnitNoIngredients();
				}
			}
			
			else if (response.equals("11")){		
				for (FunctionalUnit T : FOON_Ingredients){
					T.printFunctionalUnit();
				}
			}
			
			else if (response.equals("12")){
				expandNetwork();
			}
			
			else if (response.equals("13")){
				parseFunctionalUnits();
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
		System.out.println("\t12.	Expand FOON network by similarity measures?");
		System.out.println("(Press any other key and ENTER to exit)");
		System.out.print("\nPlease enter your response here: > ");

		String response = keyboard.nextLine();
		return response;
	}
	
	private static void averageMotionTimes() throws Exception {
		double[] averages = new double[motionList.size()], frequency = new double[motionList.size()];
		for (FunctionalUnit FU : FOON_Ingredients) {
			frequency[FU.getMotion().getType()]++;
			averages[FU.getMotion().getType()] += FU.getDuration();
		}
	
		String fileName = filePath.substring(0, filePath.length() - 4) + "_average_times.txt";
		File outputFile = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));
		for (int x = 0; x < averages.length; x++) {
			output.write("M_" + x + " :\t " + averages[x]/(frequency[x] * 1000) + " secs\n");
		}
		output.close();
	}

	private static void checkObjectsExist() throws Exception{
		// Method which simply goes through all objects in FOON and marks those present from the object list
		for (Thing T : oneModeObject){
			T.printThing();
			objectsSeen[T.getType()]++;
		}
		
		String fileName = filePath.substring(0, filePath.length() - 4) + "_objects_exist.txt";
		File outputFile = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));
		
		for (int x = 0; x < objectsSeen.length; x++) {
			if (objectsSeen[x] > 0)
				output.write(objectList.get(x)+"\n");
		}
		
		System.out.println("File saved at " + fileName);	
		output.close();
	}
	
	private static void populateLists() throws Exception {
		// Opens a dialog that will be used for opening the network file:
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle("FOON_analysis - Choose the OBJECT index file:");
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setFileHidingEnabled(true);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			System.out.println("Object Index found at location : " + chooser.getSelectedFile());
		} else {
			System.err.println("Error in getting path of file!");
			return;
		}
						
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
		chooser.setFileHidingEnabled(true);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			System.out.println("Motion Index found at location : " + chooser.getSelectedFile());
		} else {
			System.err.println("Error in getting path of file!");
			file.close();
			return;
		}
						
		file = new Scanner(new File(chooser.getSelectedFile().getAbsolutePath()));
		
		while(file.hasNext()) {
			String line = file.nextLine();
			String[] parts = line.split("\t");
			motionList.add(parts[1]);
		}
		
		chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle("FOON_analysis - Choose the STATE index file:");
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setFileHidingEnabled(true);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			System.out.println("State Index found at location : " + chooser.getSelectedFile());
		} else {
			System.err.println("Error in getting path of file!");
			file.close();
			return;
		}
						
		file = new Scanner(new File(chooser.getSelectedFile().getAbsolutePath()));
		
		while(file.hasNext()) {
			String line = file.nextLine();
			String[] parts = line.split("\t");
			stateList.add(parts[1]);
		}

		file.close();
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
		for (FunctionalUnit FU : FOON_Ingredients) {
			entireUnit = entireUnit + (FU.getInputsForFile() + FU.getMotionForFile() + FU.getOutputsForFile() + "//\n");

			output.write(entireUnit);
			entireUnit = "";
		}
		System.out.println("File saved at " + FP);	
		output.close();
	}

	private static void motionFrequency() throws Exception {
		// -- iterating through the list of motions in level 1 hierarchy
		int motions = 0;
		for (FunctionalUnit FU : FOON_Ingredients) {
			motionFrequency[FU.getMotion().getType()]++;
			motions++;
		}
		int maxIndex = 0;
		for (int x = 1; x < motionFrequency.length; x++){
			if (motionFrequency[maxIndex] < motionFrequency[x]) {
				maxIndex = x;
			}
		}

		String fileName = filePath.substring(0, filePath.length() - 4) + "_motion_frequency_containers.txt";
		File outputFile = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));
		for (int x = 0; x < motionFrequency.length; x++) {
			output.write("M_" + x + " :\t " + motionFrequency[x] + " instances\n");
		}
		output.write("Total instances: " + motions);
		output.close();

		// -- iterating through the list of motions in level 2 hierarchy
		motions = 0;
		motionFrequency = new int[motionList.size()];
		for (FunctionalUnit FU : FOON_abstract) {
			motionFrequency[FU.getMotion().getType()]++;
			motions++;
		}
		maxIndex = 0;
		for (int x = 1; x < motionFrequency.length; x++){
			if (motionFrequency[maxIndex] < motionFrequency[x]) {
				maxIndex = x;
			}
		}

		fileName = filePath.substring(0, filePath.length() - 4) + "_motion_frequency_abstract.txt";
		outputFile = new File(fileName);
		output = new BufferedWriter(new FileWriter(outputFile));
		for (int x = 0; x < motionFrequency.length; x++) {
			output.write("M_" + x + " :\t " + motionFrequency[x] + " instances\n");
		}
		output.write("Total instances: " + motions);
		output.close();
		
		// -- iterating through the list of motions in level 1 hierarchy
		motions = 0;
		motionFrequency = new int[motionList.size()];
		for (FunctionalUnit FU : FOON) {
			motionFrequency[FU.getMotion().getType()]++;
			motions++;
		}
		maxIndex = 0;
		for (int x = 1; x < motionFrequency.length; x++){
			if (motionFrequency[maxIndex] < motionFrequency[x]) {
				maxIndex = x;
			}
		}

		fileName = filePath.substring(0, filePath.length() - 4) + "_motion_frequency_no_states.txt";
		outputFile = new File(fileName);
		output = new BufferedWriter(new FileWriter(outputFile));
		for (int x = 0; x < motionFrequency.length; x++) {
			output.write("M_" + x + " :\t " + motionFrequency[x] + " instances\n");
		}
		output.write("Total instances: " + motions);
		output.close();

	}

	
	@SuppressWarnings("unused")
	private static void populateFrequencyList() throws Exception {
		int total = 0, motions = 0, objects = 0, edges = 0;
		for (Thing T : nodes_Ingredients) {
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
		for (Thing n : nodes_Ingredients) {
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

	private static void printAllOneModeNodesNoState(){
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

	private static void printAllOneModeNodes(){
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
	
	private static void printAllOneModeNodesIngredients(){
		System.out.println(oneModeObjectIngredients.size() + " nodes found in graph!");
		int count = 0;
		for (Thing n : oneModeObjectIngredients) {
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
		for (Thing T : nodes_Ingredients) {
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
		Thing temp = nodes_Ingredients.get(vertex);
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

			Thing temp = nodes_Ingredients.get(vertex);
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
			if (FOON_abstract.isEmpty()){
				return false;
			}
			for(FunctionalUnit F : FOON_abstract){
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
			if (FOON_Ingredients.isEmpty()){
				return false;
			}
			for(FunctionalUnit F : FOON_Ingredients){
				if (F.equalsWithIngredients(U)){
					System.out.println("Functional unit (with containers) already exists in FOON!");
					//U.printFunctionalUnit();
					return true;
				}
			}
			return false;			
		}
		else if (A == 3){
			if (FOON.isEmpty()){
				return false;
			}
			for(FunctionalUnit F : FOON){
				if (F.equalsNoState(U)){
					System.out.println("Functional unit (with containers) already exists in FOON!");
					//U.printFunctionalUnit();
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
		int count = totalNodes; // 'totalNodes' gives an indication of the number of object AND motion nodes are in FOON.
		String[] stateParts, objectParts, motionParts; // objects used to contain the split strings

		Object newObject; Motion newMotion; // Temporary objects to hold a new object/motion
		int objectIndex = -1; // variables to hold position of object/motion within list of Things				
		boolean isInput = true;

		FunctionalUnit newFU = new FunctionalUnit(), newFU_A = new FunctionalUnit(), newFU_NS = new FunctionalUnit(); // object which will hold the functional unit being read.

		while (readFile.hasNext()) {
			String line = readFile.nextLine();
			int objectExisting = -1;
			if (line.startsWith("//")) {
				// we are adding a new FU, so start from scratch..
				if (!FUExists(newFU_A,1)){
					// only add the Functional Unit if it is not in the list
					FOON_abstract.add(newFU_A); 
					nodes_abstract.add(newFU_A.getMotion());	// no matter what, we add new motion nodes; we will have multiple instances everywhere.
					// if this functional unit does not exist, then the reverse should not exist either!
					makeReverseFU(newFU_A);
					
					// We can then proceed to adding object nodes to a compressed one-mode projection of FOON.
					addOneModeProjection(newFU_A);
					addOneModeAbstract(newFU_A);
					addOneModeIngredients(newFU_A);	
				}							
				if (!FUExists(newFU,-1)){
					// only add the Functional Unit if it is not in the list
					nodes_Ingredients.add(newFU.getMotion());	// no matter what, we add new motion nodes; we will have multiple instances everywhere.
					FOON_Ingredients.add(newFU);
					count++; // increment number of nodes by one since we are adding a new Motion node
					// if this functional unit does not exist, then the reverse should not exist either!
					makeReverseFU_Container(newFU);
					
					// We can then proceed to adding object nodes to a compressed one-mode projection of FOON.
					addOneModeProjection(newFU);
					addOneModeAbstract(newFU);
					addOneModeIngredients(newFU);	
				}
				if (!FUExists(newFU_NS,3)){
					nodes.add(newFU_NS.getMotion());	// no matter what, we add new motion nodes; we will have multiple instances everywhere.
					FOON.add(newFU_NS);
				}
				newFU = new FunctionalUnit(); newFU_A = new FunctionalUnit(); newFU_NS = new FunctionalUnit(); // create an entirely new FU object to proceed with reading new units.
				isInput = true; // this is the end of a FU so we will now be adding input nodes; set flag to TRUE.
			} else if (line.startsWith("O")) {
				// this is an Object node, so we probably should read the next line one time
				objectParts = line.split("O", 2); // get the Object identifier by splitting first instance of O
				objectParts = objectParts[1].split("\t");

				// read the next line containing the Object state information
				line = readFile.nextLine();
				stateParts = line.split("S", 2); // get the Object's state identifier by splitting first instance of S
				stateParts = stateParts[1].split("\t");

		// -- FUNCTIONAL UNITS WITH INGREDIENTS...
				// create new Object node
				newObject = new Object(Integer.parseInt(objectParts[0]), Integer.parseInt(stateParts[0]), objectParts[1], stateParts[1]);

				// checking if this object is a container:
				if (stateParts.length > 2){
					String [] ingredients = { stateParts[2] };
					ingredients = ingredients[0].split("\\{");
					ingredients = ingredients[1].split("\\}");
					// we then need to make sure that there are ingredients to be read.
					if (ingredients.length > 0){
						ingredients = ingredients[0].split(",");
						for (String I : ingredients){
							newObject.setIngredient(I);
						}
					}
				}
				// checking if Object node exists in the list of objects
				for (Thing n : nodes_Ingredients) {
					if (n instanceof Object && ((Object)n).equalsWithIngredients(newObject)){
						objectExisting = nodes_Ingredients.indexOf(n);
					}
				}

				// Check if object already exists within the list so as to avoid duplicates
				if (objectExisting != -1){
					objectIndex = objectExisting;
				}
				else {
					// just add new object to the list of all nodes
					nodes_Ingredients.add(newObject);
					objectIndex = count++;
				}

				if (isInput){
					// this Object will be an input node to the FU
					newFU.addObjectNode(nodes_Ingredients.get(objectIndex), FunctionalUnit.nodeType.Input, Integer.parseInt(objectParts[2]));
				} else {
					// add the Objects as output nodes to the Functional Unit
					newFU.addObjectNode(nodes_Ingredients.get(objectIndex), FunctionalUnit.nodeType.Output, Integer.parseInt(objectParts[2]));
					newFU.getMotion().addConnection(newObject); // make the connection from Motion to Object
				}
				
		// -- FUNCTIONAL UNITS WITHOUT INGREDIENTS...			
				objectExisting = -1;
				newObject = new Object(Integer.parseInt(objectParts[0]), Integer.parseInt(stateParts[0]), objectParts[1], stateParts[1]);

				// checking if Object node exists in the list of objects
				for (Thing n : nodes_abstract) {
					if (n instanceof Object && ((Object)n).equals(newObject)){
						objectExisting = nodes_abstract.indexOf(n);
					}
				}

				// Check if object already exists within the list so as to avoid duplicates
				if (objectExisting != -1){
					objectIndex = objectExisting;
				}
				else {
					// just add new object to the list of all nodes
					objectIndex = nodes_abstract.size();
					nodes_abstract.add(newObject);
				}

				if (isInput){
					// this Object will be an input node to the FU
					newFU_A.addObjectNode(nodes_abstract.get(objectIndex), FunctionalUnit.nodeType.Input, Integer.parseInt(objectParts[2]));
				} else {
					// add the Objects as output nodes to the Functional Unit
					newFU_A.addObjectNode(nodes_abstract.get(objectIndex), FunctionalUnit.nodeType.Output, Integer.parseInt(objectParts[2]));
					newFU_A.getMotion().addConnection(newObject); // make the connection from Motion to Object
				}

		// -- FUNCTIONAL UNITS WITHOUT STATES/INGREDIENTS...
				
				objectExisting = -1;
				// create new Object node
				Thing noState = new Thing(Integer.parseInt(objectParts[0]), objectParts[1]);

				// checking if Object node exists in the list of objects
				for (Thing n : nodes) {
					if (!(n instanceof Motion) && n.equals(noState)){
						objectExisting = nodes.indexOf(n);
					}
				}

				// Check if object already exists within the list so as to avoid duplicates
				if (objectExisting != -1){
					objectIndex = objectExisting;
				}
				else {
					// just add new object to the list of all nodes
					objectIndex = nodes.size();
					nodes.add(noState);
				}

				if (isInput){
					// this Object will be an input node to the FU
					newFU_NS.addObjectNode(nodes.get(objectIndex), FunctionalUnit.nodeType.Input, Integer.parseInt(objectParts[2]));
				} else {
					// add the Objects as output nodes to the Functional Unit
					newFU_NS.addObjectNode(nodes.get(objectIndex), FunctionalUnit.nodeType.Output, Integer.parseInt(objectParts[2]));
					newFU_NS.getMotion().addConnection(noState); // make the connection from Motion to Object
				}
				
			} else {
				// We are adding a Motion node, so very easy to deal with
				isInput = false;
				System.out.println(line);
				motionParts = line.split("M", 2); // get the Motion number
				motionParts = motionParts[1].split("\t"); // get the Motion label

		// -- FUNCTIONAL UNITS WITH INGREDIENTS...
				// create new Motion based on what was read.
				newMotion = new Motion(Integer.parseInt(motionParts[0]), motionParts[1]);				
				for (Thing T : newFU.getInputList()){
					T.addConnection(newMotion); // make the connection from Object(s) to Motion
				}
				newFU.setMotion(newMotion);
				newFU.setTimes(motionParts[2], motionParts[3]);	

		// -- FUNCTIONAL UNITS WITHOUT INGREDIENTS...
				newMotion = new Motion(Integer.parseInt(motionParts[0]), motionParts[1]);	
				for (Thing T : newFU_A.getInputList()){
					T.addConnection(newMotion); // make the connection from Object(s) to Motion
				}
				newFU_A.setMotion(newMotion);
				newFU_A.setTimes(motionParts[2], motionParts[3]);

		// -- FUNCTIONAL UNITS WITHOUT STATES/INGREDIENTS...
				newMotion = new Motion(Integer.parseInt(motionParts[0]), motionParts[1]);	
				for (Thing T : newFU_NS.getInputList()){
					T.addConnection(newMotion); // make the connection from Object(s) to Motion
				}
				newFU_NS.setMotion(newMotion);
				newFU_NS.setTimes(motionParts[2], motionParts[3]);
			}
		}
		
		// Don't forget to close the file once we are done!
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
			
	private static void getTaskTreeLevel1(Object O, HashSet<Object> L) throws Exception{
		// Searching for task sequence to make a certain object O
		//	-- we have a set of items found in the kitchen environment given in L
		//	-- we should first find out if the node exists in FOON
		
		// searching for the object in the FOON
		int index = -1; 
		for (Thing T : nodes) {
			if (!(T instanceof Motion) && O.equals(T)){
				index = nodes.indexOf(T);
			}
		}

		// checking to see if the item has been found in FOON
		if (index == -1) {
			System.out.println("Item O" + O.getType() + " has not been found in network!");
			System.out.println("Finding the closest object..");
			//doLevel1Search(O);
			return;
		}
		
		// What structures do we need in record keeping?
		//	-- a FIFO list of all nodes we need to search (a queue)
		Queue<Thing> itemsToSearch = new LinkedList<Thing>();
		//	-- a list that keeps track of what we have seen
		HashSet<Thing> itemsSeen = new HashSet<Thing>();
		//	-- a list of all items we have/know how to make in the present case.
		HashSet<Thing> kitchen = new HashSet<Thing>();
		
		// keeping track of what our goal originally was!
		Thing goalNode = nodes.get(index);
		
		// -- Add the object we wish to search for to the two lists created above.
		itemsToSearch.add( nodes.get(index));
		itemsSeen.add( nodes.get(index));

		// -- structure to keep track of all units in FOON
		Stack<FunctionalUnit> FUtoSearch = new Stack<FunctionalUnit>(); // Queue structure needed for BFS
		// We need to get all functional units needed to create the goal
		ArrayList<FunctionalUnit> tree = new ArrayList<FunctionalUnit>();        

		for (Object T : L){
			itemsSeen.add(T);
			index = -1;
			for (Thing N : nodes) {
				if (!(N instanceof Motion) && T.equals(N)){
					index = nodes.indexOf(N);
				}
			}
			if (index != -1) {
				// This means that the object exists in FOON; if not..
				kitchen.add(nodes.get(index));
			} else {
				// .. we then need to find a substitute item in our environment.
				// TODO: how do we find this substitute item?
				
			}
		}
				
		while(!itemsToSearch.isEmpty()) {
			// -- Remove the item we are trying to make from the queue of items we need to learn how to make        	
			Thing tempObject = itemsToSearch.remove(); 
			
			if (kitchen.contains(goalNode)){
				// -- If we found the item already, why continue searching? (Base case)
				//		therefore we break here!
				break;
			}
						
			if (kitchen.contains(tempObject)){
				// Just proceed to next iteration, as we already know how to make current item!
				continue;
			}
			
			System.out.println("\tSearching for O" + tempObject.getType() + "..."); 
			// We keep track of the total number of "ways" (functional units) of making a target node.
			int numProcedures = 0;
			for (FunctionalUnit FU : FOON){
				// -- searching for all functional units with our goal as output
				int found = -1;
				for (Thing N : FU.getOutputList()){
					if ((N).equals(tempObject)){
						found++;
					}
				}
				// -- only add a functional unit if it produces a specific output.
				if (found != -1){
					FUtoSearch.push(FU);
					numProcedures++;	
				}
			}
			
			// -- currently we know how to get the entire tree needed for ALL possibilities..!
			while (!FUtoSearch.isEmpty()){
				FunctionalUnit tempFU = FUtoSearch.pop();
				int count = 0; 
				for (Thing T : tempFU.getInputList()){
					boolean found = false;
					for (Thing U : kitchen){
						if (U.equals((Object)T)){
							found = true;
						}
					}
					if (found == false){
						// if an item is not in the "objects found" list, then we add it to the list of items
						//	we then need to explore and find out how to make.
						itemsToSearch.add(T);
					} 
					else { 
					// keeping track of whether we have all items for a functional unit or not!
						count++;
					}
				}
				numProcedures--;
				if (count == tempFU.getNumberOfInputs()){
					// We will have all items needed to make something;
					//	add that item to the "kitchen", as we consider it already made.
					kitchen.add(tempObject);
					itemsSeen.add(tempObject);
					for (int x = 0; x < numProcedures; x++){
						// remove all functional units that can make an item - we take the first!
						FUtoSearch.pop();
					}
					boolean found = false;
					for (FunctionalUnit FU : tree){
						if (FU.equalsNoState(tempFU)){
							// ensuring that we do not repeat any units
							found = true; break;
						}
					}
					if (!found)
						tree.add(tempFU);
				}
				else {
					// -- if a solution has not been found yet, add the object back to queue.
					itemsToSearch.add(tempObject);
				}				
			}

			// -- how can we use heuristics to find the SHORTEST path to making a certain item?

		}
	
		// -- saving task tree sequence to file..		
		String fileName = filePath.substring(0, filePath.length() - 4) + "_task_tree_lvl1.txt";
		File outputFile = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));
		for (FunctionalUnit FU : tree){
			// just write all functional units that were put into the list 
			output.write((FU.getInputsForFile() + FU.getMotionForFile() + FU.getOutputsForFile() + "//\n"));
		}
		System.out.println(" -- Task tree sequence saved in " + fileName);
		output.close();
	}

	private static void getTaskTreeLevel2(Object O, HashSet<Object> L) throws Exception{
		// Searching for task sequence to make a certain object O
		//	-- we have a set of items found in the kitchen environment given in L
		//	-- we should first find out if the node exists in FOON
		
		// searching for the object in the FOON
		int index = -1; 
		for (Thing T : nodes_abstract) {
			if (T instanceof Object && O.equals((Object)T)){
				index = nodes_abstract.indexOf(T);
			}
		}

		// checking to see if the item has been found in FOON
		if (index == -1) {
			System.out.println("Item O" + O.getObjectType() + "_S" + O.getObjectState() + " has not been found in network!");
			System.out.println("Finding the closest object..");
			doSemanticSimilaritySearch(O);
			return;
		}
		
		// What structures do we need in record keeping?
		//	-- a FIFO list of all nodes we need to search (a queue)
		Queue<Object> itemsToSearch = new LinkedList<Object>();
		//	-- a list that keeps track of what we have seen
		HashSet<Object> itemsSeen = new HashSet<Object>();
		//	-- a list of all items we have/know how to make in the present case.
		HashSet<Object> kitchen = new HashSet<Object>();
		
		// keeping track of what our goal originally was!
		Object goalNode = (Object) nodes_abstract.get(index);
		
		// -- Add the object we wish to search for to the two lists created above.
		itemsToSearch.add((Object) nodes_abstract.get(index));
		itemsSeen.add((Object) nodes_abstract.get(index));

		// -- structure to keep track of all units in FOON
		Stack<FunctionalUnit> FUtoSearch = new Stack<FunctionalUnit>(); // Queue structure needed for BFS
		// We need to get all functional units needed to create the goal
		ArrayList<FunctionalUnit> tree = new ArrayList<FunctionalUnit>();        

		for (Object T : L){
			itemsSeen.add(T);
			index = -1;
			for (Thing N : nodes_abstract) {
				if (N instanceof Object && T.equals((Object)N)){
					index = nodes_abstract.indexOf(N);
				}
			}
			if (index != -1) {
				// This means that the object exists in FOON; if not..
				kitchen.add((Object)nodes_abstract.get(index));
			} else {
				// .. we then need to find a substitute item in our environment.
				// TODO: how do we find this substitute item?
				doSemanticSimilaritySearch(T);
			}		
		}
		
		while(!itemsToSearch.isEmpty()) {
			// -- Remove the item we are trying to make from the queue of items we need to learn how to make        	
			Object tempObject = (Object) itemsToSearch.remove(); 
			
			boolean flag = false;
			for (Object S : kitchen){
				if (S.equals((Object)goalNode)){
					flag = true;
					break;
				}
			}
			
			if (flag == true){
				// -- If we found the item already, why continue searching? (Base case)
				//		therefore we break here!
				break;
			}

			flag = false;
			for (Object S : kitchen){
				if (S.equals((Object)tempObject)){
					flag = true; break;
				}
			}
			
			if (flag == true){
				// Just proceed to next iteration, as we already know how to make current item!
				continue;
			}
			
			System.out.println("\tSearching for O" + tempObject.getObjectType() + "_S" + tempObject.getObjectState() +"...");
			
			// We keep track of the total number of "ways" (functional units) of making a target node.
			int numProcedures = 0;
			for (FunctionalUnit FU : FOON_abstract){
				// -- searching for all functional units with our goal as output
				int found = -1;
				for (Thing N : FU.getOutputList()){
					if (((Object)N).equals(tempObject)){
						found++;
					}
				}
				// -- only add a functional unit if it produces a specific output.
				if (found != -1){
					FUtoSearch.push(FU);
					numProcedures++;	
				}
			}
			
			// -- currently we know how to get the entire tree needed for ALL possibilities..!
			while (!FUtoSearch.isEmpty()){
				FunctionalUnit tempFU = FUtoSearch.pop(); int count = 0; 
				for (Thing T : tempFU.getInputList()){
					flag = false;
					for (Thing U : kitchen){
						if (((Object)U).equals((Object)T)){
							flag = true; break;
						}
					}
					if (flag == false){
						// if an item is not in the "objects found" list, then we add it to the list of items
						//	we then need to explore and find out how to make.
						itemsToSearch.add((Object) T);
					} 
					else { 
						// keeping track of whether we have all items for a functional unit or not!
						count++;
					}
					//itemsSeen.add((Object) T);
				}
				numProcedures--;
				if (count == tempFU.getNumberOfInputs()){
					// We will have all items needed to make something;
					//	add that item to the "kitchen", as we consider it already made.
					kitchen.add(tempObject);
					itemsSeen.add(tempObject);
					for (int x = 0; x < numProcedures; x++){
						// remove all functional units that can make an item - we take the first!
						FUtoSearch.pop();
					}
					boolean found = false;
					for (FunctionalUnit FU : tree){
						if (FU.equalsNoState(tempFU)){
							// ensuring that we do not repeat any units
							found = true; break;
						}
					}
					if (!found)
						tree.add(tempFU);
				}
				else {
					// -- if a solution has not been found yet, add the object back to queue.
					itemsToSearch.add(tempObject);
				}
			}

			// -- how can we use heuristics to find the SHORTEST path to making a certain item?

		}
	
		// -- saving task tree sequence to file..		
		String fileName = filePath.substring(0, filePath.length() - 4) + "_task_tree_lvl2.txt";
		File outputFile = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));
		for (FunctionalUnit FU : tree){
			// just write all functional units that were put into the list 
			output.write((FU.getInputsForFile() + FU.getMotionForFile() + FU.getOutputsForFile() + "//\n"));
		}
		System.out.println(" -- Task tree sequence saved in " + fileName);
		output.close();
	}

	private static void getTaskTreeLevel3(Object O, HashSet<Object> L) throws Exception{
		// Searching for task sequence to make a certain object O
		//	-- we have a set of items found in the kitchen environment given in L
		//	-- we should first find out if the node exists in FOON
		
		O.printObject();
		
		// searching for the object in the FOON
		int index = -1; 
		for (Thing T : nodes_Ingredients) {
			if (T instanceof Object && O.equals((Object)T)){
				index = nodes_Ingredients.indexOf(T);
			}
		}

		// checking to see if the item has been found in FOON
		if (index == -1) {
			System.out.println("Item O" + O.getObjectType() + "_S" + O.getObjectState() + " has not been found in network!");
			System.out.println("Finding the closest object..");
			doSemanticSimilaritySearch(O);
			return;
		}
		
		// What structures do we need in record keeping?
		//	-- a FIFO list of all nodes we need to search (a queue)
		Queue<Object> itemsToSearch = new LinkedList<Object>();
		//	-- a list that keeps track of what we have seen
		HashSet<Object> itemsSeen = new HashSet<Object>();
		//	-- a list of all items we have/know how to make in the present case.
		HashSet<Object> kitchen = new HashSet<Object>();
		
		// keeping track of what our goal originally was!
		Object goalNode = (Object) nodes_Ingredients.get(index);
		
		// -- Add the object we wish to search for to the two lists created above.
		itemsToSearch.add((Object) nodes_Ingredients.get(index));
		itemsSeen.add((Object) nodes_Ingredients.get(index));

		// -- structure to keep track of all units in FOON
		Stack<FunctionalUnit> FUtoSearch = new Stack<FunctionalUnit>(); // Queue structure needed for BFS
		// We need to get all functional units needed to create the goal
		ArrayList<FunctionalUnit> tree = new ArrayList<FunctionalUnit>();        

		for (Object T : L){
			itemsSeen.add((Container)T);
			index = -1;
			for (Thing N : nodes_Ingredients) {
				if (N instanceof Object && T.equalsWithIngredients((Object)N)){
					index = nodes_Ingredients.indexOf(N);
				}
			}
			if (index != -1) {
				// This means that the object exists in FOON; if not..
				kitchen.add((Object)nodes_Ingredients.get(index));
			} else {
				// .. we then need to find a substitute item in our environment.
				// TODO: how do we find this substitute item?
				
			}
		}
				
		while(!itemsToSearch.isEmpty()) {
			// -- Remove the item we are trying to make from the queue of items we need to learn how to make        	
			Object tempObject = (Object) itemsToSearch.remove(); 
			
			boolean flag = false;
			for (Thing T : kitchen){
				if (((Object)T).equalsWithIngredients(goalNode)){
					flag = true; break;
				}
			}

			if (flag){
				// -- If we found the item already, why continue searching? (Base case)
				//		therefore we break here!
				break;
			}
			
			flag = false;
			for (Thing T : kitchen){
				if (((Object)T).equalsWithIngredients(tempObject)){
					flag = true; break;
				}
			}
			
			if (flag){
				// Just proceed to next iteration, as we already know how to make current item!
				continue;
			}
			
			System.out.println("\tSearching for O" + tempObject.getObjectType() + "_S" + tempObject.getObjectState() +"...");
			
			// We keep track of the total number of "ways" (functional units) of making a target node.
			int numProcedures = 0;
			for (FunctionalUnit FU : FOON_Ingredients){
				// -- searching for all functional units with our goal as output
				int found = -1;
				for (Thing N : FU.getOutputList()){
					if (((Object)N).equalsWithIngredients(tempObject)){
						found++;
					}
				}
				// -- only add a functional unit if it produces a specific output.
				if (found != -1){
					FUtoSearch.push(FU);
					numProcedures++;	
				}
			}
			
			// -- currently we know how to get the entire tree needed for ALL possibilities..!
			while (!FUtoSearch.isEmpty()){
				FunctionalUnit tempFU = FUtoSearch.pop();
				int count = 0; 
				for (Thing T : tempFU.getInputList()){
					boolean found = false;
					for (Thing U : kitchen){
						if (!((Object)U).equalsWithIngredients((Object)T)){
							found = true; break;	
						}
					}
					if (found == false){
						// if an item is not in the "objects found" list, then we add it to the list of items
						//	we then need to explore and find out how to make.
						itemsToSearch.add((Container) T);
					} 
					else { 
					// keeping track of whether we have all items for a functional unit or not!
						count++;
					}
				}
				numProcedures--;
				if (count == tempFU.getNumberOfInputs()){
					// We will have all items needed to make something;
					//	add that item to the "kitchen", as we consider it already made.
					kitchen.add((Container)tempObject);
					itemsSeen.add((Container)tempObject);
					for (int x = 0; x < numProcedures; x++){
						// remove all functional units that can make an item - we take the first!
						FUtoSearch.pop();
					}
					boolean found = false;
					for (FunctionalUnit FU : tree){
						if (FU.equalsWithIngredients(tempFU)){
							// ensuring that we do not repeat any units
							found = true; break;
						}
					}
					if (!found) 
						tree.add(tempFU);
				}
				else {
					// -- if a solution has not been found yet, add the object back to queue.
					itemsToSearch.add((Container)tempObject);
				}				
			}
			// -- how can we use heuristics to find the SHORTEST path to making a certain item?
		}
	
		// -- saving task tree sequence to file..		
		String fileName = filePath.substring(0, filePath.length() - 4) + "_task_tree_lvl3.txt";
		File outputFile = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));
		for (FunctionalUnit FU : tree){
			// just write all functional units that were put into the list 
			output.write((FU.getInputsForFile() + FU.getMotionForFile() + FU.getOutputsForFile() + "//\n"));
		}
		System.out.println(" -- Task tree sequence saved in " + fileName);
		output.close();
	}

	private static void doSemanticSimilaritySearch(Object O) throws Exception {
		// LEVEL ONE: 
		
		// We need to store the similarities found from the Python script:
		//	-- using a HashTable-like object for convenient storage/retrieval of information
		//	-- using another List-like object for sorting the similarity values
		HashMap<Double, String> similarity = new HashMap<Double, String>();
		ArrayList<Double> sorted = new ArrayList<Double>();
		
		// User needs to complete the semantic similarity calculation first!
		System.out.print("Use the calculate_similarity.py script to find the nearest objects, then press ENTER.");
		keyboard.nextLine();

		// Opens a dialog that will be used for opening the similarity output file:
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setDialogTitle("FOON_analysis - Choose the OUTPUT file from calculate_similarity.py script:");
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setFileHidingEnabled(true);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			System.out.println("File selected in location : " + chooser.getSelectedFile());
		} else {
			System.err.println("Error in getting path of file!");
			return;
		}
		
		// Opening the file..
		Scanner file = new Scanner(new File(chooser.getSelectedFile().getAbsolutePath()));
		// ..and parsing through the file.
		while(file.hasNext()) {
			String line = file.nextLine();
			String[] parts = line.split("\t");
			// -- adding everything from the script output file to a HashTable
			similarity.put(Double.valueOf(parts[1]),parts[0]);
			sorted.add(Double.valueOf(parts[1]));
		}
		file.close();
		
		System.out.println(" -- Sorting similarity values..");
		
		// We then proceed to sorting all the values from high to low:
		Collections.sort(sorted, Collections.reverseOrder());
		
		for (double value : sorted){
			// -- if a similarity value is below some threshold, then we can assume that 
			//		the object is not similar enough to be used as a reference.
			if (value < 0.75){
				break;
			}
			
			// - finding the object associated with a given value 
			String tempObjectName = similarity.get(value);
			Object tempObject = new Object(objectList.indexOf(tempObjectName), O.getObjectState());
			
			// -- checking to see whether the similar object exists in FOON or not 
			int index = -1;
			for (Thing T : nodes_Ingredients) {
				if (T instanceof Object && tempObject.equals((Object)T)){
					index = nodes_Ingredients.indexOf(T);
				}
			}
			if (index == -1){
				// -- just move to the next closest object..
				System.out.println(" -- Object " + tempObjectName + " does not exist in FOON..");
				continue;
			}
			else {
				// -- in this case, we can look for the functional units that produce that
				//		specific object and "copy" it.
				
				// -- to prevent an Exception from adding new FUs while iterating through them all,
				//		we add the new units to a temporary list
				List<FunctionalUnit> FUtoAdd = new ArrayList<FunctionalUnit>();
				for (FunctionalUnit FU : FOON_Ingredients){
					if (FU.getOutputList().contains(nodes_Ingredients.get(index))){
						// - first, create a blank functional unit instance...
						FunctionalUnit tempFU = new FunctionalUnit();
						// - then, copy the output list; remove the object of question, but add the item we don't know how to make.
						int count = 0, copied = -1;
						for (Thing T : FU.getOutputList()){
							if (!((Object)T).equals(nodes_Ingredients.get(index))){
								// -- add object elements to the new functional unit since copies are just references!
								tempFU.addObjectNode(T, FunctionalUnit.nodeType.Output, FU.getOutputDescriptor().get(count));
							} else {
								// -- take note of the motion descriptor of the object that is found to be similar
								copied = FU.getOutputDescriptor().get(count);
							}
							count++;
						}
						// -- this object now exists in FOON, so we add it to list of all nodes
						tempFU.addObjectNode(O, FunctionalUnit.nodeType.Output, copied);
						O.setObjectLabel(objectList.get(O.getObjectType()));
						O.setStateLabel(((Object)nodes_Ingredients.get(index)).getStateLabel());
						nodes_Ingredients.add(O);

						// -- copying the same motion node, but we need to create a new instance of the motion
						Motion tempMotion = new Motion(FU.getMotion().getType(), FU.getMotion().getLabel());
						nodes_Ingredients.add(tempMotion);
						tempFU.setMotion(tempMotion);

						// -- we can copy the times (for now), assuming it will take the same amount of time to finish..
						tempFU.setTimes(FU.getStartTime(), FU.getEndTime());
			
						//  -- finally, we copy the elements from the input object list.
						int tempState = -1; count = 0;
						String initial = "";
						for (Thing T : FU.getInputList()){
							// -- this should only happen for one instance, unless we are dealing with multiple instances 
							//		of a single object in one action.
							if (!(T.equals(nodes_Ingredients.get(index)))){
								tempFU.addObjectNode(T, FunctionalUnit.nodeType.Input, FU.getInputDescriptor().get(count));
							}
							else {
								// .. but we need to note the state of the object.
								tempState = ((Object)T).getObjectState();
								copied = FU.getInputDescriptor().get(count);
								initial = ((Object)T).getStateLabel();
							}
							count++;
						}
						// -- create a new object and add it to the list of all nodes
						Object initialState = new Object(O.getObjectType(), tempState);
						initialState.setObjectLabel(O.getObjectLabel());
						initialState.setStateLabel(initial);
						tempFU.addObjectNode(initialState, FunctionalUnit.nodeType.Input, copied);
						
						// -- this object now exists in FOON, so we add it to list of all nodes
						nodes_Ingredients.add(initialState);
						
						// -- finally, we add this functional unit to the list of all units.
						FUtoAdd.add(tempFU);
					}
				}
				
				// -- add all new functional units to universal FOON
				for (FunctionalUnit FU : FUtoAdd){
					FOON_Ingredients.add(FU);
				}

				// -- display to ensure all the functional units are added and correct
				//for (FunctionalUnit FU : FOON_containers){
				//	FU.printFunctionalUnit();
				//	System.out.println("//");
				//}
				
				// -- now that we have added new functional units, we should then add them to the file!
				outputMergedGraph(filePath);

				System.out.println("Functional units with similar object usage have been added!");
				System.out.println("Repeat the search if possible.");
				return;
			}
		}
	}
	
	private static void expandNetwork() throws Exception{
		// -- This function should go through all possible objects (types and states) and expand the network
		//		based on all of those possibilities.
		
		// -- PAPER: Function should demonstrate the power in expanding the network.

		// Create file explorer dialog to select the directory
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("FOON_analysis - Choose directory with all similarity files:");
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setFileHidingEnabled(true);
		
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			System.out.println("Directory selected is : " + chooser.getSelectedFile());
		} else {
			System.err.println("Error in getting path of directory!");
			keyboard.close();
			return;
		}

		// -- Get the list of files within the similarity folder.
		File directory = chooser.getSelectedFile();
		File[] listOfFiles = directory.listFiles();

		for (File F : listOfFiles){

			HashMap<String, Double> similarity = new HashMap<String, Double>();
			ArrayList<String> sorted = new ArrayList<String>();
			System.out.println(F.getName());
			
			if (!F.getName().startsWith("FOON_similarity")){
				continue;
			}
			
			String[] ingredients = { F.getName() };
			ingredients = ingredients[0].split("\\_");
			ingredients = ingredients[3].split("\\.");

			System.out.println(" -- Finding all objects similar to " + ingredients[0] + "..");
			
			Scanner file = new Scanner(F);
			while(file.hasNext()) {
				String line = file.nextLine();
				String[] parts = line.split("\t");
				// -- adding everything from the script output file to a HashTable
				// similarity.put(Double.valueOf(parts[1]),parts[0]);
				// sorted.add(Double.valueOf(parts[1]));
				similarity.put(parts[0],Double.valueOf(parts[1]));
				sorted.add(parts[0]);
			}
			file.close();
						
			for (String value : sorted){
				// -- if a similarity value is below some threshold, then we can assume that 
				//		the object is not similar enough to be used as a reference.
				if (similarity.get(value) < 0.8){
					continue;
				}
			
				String tempObjectName = value;

				// -- if the object is being compared to itself, just skip it.
				if (tempObjectName == ingredients[0]){
					continue;
				}
				
				for (int x = 0; x < stateList.size(); x++){
			
					// -- creating a temporary object that should be similar to the object in question.
					Object O = new Object(objectList.indexOf(ingredients[0]), x);
					
					// - finding the object associated with a given value 
					Object tempObject = new Object(objectList.indexOf(tempObjectName), x);
										
					// -- checking to see whether the similar object exists in FOON or not 
					int index = -1;
					for (Thing T : nodes_Ingredients) {
						if (T instanceof Object && ((Object)tempObject).equals((Object)T)){
							index = nodes_Ingredients.indexOf(T);
						}
					}
					if (index == -1){
						// -- just move to the next closest object..
						//System.out.println(" -- Object " + tempObjectName + " (" + stateList.get(x)  + ") does not exist in FOON..");
						continue;
					}
					else {
						// -- in this case, we can look for the functional units that produce that
						//		specific object and "copy" it.
						
						// -- to prevent an Exception from adding new FUs while iterating through them all,
						//		we add the new units to a temporary list
						
						List<FunctionalUnit> FUtoAdd = new ArrayList<FunctionalUnit>();
						for (FunctionalUnit FU : FOON_Ingredients){
							if (FU.getOutputList().contains(nodes_Ingredients.get(index))){
								// - first, create a blank functional unit instance...
								FunctionalUnit tempFU = new FunctionalUnit();
								// - then, copy the output list; remove the object of question, but add the item we don't know how to make.
								int count = 0, copied = -1;
								for (Thing T : FU.getOutputList()){
									if (!((Object)T).equals(nodes_Ingredients.get(index))){
										// -- add object elements to the new functional unit since copies are just references!
										tempFU.addObjectNode(T, FunctionalUnit.nodeType.Output, FU.getOutputDescriptor().get(count));
									} else {
										// -- take note of the motion descriptor of the object that is found to be similar
										copied = FU.getOutputDescriptor().get(count);
									}
									count++;
								}
								// -- this object now exists in FOON, so we add it to list of all nodes
								tempFU.addObjectNode(O, FunctionalUnit.nodeType.Output, copied);
								O.setObjectLabel(objectList.get(O.getObjectType()));
								O.setStateLabel(((Object)nodes_Ingredients.get(index)).getStateLabel());
								
								// TODO: what do we do about the ingredients?
//								for (String S : ((Object)nodes.get(index)).getIngredientsList()){
//									if (S.equals(tempObjectName)){
//										O.setIngredient(O.getObject());
//									}
//									else O.setIngredient(S);
//								}
//								nodes.add(O);
	
								// -- copying the same motion node, but we need to create a new instance of the motion
								Motion tempMotion = new Motion(FU.getMotion().getType(), FU.getMotion().getLabel());
								nodes_Ingredients.add(tempMotion);
								tempFU.setMotion(tempMotion);
	
								// -- we can copy the times (for now), assuming it will take the same amount of time to finish..
								tempFU.setTimes(FU.getStartTime(), FU.getEndTime());
					
								//  -- finally, we copy the elements from the input object list.
								int tempState = -1; count = 0;
								String initial = "";
								for (Thing T : FU.getInputList()){
									// -- this should only happen for one instance, unless we are dealing with multiple instances 
									//		of a single object in one action.
									if (!(T.equals(nodes_Ingredients.get(index)))){
										tempFU.addObjectNode(T, FunctionalUnit.nodeType.Input, FU.getInputDescriptor().get(count));
									}
									else {
										// .. but we need to note the state of the object.
										tempState = ((Object)T).getObjectState();
										copied = FU.getInputDescriptor().get(count);
										initial = ((Object)T).getStateLabel();
									}
									count++;
								}
								
								if (tempState == -1){
									// issue with some items not actually being in the input; 
									//	simply label them as "no state".
									tempState = stateList.indexOf("no state");
									initial = "no state";
								}
								
								// -- create a new object and add it to the list of all nodes
								Object initialState = new Object(O.getObjectType(), tempState);
								initialState.setObjectLabel(O.getObjectLabel());
								initialState.setStateLabel(initial);
								tempFU.addObjectNode(initialState, FunctionalUnit.nodeType.Input, copied);
								
								// -- this object now exists in FOON, so we add it to list of all nodes
								nodes_Ingredients.add(initialState);
								
								// -- finally, we add this functional unit to the list of all units.
								FUtoAdd.add(tempFU);
							}
						}
						
						// -- add all new functional units to universal FOON
						for (FunctionalUnit FU : FUtoAdd){
							// only add the functional unit if there isn't a copy of it already in FOON.
							if (!FUExists(FU,-1)){
								FOON_Ingredients.add(FU);								
							}
						}
					}
				}
			}
		}
		// -- write all the new functional units to the file..
		outputMergedGraph(filePath);
		// 		... and then read them again into the one-mode projected graphs.
		constructFUGraph(new Scanner(filePath));
	}
	
	private static void parseFunctionalUnits() throws Exception{

		// Create file explorer dialog to select the directory
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setDialogTitle("FOON_analysis - Choose file with bounding-box descriptions!");
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setFileHidingEnabled(true);
		
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			System.out.println("File selected is : " + chooser.getSelectedFile());
		} else {
			System.err.println("Error in getting file!");
			keyboard.close();
			return;
		}
		
		ArrayList<Integer> boundingBox = new ArrayList<Integer>();
		
		Scanner file = new Scanner(new File(chooser.getSelectedFile().getAbsolutePath()));
		
		while(file.hasNext()) {
			String line = file.nextLine();
			String[] parts = line.split("\t");
			if (parts.length > 2){
				if (parts[2].equals("B")){
					boundingBox.add(Integer.parseInt(parts[0]));
				}
			}
		}
		
		for (int X : boundingBox){
			System.out.println(objectList.get(X));
		}

		System.out.println(boundingBox.size() + " elements in the bounding box list!");
		
		file.close();		
		
		// Create file explorer dialog to select the directory
		chooser = new JFileChooser();
		chooser.setCurrentDirectory(new java.io.File("."));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("FOON_analysis - Choose folder with the text files to crop!");
		chooser.setAcceptAllFileFilterUsed(true);
		chooser.setFileHidingEnabled(true);
				
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			System.out.println("File selected is : " + chooser.getSelectedFile());
		} else {
			System.err.println("Error in getting file!");
			keyboard.close();
			return;
		}
				
		File directory = chooser.getSelectedFile();
		File[] listOfFiles = directory.listFiles();

		for (File F : listOfFiles){
			
			if (F.getName().contains("BB")){
				continue;
			}
			
			System.out.println(F.getName());
			
			FOON_Ingredients = new ArrayList<FunctionalUnit>();
			FOON = new ArrayList<FunctionalUnit>();
			FOON_abstract = new ArrayList<FunctionalUnit>();
			nodes_Ingredients = new ArrayList<Thing>();
			totalNodes = 0;
			constructFUGraph(new Scanner(F));
			
			Iterator<FunctionalUnit> I = FOON_Ingredients.iterator();
			
			while (I.hasNext()){
				int count = 0;
				FunctionalUnit temp = (FunctionalUnit) I.next();
				for (Thing O : temp.getOutputList()){
					if (boundingBox.contains(O.getType())){
						count++;
					}
				}
				if (count != temp.getOutputList().size()){
					I.remove();
					continue;
				}

				count = 0;
				for (Thing O : temp.getInputList()){
					if (boundingBox.contains(O.getType())){
						count++;
					}
				}
				if (count != temp.getInputList().size()){
					I.remove();
				}
			}
			
			String fileName = "BB_FILES/" + F.getName().substring(0, F.getName().length() - 4) + "_BB.txt";
			outputMergedGraph(fileName);
		}

		
	}
	
	@SuppressWarnings("unused")
	private static void doLevel2Search(Object O){
		// LEVEL TWO: we have instance of object in secondary state, but we have no idea how to go to primary state
	}
	
	
	@SuppressWarnings("unused")
	private static void doLevel3Search(Object O){
		// LEVEL THREE: Objects never ever seen in FOON.. no instance of object
	}
	
	

	
// -- Previous versions of functions which never worked well..
	
	@SuppressWarnings("unused")
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
	
	@SuppressWarnings("unused")
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
	
	@SuppressWarnings("unused")
	private static void searchForRecipe2(Object O) {
		Queue<Thing> itemsToSearch = new LinkedList<Thing>(); // Queue structure needed for BFS
		int index = -1; 
		// searching for the object in the FOON
		for (Thing T : nodes_Ingredients) {
			if (T instanceof Object && O.equals((Object)T)){
				index = nodes_Ingredients.indexOf(T);
			}
		}

		// checking to see if the item has been found in FOON
		if (index == -1) {
			System.out.println("Item O" + O.getObjectType() + "_S" + O.getObjectState() + " has not been found in network!");
			return;
		}

		int start = index; //start at the goal node's index
		boolean[] isVisited = new boolean[FOON_abstract.size()]; // this is a structure to keep track of all visited nodes;
		//  the values of the boolean array will be FALSE by default.
		boolean[] itemsV = new boolean[nodes_Ingredients.size()];
		Scanner keyboard = new Scanner(System.in); // checking for user's input
		String response = "";

		itemsToSearch.add(nodes_Ingredients.get(start));
		Stack<FunctionalUnit> FUtoSearch = new Stack<FunctionalUnit>(); // Queue structure needed for BFS
		ArrayList<FunctionalUnit> tree = new ArrayList<FunctionalUnit>();        

		while(!itemsToSearch.isEmpty()) {
			Thing tempObject = itemsToSearch.remove(); // remove the item we are trying to make from the list        	
			// Just a test for printing an appropriate message for each item!

			boolean flag = false;
			for (FunctionalUnit FU : FOON_abstract){
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
				if (isVisited[FOON_abstract.indexOf(tempFU)] == false){
					for (Thing I : tempFU.getInputList()){
						System.out.println("\t" + (++count) + ".	O" + ((Object)I).getObjectType() + "_S" + ((Object)I).getObjectState());
						System.out.print("\t - Do you have object O" + ((Object)I).getObjectType() + "_S" + ((Object)I).getObjectState() + "[" 
								+ ((Object)I).getLabel() + " (" + ((Object)I).getStateLabel() + ")]? (Y/N) > ");
						response = keyboard.nextLine();
						if (response.equals("N")){
							System.out.print("\t - now searching for how to make item ");
							((Object)I).printObject();
							if (itemsV[nodes_Ingredients.indexOf(I)] == false){
								itemsToSearch.add(I); // if we do not know how to make the item, then we need to backtrack further!
								itemsV[nodes_Ingredients.indexOf(I)] = true;
							}
						}
						else {
							itemsNeeded++;
						}
						System.out.println();
					}
					isVisited[FOON_abstract.indexOf(tempFU)] = !(isVisited[FOON_abstract.indexOf(tempFU)]);
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