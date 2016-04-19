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
    
    static String[] objects = { 
    	"bread",  			// 0
    	"cutting_board", 	// 1
    	"bread_knife", 		// 2
    	"bowl", 			// 3
    	"garlic", 			// 4
    	"butter", 			// 5
    	"parsley", 			// 6
    	"fork", 			// 7	
    	"garlic_presser", 	// 8
    	"salt", 			// 9
    	"salt_grinder", 	// 10
    	"butter_knife",     // 11
    	"baking_pan/tray",  // 12
    	"plate",			// 13
    	"garlic_salt", 		// 14
    	"oven",  			// 15
    	"spoon",  			// 16
    	"aluminum_foil", 	// 17
    	"pepper_shaker", 	// 18
    	"eggs", 			// 19
    	"cheese", 			// 20
    	"cup", 				// 21
    	"teacup", 			// 22
    	"tea_bag", 			// 23
    	"kettle", 			// 24
    	"stove",  			// 25
    	"saucepan",  		// 26
    	"paper_filter",  	// 27
    	"milk",  			// 28
    	"sugar",  			// 29
    	"jar", 				// 30
    	"teapot", 			// 31
    	"water_faucet",  	// 32
    	"water_filter" 		// 33
    };

    static String[][] states = {
    	new String[]{"whole", "half", "buttered", "baked"},
    	new String[]{""},
    	new String[]{""},
    	new String[]{"empty", "garlic", "butter", "parsley", "butter+garlic", "butter+garlic+parsley", "butter+garlic+parsley+salt", "mixed_butter"},
    	new String[]{"complete_clove", "chopped"},
    	new String[]{"80g_room_temperature", "mixed"},
    	new String[]{"finely chopped"},
    	new String[]{"clean", "dirty"},
    	new String[]{"clean", "open", "closed", "garlic", "empty"},
    	new String[]{""},
    	new String[]{""},
    	new String[]{"clean", "unclean"},
    	new String[]{""},
    	new String[]{""},
    	new String[]{""}, 
    	new String[]{"on", "off"}, 
    	new String[]{"clean", "dirty"}, 
    	new String[]{"flat", "folded", "unfolded"},
    	new String[]{""},
    	new String[]{"whole", "unshelled", "beaten"},
    	new String[]{"whole", "shredded", "melted"},
    	new String[]{"empty", "contains"},
    	new String[]{"empty", "sugar", "milk", "sugar+milk", "tea"},
    	new String[]{""},
    	new String[]{"empty", "room_temp_water", "hot_water"},
    	new String[]{"on", "off"},
    	new String[]{"water", "water+tea_leaves", "oil"},
    	new String[]{"clean", "with_leaves"},
    	new String[]{""},
    	new String[]{""},
    	new String[]{"open+teabag", "closed+teabag"},
    	new String[]{"empty+closed", "empty+open", "open+teabag", "closed+teabag", "open+teabag+water", "closed+teabag+water", "tea"},
    	new String[]{"on", "off"},
    	new String[]{""} 
    };

    static String[] motions = { 
    	"pick+place",	//1 1
    	"cut",			//2	1
    	"close tool",	//3  rotate garlic presser ends to 0 degrees 
    	"hold",			//4	1
    	"brush off",	//5 don't need it for simulation
    	"pour",			//6 tilt object 
    	"mix",			//7 mix ingredients in bowl
    	"spread",		//8	1
    	"scrape",		//9 scrape off garlic from the garlic presser
    	"grind",		//10 
    	"scoop",		//11 move knife with butter to the bread
    	"bake",			//12
    	"cool/sit",		//13
    	"wrap",			//14
    	"unwrap",		//15
    	"switch/turn_on", //16
    	"dip*",			//17
    	"wait",			//18
    	"open_tool/object", //19
    	"boil",			//20 
    };

    static String[] description = { 
    	"<in motion>", 
    	"<not in motion>",
    	"<assumed>" 
    };


}

class Object extends Thing {
    // Each object has a type (from Thing property), a state, and a flag indicating if it is in motion.
    private int objState;
    private String stateLabel;

    // constructor method for an Object object (lol)
    public Object(int n, int S, String M, String L){
    	super(n); 
    	objState = S;
        setLabel(M);
        setStateLabel(L);
    }
    
    public Object(int n, int S){
    	super(n); 
    	objState = S;
    }
    
    public boolean equals(Object O){
    	return (O.getObjectType() == this.getObjectType() && O.getObjectState() == this.getObjectState()); 
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
		System.out.println("O" + this.getType() + "\t" + this.getLabel() + "\nS" + this.getObjectState() + "\t" + this.getStateLabel());
	}
    
    public String getObject(){
    	return ("O" + this.getType() + "\t"+this.getLabel()+ "\nS" + this.getObjectState() + "\t" + this.getStateLabel());
    }
    
    public void setObjectType(int T){
    	setType(T);
    }
    
    public void setObjectState(int S){
    	objState = S;
    }

    public void setStateLabel(String S){
    	stateLabel = S;
    }
    
    public int existsFU(){
    	// a functional unit begins with an object and ends with objects;
    	// 	therefore, we need to check if a motion exists and if the objects attached are the same.
    	for (int i = this.countNeighbours() - 1; i >= 0 ; i--){
    		for (int j = 0; j < this.countNeighbours() && i != j; j++){
    			if (((Motion)this.getNeigbourList().get(i)).equals(((Motion)this.getNeigbourList().get(j)))){
    				// there is a motion of the same type
    				if (((Motion)this.getNeigbourList().get(i)).countNeighbours() == ((Motion)this.getNeigbourList().get(j)).countNeighbours()){
    					// they have the same number of neighbours
    					int count = 0, total = ((Motion)this.getNeigbourList().get(j)).countNeighbours();
    					for (int x = 0; x < total; x++){
    						if (((Motion)this.getNeigbourList().get(i)).getNeigbourList().contains(((Motion)this.getNeigbourList().get(j)).getNeigbourList().get(x))){
    							count++;
    						}
    					}
    					if (count == total){
    						// if all objects have been found connected to that motion then we have that FU already!
    						return i;
    					}
    				}
    			}
    		}
    	}
    	return -1;
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
		System.out.println("M"+this.getMotionType()+"\t"+this.getLabel()+"");
	}
	
	public String getMotion(){
		return ("M"+this.getType()+"\t"+this.getLabel());
	}
	
    public boolean equals(Motion M){
    	return M.getType() == this.getType();
    }
    
    // Prints the motion name associated with the Motion object.
    public void printMotionWithDescription()
    {
        System.out.println("[" + motions[getType() - 1] + "]");
    }
    
    public void setMotionType(int T){
    	setType(T);
    }
    
    public Motion(){
    	
    };
}