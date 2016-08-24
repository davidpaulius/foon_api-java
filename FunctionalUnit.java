package foon;

import java.text.SimpleDateFormat;
import java.util.*;

public class FunctionalUnit {

	public enum nodeType {Input, Output}; // just to make one single function to add stuff to FU
	
	private List<Thing> inputNodes, // input Objects
						outputNodes; // output Objects
	private Thing motionNode; // Motion observed in the FU					
	// This will be a pair of lists maintained to say whether object was seen moving/not moving	
	private List<Integer> inDescriptor, outDescriptor;  
	// Variables which record the starting and ending time of a certain motion
	private String startTime, endTime;
	
	public FunctionalUnit() {
		inputNodes = new ArrayList<Thing>();
		outputNodes = new ArrayList<Thing>();
		motionNode = new Thing();
		inDescriptor = new ArrayList<Integer>();
		outDescriptor = new ArrayList<Integer>();
		startTime = "";
		endTime = "";
	}
	
	public void addObjectNode(Thing O, nodeType N, int D) {
		if (N == nodeType.Input){
			inputNodes.add(O);
			inDescriptor.add(D);
		} else {
			outputNodes.add(O);
			outDescriptor.add(D);
		}
	}
	
	public boolean equalsWithIngredients(FunctionalUnit U){
		int results = 0; // this number must add up to three (3) which suggests that all parts match!
		int count = 0; // counter used to determine number of hits (true matches)
		// checking if the input nodes are all the same!
		for(Thing T : this.inputNodes){
			for (Thing TU : U.inputNodes){
				if (((Object)T).equalsWithIngredients((Object)TU)){
					count++;
				}
			}
		}
		// if the counter matches up to the number of inputs,
		//	then that means we have the same set of inputs.
		if (count == this.getNumberOfInputs()){
			results++;
		} 
		
		// checking if the Motion is the same
		if (((Motion)this.motionNode).equals((Motion)U.motionNode)){
			results++;
		} 
		
		// checking if the output nodes are all the same!
		count = 0;
		for(Thing T : this.outputNodes){
			for (Thing TU : U.outputNodes){
				if (((Object)T).equalsWithIngredients((Object)TU)){
					count++;
				}
			}
		}
		if (count == this.getNumberOfOutputs()){
			results++;
		} 
		
		// simply return true or false depending on the value of results
		return (results == 3);
	}
	public boolean equals(FunctionalUnit U){
		int results = 0; // this number must add up to three (3) which suggests that all parts match!
		int count = 0; // counter used to determine number of hits (true matches)
		// checking if the input nodes are all the same!
		for(Thing T : this.inputNodes){
			for (Thing TU : U.inputNodes){
				if (((Object)T).equals((Object)TU)){
					count++;
				}
			}
		}
		// if the counter matches up to the number of inputs,
		//	then that means we have the same set of inputs.
		if (count == this.getNumberOfInputs()){
			results++;
		} 
		
		// checking if the Motion is the same
		if (((Motion)this.motionNode).equals((Motion)U.motionNode)){
			results++;
		} 
		
		// checking if the output nodes are all the same!
		count = 0;
		for(Thing T : this.outputNodes){
			for (Thing TU : U.outputNodes){
				if (((Object)T).equals((Object)TU)){
					count++;
				}
			}
		}
		if (count == this.getNumberOfOutputs()){
			results++;
		} 
		
		// simply return true or false depending on the value of results
		return (results == 3);
	}
	
	public Thing getMotion(){
		return motionNode;
	}
	
	public List<Thing> getInputList(){
		return inputNodes;
	}
	
	public List<Integer> getInputDescriptor(){
		return inDescriptor;
	}
	
	public List<Integer> getOutputDescriptor(){
		return outDescriptor;
	}
	public List<Thing> getOutputList(){
		return outputNodes;
	}
	
	public void setMotion(Thing M){
		motionNode = M;
	}
	
	public void setInputList(List<Thing> L){
		inputNodes = L;
	}
	
	public void setOutputList(List<Thing> L){
		outputNodes = L;
	}
		
	public int getNumberOfInputs(){
		return inputNodes.size();
	}
	
	public int getNumberOfOutputs(){
		return outputNodes.size();
	}
	
	// Timing functions
	public void setTimes(String S, String E){
		startTime = S; endTime = E;
	}
	
	public String getStartTime(){
		return startTime;
	}

	public String getEndTime(){
		return endTime;
	}
	
	public long getDuration() throws Exception{
		// valid times will be in the format <minute>:<seconds>
		if (startTime.contains(":") && endTime.contains(":")){
			SimpleDateFormat format = new SimpleDateFormat("mm:ss");
			Date start = format.parse(startTime), end = format.parse(endTime);
			return end.getTime() - start.getTime();
		}
		else {
			// If not, just return 0 for anything that is invalid.
			return 0;
		}
	}
	
	public void printFunctionalUnit(){
		// print all input Object nodes
		int count = 0;
		for (Thing T: inputNodes){
			System.out.print("O" + ((Object)T).getObjectType() + "\t" + ((Object)T).getObjectLabel());
			System.out.println("\t" + inDescriptor.get(count++));
			System.out.println("S" + ((Object)T).getObjectState() + "\t" + ((Object)T).getStateLabel() + "\t" + ((Object)T).getIngredients());
		}
		// print the Motion node
		((Motion)motionNode).printMotion();
		System.out.println("\t" + startTime + "\t" +endTime);
		// print all output Object nodes
		count = 0;
		for (Thing T: outputNodes){
			System.out.print("O" + ((Object)T).getObjectType() + "\t" + ((Object)T).getObjectLabel());
			System.out.println("\t" + outDescriptor.get(count++));
			System.out.println("S" + ((Object)T).getObjectState() + "\t" + ((Object)T).getStateLabel() + ((Object)T).getIngredients());
		}
	}
	
	public void printFunctionalUnitNoIngredients(){
		// print all input Object nodes
		int count = 0;
		for (Thing T: inputNodes){
			((Object)T).printObjectNoIngredients();
			System.out.println("\t" + inDescriptor.get(count++));
		}
		// print the Motion node
		((Motion)motionNode).printMotion();
		System.out.println(startTime+"\t"+endTime);
		// print all output Object nodes
		count = 0;
		for (Thing T: outputNodes){
			((Object)T).printObjectNoIngredients();
			System.out.println("\t" + outDescriptor.get(count++));
		}
	}

	
	public String getInputsForFile(){
		String cat = ""; 
		int count = 0;
		for (Thing T : inputNodes){
			// just keep adding all Strings which describe all Objects and then return
			cat += "O" + ((Object)T).getObjectType() + "\t" + ((Object)T).getObjectLabel();
			cat += "\t" + inDescriptor.get(count++) + "\n";
			cat += "S" + ((Object)T).getObjectState() + "\t" + ((Object)T).getStateLabel() + "\t" + ((Object)T).getIngredients() + "\n";
		}
		return cat;
	}
	
	public String getMotionForFile(){
		return ((Motion)motionNode).getMotion() + "\t" + startTime + "\t" + endTime + "\n";
	}
	
	public String getOutputsForFile(){
		String cat = "";
		int count = 0;
		for (Thing T : outputNodes){
			// just keep adding all Strings which describe all Objects and then return
			cat += "O" + ((Object)T).getObjectType() + "\t" + ((Object)T).getObjectLabel();
			cat += "\t" + outDescriptor.get(count++) + "\n";
			cat += "S" + ((Object)T).getObjectState() + "\t" + ((Object)T).getStateLabel() + "\t" + ((Object)T).getIngredients() + "\n";
		}
		return cat;
	}
}
