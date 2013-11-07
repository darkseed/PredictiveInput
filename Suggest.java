import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Template for predictive text suggester
 */
public class Suggest {
	
	private static PrefixTree prefixDict;
	private static Map<String, Map<String, Integer>> exactDict;
	private static final String[] ESCAPE_CHAR = 
		new String[] {"'", "\"", "_", ".", ",", "?", ":", "!", ";", "(", ")", "[", "]", "{", "}" };
	private static final String[] ROMAN_NUM = 
		new String[] {"ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x", "xi", "xii"};
	
    /**
     * Implemented below to print out the exact matches and 
     * prefix matches of the entered numeric sequence
     */
    private static void printSuggestions(File f, String seq) {
        log("Parsing file " + f.getAbsolutePath());

        Set<String> allPrefixMatches = new HashSet<String>();
        
        initializeDictionary(f);
        
        log("Exact matches for " + seq + ": ");
        
        if(seq.length()>0){
        	Map<String, Integer> permutations = (HashMap<String, Integer>)exactDict.get(seq);
        	if(permutations!=null){
        		/** get sorted list by popularity */
        		Set<Map.Entry<String, Integer>> sortedSet = sortedSetByPopularity(permutations);
        		for(Entry<String, Integer> entry:sortedSet){
        			log(entry.getKey());
        	        Set<String> currentPrefixMatches = listAllPrefixMatches(entry.getKey());
        	        allPrefixMatches.addAll(currentPrefixMatches);
        		}
        	}else{
        		log("[None was found]");
        	}
        }
        
        log("Prefix matches for " + seq + ": ");
        if(allPrefixMatches.size()>0){
	        for(String prefix:allPrefixMatches){
	        	log(prefix);
	        }
        }else{
        	log("[None was found]");
        }
    }
    
