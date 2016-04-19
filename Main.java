package foon;

import java.io.*;
import java.util.*;

import org.jblas.*;

public class Main {

	// Paths that need to be changed depending on computer being used!
	//	- File which contains the ENTIRE, COMBINED network
	static String filePath = "C:/Users/David Paulius/Documents/USF/Research/Graphs/Parsed 2.29.2016/Text Files/F7-FOON.txt";
	//	- File which contains the sequence which serves as INPUT for Roger's simulation,
	static String sequenceOutput = "C:/Users/David Paulius/Documents/Eclipse/Eclipse Projects/FoodNetwork/src/mainGraph.txt";
	//	- File which contains another network we wish to merge with the existing network (temporary)
	static String graphToBeMerged = "C:/Users/David Paulius/Documents/USF/Research/Graphs/Parsed 2.29.2016/Text Files/x-barbeque ribsNewNewNew2New" + ".txt";

	static ArrayList<Thing> nodes, nodesReversed; // dynamic list of all objects/motions observed in file; keeps track of matrix rows and columns 
	static ArrayList<Thing> oneModeObject; // one-mode projection of only object nodes
	static ArrayList<Thing> oneModeObjectNil; // one-mode projection of only object nodes
	static ArrayList<Thing> oneModeMotion; // one-mode projection of only motion nodes
	static ArrayList<Thing> functionalMotions; 
	static int[] distances;
			
	static int totalNodes = 0; // total number of nodes that are in the network
	static boolean[] visited;
		
	static int[] motionFrequency; // array to count the number of instances of each motion in a graph
	
	static ArrayList<String> file; 
	static ArrayList<FunctionalUnit> FOON; // list of all Functional Units in the network
	
	// for backtracking/branch-and-bound algorithm
	static ArrayList<FunctionalUnit> reverseFOON; // list of all Functional Units in the network but edges are in REVERSE
	
	// adjacency matrix of all objects
	static double[][] oneModeObjectMatrix;
	static double[][] oneModeObjectNilMatrix;
	
	// Testing stack for backtracking purposes
	static Stack<Thing> backtrack, tempStack;

