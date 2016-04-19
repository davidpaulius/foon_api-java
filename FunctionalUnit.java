package foon;

import java.util.*;

public class FunctionalUnit {

	public enum nodeType {Input, Output}; // just to make one single function to add stuff to FU
	
	private List<Thing> inputNodes, // input Objects
						outputNodes; // output Objects
	private Thing motionNode; // Motion observed in the FU					
	
	public FunctionalUnit() {
		inputNodes = new ArrayList<Thing>();
		outputNodes = new ArrayList<Thing>();
		motionNode = new Thing();
	}
	
	public void addObjectNode(Thing O, nodeType N) {
		if (N == nodeType.Input){
			inputNodes.add(O); 
		} else {
			outputNodes.add(O);
		}
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
		if (count == this.getNumberOfInputs()){
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
	
	public void printFunctionalUnit(){
		// print all input Object nodes
		for (Thing T: inputNodes){
			((Object)T).printObject();
		}
		// print the Motion node
		((Motion)motionNode).printMotion();
		// print all output Object nodes
		for (Thing T: outputNodes){
			((Object)T).printObject();
		}
	}
	
	public String getInputsForFile(){
		String cat = "";
		for (Thing T : inputNodes){
			// just keep adding all Strings which describe all Objects and then return
			cat = cat + (((Object)T).getObject()) + "\n"; 
		}
		return cat;
	}
	
	public String getMotionForFile(){
		return ((Motion)motionNode).getMotion() + "\n";
	}
	
	public String getOutputsForFile(){
		String cat = "";
		for (Thing T : outputNodes){
			// just keep adding all Strings which describe all Objects and then return
			cat = cat + (((Object)T).getObject()) + "\n";
		}
		return cat;
	}
}
