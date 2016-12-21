package foon;

import java.util.*;

public class Thing {
	// An object or motion no matter what has a type which is used for reference to the string arrays made above.
    private int identifier;
    private List<Thing> list; // for adjacency list implementation
    private String label;
    
    public Thing(){
    	list = new ArrayList<Thing>();
    	label = "";
    }
    
    public Thing(int n, String l){
        identifier = n;
        label = l;
        list = new ArrayList<Thing>();
    }
    
    // constructor method for a Thing object
    public Thing(int n)
    {
       identifier = n;
       list = new ArrayList<Thing>();
       label = "";
    }
    
    public void setLabel(String S){
    	label = S;
    }
    
    public String getLabel(){
    	return label;
    }
    
    // parse through list of neighbours to print neighbours 
    public void printNeighbours(){
    	if (list.isEmpty()){
    		System.out.println("Object has no neighbours!\n");
    		return;
    	}
    	System.out.print(" - Neighbours are: \t");
    	for (Thing t: list) {
    		if (t instanceof Object){
    			((Object)t).printObject();
    		}
    		else if (t instanceof Motion){
       			((Motion)t).printMotion();
       		}
    		else {
    			t.printThing();
    		}
    		System.out.print("\t\t\t");

    	}
    	System.out.println();
    }
    
    public String getNeighbours(){
    	String line = "";
    	if (list.isEmpty()){
    		return "Object has no neighbours!\n";
    	}
    	line += " - Neighbours are: \t";
    	for (Thing t: list) {
    		if (t instanceof Object){
    			line += ((Object)t).getObject();
    		}
    		else if (t instanceof Motion){
       			line += ((Motion)t).getMotion();
       		}
    		else {
    			line += t.getType();
    		}
    		line += "\n\t\t\t";
    	}
    	return line;
    }
    
    public int countNeighbours() {
    	// returns the number of degrees of a single node (i.e. the number of neighbours)
    	return list.size();
    }
 
    public List<Thing> getNeigbourList(){
    	return list;
    }
    
    public void addConnection(Thing t){
    	list.add(t);
    }
    
    public int getType(){
        return identifier;
    }
    
    public void setType(int T){
    	identifier = T;
    }
    
    public boolean equals(Thing T){
    	return T.identifier == this.identifier;
    }
    
    public void printThing(){
    	if (this instanceof Object){
    		System.out.print(((Object)this).getObject());
    	}
    	else if (this instanceof Motion){
    		System.out.print(((Motion)this).getMotion());    		
    	}
    	else {
    		System.out.println("O" + this.getType() + "\t" + label);
    	}
    }
}

class Object extends Thing {
    // Each object has a type (from Thing property), a state, and a flag indicating if it is in motion.
    private int objState;
    private String stateLabel;
    // Objects can also be containers of other objects, so a list is maintained for each object 
    //	which can point to other objects.
    private List<String> contained;
    private int numIngredients;

    // constructor method for an Object object (lol)
    public Object(int n, int S, String M, String L){
    	super(n); 
    	objState = S;
        setObjectLabel(M);
        setStateLabel(L);
        contained = new ArrayList<String>();
        numIngredients = 0;
    }
    
    public Object(int n, int S){
    	super(n); 
    	objState = S;
        contained = new ArrayList<String>();
        numIngredients = 0;
    }
    
    public Object(){
       	// empty constructor just for the option of creating empty Object.
    }
    
    public boolean equals(Object O){
    	return (O.getObjectType() == this.getObjectType() && O.getObjectState() == this.getObjectState()); 
    }
    
    public boolean equalsWithIngredients(Object O){
    	return equals(O) && isSameIngredients(O);
    }
    
    public boolean isSameIngredients(Object O){
    	int count = 0;
    	//this.printObject();
    	//System.out.println(this.contained.size());
    	//O.printObject();
    	//System.out.println(O.contained.size());
    	for (String I : O.contained){
    		if (this.contained.contains(I)){
    			count++;
    		}
    	}
    	if (count == O.numIngredients && O.getIngredientsList().size() == this.getIngredientsList().size()){
    		return true;
    	}
    	return false;
    }

    public String getObjectLabel(){
    	return getLabel();
    }
    
    public int getObjectType(){
        return getType();
    }
    
    public String getStateLabel(){
    	return stateLabel;
    }
    public int getObjectState(){
        return objState;
    }

    public void printObject(){
    	// -- print the object's identifier as well as state number in format: 
    	//			O##		<name of object>
    	//			S##		<name of state>	{<contained objects (if any)}
		System.out.print("O" + this.getType() + "\t" + this.getLabel() + "\nS" + this.getObjectState() + "\t" + this.getStateLabel());
		System.out.print("\t" + this.getIngredients());
		System.out.println();
	}

    public void printObjectNoIngredients(){
    	// -- print the object's identifier as well as state number in format: 
    	//			O##		<name of object>
    	//			S##		<name of state>	{<contained objects (if any)}
		System.out.print("O" + this.getType() + "\t" + this.getLabel() + "\nS" + this.getObjectState() + "\t" + this.getStateLabel());
	}

    public String getObject(){
    	return ("O" + this.getType() + "\t"+this.getLabel()+ "\nS" + this.getObjectState() + "\t" + this.getStateLabel() + "\t" + this.getIngredients());
    }
    
    public void setObjectType(int T){
    	setType(T);
    }
    
    public List<String> getIngredientsList(){
    	return contained;
    }
    
    public void setIngredientsList(List<String> L){
    	contained = L;
    	numIngredients = contained.size();
    }
    
    public void setObjectState(int S){
    	objState = S;
    }

    public void setObjectLabel(String S){
    	setLabel(S);
    }
    
    public void setStateLabel(String S){
    	stateLabel = S;
    }
    
    public void setIngredient(String O){
    	if (!contained.contains(O)){
    		contained.add(O);
    		numIngredients++;
    	}
    }
    
    public String getIngredients(){
    	String result = "";
    	if (numIngredients == 0){
    		return result;
    	}
    	result += "{";
    	for (int count = 0; count < numIngredients; count++){
			result += contained.get(count);
			if (count < numIngredients - 1){
				result += ",";
			}
		}    	
    	result += "}";
    	return result;
    }
    
}

class Motion extends Thing {
	
	// constructor method for a Motion object
	public Motion(int n){
        super(n);        
    }

	public Motion(int n, String L){
        super(n);       
        this.setLabel(L);
    }
	
	public int getMotionType(){
        return getType();
    }

	public void printMotion(){
		System.out.print("M"+this.getMotionType()+"\t"+this.getLabel()+"");
	}
	
	public String getMotion(){
		return ("M"+this.getType()+"\t"+this.getLabel());
	}
	
    public boolean equals(Motion M){
    	return M.getType() == this.getType();
    }
    
    public void setMotionType(int T){
    	setType(T);
    }
    
    public Motion(){
    	// empty constructor just for the option of creating empty Motion.
    };
}

class Container extends Object{
	public boolean equals(Object O){
		return this.equals(O) && this.equalsWithIngredients(O);
	}
}