	public static void main(String[] args) throws Exception {
		// Initialize the ArrayList and Stack objects
		nodes = new ArrayList<Thing>();  
		FOON = new ArrayList<FunctionalUnit>();  
		
		// initializing all ArrayList objects used for representing network (forward + backward)
		nodesReversed = new ArrayList<Thing>();  
		reverseFOON = new ArrayList<FunctionalUnit>();  
		oneModeObject = new ArrayList<Thing>();  
		oneModeObjectNil = new ArrayList<Thing>(); // trying a thing with recording only objects with NO states  
		functionalMotions = new ArrayList<Thing>();  
		
		// Populate the adjacency matrices and record number of UNIQUE nodes
		totalNodes = constructFUGraph(new Scanner(new File(filePath)));
		distances = new int[oneModeObjectNil.size()];
		
		//int index = 0;
		//for (Thing T : oneModeObjectNil){
		//	distances[index] = exploreNeighbours(index);
		//	System.out.println(distances[index++]);
		//}
		
		// creating adjacency matrix for the object graph (TESTING)
		oneModeObjectMatrix = new double[oneModeObject.size()][oneModeObject.size()];
		oneModeObjectNilMatrix = new double[oneModeObjectNil.size()][oneModeObjectNil.size()];		
		populateAdjacencyMatrix(); // populate the structures created above

		// Setting matrix object used in centrality analysis
		DoubleMatrix OMOmatrix = new DoubleMatrix(oneModeObjectNilMatrix);
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
		DoubleMatrix onesVector = DoubleMatrix.ones(oneModeObjectNil.size(), 1), 
				I = DoubleMatrix.eye(oneModeObjectNil.size()); // identity matrix
		DoubleMatrix ans = Solve.pinv((I.sub((OMOmatrix.transpose().mul(alpha))))).mmul(onesVector); // as per 7.10
		System.out.println("~");
		int count = 0, maxIndex = 0;;
		for (double D : ans.toArray()) {
			if (D > ans.toArray()[maxIndex]) {
				maxIndex = count;
			}
			System.out.println("O" + oneModeObjectNil.get(count++).getType() + "\t" + String.format("%.5f ", (D)));
		}
		System.out.print("\nKATZ: Node " + (maxIndex+1) + " has the largest centrality value associated with it -> \n");
		oneModeObjectNil.get(maxIndex).printThing();
		
		System.out.println("~");		
				
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
		System.out.print("EIGEN: Node " + (maxIndex+1) + " has the largest eigenvalue associated with it -> ");
		oneModeObjectNil.get(maxIndex).printThing();
		
		Scanner keyboard = new Scanner(System.in);
		visited = new boolean[totalNodes]; // boolean array used for DFS/BFS

		//for (Thing T : nodes){
		//	if (T instanceof Object){
		//		System.out.println("O" + T.getType() + "_S" + ((Object)T).getObjectState());
		//	}
		//}
		
		getObjectMotions(); 
		motionFrequency = new int[87];
		populateFrequencyList();
		
		System.err.println("FOON Graph Merging Program (revised 21/1/2016)\n");
		
		// Test to print all nodes
		System.out.print("Print all nodes (test)? [Y/N] > ");
		String response = keyboard.nextLine();
		if (response.equals("Y")){
			printAllNodes();
		}

		System.out.println("\n~\n");
		
		System.out.print("Print all nodes in REVERSE order? [Y/N] > ");
		response = keyboard.nextLine();
		if (response.equals("Y")){
			printAllNodesReversed();
		}

		System.out.println("\n~\n");

		System.out.print("Print objects as one-mode projected graph? [Y/N] > ");
		response = keyboard.nextLine();
		if (response.equals("Y")){
			printAllOneModeNodes();
			System.out.println("\n~\n");
			printAllOneModeNodesNoState();
			System.out.println("\n~\n");
			maxIndex = 0;
			int maxDegree = oneModeObject.get(maxIndex).countNeighbours(); 
			for (int x = maxIndex + 1; x < oneModeObject.size(); x++) {
				if (oneModeObject.get(maxIndex).countNeighbours() < oneModeObject.get(x).countNeighbours()) {
					maxIndex = x; maxDegree = oneModeObject.get(x).countNeighbours();
				}
			}
			System.out.println("Node " + (maxIndex+1) + " has the largest number of degrees with value of " + maxDegree);
			oneModeObject.get(maxIndex).printThing();
			System.out.println();
			maxIndex = 0;
			maxDegree = oneModeObjectNil.get(maxIndex).countNeighbours(); 
			for (int x = maxIndex + 1; x < oneModeObjectNil.size(); x++) {
				if (oneModeObjectNil.get(maxIndex).countNeighbours() < oneModeObjectNil.get(x).countNeighbours()) {
					maxIndex = x; maxDegree = oneModeObjectNil.get(x).countNeighbours();
				}
			}			
			System.out.println("Node " + (maxIndex+1) + " has the largest number of degrees with value of " + maxDegree);
			oneModeObjectNil.get(maxIndex).printThing();
			System.out.println();
			outputGraphDegree(filePath);
		}

		System.out.println("\n~\n");
		
		// Merging new graph (given text file) by calling upon constructGraphs() method; just pass Scanner of that file
		System.out.print("Merge graphs? [Y/N] > ");
		response = keyboard.nextLine();
		if (response.equals("Y")){
			File directory = new File("C:/Users/David Paulius/Documents/USF/Research/Graphs/Parsed 2.29.2016/Text Files");
			File[] listOfFiles = directory.listFiles();
			//for (File F : listOfFiles){
			//	System.out.println(F.getName());
			//	if (!F.getName().startsWith("F7-FOON")) {
			//		totalNodes = constructFUGraph(new Scanner(F));
			//	}
			//}
			totalNodes = constructFUGraph(new Scanner(new File(graphToBeMerged)));
			printAllNodes();
			outputMergedGraph(filePath);
			outputGraphDegree(filePath);
		}

		System.out.println("\n~\n");
		
		// Giving
		System.out.print("Count all nodes in the graph? [Y/N] > ");
		response = keyboard.nextLine();
		if (response.equals("Y")){
			count = 0;
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

		System.out.println("\n~\n");
		
		System.out.print("Search for recipe? [Y/N] > ");
		response = keyboard.nextLine();
		if (response.equals("Y")){
			System.out.print("\tType the Object NUMBER to find: > ");
			//response = keyboard.nextLine();
			int objectN = keyboard.nextInt();
			System.out.print("\tType the Object STATE to find: > ");
			//response = keyboard.nextLine();
			int objectS = keyboard.nextInt();
			Object searchObject = new Object(objectN, objectS);
			System.out.println();
			searchForRecipe2(searchObject);	
		}
		keyboard.close();
		
	}
	
	public static int exploreNeighbours(int N){
		Thing temp = oneModeObjectNil.get(N);
		int count = 0;
		if (temp.countNeighbours() == 0){
			return 0;
		}
		for (Thing T : temp.getNeigbourList()){
			count = (count++) + exploreNeighbours(oneModeObjectNil.indexOf(T));
		}
		return count;
	}

	public static void outputMergedGraph(String FP) throws Exception{
		// Preparing for output
		File outputFile = new File(FP);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));