    /** Sort the result set by count(popularity) of the word in the corpus
     * since cannot use TreeSet/TreeMap which only sort on key, need to implement
     * customized comparator
     * @param map
     * @return
     */
    private static SortedSet<Map.Entry<String,Integer>> sortedSetByPopularity(Map<String,Integer> map) {
        SortedSet<Map.Entry<String,Integer>> sortedEntries = new TreeSet<Map.Entry<String,Integer>>(
            new Comparator<Map.Entry<String,Integer>>() {
                @Override public int compare(Map.Entry<String,Integer> e1, Map.Entry<String,Integer> e2) {
                    int res = e2.getValue().compareTo(e1.getValue());
                    return res!= 0 ? res : 1; //in equal situation, return 1
                }
            }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
    
    /**
     * @param f - input file object from the command line
     */
    private static void initializeDictionary(File f){
    	try{
			FileInputStream fStream = new FileInputStream(f.getAbsolutePath());
			DataInputStream in = new DataInputStream(fStream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			if(exactDict==null)
				exactDict = new HashMap<String, Map<String, Integer>>();
			
			if(prefixDict==null)
				prefixDict = new PrefixTree('\n');
			
			String strLine;
			String[] strArray;
			while((strLine = br.readLine())!=null){
				strArray = strLine.trim().split(" ");
				for(String str: strArray)
					addStringToDictionary(str);
			}
		} catch (FileNotFoundException e) {
			log("File " + f.getAbsolutePath() + " does not exist.");
		} catch (IOException e) {
			log("Error in I/O: " + e.getStackTrace());
		}
    }
    
	/** Take the entry from the data corpus, pre-process so that there are
	 * no invalid data, the add the entry to both the exact match dictionary
	 * as well as the prefix tree for prefix matching
	 * @param str
	 */
	private static void addStringToDictionary(String str){
    	/** process each string from the line, might have multiple entries*/
    	Set<String> entries = preProcessString(str);
    	
    	if(entries!=null){
    		for(String entry:entries){
    			
    			/** add string to exact match dictionary */
    			Map<String, Integer> valSet = (Map<String, Integer>)exactDict.get(toNumeric(entry));
    			if(valSet==null){
    				valSet = new HashMap<String, Integer>();
    				valSet.put(entry, 1);
    				exactDict.put(toNumeric(entry), valSet);
    			}else{
    				if(valSet.get(entry)==null)
    					valSet.put(entry, 1);
    				else{
    					int count = valSet.get(entry);
    					valSet.put(entry, count+1);
    				}
    				exactDict.put(toNumeric(entry), valSet);
    			}
    			
    			/** add to prefix dictionary */
    			prefixDict.insertWord(entry);
    			//log("Added to dictionary:" + entry);
    		}
    	}
    }
    
    /**remove the invalid characters, remove apostrophes and
     * break up hyphenated word to process recursively
     * @param str
     * @return
     */
    private static Set<String> preProcessString(String str){
    	
    	if(str.length()==0) return null;
    	
    	/** for string with hyphens has multiple entries */
    	Set<String> result = new HashSet<String>();
    	
    	str = str.toLowerCase();
    	
    	if(str.contains("\'t")) return null; // ignore short hands of negative

    	/** remove apostrophes */
    	if(str.length()>1 &&
    		(str.substring(str.length()-2).equals("'s")||
    		str.substring(str.length()-2).equals("'d")||
    		str.substring(str.length()-2).equals("'m"))
    	) 
    		str = str.substring(0, str.length()-2);
        
    	if(str.length()>2 &&
    		(str.substring(str.length()-2).equals("'ve")||
    		str.substring(str.length()-2).equals("'ll"))
    	)
    		str = str.substring(0, str.length()-3);    	
    	
    	/** trim escape characters at end */
    	for(String chr:ESCAPE_CHAR)
    		str = str.replace(chr, "");
    	/** trim chapter names */
    	for(String chr:ROMAN_NUM)
    		str = str.replace(chr, "");
    	
    	/** if the word has hyphens, split the string and work recursively */ 
    	if(str.indexOf('-')!=-1){
    		String[] strArray = str.split("-");
    		Set<String> subSet;
    		for(String itr:strArray){
    			
    			subSet = preProcessString(itr);
    			
    			if(subSet!=null){
	    			for(String subItr:subSet){
						try{
							result.add(subItr);
						}catch(RuntimeException re){
							log("Error when adding word:" +subItr);
							log(re.getStackTrace().toString());
						}
	    			}
    			}
    		}
    		return result;
    	}
    	
        /** test whether the input is alphabetic */
    	try{
    		@SuppressWarnings("unused")
			String numStr = toNumeric(str); // verify string is a valid input to dictionary
    		result.add(str);
        	return result;
    	}catch(RuntimeException re){
    		//log("Input from dataset is invalid and cannot be converted: " + str);
    		return null;
    	}
    }
	
	private static Set<String> listAllPrefixMatches(String input){
		if(prefixDict.contains(input)==false) return null;
		
		PrefixTree node = prefixDict;
		for(int i=0;i<input.length();i++){
			node = node.getChild(input.charAt(i));
		}
		
		Set<String> result = new HashSet<String>();
		searchCompleteWords(node, input, 0, result);
		return result;
	}
	
	/** recursively search children to see if there are complete words at lower level
	 * @param node - current node being iterated on
	 * @param input - current formation of the string
	 * @param result - list of complete words at lower level
	 */
	private static void searchCompleteWords(PrefixTree node, String input, int level, Set<String> result){
		String currentSeq = input;
		if(level>0){
			currentSeq = input.concat(Character.toString(node.item));
			if(node.isWordEnd)
				result.add(currentSeq);
		}
		
		if(node.children!=null){
			for(PrefixTree child:node.children){
				if(child!=null)
					searchCompleteWords(child, currentSeq, level+1, result);
			}
		}
	}
	
    /**
     * Utility method to convert a word (e.g. "cat") to its numeric
     * representation (e.g. 228). Input must be lowercase. A runtime
     * exception is thrown in case non alphabet characters are provided.
     */
    private static String toNumeric(String word) {
        char[] numeric = new char[word.length()];
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            numeric[i] = convert(c);
        }
        return new String(numeric);
    }

    @SuppressWarnings("serial")
	private static final Map<String, Character> KEYPAD_MAP =
            new HashMap<String, Character>() {
        {   put("abc",  '2');
            put("def",  '3');
            put("ghi",  '4');
            put("jkl",  '5');
            put("mno",  '6');
            put("pqrs", '7');
            put("tuv",  '8');
            put("wxyz", '9');
        }
    };
    
    private static char convert(char c) {
        for (String s : KEYPAD_MAP.keySet()) {
            if (s.contains(Character.toString(c))) {
                return KEYPAD_MAP.get(s);
            }
        }
        throw new RuntimeException("Can't convert char: " + c);
    }

    private static void log(String s) {
        System.out.println(s);
    }
    
    private static boolean isValidNumSeq(String seq){
    	if(seq.length()==0) return true;
    	
    	for(int i=0;i<seq.length();i++){
    		int val = (int)seq.charAt(i)-48;
    		if(val<2 || val>9)
    			return false;
    	}
    	return true;
    }
    /** inner static class of PrefixTree to fulfill feature of prefix match
     * the class implements a trie data structure with search capability
     *
     */
    static class PrefixTree{
    	public static final int BUCKET_SIZE = 26;
    	public char item; // character value of the node
    	public PrefixTree[] children; // head of nodes in children, size predefined
    	public boolean isWordEnd;
    	
    	public PrefixTree(char chr){
    		this.item = chr;
    		this.children = new PrefixTree[BUCKET_SIZE];
    		this.isWordEnd = false;
    	}
    	
    	/** Get the child of the current PrefixTree node by character
    	 * @param item
    	 * @return
    	 */
    	public PrefixTree getChild(char item){
    		int idx = ((int)item-'a')%BUCKET_SIZE;
    		PrefixTree node = children[idx];
    		
    		if(node==null) 
    			return null;
    		else if(node.item==item) 
    			return node;
    		
    		return null;
    	}
    	
    	/** insert a string into the prefix tree. if the 
    	 * @param input
    	 */
    	public void insertWord(String input){
    		if(input==null||input.length()==0) return;
    		
    		PrefixTree root = getChild(input.charAt(0));
    		int idx = ((int)input.charAt(0)-'a')%BUCKET_SIZE;
    		PrefixTree node = new PrefixTree(input.charAt(0));
    		
    		if(root==null){
    			children[idx]=node;
    		}else{
    			node = root;
    		}
    		
    		if(input.length()>1) 
    			node.insertWord(input.substring(1));
    		else
    			node.isWordEnd=true;
    	}
    	
    	/** check whether a certain string exists in the path of the prefix tree
    	 * @param input
    	 * @return
    	 */
    	public boolean contains(String input){
    		if(input.length()==0) return this.isWordEnd;
    		PrefixTree root = getChild(input.charAt(0));
    		if(root==null) 
    			return false;//no children start with the char
    		return root.contains(input.substring(1));//search next level down
    	}
    }
	
	
    /** Main method
     * @param args
     */
    public static void main(String[] args) {
    	System.out.println(toNumeric("caterpillar"));
    	
        if (args.length != 2) {
            log("Usage: java Suggest filename seq");
            System.exit(1);
        }
        File f = new File(args[0]);
        if (!f.exists() || !f.isFile()) {
            log(args[0] + " is not a valid file");
            System.exit(2);
        }
        
        String seq = args[1];
        if(isValidNumSeq(seq))
        	printSuggestions(f, seq);
        else
        	log("Invalid input Sequence:" + seq);
    }
}