		// Save the entire network to the  file
		System.out.println("Saving network to file..");
		String entireUnit = "";		
		for (FunctionalUnit FU : FOON) {
			entireUnit = entireUnit + (FU.getInputsForFile() + FU.getMotionForFile() + FU.getOutputsForFile() + "//\n");
			
			output.write(entireUnit);
			entireUnit = "";
		}
		System.out.println("File saved at "+FP);	
		output.close();
	}
	
	public static void populateFrequencyList() throws Exception {
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
		System.out.println(" -> " + objects + " object nodes in FOON presently!" );
		System.out.println(" -> " + motions + " motion nodes in FOON presently!" );
		System.out.println("Most frequent motion found in FOON was M_" + maxIndex + ", with frequency of " + (double)motionFrequency[maxIndex]/motions * 1.0);
		
		String fileName = filePath.substring(0, filePath.length() - 4) + "_motions.txt";
		File outputFile = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));
		for (int x = 0; x < motionFrequency.length; x++) {
			output.write("M_" + x + " :\t " + motionFrequency[x] + " instances\n");
		}
		output.write("Total instances: " + motions);
		output.close();
	}
	
	public static void outputGraphDegree(String FP) throws Exception{
		// Preparing for output
		String fileName = FP.substring(0, FP.length() - 4) + "_degree.txt";
		File outputFile = new File(fileName);
		BufferedWriter output = new BufferedWriter(new FileWriter(outputFile));

		// Save the entire network to the  file
		System.out.println("Saving node degrees to file..");
		String entireUnit = "";
		for (Thing FU : oneModeObject) {
			entireUnit = (((Object)FU).getObject()).replace("\n", ", ") + " : " + FU.countNeighbours() + " degrees\n";
			output.write(entireUnit);
			entireUnit = "";
		}
		System.out.println("File saved at "+fileName);	
		output.close();
		
		// saving the connections of each object to its neighbouring objects
		fileName = FP.substring(0, FP.length() - 4) + "_edges.txt";
		outputFile = new File(fileName);
		output = new BufferedWriter(new FileWriter(outputFile));
		entireUnit = "";
		for (Thing FU : oneModeObject) {
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
	
	public static void printAllNodes() throws Exception{
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
	
	public static void printAllNodesReversed(){
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
			}
			n.printNeighbours();
		}
	}
	
	public static void printAllOneModeNodes(){
		System.out.println(oneModeObject.size() + " nodes found in graph!");
		int count = 0;
		for (Thing n : oneModeObject) {
			System.out.print("node "+ (++count) +" : ");
			// all of these nodes will be purely objects! No need to test.
			((Object)n).printObject(); 
			// Display the number of degrees for each node, which are the number of neighbours
			System.out.println("Number of degrees: " + n.countNeighbours());
			n.printNeighbours();
		}
	}
	
	public static void printAllOneModeNodesNoState(){
		System.out.println(oneModeObjectNil.size() + " nodes found in graph!");
		int count = 0;
		for (Thing n : oneModeObjectNil) {
			System.out.print("node "+ (++count) +" : ");
			// all of these nodes will be purely objects! No need to test.
			n.printThing(); 
			// Display the number of degrees for each node, which are the number of neighbours
			System.out.println("Number of degrees: " + n.countNeighbours());
			n.printNeighbours();
		}
	}
	
	public static void populateAdjacencyMatrix() {
		for (int x = 0; x < oneModeObject.size(); x++) {
			oneModeObjectMatrix[x][x] = 1;
			for (Thing T : oneModeObject.get(x).getNeigbourList()){
				int toEdge = oneModeObject.indexOf(T);
				oneModeObjectMatrix[x][toEdge] = 1;
			}
		}
		
		for (int x = 0; x < oneModeObjectNil.size(); x++) {
			oneModeObjectNilMatrix[x][x] = 1;
			for (Thing T : oneModeObjectNil.get(x).getNeigbourList()){
				int toEdge = oneModeObjectNil.indexOf(T);
				oneModeObjectNilMatrix[x][toEdge] = 1;
			}
		}
		
		for (int x = 0; x < oneModeObjectNil.size(); x++) {
			for (int y = 0; y < oneModeObjectNil.size(); y++) {
				int edge = (int) (oneModeObjectNilMatrix[x][y]);
				//System.out.print(edge + " ");
			}
			//System.out.println();
		}
	}
	
	public static void getObjectMotions() throws Exception{
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
		
		String fileName = filePath.substring(0, filePath.length() - 4) + "_objects_motions.txt";
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

	public static int constructGraph(Scanner readFile) throws Exception {
		String[] items, tempParts, objectParts, motionParts; // objects used to contain the split strings
		int count = totalNodes; // we have an idea of how many objects may be in the graph by the number of lines
		
		// temporary containers for the adjacency matrix we are creating

		// Temporary objects to hold a new object/motion
		Object newObject; Motion newMotion;
		int objectIndex = -1, motionIndex = -1; // variables to hold position of object/motion within list of Things				
		int isSameFU = 0;
			
		while (readFile.hasNext()) {
			String line = readFile.nextLine();
			file.add(line); // push the line we found into structure so printing new file will be easy!
			items = line.split("\t");			
			int objectExisting = -1;
			if (line.startsWith("//")) {
				// do nothing
			}
			else if (items[0].startsWith("O")) {
				// Object to motion edge: M_# --> O_#_#
				tempParts = items[0].split("O"); motionParts = items[1].split("M"); // tokenize string
				objectParts = tempParts[1].split("S");
				newObject = new Object(Integer.parseInt(objectParts[0]),
						Integer.parseInt(objectParts[1])); //, items[2]
				newMotion = new Motion(Integer.parseInt(motionParts[1])); // only create a new motion IF we encounter a new functional unit
 
				// Search through list of nodes to see if a node is already within the list. 
				for (Thing n : nodes) {
					if (n instanceof Object && ((Object) n).equals(newObject)){
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

				// This is to ensure we account for the first line case OR if we have multiple objects within FU
				if (isSameFU < 1 || motionIndex == -1) {
					nodes.add(newMotion);
					motionIndex = count++;
				}
				
				nodes.get(objectIndex).addConnection(nodes.get(motionIndex)); // make sure we add the connection
								
				isSameFU++;
				
			} else {
				// Motion to object edge: M_# --> O_#_#
				tempParts = items[1].split("O");  // tokenize string
				objectParts = tempParts[1].split("S");
				newObject = new Object(Integer.parseInt(objectParts[0]),
						Integer.parseInt(objectParts[1])); // , items[2]
				
				// Search through list of nodes to see if a node is already within the list. 
				for (Thing n : nodes) {
					if (n instanceof Object && ((Object) n).equals(newObject)){
						objectExisting = nodes.indexOf(n);
					}
				}				
				// Check if object already exists within the list
				if (objectExisting != -1) {
					objectIndex = objectExisting;
				}
				else {
					// just add new object to the list of all nodes
					nodes.add(newObject);
					objectIndex = count++;
				}
				
				// add connection from motion to object node using the previously added Motion node
				nodes.get(motionIndex).addConnection(nodes.get(objectIndex));

				isSameFU--;
			}
		}
		
		readFile.close();
		return count;
	}
	
	public static boolean FUExists(FunctionalUnit U){
		if (FOON.isEmpty()){
			return false;
		}
		for(FunctionalUnit F : FOON){
			if (F.equals(U)){
				System.out.println("Functional unit already exists in FOON!");
				U.printFunctionalUnit();
				return true;
			}
		}
		return false;
	}
	
	public static boolean FUExists(FunctionalUnit U, int A){
		if (reverseFOON.isEmpty()){
			return false;
		}
		for(FunctionalUnit F : reverseFOON){
			if (F.equals(U)){
				System.out.println("Functional unit already exists in FOON!");
				U.printFunctionalUnit();
				return true;
			}
		}
		return false;
	}
	
	public static int constructFUGraph(Scanner readFile) throws Exception {
		int count = totalNodes; // we have an idea of how many objects may be in the graph by the number of lines
		String[] stateParts, objectParts, motionParts; // objects used to contain the split strings
		
		// Temporary objects to hold a new object/motion
		Object newObject; Motion newMotion; FunctionalUnit newFU = new FunctionalUnit();
		int objectIndex = -1; // variables to hold position of object/motion within list of Things				
		boolean isInput = true;
		
		FunctionalUnit reverseFU = new FunctionalUnit(); // this is to hold reverse edges
		
		while (readFile.hasNext()) {
			String line = readFile.nextLine();
			int objectExisting = -1;
			if (line.startsWith("//")) {
				// we are adding a new FU, so start from scratch
				if (!FUExists(newFU)){
					FOON.add(newFU); // only add the Functional Unit if it is not in the list
					
					ArrayList<Thing> tempList = new ArrayList<Thing>();
					
					// creating one-mode projection: take the input first and then the output nodes.
					for (Thing T : newFU.getInputList()) {
						Object tempObject; int found = -1;
						for (Thing N : oneModeObject){
							if (N instanceof Object && ((Object)T).equals((Object)N)){
								found = oneModeObject.indexOf(N);
							}
						}
						if (found == -1){
							tempObject = new Object(((Object)T).getObjectType(), ((Object)T).getObjectState());
							tempObject.setLabel(T.getLabel());
							tempObject.setStateLabel(((Object)T).getStateLabel());
							oneModeObject.add(tempObject);
						} else {
							tempObject = (Object) oneModeObject.get(found);
						}
						tempList.add(tempObject);
					}
					
					for (Thing T : newFU.getOutputList()) {
						Object tempObject; int found = -1;
						for (Thing N : oneModeObject){
							if (N instanceof Object && ((Object)T).equals((Object)N)){
								found = oneModeObject.indexOf(N);
							}
						}
						if (found == -1){
							tempObject = new Object(((Object) T).getObjectType(), ((Object)T).getObjectState());
							tempObject.setLabel(T.getLabel());
							tempObject.setStateLabel(((Object)T).getStateLabel());
							oneModeObject.add(tempObject);
						} else {
							tempObject = (Object) oneModeObject.get(found);
						}
						
						for (Thing N : tempList) {
							N.addConnection(tempObject);
						}
					}
					
					// for storing OBJECTS WITH NO STATES!
					
					tempList = new ArrayList<Thing>();
					
					// creating one-mode projection: take the input first and then the output nodes.
					for (Thing T : newFU.getInputList()) {
						Thing tempObject; int found = -1;
						for (Thing N : oneModeObjectNil){
							if (N.equals(T)){
								found = oneModeObjectNil.indexOf(N);
							}
						}
						if (found == -1){
							tempObject = new Thing(((Object)T).getObjectType());
							tempObject.setLabel(T.getLabel());
							oneModeObjectNil.add(tempObject);
						} else {
							tempObject = oneModeObjectNil.get(found);
						}
						tempList.add(tempObject);
					}
					
					for (Thing T : newFU.getOutputList()) {
						Thing tempObject; int found = -1;
						for (Thing N : oneModeObjectNil){
							if (N.equals(T)){
								found = oneModeObjectNil.indexOf(N);
							}
						}
						if (found == -1){
							tempObject = new Thing(((Object)T).getObjectType());
							tempObject.setLabel(T.getLabel());
							oneModeObjectNil.add(tempObject);
						} else {
							tempObject = oneModeObjectNil.get(found);
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
					
					// if this functional unit does not exist, then the reverse should not exist either!
					Motion tempMotion = new Motion(((Motion)newFU.getMotion()).getMotionType());
					tempMotion.setLabel(((Motion)newFU.getMotion()).getLabel());
					// the typical FOON graph goes from the start to the goal;
					//	for the searching, we need to go from the goal to the starting nodes.
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
							nodesReversed.add(tempObject);
						} else {
							tempObject = (Object) nodesReversed.get(found);
						}
						tempObject.addConnection(tempMotion);
						reverseFU.addObjectNode(tempObject, FunctionalUnit.nodeType.Input);
					}
					reverseFU.setMotion(tempMotion);
					nodesReversed.add(tempMotion);
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
							nodesReversed.add(tempObject);
						} else {
							tempObject = (Object) nodesReversed.get(found);
						}
						tempMotion.addConnection(tempObject);
						reverseFU.addObjectNode(tempObject, FunctionalUnit.nodeType.Output);
					}

					reverseFOON.add(reverseFU);
				}				
				
				reverseFU = new FunctionalUnit(); newFU = new FunctionalUnit(); // create an entirely new FU
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
				
				// checking if Object node exists in the list of objects
				for (Thing n : nodes) {
					if (n instanceof Object && ((Object) n).equals(newObject)){
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
					newFU.addObjectNode(nodes.get(objectIndex), FunctionalUnit.nodeType.Input);
				} else {
					// add the Objects as output nodes to the Functional Unit
					newFU.addObjectNode(nodes.get(objectIndex), FunctionalUnit.nodeType.Output);
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

public static void searchForRecipe2(Object O) {
		
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

	
	public static void searchForRecipe(Object O) {
		
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
}
